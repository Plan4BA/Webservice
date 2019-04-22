package de.ddkfm.plan4ba.controller

import com.mashape.unirest.http.Unirest
import de.ddkfm.plan4ba.models.*
import de.ddkfm.plan4ba.utils.DBService
import de.ddkfm.plan4ba.utils.toModel
import io.swagger.annotations.*
import org.apache.commons.lang3.StringEscapeUtils
import org.json.JSONObject
import spark.Request
import spark.Response

import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces

@Api(value = "/meals", description = "return all meals for own university")
@Path("/meals")
@Produces("application/json")
class MealController(req : Request, resp : Response, user : User) : ControllerInterface(req = req, resp = resp, user = user) {

    @GET
    @ApiOperation(value = "get all Meals for the current week")
    @ApiResponses(
            ApiResponse(code = 200, message = "successfull", response = Meal::class, responseContainer = "List")
    )
    @Path("")
    fun getMeals(): List<Meal> {
        val group = DBService.get<UserGroup>(user.groupId).maybe ?: throw InternalServerError("group does not exist").asException()
        val university = DBService.get<University>(group.universityId).maybe ?: throw InternalServerError("university does not exist").asException()
        val meals = Unirest.get("${config.dbServiceEndpoint}/universities/${university.id}/meals")
                .asJson().body.array
                .map { (it as JSONObject).toModel(Meal::class.java) }
                .map {meal ->
                    meal.copy(meals = meal.meals.map { food ->
                        food.copy(description = StringEscapeUtils.unescapeHtml4(food.description))
                    })
                }
        return meals
    }
}