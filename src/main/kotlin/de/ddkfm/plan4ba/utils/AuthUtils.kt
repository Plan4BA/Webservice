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
        val (status, token) = Unirest.get("${config.dbServiceEndpoint}/tokens/$tokenString").toModel(Token::class.java)
        when(status) {
            404 -> return halt(401, "Unauthorized")
            200-> {
                val token = token as Token
                if(token.isCalDavToken)
                    return halt(401, "caldav token is not useable for other webservice methods")

                if(!token.isRefreshToken)
                    return halt(401, "short token cannot be used for token refresh")

                if(System.currentTimeMillis() > token.validTo ){
                    return halt(401, "Token not valid")
                }

                var tokens = (Unirest.get("${config.dbServiceEndpoint}/tokens?userId=${token.userId}&caldavToken=false&refreshToken=false&valid=true")
                        .toModel(Token::class.java).second as List<Token>)
                        .firstOrNull()
                if(tokens == null) {
                    val shortToken = Token(UUID.randomUUID().toString().replace("-", ""),
                            token.userId, false, false, System.currentTimeMillis() + config.shortTokenInterval)
                    var (status, _) = Unirest.put("${config.dbServiceEndpoint}/tokens")
                            .body(shortToken.toJson())
                            .toModel(Token::class.java)
                    when(status) {
                        200, 201 -> {
                            val createdToken = (Unirest.get("${config.dbServiceEndpoint}/tokens/${shortToken.token}").toModel(Token::class.java).second as Token)
                            return createdToken.toJson()
                        }
                        else -> return halt(500, "Internal Server Error")
                    }
                }
                return tokens.toJson()
            }
        }
    }
    return halt(400, BadRequest().toJson())
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
            var user = (Unirest.get("${config.dbServiceEndpoint}/users?matriculationNumber=$username")
                    .toModel(User::class.java)
                    .second as List<User>)
                    .firstOrNull()

            if(user == null)
                user = loginCampusDual(username, password)
            if(user == null) {
                //resp.header("WWW-Authenticate", "Basic realm=\"Anmeldung wird benötigt\"")
                resp.status(401)
                return "Unauthorized"
            }

            val authResp = Unirest.post("${config.dbServiceEndpoint}/users/${user?.id}/authenticate")
                    .body(JSONObject("{ \"password\" : \"$password\"}")).asString()
            when(authResp.status) {
                in 400..404 -> {
                    //resp.header("WWW-Authenticate", "Basic realm=\"Anmeldung wird benötigt\"")
                    resp.status(401)
                    return "Unauthorized"
                }
                200 -> {
                    val storeHash = req.headers("StoreHash")?.toLowerCase()?.equals("true") ?: false
                    user = modifyHash(user, storeHash, password)
                    //authenticated
                    var token = (Unirest.get("${config.dbServiceEndpoint}/tokens?userId=${user?.id}&caldavToken=false&refreshToken=true&valid=true")
                            .toModel(Token::class.java).second as List<Token>)
                            .firstOrNull()
                    if(token == null) {
                        token = Token(UUID.randomUUID().toString().replace("-", ""), user.id,
                                false, true, System.currentTimeMillis() + config.refreshTokenInterval)
                        token = (Unirest.put("${config.dbServiceEndpoint}/tokens")
                                .body(token.toJson())
                                .toModel(Token::class.java).second as Token)
                        resp.type("application/json")
                        return token.toJson()
                    } else {
                        resp.type("application/json")
                        return token.toJson()
                    }
                }
            }
        } catch (e : Exception) {
            e.printStackTrace()
            throw e
        }
    }
    return ""
}

fun modifyHash(u : User, storeHash : Boolean, password: String) : User {
    var user = u
    if(user.userHash.isNullOrEmpty() xor storeHash)
        return user
    if(!storeHash)
        user.userHash = ""
    else
        user = loginCampusDual(user.matriculationNumber, password)!!
    val resp = Unirest.post("${config.dbServiceEndpoint}/users/${user.id}")
            .body(user.toJson())
            .toModel(User::class.java)
    return when(resp.first) {
        in 200..299 -> resp.second as User
        else -> user
    }
}
fun loginCampusDual(username : String, password : String) : User? {
    val (status, login) = Unirest.get("${config.loginServiceEndpoint}/login")
            .basicAuth(username, password)
            .toModel(Login::class.java)
    if(status == 401 || status == 400)
        return null
    if(login is Login) {

        var uni = (Unirest.get("${config.dbServiceEndpoint}/universities?name=${login.university.encode()}")
                .toModel(University::class.java).second as List<University>)
                .firstOrNull()
        if(uni == null) {
            uni = University(0, login.university, "", "")
            val (status, createUni) = Unirest.put("${config.dbServiceEndpoint}/universities")
                    .body(uni.toJson())
                    .toModel(University::class.java)
            when(status) {
                200,201 -> {
                    uni = createUni as University
                }
                409 -> {
                    uni = (Unirest.get("${config.dbServiceEndpoint}/universities?name=${login.university}")
                            .toModel(University::class.java).second as List<University>)
                            .firstOrNull()
                }
                500 -> return null
            }
        }

        var group = (Unirest.get("${config.dbServiceEndpoint}/groups?uid=${login.group}")
                .toModel(UserGroup::class.java).second as List<UserGroup>)
                .firstOrNull()
        if(group == null) {
            group = UserGroup(0, login.group, uni!!.id)
            val (status, createGroup) = Unirest.put("${config.dbServiceEndpoint}/groups")
                    .body(group.toJson())
                    .toModel(UserGroup::class.java)
            when(status) {
                200,201 -> {
                    group = createGroup as UserGroup
                }
                409 -> {
                    group = (Unirest.get("${config.dbServiceEndpoint}/groups?uid=${login.group}")
                            .toModel(UserGroup::class.java).second as List<UserGroup>)
                            .firstOrNull()
                }
                500 -> return null
            }
        }
        var user = User(0, username, login.hash, password, group!!.id, 0, 0)
        var (status, createUser) = Unirest.put("${config.dbServiceEndpoint}/users")
                .body(user.toJson())
                .toModel(User::class.java)
        when(status) {
            200,201 -> {
                user = createUser as User
            }
            409 -> {
                user  = (Unirest.get("${config.dbServiceEndpoint}/users?matriculationNumber=$username")
                        .toModel(User::class.java).second as List<User>)
                        .first()
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