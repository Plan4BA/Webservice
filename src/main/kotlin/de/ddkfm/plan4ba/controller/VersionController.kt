package de.ddkfm.plan4ba.controller

import de.ddkfm.plan4ba.models.*
import de.ddkfm.plan4ba.utils.DBService
import de.ddkfm.plan4ba.utils.ShortToken
import de.ddkfm.plan4ba.utils.toListModel
import kong.unirest.Unirest
import spark.Request
import spark.Response
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces

@Path("/versions")
@Produces("application/json")
@ShortToken
class VersionController(req : Request, resp : Response, user : User) : ControllerInterface(req = req, resp = resp, user = user) {

    @GET
    @Path("")
    fun getAllVersions() : List<AppVersion> {
        val versions = DBService.all<AppVersion>().maybe ?: emptyList()
        return versions
    }

    @GET
    @Path("/:versionId")
    fun getVersion(@PathParam("versionId") versionId: Int): AppVersion {
        val version = DBService.get<AppVersion>(versionId).maybe
            ?: throw NotFound("version not found").asException()
        return version
    }

}