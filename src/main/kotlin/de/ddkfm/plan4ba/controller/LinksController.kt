package de.ddkfm.plan4ba.controller

import kong.unirest.Unirest
import de.ddkfm.plan4ba.models.Link
import de.ddkfm.plan4ba.models.SimpleLink
import de.ddkfm.plan4ba.models.Token
import de.ddkfm.plan4ba.models.User
import de.ddkfm.plan4ba.utils.ShortToken
import de.ddkfm.plan4ba.utils.toModel
import spark.Request
import spark.Response
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam

@Path("/links")
@Produces("application/json")
@ShortToken
class LinksController(req : Request, resp : Response, user : User) : ControllerInterface(req = req, resp = resp, user = user) {

    @GET
    @Path("")
    fun getUniversityLinks(@QueryParam("language") language : String): List<SimpleLink> {
        val actualLanguage = if(language.isEmpty()) "de" else language
        val caldavToken = UserController(req, resp,user).getCaldavToken()
        val userLinks = (Unirest.get("${config.dbServiceEndpoint}/users/${user.id}/links")
            .toModel(Link::class.java)
            .second as List<Link>)
            .map { SimpleLink(it.id, it.label, it.url, it.language) }
            .filter { actualLanguage == it.language }
        val caldavLink = SimpleLink(0, "CALDAV-Link", "${req.scheme()}://${req.host()}/api/caldav/all?token=${caldavToken.token}", actualLanguage)
        return userLinks.union(listOf(caldavLink)).toList()
    }
}