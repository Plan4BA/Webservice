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

    @GET
    @Path("")
    fun getNotifications(@QueryParam("language") language : String): List<SimpleNotification> {
        val language = if(language.isEmpty()) "de" else language
        val notificationsResponse = DBService.all<Notification>("userId" to user.id)
        if(notificationsResponse.error != null)
            throw notificationsResponse.error.asException()
        val notifications = notificationsResponse.maybe?.map { notification ->
            val translation = DBService.all<NotificationTranslation>("type" to notification.type, "language" to language).maybe?.firstOrNull()
                ?: NotificationTranslation(0, notification.type, "de", "nicht näher benannte Benachrichtigung", "Ich bin eine Notification")
            val callback = when(notification.type) {
                NotificationType.LECTURE_CHANGE.value -> {
                    "/changes/${notification.id}"
                }
                NotificationType.APP_CHANGE.value -> {
                    val versionId = notification.versionId
                    if(versionId == null)
                        System.err.println("VersionID of notification ${notification.id} is null, but type is appChange")
                    "/versions/$versionId"
                }
                else -> throw InternalServerError("type ${notification.type} is not supported").asException()
            }
            val simpleNotification = SimpleNotification(
                id = notification.id,
                label = translation.label,
                description = translation.description,
                type = notification.type,
                callback = callback
            )
            simpleNotification
        }
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