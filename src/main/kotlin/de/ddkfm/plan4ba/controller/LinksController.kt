package de.ddkfm.plan4ba.controller

import com.mashape.unirest.http.Unirest
import de.ddkfm.plan4ba.models.*
import de.ddkfm.plan4ba.utils.loginCampusDual
import de.ddkfm.plan4ba.utils.toJson
import de.ddkfm.plan4ba.utils.toModel
import de.ddkfm.plan4ba.utils.triggerCaching
import io.swagger.annotations.*
import org.json.JSONObject
import spark.Request
import spark.Response
import java.util.*
import javax.ws.rs.DELETE

import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces

@Api(value = "/links", description = "get all links")
@Path("/links")
@Produces("application/json")
class LinksController(req : Request, resp : Response, user : User) : ControllerInterface(req = req, resp = resp, user = user) {

    @GET
    @ApiOperation(value = "get all links by user")
    @ApiImplicitParam(name = "language", dataType = "string", paramType = "query")
    @ApiResponses(
            ApiResponse(code = 200, message = "successfull", response = SimpleLink::class, responseContainer = "List")
    )
    @Path("")
    fun getUniversityLinks(@ApiParam(hidden = true) language : String): List<SimpleLink> {
        val actualLanguage = if(language == null || language.isEmpty()) "de" else language
        val caldavToken = UserController(req, resp,user).getCaldavToken() as Token
        val userLinks = (Unirest.get("${config.dbServiceEndpoint}/users/${user.id}/links")
            .toModel(Link::class.java)
            .second as List<Link>)
            .map { SimpleLink(it.id, it.label, it.url, it.language) }
            .filter { actualLanguage == it.language }
        val caldavLink = SimpleLink(0, "CALDAV-Link", "${req.scheme()}://${req.host()}/api/caldav/all?token=${caldavToken.token}", actualLanguage)
        return userLinks.union(listOf(caldavLink)).toList()
    }
}