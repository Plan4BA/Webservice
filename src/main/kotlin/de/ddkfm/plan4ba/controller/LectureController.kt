package de.ddkfm.plan4ba.controller

import com.mashape.unirest.http.Unirest
import de.ddkfm.plan4ba.models.*
import de.ddkfm.plan4ba.utils.loginCampusDual
import de.ddkfm.plan4ba.utils.toModel
import de.ddkfm.plan4ba.utils.triggerCaching
import io.swagger.annotations.*
import org.json.JSONObject
import spark.Request
import spark.Response
import java.util.*

import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces

@Api(value = "/lectures", description = "return lectures")
@Path("/lectures")
@Produces("application/json")
class LectureController(req : Request, resp : Response, user : User) : ControllerInterface(req = req, resp = resp, user = user) {

    @GET
    @ApiOperation(value = "get all lectures for the give user")
    @ApiResponses(
            ApiResponse(code = 200, message = "successfull", response = Lecture::class, responseContainer = "List")
    )
    @Path("")
    fun getUserLectures(): Any? {
        val lectures = Unirest.get("${config.dbServiceEndpoint}/lectures?userId=${user.id}")
                .asJson()
                .body.array
                .map { (it as JSONObject).toModel(Lecture::class.java) }
        return lectures
    }

    @GET
    @ApiOperation(value = "trigger the lecture caching")
    @ApiResponses(
            ApiResponse(code = 200, message = "successfull", response = OK::class),
            ApiResponse(code = 401, message = "Unauthorized")
    )
    @Path("/trigger")
    fun trigger() : Any? {
        if(user.userHash.isNullOrEmpty()) {
            val auth = req.headers("Authorization")
            if (auth == null || !auth.startsWith("Basic ")) {
                resp.header("WWW-Authenticate", "Basic realm=\"Anmeldung wird benötigt\"")
                resp.status(401)
                return "Unauthorized"
            } else {
                val encoded = String(Base64.getDecoder().decode(auth.replace("Basic", "").trim().toByteArray()))
                val username = encoded.split(":")[0]
                val password = encoded.split(":")[1]
                //login will also trigger the Caching
                loginCampusDual(username, password)
                return OK()
            }
        } else {
            val hash = user.userHash
            triggerCaching(user, hash!!)
            return OK()
        }
    }
}