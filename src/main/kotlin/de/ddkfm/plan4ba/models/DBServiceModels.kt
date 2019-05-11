package de.ddkfm.plan4ba.models

import de.ddkfm.plan4ba.config
import java.util.*

data class User(
    var id: Int,
    var matriculationNumber: String,
    var userHash : String?,
    var password : String,
    var groupId : Int,
    var lastLecturePolling : Long,
    var lastLectureCall : Long,
    var storeExamsStats : Boolean = false,
    var storeReminders : Boolean = false
)


data class Token(
        var token : String,
        var userId : Int,
        var isCalDavToken : Boolean,
        var isRefreshToken : Boolean,
        var validTo : Long
) {
    companion object {
        fun getValidShortToken(userId : Int) : Token {
            return Token(UUID.randomUUID().toString().replace("-", ""), userId, false, false, 0).makeValid()
        }
    }

    fun makeValid() : Token {
        val interval = if(isRefreshToken)
            config.refreshTokenInterval
        else if(isCalDavToken)
            config.caldavTokenInterval
        else
            config.shortTokenInterval
        return copy(validTo = System.currentTimeMillis() + interval)
    }
}


data class UserGroup(
    var id : Int,
    var uid : String,
    var universityId : Int
)

data class University(
    var id : Int,
    var name : String,
    var accentColor : String,
    var logoUrl : String
)

data class Meal(
    var universityId: Int,
    var day : Long,
    var meals : List<Food>

)

data class Food(
    var description: String,
    var prices : String,
    var vegetarian : Boolean,
    var vegan : Boolean,
    var additionalInformation : String
)

data class Lecture(
    var id : Int,
    var title : String,
    var start : Long,
    var end : Long,
    var allDay : Boolean,
    var description : String,
    var color : String,
    var room : String,
    var sroom : String,
    var instructor : String,
    var remarks : String,
    var exam : Boolean,
    var userId : Int,
    var deprecated : Boolean = false
)

data class Infotext(
    var id : Int,
    var key : String,
    var description: String,
    var language : String
)

data class Notification(
    var id : Int,
    var type : String,
    var userId : Int,
    var versionId : Int?
)
data class NotificationTranslation(
    var id : Int,
    var type : String,
    var language : String,
    var label : String,
    var description : String
)


data class LectureChange(
    var id : Int,
    var notificationId : Int,
    var old : Int?,
    var new : Int?
)

data class Link(
    var id : Int,
    var label : String,
    var url : String,
    var universityId : Int,
    var groupId : Int,
    var language : String
)

data class ExamStat(
    var id : Int,
    var userId : Int,
    var booked : Int,
    var exams : Int,
    var failure : Int,
    var mbooked : Int,
    var modules : Int,
    var success : Int,
    var creditpoints : Int
)

data class AppVersion(
    var id : Int,
    var version : String,
    var timestamp : Long
)

data class AppChange(
    var id : Int,
    var appVersion : Int,
    var description: String,
    var path : String
)

data class LatestExamResult(
    var id : Int,
    var reminderId : Int,
    var grade: Double,
    var agrDate : Long,
    var status : String,
    var title : String,
    var shortTitle : String,
    var type : String
)

data class Reminder(
    var id : Int,
    var userId : Int,
    var semester : Int,
    var exams: Int,
    var electives : Int
)

data class Upcoming(
    var id : Int,
    var reminderId : Int,
    var begin : Long,
    var end : Long,
    var shortTitle : String,
    var title : String,
    var room : String,
    var instructor : String,
    var comment : String
)