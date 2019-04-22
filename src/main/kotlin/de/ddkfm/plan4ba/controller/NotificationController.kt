package de.ddkfm.plan4ba.controller

import com.mashape.unirest.http.Unirest
import de.ddkfm.plan4ba.models.*
import de.ddkfm.plan4ba.utils.DBService
import de.ddkfm.plan4ba.utils.toModel
import io.swagger.annotations.*
import spark.Request
import spark.Response
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import kotlin.contracts.ExperimentalContracts

@Api(value = "/notifications", description = "all operations about the notification api")
@Path("/notifications")
@Produces("application/json")
class NotificationController(req : Request, resp : Response, user : User) : ControllerInterface(req = req, resp = resp, user = user) {
    @ExperimentalContracts
    @GET
    @ApiOperation(value = "get user notifications")
    @ApiResponses(
            ApiResponse(code = 200, message = "successfull", response = SimpleNotification::class, responseContainer = "List")
    )
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
    @ApiOperation(value = "delete a notification")
    @ApiImplicitParam(name = "id", dataType = "integer", paramType = "path")
    @ApiResponses(
            ApiResponse(code = 200, message = "successfull", response = OK::class),
            ApiResponse(code = 401, message = "Bad Request", response = BadRequest::class),
            ApiResponse(code = 404, message = "Not Found", response = NotFound::class)
    )
    @Path("/:id")
    fun delete(@ApiParam(hidden = true) id : Int) : OK {
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