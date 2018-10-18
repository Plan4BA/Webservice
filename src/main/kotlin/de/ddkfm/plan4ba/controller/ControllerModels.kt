package de.ddkfm.plan4ba.controller

import de.ddkfm.plan4ba.models.Config
import de.ddkfm.plan4ba.models.User
import spark.Request
import spark.Response

open class ControllerInterface(
        var req : Request,
        var resp : Response,
        var config : Config = Config(),
        val user : User
) {
    init {
        config.buildFromEnv()
    }
}


