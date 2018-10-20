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
}