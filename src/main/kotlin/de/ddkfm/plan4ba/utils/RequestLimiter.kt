package de.ddkfm.plan4ba.utils

import de.ddkfm.plan4ba.models.User
import java.time.LocalDateTime
import java.time.LocalTime

object RequestLimiter {
    val lock : MutableMap<Int, Int> = mutableMapOf()
    fun hasLock(user : User) : Boolean {
        val contains = lock.containsKey(user.id)
        if(contains)
            return lock[user.id] == LocalTime.now().hour
        else
            return false
    }
    fun addLock(user : User) {
        lock[user.id] = LocalDateTime.now().hour
    }
}