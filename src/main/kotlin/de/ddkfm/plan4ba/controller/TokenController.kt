package de.ddkfm.plan4ba.controller

import com.mashape.unirest.http.Unirest
import de.ddkfm.plan4ba.models.Token
import de.ddkfm.plan4ba.models.User
import de.ddkfm.plan4ba.utils.DBService
import de.ddkfm.plan4ba.utils.RefreshToken
import de.ddkfm.plan4ba.utils.toJson
import de.ddkfm.plan4ba.utils.toModel
import spark.Request
import spark.Response
import spark.Spark
import java.util.*
import javax.ws.rs.GET
import javax.ws.rs.Path

@Path("/token")
@RefreshToken
class TokenController(val req : Request, val resp : Response, user : User){

    @GET
    @Path("")
    fun getShortToken() : Any? {
        resp.type("application/json")
        val auth = req.headers("Authorization")
        if(auth == null || !auth.startsWith("Bearer")) {
            resp.status(401)
            return "Unauthorized"
        } else {
            val tokenString = auth.replace("Bearer", "").trim()
            val (token, status) = DBService.get<Token>(tokenString)
            when(status) {
                null -> {
                    token as Token
                    if(token.isCalDavToken)
                        return Spark.halt(401, "caldav token is not useable for other webservice methods")

                    if(!token.isRefreshToken)
                        return Spark.halt(401, "short token cannot be used for token refresh")

                    if(System.currentTimeMillis() > token.validTo ){
                        return Spark.halt(401, "Token not valid")
                    }
                    val tokens = DBService.all<Token>(
                        "userId" to token.userId,
                        "caldavToken" to false,
                        "refreshToken" to false,
                        "valid" to true
                    ).maybe?.firstOrNull()
                    if(tokens == null) {
                        val shortToken = Token(UUID.randomUUID().toString().replace("-", ""),
                            token.userId, false, false, System.currentTimeMillis() + de.ddkfm.plan4ba.config.shortTokenInterval)
                        var (status, _) = Unirest.put("${de.ddkfm.plan4ba.config.dbServiceEndpoint}/tokens")
                            .body(shortToken.toJson())
                            .toModel(Token::class.java)
                        when(status) {
                            200, 201 -> {
                                val createdToken = DBService.get<Token>(shortToken.token).getOrThrow()
                                return createdToken
                            }
                            else -> return Spark.halt(500, "Internal Server Error")
                        }
                    }
                    return tokens
                }
                else -> return Spark.halt(401, "Unauthorized")
            }
        }
    }

}