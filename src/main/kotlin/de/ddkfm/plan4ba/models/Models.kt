package de.ddkfm.plan4ba.models

data class UserInfo(
        var matriculationNumber : String,
        var group : String,
        var university : String,
        var hashStored : Boolean,
        var lastLecturePolling : Long,
        var lastLectureCall : Long
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

data class SimpleNotification(
        var id : Int,
        var label : String,
        var description : String,
        var type : String
        /*var viewed : Boolean,
        var data : NotificationData?*/
)

data class SimpleLink(
        var id : Int,
        var label : String,
        var url : String,
        var language : String
)