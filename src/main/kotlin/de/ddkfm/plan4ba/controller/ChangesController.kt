package de.ddkfm.plan4ba.controller

import de.ddkfm.plan4ba.models.*
import de.ddkfm.plan4ba.utils.DBService
import de.ddkfm.plan4ba.utils.ShortToken
import spark.Request
import spark.Response
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces

@Path("/changes")
@Produces("application/json")
@ShortToken
class ChangesController(req : Request, resp : Response, user : User) : ControllerInterface(req = req, resp = resp, user = user) {

    @GET
    @Path("/:notificationId")
    fun getChanges(@PathParam("notificationId") notificationId: Int): List<SimpleLectureChange> {
        val notifcation = DBService.get<Notification>(notificationId).maybe
            ?: throw NotFound("notification id not found").asException()
        if(notifcation.userId != user.id)
            throw NotFound("no access").asException()
        val changes = DBService.all<LectureChange>("notificationId" to notifcation.id).maybe
            ?: return emptyList()
        val lectures = DBService.all<Lecture>("userId" to user.id).maybe ?: emptyList()
        val mappedLectures = lectures.map { it.id to it }.toMap()
        val simpleChanges = changes.map { change ->
            val old = change.old?.let { mappedLectures[it] }
            val new = change.new?.let { mappedLectures[it] }
            SimpleLectureChange(
                id = change.id,
                notificationId = change.notificationId,
                new = new,
                old = old
            )
        }
        return simpleChanges
    }

}