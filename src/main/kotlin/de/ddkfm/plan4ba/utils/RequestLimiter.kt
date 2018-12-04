package de.ddkfm.plan4ba.utils

import de.ddkfm.plan4ba.models.User
import java.time.LocalDateTime
import java.time.LocalTime

object RequestLimiter {
    val lock : MutableMap<User, Int> = mutableMapOf()
    fun hasLock(user : User) : Boolean {
        val contains = lock.containsKey(user)
        if(contains)
            return lock[user] == LocalTime.now().hour
        else
            return false
    }
    fun addLock(user : User) {
        lock[user] = LocalDateTime.now().hour
    }
}