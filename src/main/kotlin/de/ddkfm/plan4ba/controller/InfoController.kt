package de.ddkfm.plan4ba.controller

import de.ddkfm.plan4ba.models.Infotext
import de.ddkfm.plan4ba.models.SimpleInfotext
import de.ddkfm.plan4ba.utils.DBService
import de.ddkfm.plan4ba.utils.toJson
import spark.Request
import spark.Response
import javax.ws.rs.Path
import javax.ws.rs.QueryParam

@Path("/info")

class InfoController(val req : Request, val resp : Response) {
    fun info(@QueryParam("language") language : String, @QueryParam("key") key : String) : List<SimpleInfotext> {
        val filter = if(key.isNullOrEmpty())
            arrayOf<Pair<String, Any>>()
        else
            arrayOf("key" to key)
        val infoTexts = DBService.all<Infotext>(*filter).getOrThrow().filter { it.language == language }
        resp.type("application/json")
        return infoTexts.map { SimpleInfotext(it.key, it.description, it.language) }
    }
}