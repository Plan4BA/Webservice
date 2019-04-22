import com.mashape.unirest.http.Unirest
import de.ddkfm.plan4ba.models.Meal
import de.ddkfm.plan4ba.models.SimpleLink
import de.ddkfm.plan4ba.utils.toListModel
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertNotNull

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MealTest {
    val endpoint = Webservice.endpoint
    init {
        Webservice.start()
    }

    @Test
    fun getMeals() {
        val token = Webservice.getShortToken()
        val meals = Unirest.get("$endpoint/meals")
            .bearer(token.token)
            .asString().getOrThrow().toListModel<Meal>()
        println(meals)
        assertNotNull(meals)
    }
}