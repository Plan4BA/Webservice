package de.ddkfm.plan4ba.models

data class UserInfo(
        var matriculationNumber : String,
        var group : String,
        var university : String,
        var hashStored : Boolean,
        var lastLecturePolling : Long,
        var lastLectureCall : Long,
        var storeExamsStats : Boolean,
        var storeReminders : Boolean
)

data class UniversityInfo(
        var name : String,
        var accentColor : String,
        var logoUrl : String
)

data class Login(
        var forename : String,
        var surename : String,
        var hash : String,
        var group : String,
        var course : String,
        var university : String
)

data class LectureJob(
        var userId: Int,
        var hash: String,
        var matriculationNumber: String
)

data class Authentication(
        var username : String,
        var password : String
)
data class SimpleInfotext(
        var key : String,
        var description : String,
        var language : String
)

enum class NotificationType(val value : String) {
    LECTURE_CHANGE("lectureChanged"),
    APP_CHANGE("appChanged")
}
data class SimpleNotification(
        var id : Int,
        var label : String,
        var description : String,
        var type : String,
        var callback : String
)

data class SimpleLink(
        var id : Int,
        var label : String,
        var url : String,
        var language : String
)
data class SimpleLectureChange(
    var id : Int,
    var notificationId : Int,
    var old : Lecture?,
    var new : Lecture?
)

data class SimpleAppVersion(
    var id : Int,
    var version : String,
    var timestamp : Long,
    var changes : List<SimpleAppChange>
)

data class SimpleAppChange(
    var id : Int,
    var description: String,
    var path : String
)

data class SimpleReminder(
    var semester : Int,
    var exams: Int,
    var electives : Int,
    var latest : List<LatestExamResult>,
    var upcoming : List<Upcoming>
)
