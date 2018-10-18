package de.ddkfm.plan4ba.controller

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.core.JsonProcessingException
import io.swagger.annotations.*
import io.swagger.jaxrs.Reader
import io.swagger.models.Swagger
import io.swagger.jaxrs.config.BeanConfig
import org.reflections.Reflections


@SwaggerDefinition(host = "localhost:8080",
        info = Info(description = "Plan4BA Rest-Service",
                version = "V1.0",
                title = "API for accessing the DB",
                contact = Contact(name = "DDKFM", url = "https://github.com/Plan4BA") ),
        schemes = arrayOf(SwaggerDefinition.Scheme.HTTP, SwaggerDefinition.Scheme.HTTPS),
        produces = arrayOf("application/json")
)
class App {

}
object SwaggerParser {

    @Throws(JsonProcessingException::class)
    fun getSwaggerJson(packageName: String): String {
        val swagger = getSwagger(packageName)
        return swaggerToJson(swagger)
    }

    fun getSwagger(packageName: String): Swagger {
        val reflections = Reflections(packageName)
        val beanConfig = BeanConfig()
        beanConfig.resourcePackage = packageName
        beanConfig.scan = true
        beanConfig.scanAndRead()
        val swagger = beanConfig.swagger

        val reader = Reader(swagger)

        val apiClasses = reflections.getTypesAnnotatedWith(Api::class.java)
        return reader.read(apiClasses)
    }

    @Throws(JsonProcessingException::class)
    fun swaggerToJson(swagger: Swagger): String {
        val objectMapper = ObjectMapper()
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
        return objectMapper.writeValueAsString(swagger)
    }

}