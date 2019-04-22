package de.ddkfm.plan4ba

import com.fasterxml.jackson.databind.ObjectMapper
import com.mashape.unirest.http.Unirest
import de.ddkfm.plan4ba.models.*
import de.ddkfm.plan4ba.utils.*
import org.json.JSONObject
import org.reflections.Reflections
import spark.Request
import spark.Response
import spark.Spark.*
import spark.kotlin.after
import spark.kotlin.port
import java.lang.reflect.Method
import java.util.*
import javax.ws.rs.*

var config = Config()

fun main(args : Array<String>) {
    port(System.getenv("HTTP_PORT")?.toIntOrNull() ?: 8080)

    config.buildFromEnv()
    println(config)
    var reflections = Reflections("de.ddkfm.plan4ba.controller")

    var controllers = reflections.getTypesAnnotatedWith(Path::class.java)
    for(controller in controllers) {
        path(controller.getAnnotation(Path::class.java).value) {
            var methods = controller.declaredMethods.filter { it.isAnnotationPresent(Path::class.java) }
            for(method in methods) {
                if(method.isAnnotationPresent(GET::class.java))
                    get(method.getAnnotation(Path::class.java).value) {req, resp -> invokeFunction(controller, method, req, resp)}

                if(method.isAnnotationPresent(POST::class.java))
                    post(method.getAnnotation(Path::class.java).value) {req, resp -> invokeFunction(controller, method, req, resp)}

                if(method.isAnnotationPresent(PUT::class.java))
                    put(method.getAnnotation(Path::class.java).value) {req, resp -> invokeFunction(controller, method, req, resp)}

                if(method.isAnnotationPresent(DELETE::class.java))
                    delete(method.getAnnotation(Path::class.java).value) {req, resp -> invokeFunction(controller, method, req, resp)}
            }
        }
    }
    exception(Exception::class.java) { exception, request, response ->
        exception.printStackTrace()
        response.status(500)
        response.body(jacksonObjectMapper().writeValueAsString(InternalServerError()))
    }
    after {
        request.headers("Accept-Encoding")?.equals("gzip")?.run {
            response.header("Content-Encoding", "gzip")
        }
    }

}

fun invokeFunction(controller : Class<*>, method : Method, req : Request, resp : Response) : Any {
    val authorizations = method.annotations.union(controller.annotations.toList()).filter {
        it.annotationClass in listOf(BasicAuth::class, RefreshToken::class, ShortToken::class, CaldavToken::class)
    }

    var aUser : User? = null

    loop@ for(auth in authorizations) {
        when(auth.annotationClass) {
            BasicAuth::class -> {
                val auth = req.getBasicAuth() ?: continue@loop
                val username = auth.username
                val password = auth.password
                val loginUser = DBService.all<User>("matriculationNumber" to username)
                    .maybe
                    ?.first()
                    ?: continue@loop
                val authResp = Unirest.post("${config.dbServiceEndpoint}/users/${loginUser.id}/authenticate")
                    .body(JSONObject("{ \"password\" : \"${password}\"}")).asString()
                if(authResp.status == 200)
                    aUser = loginUser
            }
            ShortToken::class -> {
                val bearerToken = req.getAuthToken() ?: continue@loop
                val token = DBService.get<Token>(bearerToken).maybe ?: continue@loop
                if(token.validTo < System.currentTimeMillis())
                    continue@loop
                if(token.isCalDavToken || token.isRefreshToken)
                    continue@loop

                val user = DBService.get<User>(token.userId).maybe ?: continue@loop
                aUser = user
            }
            CaldavToken::class -> {
                val bearerToken = req.queryParams("token") ?: continue@loop
                val token = DBService.get<Token>(bearerToken).maybe ?: continue@loop
                if(token.validTo < System.currentTimeMillis())
                    continue@loop
                if(!token.isCalDavToken)
                    continue@loop
                val user = DBService.get<User>(token.userId).maybe ?: continue@loop
                aUser = user
            }
            RefreshToken::class -> {
                val bearerToken = req.getAuthToken() ?: continue@loop
                val token = DBService.get<Token>(bearerToken).maybe ?: continue@loop
                if(token.validTo < System.currentTimeMillis())
                    continue@loop
                if(!token.isRefreshToken)
                    continue@loop
                val user = DBService.get<User>(token.userId).maybe ?: continue@loop
                aUser = user
            }
        }
    }
    if(authorizations.isNotEmpty() && aUser == null)
        return halt(401, "Unauthorized")

    val instance = if(aUser == null)
        controller.getConstructor(Request::class.java, Response::class.java).newInstance(req, resp)
    else
        controller.getConstructor(Request::class.java, Response::class.java, User::class.java).newInstance(req, resp, aUser)
    var args = mutableListOf<Any>()
    val bodyParam = method.parameters
        .filter { !(it.isAnnotationPresent(QueryParam::class.java) || it.isAnnotationPresent(PathParam::class.java)) }
        .firstOrNull()
    if(bodyParam != null) {
        try {
            var bodyObject = jacksonObjectMapper().readValue(req.body(), bodyParam.type)
            args.add(bodyObject)
        } catch (e : Exception) {
            return halt(400, jacksonObjectMapper().writeValueAsString(BadRequest()))
        }
    }
    val otherParams = method.parameters
        .filter { it.isAnnotationPresent(QueryParam::class.java) || it.isAnnotationPresent(PathParam::class.java) }
        .map { parameter ->
            val value = if(parameter.isAnnotationPresent(QueryParam::class.java))
                req.queryParams(parameter.getAnnotation(QueryParam::class.java).value)
            else if(parameter.isAnnotationPresent(PathParam::class.java))
                req.params(parameter.getAnnotation(PathParam::class.java).value)
            else
                ""
            parameter to value
        }
        .filterNotNull()
        .map { mapDataTypes(it) }

    args.addAll(otherParams)

    val invokeResult = try {
        method.invoke(instance, *args.toTypedArray())
    } catch (e : Exception) {
        if(e is HttpStatusException)
            e.status
        else if(e.cause is HttpStatusException)
            (e.cause as HttpStatusException).status
        else {
            InternalServerError()
        }
    }
    if (invokeResult is HttpStatus)
        resp.status(invokeResult.code)
    val produces = controller.getAnnotation(Produces::class.java)?.value?.get(0) ?: "application/json"
    resp.type(produces)
    return if(produces == "application/json")
        jacksonObjectMapper().writeValueAsString(invokeResult)
    else
        invokeResult
}

fun Request.getAuthToken() : String? {
    var authHeader = this.headers("Authorization")?.trim()
    if(authHeader == null || !authHeader.startsWith("Bearer"))
        authHeader = this.queryParams("token")
    return authHeader?.replace("Bearer", "")?.trim()
}
fun Request.getBasicAuth() : Authentication? {
    val auth = this.headers("Authorization") ?: return null
    return if(auth.startsWith("Basic")) {
        val encoded = String(Base64.getDecoder().decode(auth.replace("Basic", "").trim().toByteArray()))
        val username = encoded.split(":")[0]
        val password = encoded.split(":")[1]
        return Authentication(username, password)
    } else null
}
fun jacksonObjectMapper()  : ObjectMapper {
    var mapper = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper()
    return mapper
}