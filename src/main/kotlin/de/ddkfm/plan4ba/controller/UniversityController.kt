package de.ddkfm.plan4ba.controller

import com.mashape.unirest.http.Unirest
import de.ddkfm.plan4ba.models.*
import de.ddkfm.plan4ba.utils.DBService
import de.ddkfm.plan4ba.utils.toJson
import de.ddkfm.plan4ba.utils.toModel
import io.swagger.annotations.*
import org.json.JSONObject
import spark.Request
import spark.Response
import java.util.*

import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces

@Api(value = "/university", description = "all operations abount a users university")
@Path("/university")
@Produces("application/json")
class UniversityController(req : Request, resp : Response, user : User) : ControllerInterface(req = req, resp = resp, user = user) {

    @GET
    @ApiOperation(value = "get all informations about a university")
    @ApiResponses(
            ApiResponse(code = 200, message = "successfull", response = UniversityInfo::class)
    )
    @Path("")
    fun getUniversistyInfo(): UniversityInfo {
        val group = DBService.get<UserGroup>(user.groupId).getOrThrow()
        val university = DBService.get<University>(group.universityId).getOrThrow()
        return UniversityInfo(university.name, university.accentColor, university.logoUrl)
    }
}