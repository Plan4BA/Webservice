import kong.unirest.GetRequest
import kong.unirest.HttpRequestWithBody
import kong.unirest.HttpResponse
import java.util.*


fun <T> HttpResponse<T>.getOrThrow() : T {
    if(this.status == 200)
        return this.body
    else
        throw Exception(this.body.toString())
}
fun randomString(length : Int = 10) : String {
    val chars = ('A'..'Z').joinToString(separator = "")
    return (0 until length).map { chars[Random().nextInt(chars.length)] }.joinToString(separator = "")
}

fun GetRequest.bearer(token : String) : GetRequest {
    return this.header("Authorization", "Bearer $token")
}


fun HttpRequestWithBody.bearer(token : String) : HttpRequestWithBody {
    return this.header("Authorization", "Bearer $token")
}