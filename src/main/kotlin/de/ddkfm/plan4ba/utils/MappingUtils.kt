package de.ddkfm.plan4ba.utils

import de.ddkfm.plan4ba.jacksonObjectMapper
import de.ddkfm.plan4ba.models.HttpStatus
import kong.unirest.*
import org.json.JSONObject
import java.lang.reflect.Parameter
import java.net.URLEncoder
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*


fun mapDataTypes(pair : Pair<Parameter, String>) : Any {
    val returnValue = when(pair.first.type) {
        Int::class.java-> pair.second.toIntOrNull()
        Long::class.java -> pair.second.toLongOrNull()
        Boolean::class.java -> pair.second.toBoolean()
        Double::class.java -> pair.second.toDoubleOrNull()
        else -> pair.second
    }
    return returnValue ?: getDefaultValue(pair.first.type)
}

fun getDefaultValue(type : Class<*>) : Any {
    return when(type) {
        Int::class.java, Long::class.java, Double::class.java -> -1
        Boolean::class.java -> false
        else -> ""
    }
}


fun getEnvOrDefault(key : String, default : String) : String {
    return System.getenv(key) ?: default
}

fun Long.toLocalDateTime() : LocalDateTime {
    val dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(this),
            TimeZone.getDefault().toZoneId())
    return dateTime
}

fun LocalDateTime.toMillis() : Long {
    return this.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

fun Any.toJson() : String {
    return jacksonObjectMapper().writeValueAsString(this)
}
fun <T> JSONObject.toModel(type : Class<T>) : T {
    return jacksonObjectMapper().readValue(this.toString(), type)
}

inline fun <reified T> String.toModel() : T? {
    return  jacksonObjectMapper().readValue(this, T::class.java)
}
inline fun <reified T> String.toListModel() : List<T>? {
    return  jacksonObjectMapper().readValue(this, jacksonObjectMapper().typeFactory.constructCollectionType(List::class.java, T::class.java))
}
fun HttpResponse<JsonNode>.mapModel(type : Class<*>) : Pair<Int, Any> {
    return this.status to when(this.status) {
        in 200..299 -> {
            val body = this.body
            if(body.isArray)
                body.array.map { (it as JSONObject).toModel(type) }
            else
                body.`object`.toModel(type)
        }
        else -> this.body.`object`
    }
}
fun GetRequest.toModel(type : Class<*>) : Pair<Int, Any> = this.asJson().mapModel(type)
fun HttpRequestWithBody.toModel(type : Class<*>) : Pair<Int, Any> = this.asJson().mapModel(type)
fun RequestBodyEntity.toModel(type : Class<*>) : Pair<Int, Any> = this.asJson().mapModel(type)


fun String.encode() : String = URLEncoder.encode(this, "UTF-8")

data class Maybe<T>(
    val maybe : T?,
    val error : HttpStatus?
) {
    companion object {
        fun <T> of(maybe : T) : Maybe<T> {
            return Maybe(maybe, null)
        }
        fun <T> ofError(error : HttpStatus) : Maybe<T> {
            return Maybe(null, error)
        }
    }
    fun getOrThrow() : T {
        return maybe ?: throw (error?.asException() ?: NullPointerException())
    }
}