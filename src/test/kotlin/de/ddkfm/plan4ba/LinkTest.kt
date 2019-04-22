import kong.unirest.Unirest
import de.ddkfm.plan4ba.models.Lecture
import de.ddkfm.plan4ba.models.OK
import de.ddkfm.plan4ba.models.SimpleLink
import de.ddkfm.plan4ba.utils.toListModel
import de.ddkfm.plan4ba.utils.toModel
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertNotNull

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LinkTest {
    val endpoint = Webservice.endpoint
    init {
        Webservice.start()
    }

    @Test
    fun getDefaultLinks() {
        val token = Webservice.getShortToken()
        val links = Unirest.get("$endpoint/links")
            .bearer(token.token)
            .asString().getOrThrow().toListModel<SimpleLink>()
        println(links)
        assertNotNull(links)
    }

    @Test
    fun getGermanLinks() {
        val token = Webservice.getShortToken()
        val links = Unirest.get("$endpoint/links?language=de")
            .bearer(token.token)
            .asString().getOrThrow().toListModel<SimpleLink>()
        println(links)
        assertNotNull(links)
    }

    @Test
    fun getEnglishLinks() {
        val token = Webservice.getShortToken()
        val links = Unirest.get("$endpoint/links?language=en")
            .bearer(token.token)
            .asString().getOrThrow().toListModel<SimpleLink>()
        println(links)
        assertNotNull(links)
    }
}