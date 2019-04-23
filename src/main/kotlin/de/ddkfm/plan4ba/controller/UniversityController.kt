package de.ddkfm.plan4ba.controller

import de.ddkfm.plan4ba.models.University
import de.ddkfm.plan4ba.models.UniversityInfo
import de.ddkfm.plan4ba.models.User
import de.ddkfm.plan4ba.models.UserGroup
import de.ddkfm.plan4ba.utils.DBService
import de.ddkfm.plan4ba.utils.ShortToken
import spark.Request
import spark.Response
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces

@Path("/university")
@Produces("application/json")
@ShortToken
class UniversityController(req : Request, resp : Response, user : User) : ControllerInterface(req = req, resp = resp, user = user) {

    @GET
    @Path("")
    fun getUniversistyInfo(): UniversityInfo {
        val group = DBService.get<UserGroup>(user.groupId).getOrThrow()
        val university = DBService.get<University>(group.universityId).getOrThrow()
        return UniversityInfo(university.name, university.accentColor, university.logoUrl)
    }
}