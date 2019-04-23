import kong.unirest.Unirest
import de.ddkfm.plan4ba.main
import de.ddkfm.plan4ba.models.Token
import de.ddkfm.plan4ba.utils.toModel
import kotlin.test.assertNotNull

object Webservice {
    val endpoint = "http://localhost:8084"
    var started = false
    var refreshToken : Token? = null
    private var shortToken : Token? = null

    init {
        main(emptyArray())
        started = true
    }

    fun start() {
        if (!started) {
            main(emptyArray())
            started = false
        }
    }

    fun clearTokens() {
        this.refreshToken = null
        this.shortToken = null
    }
    fun getShortToken() : Token {
        checkShortToken()
        return this.shortToken!!
    }
    fun getCaldavToken() : Token {
        val token = Webservice.getShortToken()
        val caldavToken = Unirest.get("$endpoint/user/caldavToken")
            .bearer(token.token)
            .asString().getOrThrow().toModel<Token>()
        assertNotNull(caldavToken)
        assert(caldavToken!!.isCalDavToken)
        assert(caldavToken!!.validTo > System.currentTimeMillis())
        return caldavToken!!
    }
    private fun checkShortToken() {
        checkRefreshToken()
        if(shortToken == null || shortToken!!.validTo < System.currentTimeMillis()) {
            if(refreshToken == null || refreshToken!!.validTo < System.currentTimeMillis())
                throw java.lang.Exception("username or password is not set")
            val loginResponse = Unirest.get("$endpoint/token")
                .header("Authorization", "Bearer ${refreshToken?.token}")
                .asString().getOrThrow().toModel<Token>() ?: throw java.lang.Exception("login not possible")
            this.shortToken= loginResponse
        }
    }
    private fun checkRefreshToken(storeHash : Boolean = true) {
        if(refreshToken == null || refreshToken!!.validTo < System.currentTimeMillis()) {
            val username = System.getenv("WEBSERVICE_USERNAME")
            val password = System.getenv("WEBSERVICE_PASSWORD")
            if(username.isNullOrEmpty() || password.isNullOrEmpty())
                throw java.lang.Exception("username or password is not set")
            val loginResponse = Unirest.get("$endpoint/login")
                .basicAuth(username, password)
                .header("StoreHash", storeHash.toString())
                .asString().getOrThrow().toModel<Token>() ?: throw java.lang.Exception("login not possible")
            this.refreshToken = loginResponse
        }
    }
}