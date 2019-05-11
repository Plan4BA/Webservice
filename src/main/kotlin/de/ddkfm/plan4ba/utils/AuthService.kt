package de.ddkfm.plan4ba.utils

import kong.unirest.Unirest
import de.ddkfm.plan4ba.models.*

object AuthService {
    fun modifyHash(u : User, storeHash : Boolean, storeReminders : Boolean, storeExamStats : Boolean, password: String) : User {
        var user = u.copy()

        if(!storeHash)
            user.userHash = ""
        else if(storeHash && user.userHash.isNullOrEmpty()) {
            user = loginCampusDual(user.matriculationNumber, password)!!
        }
        if(user.storeExamsStats && !storeExamStats) {
            val examStats = DBService.all<ExamStat>("userId" to user.id).maybe ?: emptyList()
            for(examStat in examStats)
                DBService.delete<ExamStat>(examStat.id)
        }
        if(user.storeReminders && !storeReminders) {
            val reminders = DBService.all<Reminder>("userId" to user.id).maybe ?: emptyList()
            for(reminder in reminders) {
                DBService.delete<Reminder>(reminder.id)
            }
        }
        user.storeExamsStats = storeExamStats
        user.storeReminders = storeReminders
        val respUser = DBService.update(user) { it.id }.getOrThrow()
        if(!u.storeExamsStats && storeExamStats || !u.storeReminders && storeReminders) {
            triggerCaching(respUser, respUser.userHash ?: "")
        }
        return respUser
    }
    fun loginCampusDual(username : String, password : String) : User? {
        val (status, login) = Unirest.get("${de.ddkfm.plan4ba.config.loginServiceEndpoint}/login")
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
        println("trigger caching for user ${user.id}")
        val job = LectureJob(user.id, hash, user.matriculationNumber)
        Unirest.post("${de.ddkfm.plan4ba.config.cachingServiceEndpoint}/trigger")
            .body(job.toJson())
            .asJson()
    }
}