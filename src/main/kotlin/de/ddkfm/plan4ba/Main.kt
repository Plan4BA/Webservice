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

    if(getEnvOrDefault("ENABLE_SWAGGER", "true").toBoolean()) {
        var swaggerJson = SwaggerParser.getSwaggerJson("de.ddkfm.plan4ba.controller");

        get("/swagger", { req, res ->
            swaggerJson
        });

        get("/swagger/html") { req, resp ->
            IOUtils.copy(SwaggerParser.javaClass.getResourceAsStream("/index.html"), resp.raw().outputStream)
        }
    }

}

fun invokeFunction(controller : Class<*>, method : Method, req : Request, resp : Response) : Any {
    val authHeader = req.headers("Authorization")?.trim()
    if(authHeader == null || !authHeader.startsWith("Bearer"))
        return halt(401, "Unauthorized")
    val tokenString = authHeader.replace("Bearer", "").trim()
    val (status, token) = Unirest.get("${config.dbServiceEndpoint}/tokens/$tokenString").toModel(Token::class.java)
    return when(status) {
        404 -> return halt(401, "Unauthorized")
        200 -> {
            val token = token as Token
            if(System.currentTimeMillis() > token.validTo) {
                return halt(401, "Token not valid")
            }
            val (status, user) = Unirest.get("${config.dbServiceEndpoint}/users/${token.userId}").toModel(User::class.java)

            var instance = controller.getConstructor(Request::class.java, Response::class.java, User::class.java).newInstance(req, resp, user)
            resp.type("application/json")
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
                return jacksonObjectMapper().writeValueAsString(invokeResult)
            }
        }
        else -> BadRequest()
    }
}

fun jacksonObjectMapper()  : ObjectMapper {
    var mapper = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper()
    return mapper
}