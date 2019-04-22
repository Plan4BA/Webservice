import kong.unirest.Unirest
import de.ddkfm.plan4ba.models.UniversityInfo
import de.ddkfm.plan4ba.utils.toModel
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertNotNull

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UniversityTest {
    val endpoint = Webservice.endpoint
    init {
        Webservice.start()
    }

    @Test
    fun getUniversityInfo() {
        val token = Webservice.getShortToken()
        val uni = Unirest.get("$endpoint/university")
            .bearer(token.token)
            .asString().getOrThrow()
            .toModel<UniversityInfo>()
        println(uni)
        assertNotNull(uni)
    }
}