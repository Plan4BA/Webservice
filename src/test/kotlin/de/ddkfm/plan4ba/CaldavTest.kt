import biweekly.Biweekly
import com.mashape.unirest.http.Unirest
import de.ddkfm.plan4ba.models.Lecture
import de.ddkfm.plan4ba.models.OK
import de.ddkfm.plan4ba.models.Token
import de.ddkfm.plan4ba.utils.toListModel
import de.ddkfm.plan4ba.utils.toModel
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertNotNull

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CaldavTest {
    val endpoint = Webservice.endpoint
    init {
        Webservice.start()
        Unirest.setDefaultHeader("Accept-Encoding", "gzip")
    }
    @Test
    fun getCaldavLectures() {
        val token = Webservice.getCaldavToken()
        val response = Unirest.get("$endpoint/caldav/lectures?token=${token.token}")
            .asString()
            .getOrThrow()
        println(response)
        val calendar = Biweekly.parse(response).first()
        assertNotNull(calendar)
    }

    @Test
    fun getCaldavMeals() {
        val token = Webservice.getCaldavToken()
        val response = Unirest.get("$endpoint/caldav/meals?token=${token.token}")
            .asString()
            .getOrThrow()
        println(response)
        val calendar = Biweekly.parse(response).first()
        assertNotNull(calendar)
    }

    @Test
    fun getAll() {
        val token = Webservice.getCaldavToken()
        val response = Unirest.get("$endpoint/caldav/all?token=${token.token}")
            .asString()
            .getOrThrow()
        println(response)
        val calendar = Biweekly.parse(response).first()
        assertNotNull(calendar)
    }
}