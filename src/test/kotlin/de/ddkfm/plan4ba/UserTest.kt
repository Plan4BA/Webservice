import com.mashape.unirest.http.Unirest
import de.ddkfm.plan4ba.models.*
import de.ddkfm.plan4ba.utils.DBService
import de.ddkfm.plan4ba.utils.toListModel
import de.ddkfm.plan4ba.utils.toModel
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.lang.IllegalArgumentException
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserTest {
    val endpoint = Webservice.endpoint
    init {
        Webservice.start()
    }
    @Test
    fun getUserInfo() {
        val token = Webservice.getShortToken()
        val userInfo = Unirest.get("$endpoint/user")
            .bearer(token.token)
            .asString()
            .getOrThrow()
            .toModel<UserInfo>()
        println(userInfo)
        assertNotNull(userInfo)
    }

    @Test
    fun getCaldavToken() {
        val token = Webservice.getShortToken()
        val caldavToken = Unirest.get("$endpoint/user/caldavToken")
            .bearer(token.token)
            .asString()
            .getOrThrow()
            .toModel<Token>()
        assertNotNull(caldavToken)
        assert(caldavToken!!.isCalDavToken)
        assert(caldavToken!!.validTo > System.currentTimeMillis())
    }

    //@Test
    fun deleteUserData() {
        val deleteResponse = Unirest.delete("$endpoint/user/delete")
            .basicAuth(System.getenv("WEBSERVICE_USERNAME"), System.getenv("WEBSERVICE_PASSWORD"))
            .asString()
            .getOrThrow()
            .toModel<OK>()
        println(deleteResponse)
        assertNotNull(deleteResponse)
        val user = DBService.all<User>("matriculationNumber" to System.getenv("WEBSERVICE_USERNAME"))
            .maybe
            ?.firstOrNull()
        assertNull(user)

        //trigger relogin
        Webservice.clearTokens()
        UniversityTest().getUniversityInfo()

    }

}