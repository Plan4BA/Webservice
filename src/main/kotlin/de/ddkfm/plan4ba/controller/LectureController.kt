package de.ddkfm.plan4ba.controller

import de.ddkfm.plan4ba.models.*
import de.ddkfm.plan4ba.utils.*
import spark.Request
import spark.Response
import java.util.*
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces

@Path("/lectures")
@Produces("application/json")
@ShortToken
class LectureController(req : Request, resp : Response, user : User) : ControllerInterface(req = req, resp = resp, user = user) {

    @GET
    @Path("")
    fun getUserLectures(): List<Lecture>? {
        val lectures = DBService.all<Lecture>("userId" to user.id).maybe
            ?.filter { !it.deprecated }
        return lectures
    }

    @GET
    @Path("/trigger")
    @BasicAuth
    fun trigger() : OK {
        if(RequestLimiter.hasLock(user))
            throw BadRequest("locked").asException()
        val disableLocking = System.getenv("DISABLE_LOCKING")?.equals("true", true) ?: false
        if(!disableLocking)//if DISABLE_LOCKING is set, disable the RequestLimiter
            RequestLimiter.addLock(user)
        if(user.userHash.isNullOrEmpty()) {
            val auth = req.headers("Authorization")
            if (auth == null || !auth.startsWith("Basic ")) {
                //resp.header("WWW-Authenticate", "Basic realm=\"Anmeldung wird benötigt\"")
                resp.status(401)
                throw Unauthorized("Hash not stored. Basic Authentication is required").asException()
            } else {
                val encoded = String(Base64.getDecoder().decode(auth.replace("Basic", "").trim().toByteArray()))
                val username = encoded.split(":")[0]
                val password = encoded.split(":")[1]
                //login will also trigger the Caching
                val loginUser = AuthService.loginCampusDual(username, password)
                if(loginUser == null || loginUser.userHash.isNullOrEmpty())
                    throw Unauthorized("Login and Password are not correct").asException()
                val hash = user.userHash
                AuthService.triggerCaching(user, hash!!)
                return OK()
            }
        } else {
            val hash = user.userHash
            AuthService.triggerCaching(user, hash!!)
            return OK()
        }
    }
}