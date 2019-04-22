package de.ddkfm.plan4ba.utils

import com.mashape.unirest.http.Unirest
import de.ddkfm.plan4ba.config
import de.ddkfm.plan4ba.models.*
import org.json.JSONObject
import spark.Request
import spark.Response
import spark.Spark
import spark.Spark.halt
import java.net.URLEncoder
import java.util.*

fun getShortToken(req : Request, resp : Response) : Any? {
    resp.type("application/json")
    val auth = req.headers("Authorization")
    if(auth == null || !auth.startsWith("Bearer")) {
        resp.status(401)
        return "Unauthorized"
    } else {
        val tokenString = auth.replace("Bearer", "").trim()
        val (token, status) = DBService.get<Token>(tokenString)
        when(status) {
            null -> {
                token as Token
                if(token.isCalDavToken)
                    return halt(401, "caldav token is not useable for other webservice methods")

                if(!token.isRefreshToken)
                    return halt(401, "short token cannot be used for token refresh")

                if(System.currentTimeMillis() > token.validTo ){
                    return halt(401, "Token not valid")
                }
                val tokens = DBService.all<Token>(
                    "userId" to token.userId,
                    "caldavToken" to false,
                    "refreshToken" to false,
                    "valid" to true
                ).maybe?.firstOrNull()
                if(tokens == null) {
                    val shortToken = Token(UUID.randomUUID().toString().replace("-", ""),
                            token.userId, false, false, System.currentTimeMillis() + config.shortTokenInterval)
                    var (status, _) = Unirest.put("${config.dbServiceEndpoint}/tokens")
                            .body(shortToken.toJson())
                            .toModel(Token::class.java)
                    when(status) {
                        200, 201 -> {
                            val createdToken = DBService.get<Token>(shortToken.token).getOrThrow()
                            return createdToken.toJson()
                        }
                        else -> return halt(500, "Internal Server Error")
                    }
                }
                return tokens.toJson()
            }
            else -> return halt(401, "Unauthorized")
        }
    }
}
fun login(req : Request, resp : Response) : Any? {
    val auth = req.headers("Authorization")
    if(auth == null || !auth.startsWith("Basic ")) {
        //resp.header("WWW-Authenticate", "Basic realm=\"Anmeldung wird benötigt\"")
        resp.status(401)
        return "Unauthorized"
    } else {
        val encoded = String(Base64.getDecoder().decode(auth.replace("Basic", "").trim().toByteArray()))
        val username = encoded.split(":")[0]
        val password = encoded.split(":")[1]
        try {
            var user = DBService.all<User>("matriculationNumber" to username).maybe?.firstOrNull()

            if(user == null)
                user = loginCampusDual(username, password)
            if(user == null) {
                //resp.header("WWW-Authenticate", "Basic realm=\"Anmeldung wird benötigt\"")
                resp.status(401)
                return "Unauthorized"
            }
            val authenticated = user.authenticate(password)
            if(authenticated) {
                val storeHash = req.headers("StoreHash")?.toLowerCase()?.equals("true") ?: false
                user = modifyHash(user, storeHash, password)
                //authenticated
                var token = DBService.all<Token>(
                    "userId" to user.id,
                    "caldavToken" to false,
                    "refreshToken" to true,
                    "valid" to true
                ).maybe?.firstOrNull()
                if(token == null) {
                    token = Token(UUID.randomUUID().toString().replace("-", ""), user.id,
                        false, true, System.currentTimeMillis() + config.refreshTokenInterval)
                    token = DBService.create(token).getOrThrow()
                    resp.type("application/json")
                    return token.toJson()
                } else {
                    resp.type("application/json")
                    return token.toJson()
                }
            } else {
                //resp.header("WWW-Authenticate", "Basic realm=\"Anmeldung wird benötigt\"")
                resp.status(401)
                return "Unauthorized"
            }
        } catch (e : Exception) {
            e.printStackTrace()
            throw e
        }
    }
}

fun modifyHash(u : User, storeHash : Boolean, password: String) : User {
    var user = u
    if(user.userHash.isNullOrEmpty() xor storeHash)
        return user
    if(!storeHash)
        user.userHash = ""
    else
        user = loginCampusDual(user.matriculationNumber, password)!!
    val respUser = DBService.update(user) { it.id }.getOrThrow()
    return respUser
}
fun loginCampusDual(username : String, password : String) : User? {
    val (status, login) = Unirest.get("${config.loginServiceEndpoint}/login")
            .basicAuth(username, password)
            .toModel(Login::class.java)
    if(status == 401 || status == 400)
        return null
    if(login is Login) {
        var uni = DBService.all<University>("name" to login.university.encode()).maybe?.firstOrNull()
        if(uni == null) {
            uni = University(0, login.university, "", "")
            val createUni = DBService.create(uni)
            when(createUni.error?.code) {
                null, 200,201 -> {
                    uni = createUni.maybe
                }
                409 -> {
                    uni = DBService.all<University>("name" to login.university.encode()).maybe?.firstOrNull()
                }
                500 -> return null
            }
        }

        var group = DBService.all<UserGroup>("uid" to login.group).maybe?.firstOrNull()
        if(group == null) {
            group = UserGroup(0, login.group, uni!!.id)
            val createGroup = DBService.create(group)
            when(createGroup.error?.code) {
                null, 200,201 -> {
                    group = createGroup.maybe
                }
                409 -> {
                    group = DBService.all<UserGroup>("uid" to login.group).maybe?.firstOrNull()
                }
                500 -> return null
            }
        }
        var user = User(0, username, login.hash, password, group!!.id, 0, 0)
        val createUser = DBService.create(user)
        when(createUser.error?.code) {
            null, 200,201 -> {
                user = createUser.maybe!!
            }
            409 -> {
                user = DBService.all<User>("matriculationNumber" to username).maybe?.firstOrNull()!!
                user.userHash = login.hash
            }
            500 -> return null
        }
        //trigger caching service
        triggerCaching(user, login.hash)
        user.password = password
        return user
    }
    return null
}

fun triggerCaching(user : User, hash : String) {
    val job = LectureJob(user.id, hash, user.matriculationNumber)
    Unirest.post("${config.cachingServiceEndpoint}/trigger")
            .body(job.toJson())
            .asJson()
}