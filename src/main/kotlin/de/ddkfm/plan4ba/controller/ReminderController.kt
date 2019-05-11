package de.ddkfm.plan4ba.controller

import de.ddkfm.plan4ba.models.*
import de.ddkfm.plan4ba.utils.DBService
import de.ddkfm.plan4ba.utils.ShortToken
import spark.Request
import spark.Response
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces

@Path("/reminder")
@Produces("application/json")
@ShortToken
class ReminderController(req : Request, resp : Response, user : User) : ControllerInterface(req = req, resp = resp, user = user) {

    @GET
    @Path("")
    fun getReminders(): SimpleReminder {
        val reminder = DBService.all<Reminder>("userId" to user.id).maybe?.firstOrNull()
            ?: throw NotFound().asException()
        val latest = DBService.all<LatestExamResult>("reminderId" to reminder.id).maybe
            ?: emptyList()
        val upcoming = DBService.all<Upcoming>("reminderId" to reminder.id).maybe
            ?: emptyList()
        return SimpleReminder(
            semester = reminder.semester,
            electives = reminder.electives,
            exams = reminder.exams,
            latest = latest,
            upcoming = upcoming
        )
    }

}