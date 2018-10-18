package de.ddkfm.plan4ba.models

import de.ddkfm.plan4ba.utils.getEnvOrDefault
data class Config(
        var dbServiceEndpoint : String = "http://localhost:8080",
        var loginServiceEndpoint : String = "http://localhost:8082"
) {
    fun buildFromEnv() {
        this.dbServiceEndpoint = getEnvOrDefault("DBSERVICE_ENDPOINT", this.dbServiceEndpoint)
        this.loginServiceEndpoint = getEnvOrDefault("LOGINSERVICE_ENDPOINT", this.loginServiceEndpoint)
    }
}