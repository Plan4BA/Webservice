package de.ddkfm.plan4ba.controller

import biweekly.Biweekly
import biweekly.ICalendar
import biweekly.component.VEvent
import biweekly.property.Image
import com.mashape.unirest.http.Unirest
import de.ddkfm.plan4ba.models.*
import de.ddkfm.plan4ba.utils.toModel
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import spark.Request
import spark.Response
import java.util.*
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces

@Api(value = "/caldav", description = "return all lectures in caldav-format")
@Path("/caldav")
@Produces("text/calendar")
class CaldavController(req : Request, resp : Response, user : User) : ControllerInterface(req = req, resp = resp, user = user) {

    @GET
    @ApiOperation(value = "get caldav lectures")
    @ApiResponses(
            ApiResponse(code = 200, message = "successfull", response = Meal::class, responseContainer = "List")
    )
    @Path("")
    fun getCalDav(): Any? {
        try {
            val lectureController = LectureController(req, resp, user)
            val lectures = lectureController.getUserLectures() as List<Lecture>
            val calendar = ICalendar()
            val group = Unirest
                    .get("${config.dbServiceEndpoint}/groups/${user.groupId}")
                    .toModel(UserGroup::class.java)
                    .second as UserGroup
            val university = Unirest.get("${config.dbServiceEndpoint}/universities/${group.universityId}")
                    .toModel(University::class.java)
                    .second as University

            val geoLocation = Unirest.get("${config.dbServiceEndpoint}/universities/${university.id}/location")
                    .asJson().body.`object`.toModel(Geo::class.java)

            val events = lectures.map { lecture ->
                val event = VEvent()
                val summary = event.setSummary(lecture.description)
                summary.language = "de-DE"
                event.setColor(lecture.color)
                event.setDateStart(Date(lecture.start * 1000))
                event.setDateEnd(Date(lecture.end * 1000))
                event.setLocation(lecture.room)
                val description = with(lecture) {
                    "$description\n" +
                            "Dozent: $instructor\n" +
                            "Raum: $room"
                }
                event.setDescription(description)

                event.geo = biweekly.property.Geo(geoLocation.latitude, geoLocation.longitude)
                val image = Image("image/png", university.logoUrl)
                event.addImage(image)
                event
            }
            calendar.setLastModified(Date())
            calendar.events.addAll(events)
            return Biweekly.write(calendar).go()
        } catch (e : Exception) {
            e.printStackTrace()
            return ""
        }
    }
}