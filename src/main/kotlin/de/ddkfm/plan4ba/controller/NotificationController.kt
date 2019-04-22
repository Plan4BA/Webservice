package de.ddkfm.plan4ba.controller

import kong.unirest.Unirest
import de.ddkfm.plan4ba.models.*
import de.ddkfm.plan4ba.utils.DBService
import de.ddkfm.plan4ba.utils.ShortToken
import de.ddkfm.plan4ba.utils.toModel
import spark.Request
import spark.Response
import javax.ws.rs.*
import kotlin.contracts.ExperimentalContracts

@Path("/notifications")
@Produces("application/json")
@ShortToken
class NotificationController(req : Request, resp : Response, user : User) : ControllerInterface(req = req, resp = resp, user = user) {
    @ExperimentalContracts
    @GET
    @Path("")
    fun getNotifications(): List<SimpleNotification> {
        val notificationsResponse = DBService.all<Notification>("userId" to user.id)
        if(notificationsResponse.error != null)
            throw notificationsResponse.error.asException()
        val notifications = notificationsResponse.maybe
            ?.map { SimpleNotification(it.id, it.label, it.description, it.type) }
        return notifications ?: emptyList()
    }

    @DELETE
    @Path("/:id")
    fun delete(@PathParam("id") id : Int) : OK {
        val notification = DBService.get<Notification>(id).maybe
            ?: throw NotFound().asException()

        return if(notification.userId != user.id) {
            throw BadRequest().asException()
        } else {
            val (status, ok) = Unirest.delete("${config.dbServiceEndpoint}/notifications/$id").toModel(OK::class.java)
            when (status) {
                200 -> ok as OK
                else -> throw BadRequest().asException()
            }
        }
    }

}