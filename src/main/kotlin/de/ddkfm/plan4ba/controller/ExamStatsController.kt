package de.ddkfm.plan4ba.controller

import de.ddkfm.plan4ba.models.ExamStat
import de.ddkfm.plan4ba.models.NotFound
import de.ddkfm.plan4ba.models.User
import de.ddkfm.plan4ba.utils.DBService
import de.ddkfm.plan4ba.utils.ShortToken
import spark.Request
import spark.Response
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces

@Path("/examstats")
@Produces("application/json")
@ShortToken
class ExamStatsController(req : Request, resp : Response, user : User) : ControllerInterface(req = req, resp = resp, user = user) {

    @GET
    @Path("")
    fun getExamStat(): ExamStat{
        return DBService.all<ExamStat>("userId" to user.id).maybe?.firstOrNull()
            ?: throw NotFound().asException()
    }

}