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
    fun getAllVersions() : List<SimpleAppVersion> {
        val versions = DBService.all<AppVersion>().maybe ?: emptyList()
        val simpleVersions = versions.map {version ->
            val changes = Unirest.get("${config.dbServiceEndpoint}/app/${version.id}/changes")
                .asString()?.body?.toListModel<AppChange>()
                ?: emptyList()
            val simpleVersion = SimpleAppVersion(
                id = version.id,
                version = version.version,
                timestamp = version.timestamp,
                changes = changes.map { change ->
                    SimpleAppChange(
                        id = change.id,
                        description = change.description,
                        path = change.path
                    )
                }
            )
            simpleVersion
        }
        return simpleVersions
    }

    @GET
    @Path("/:versionId")
    fun getVersion(@PathParam("versionId") versionId: Int): SimpleAppVersion {
        val version = DBService.get<AppVersion>(versionId).maybe
            ?: throw NotFound("version not found").asException()
        val changes = Unirest.get("${config.dbServiceEndpoint}/app/${version.id}/changes")
            .asString()?.body?.toListModel<AppChange>()
            ?: emptyList()
        val simpleVersion = SimpleAppVersion(
            id = version.id,
            version = version.version,
            timestamp = version.timestamp,
            changes = changes.map { change ->
                SimpleAppChange(
                    id = change.id,
                    description = change.description,
                    path = change.path
                )
            }
        )
        return simpleVersion
    }

}