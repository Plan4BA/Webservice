package de.ddkfm.plan4ba.utils

import com.mashape.unirest.http.Unirest
import de.ddkfm.plan4ba.config
import de.ddkfm.plan4ba.models.Infotext
import de.ddkfm.plan4ba.models.SimpleInfotext
import spark.Request
import spark.Response

fun info(req : Request, resp : Response) : Any {
    val key = req.queryParams("key")
    val filter = if(key.isNullOrEmpty()) "" else "?key=$key"
    val infoTexts = Unirest.get("${config.dbServiceEndpoint}/info$filter")
            .toModel(Infotext::class.java).second as List<Infotext>
    resp.type("application/json")
    return infoTexts.map { SimpleInfotext(it.key, it.description) }.toJson()
}