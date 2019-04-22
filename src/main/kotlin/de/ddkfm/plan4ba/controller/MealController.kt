package de.ddkfm.plan4ba.controller

import com.mashape.unirest.http.Unirest
import de.ddkfm.plan4ba.models.*
import de.ddkfm.plan4ba.utils.DBService
import de.ddkfm.plan4ba.utils.ShortToken
import de.ddkfm.plan4ba.utils.toModel
import org.apache.commons.text.StringEscapeUtils
import org.json.JSONObject
import spark.Request
import spark.Response
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces

@Path("/meals")
@Produces("application/json")
@ShortToken
class MealController(req : Request, resp : Response, user : User) : ControllerInterface(req = req, resp = resp, user = user) {

    @GET
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