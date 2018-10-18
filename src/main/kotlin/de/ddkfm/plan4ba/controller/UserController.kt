package de.ddkfm.plan4ba.controller

import com.mashape.unirest.http.Unirest
import de.ddkfm.plan4ba.models.*
import de.ddkfm.plan4ba.utils.toModel
import io.swagger.annotations.*
import org.json.JSONObject
import spark.Request
import spark.Response

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
    fun getUserInfo(): Any? {
        val group = Unirest
                .get("${config.dbServiceEndpoint}/groups/${user.groupId}")
                .toModel(UserGroup::class.java)
                .second as UserGroup
        val university = Unirest.get("${config.dbServiceEndpoint}/universities/${group.universityId}")
                .toModel(University::class.java)
                .second as University
        return UserInfo(user.matriculationNumber, group.uid, university.name, !user.userHash.isNullOrEmpty(),
                user.lastLecturePolling, user.lastLecturePolling)
    }
}