package de.ddkfm.plan4ba

import com.fasterxml.jackson.databind.ObjectMapper
import com.mashape.unirest.http.Unirest
import de.ddkfm.plan4ba.controller.SwaggerParser
import de.ddkfm.plan4ba.models.*
import de.ddkfm.plan4ba.utils.*
import io.swagger.annotations.*
import org.json.JSONObject
import org.reflections.Reflections
import spark.Request
import spark.Response
import spark.Spark.*
import spark.kotlin.port
import spark.utils.IOUtils
import java.lang.reflect.Method
import java.util.*
import javax.ws.rs.*

var config = Config()

fun main(args : Array<String>) {
    port(8080)

    config.buildFromEnv()
    println(config)
    var reflections = Reflections("de.ddkfm.plan4ba.controller")

    var controllers = reflections.getTypesAnnotatedWith(Api::class.java)
    for(controller in controllers) {
        path(controller.getAnnotation(Path::class.java).value) {
            var methods = controller.declaredMethods.filter { it.isAnnotationPresent(ApiOperation::class.java) }
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
    get("/login", ::login)
    get("/token", ::getShortToken)
    get("/info", ::info)

    if(getEnvOrDefault("ENABLE_SWAGGER", "false").toBoolean()) {
        var swaggerJson = SwaggerParser.getSwaggerJson("de.ddkfm.plan4ba.controller");

        get("/swagger") { req, res ->
            swaggerJson
        }

        get("/swagger/html") { req, resp ->
            IOUtils.copy(SwaggerParser.javaClass.getResourceAsStream("/index.html"), resp.raw().outputStream)
        }
    }

}

fun invokeFunction(controller : Class<*>, method : Method, req : Request, resp : Response) : Any {
    val tokenString = req.getAuthToken()

    var (status, token) = Unirest.get("${config.dbServiceEndpoint}/tokens/$tokenString").toModel(Token::class.java)
    val apiOperationAnnotation = method.annotations
            .filter { it is ApiOperation }
            .map { it as ApiOperation }
            .firstOrNull()
    if(apiOperationAnnotation != null) {
        val authorizations = apiOperationAnnotation.authorizations
        if(authorizations.isNotEmpty()) {
            if(authorizations.firstOrNull {it.value == "Basic"} != null) {
                val auth = req.getAuth()
                if(auth != null) {
                    val user = (Unirest.get("${config.dbServiceEndpoint}/users?matriculationNumber=${auth.username}")
                            .toModel(User::class.java)
                            .second as List<User>)
                            .firstOrNull() ?: return halt(401, "Unauthorized")

                    val authResp = Unirest.post("${config.dbServiceEndpoint}/users/${user.id}/authenticate")
                            .body(JSONObject("{ \"password\" : \"${auth.password}\"}")).asString()
                    when(authResp.status) {
                        in 400..404 -> {
                            return halt(401, "Unauthorized")
                        }
                        200 -> {
                            status = 200
                            token = Token.getValidShortToken(user.id)
                        }
                    }
                } else if(authorizations.firstOrNull { it.value == "Token" } != null) {
                    //do nothing --> normal authToken will be used
                } else {
                    return halt(401, "Unauthorized")
                }
            }
        }
    }
    return when(status) {
        404 -> return halt(401, "Unauthorized")
        200 -> {
            val token = token as Token
            if(token.isCalDavToken && controller.simpleName != "CaldavController")
                return halt(401, "caldav token is not useable for other webservice methods")

            if(token.isRefreshToken)
                return halt(401, "refreshtoken cannot be used for API-Calls")

            if(System.currentTimeMillis() > token.validTo ){
                return halt(401, "Token not valid")
            }
            val (status, user) = Unirest.get("${config.dbServiceEndpoint}/users/${token.userId}").toModel(User::class.java)

            var instance = controller.getConstructor(Request::class.java, Response::class.java, User::class.java).newInstance(req, resp, user)
            var args = mutableListOf<Any>()
            var bodyParam = method.parameters
                    .filter { it.isAnnotationPresent(ApiParam::class.java) }
                    .filter { !it.getAnnotation(ApiParam::class.java).hidden }
                    .firstOrNull()
            var badRequest = false
            if(bodyParam != null) {
                if(req.body() == null) {
                    badRequest = true
                }
                try {
                    var bodyObject = jacksonObjectMapper().readValue(req.body(), bodyParam.type)
                    args.add(bodyObject)
                } catch (e : Exception) {
                    badRequest = true
                }
            }
            if(badRequest) {
                resp.status(400)
                return jacksonObjectMapper().writeValueAsString(BadRequest())
            } else {
                var implicitParams = method.annotations
                        .filter { it is ApiImplicitParams || it is ApiImplicitParam }
                        .flatMap {
                            if (it is ApiImplicitParams)
                                it.value.toList()
                            else
                                listOf(it)
                        }
                        .map { it as ApiImplicitParam }
                        .map { param ->
                            var value =
                                    if (param.paramType == "path") {
                                        req.params(param.name)
                                    } else {
                                        req.queryParams(param.name)
                                    } ?: ""
                            param to value
                        }
                        .filter { it.second != null }
                        .map(::mapDataTypes)

                args.addAll(implicitParams)

                var invokeResult = method.invoke(instance, *args.toTypedArray())
                if (invokeResult is HttpStatus)
                    resp.status(invokeResult.code)
                val producesAnnotation = controller.getAnnotation(Produces::class.java)
                resp.type(producesAnnotation.value[0])
                return if(producesAnnotation.value.contains("application/json"))
                    jacksonObjectMapper().writeValueAsString(invokeResult)
                else
                    invokeResult
            }
        }
        else -> BadRequest()
    }
}

fun Request.getAuthToken() : String? {
    var authHeader = this.headers("Authorization")?.trim()
    if(authHeader == null || !authHeader.startsWith("Bearer"))
        authHeader = this.queryParams("token")
    return authHeader?.replace("Bearer", "")?.trim()
}
fun Request.getAuth() : Authentication? {
    val auth = this.headers("Authorization")
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