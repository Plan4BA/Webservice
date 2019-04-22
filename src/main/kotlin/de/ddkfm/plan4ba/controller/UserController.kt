package de.ddkfm.plan4ba.controller

import com.mashape.unirest.http.Unirest
import de.ddkfm.plan4ba.models.*
import de.ddkfm.plan4ba.utils.*
import io.swagger.annotations.*
import org.json.JSONObject
import spark.Request
import spark.Response
import java.util.*
import javax.ws.rs.DELETE

import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces

@Api(value = "/user", description = "all operations about the current user")
@Path("/user")
@Produces("application/json")
class UserController(req : Request, resp : Response, user : User) : ControllerInterface(req = req, resp = resp, user = user) {

    @GET
    @ApiOperation(value = "get all informations about a user")
    @ApiResponses(
            ApiResponse(code = 200, message = "successfull", response = UserInfo::class)
    )
    @Path("")
    fun getUserInfo(): UserInfo {
        val group = DBService.get<UserGroup>(user.groupId).getOrThrow()
        val university = DBService.get<University>(group.universityId).getOrThrow()
        return UserInfo(user.matriculationNumber, group.uid, university.name, !user.userHash.isNullOrEmpty(),
                user.lastLecturePolling, user.lastLectureCall)
    }

    @GET
    @ApiOperation(value = "get caldav - token")
    @ApiResponses(
            ApiResponse(code = 200, message = "successfull", response = Token::class)
    )
    @Path("/caldavToken")
    fun getCaldavToken(): Token {
        val tokens = DBService.all<Token>("userId" to user.id, "caldavToken" to true).maybe
        val caldavToken = tokens?.firstOrNull()
        return if(caldavToken == null) {
            val token = Token(UUID.randomUUID().toString().replace("-", ""),
                    user.id, true, false, (System.currentTimeMillis() + 365 * 24 * 3600 * 1000L));
            val createdToken = DBService.create(token)
            when(createdToken.error) {
                null-> createdToken.maybe!!
                else -> throw BadRequest().asException()
            }
        } else {
            caldavToken
        }
    }

    @DELETE
    @ApiOperation(value = "delete all userdata", authorizations = [Authorization(value = "Basic")])
    @ApiResponses(
            ApiResponse(code = 200, message = "successfull", response = OK::class),
            ApiResponse(code = 401, message = "Unauthorized")
    )
    @Path("/delete")
    fun delete() : OK {
        val auth = req.headers("Authorization")
        return if (auth == null || !auth.startsWith("Basic ")) {
            throw Unauthorized().asException()
        } else {
            val encoded = String(Base64.getDecoder().decode(auth.replace("Basic", "").trim().toByteArray()))
            val username = encoded.split(":")[0]
            val password = encoded.split(":")[1]
            val deleteResp = Unirest.delete("${config.dbServiceEndpoint}/users/${user.id}")
                    .body("{ \"password\": \"$password\" }")
                    .asJson()
            if(deleteResp.status == 200)
                OK()
            else
                throw Unauthorized().asException()
        }
    }

}