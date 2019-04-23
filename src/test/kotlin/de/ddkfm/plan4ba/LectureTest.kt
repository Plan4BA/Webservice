import kong.unirest.Unirest
import de.ddkfm.plan4ba.models.Lecture
import de.ddkfm.plan4ba.models.OK
import de.ddkfm.plan4ba.utils.toListModel
import de.ddkfm.plan4ba.utils.toModel
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertNotNull

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LectureTest {
    val endpoint = Webservice.endpoint
    init {
        Webservice.start()
    }
    @Test
    fun getUserLectures() {
        val token = Webservice.getShortToken()
        val lectures = Unirest.get("$endpoint/lectures")
            .bearer(token.token)
            .asString().getOrThrow().toListModel<Lecture>()
        assertNotNull(lectures)
    }

    @Test
    fun triggerCachingWithToken() {
        val token = Webservice.getShortToken()
        println(token)
        val trigger = Unirest.get("$endpoint/lectures/trigger")
            .bearer(token.token)
            .asString()
            .getOrThrow()
            .toModel<OK>()
        assertNotNull(trigger)
    }

    @Test
    fun triggerCachingWithUsername() {
        val username = System.getenv("WEBSERVICE_USERNAME")
        val password = System.getenv("WEBSERVICE_PASSWORD")
        val trigger = Unirest.get("$endpoint/lectures/trigger")
            .basicAuth(username, password)
            .asString()
            .getOrThrow()
            .toModel<OK>()
        assertNotNull(trigger)
    }
}