package de.ddkfm.plan4ba.controller

import kong.unirest.Unirest
import de.ddkfm.plan4ba.models.*
import de.ddkfm.plan4ba.utils.*
import spark.Request
import spark.Response
import java.util.*
import javax.ws.rs.GET
import javax.ws.rs.Path

@Path("/login")
class LoginController(val req : Request, val resp : Response) {

    @GET
    @Path("")
    fun login() : Any? {
        val auth = req.headers("Authorization")
        if(auth == null || !auth.startsWith("Basic ")) {
            //resp.header("WWW-Authenticate", "Basic realm=\"Anmeldung wird benötigt\"")
            resp.status(401)
            return "Unauthorized"
        } else {
            val encoded = String(Base64.getDecoder().decode(auth.replace("Basic", "").trim().toByteArray()))
            val username = encoded.split(":")[0]
            val password = encoded.split(":")[1]
            try {
                var user = DBService.all<User>("matriculationNumber" to username).maybe?.firstOrNull()

                if(user == null)
                    user = AuthService.loginCampusDual(username, password)
                if(user == null) {
                    //resp.header("WWW-Authenticate", "Basic realm=\"Anmeldung wird benötigt\"")
                    resp.status(401)
                    return "Unauthorized"
                }
                val authenticated = user.authenticate(password)
                if(authenticated) {
                    val storeHash = req.headers("StoreHash")?.toLowerCase()?.equals("true") ?: false
                    val storeReminders = req.headers("StoreReminders")?.toLowerCase()?.equals("true") ?: false
                    val storeExamStats = req.headers("StoreExamStats")?.toLowerCase()?.equals("true") ?:false
                    user = AuthService.modifyHash(user, storeHash, storeReminders, storeExamStats, password)
                    //authenticated
                    var token = DBService.all<Token>(
                        "userId" to user.id,
                        "caldavToken" to false,
                        "refreshToken" to true,
                        "valid" to true
                    ).maybe?.firstOrNull()
                    if(token == null) {
                        token = Token(UUID.randomUUID().toString().replace("-", ""), user.id,
                            false, true, System.currentTimeMillis() + de.ddkfm.plan4ba.config.refreshTokenInterval)
                        token = DBService.create(token).getOrThrow()
                        resp.type("application/json")
                        return token
                    } else {
                        resp.type("application/json")
                        return token
                    }
                } else {
                    //resp.header("WWW-Authenticate", "Basic realm=\"Anmeldung wird benötigt\"")
                    resp.status(401)
                    return "Unauthorized"
                }
            } catch (e : Exception) {
                e.printStackTrace()
                throw e
            }
        }
    }

}