package de.ddkfm.plan4ba.models

data class UserInfo(
        var matriculationNumber : String,
        var group : String,
        var university : String,
        var hashStored : Boolean,
        var lastLecturePolling : Long,
        var lastLectureCall : Long
)

data class Login(
        var forename : String,
        var surename : String,
        var hash : String,
        var group : String,
        var course : String,
        var university : String
)