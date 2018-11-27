package de.ddkfm.plan4ba.models

import de.ddkfm.plan4ba.utils.getEnvOrDefault
data class Config(
        var dbServiceEndpoint : String = "http://localhost:8080",
        var loginServiceEndpoint : String = "http://localhost:8082",
        var cachingServiceEndpoint : String = "http://localhost:8083",
        var refreshTokenInterval : Long = 365 * 24 * 3600 * 1000L,
        var shortTokenInterval : Long = 10 * 60 * 1000,
        var caldavTokenInterval : Long = 365 * 24 * 3600 * 1000L
) {
    fun buildFromEnv() {
        this.dbServiceEndpoint = getEnvOrDefault("DBSERVICE_ENDPOINT", this.dbServiceEndpoint)
        this.loginServiceEndpoint = getEnvOrDefault("LOGINSERVICE_ENDPOINT", this.loginServiceEndpoint)
        this.cachingServiceEndpoint = getEnvOrDefault("CACHINGSERVICE_ENDPOINT", this.cachingServiceEndpoint)
        this.caldavTokenInterval = getEnvOrDefault("CALDAVTOKEN_INTERVAL", this.caldavTokenInterval.toString()).toLong()
        this.refreshTokenInterval = getEnvOrDefault("REFRESHTOKEN_INTERVAL", this.refreshTokenInterval.toString()).toLong()
        this.shortTokenInterval = getEnvOrDefault("SHORTTOKEN_INTERVAL", this.shortTokenInterval.toString()).toLong()
    }
}