package de.ddkfm.plan4ba.utils

import com.mashape.unirest.http.Unirest
import de.ddkfm.plan4ba.config
import de.ddkfm.plan4ba.models.Infotext
import de.ddkfm.plan4ba.models.SimpleInfotext
import spark.Request
import spark.Response

fun info(req : Request, resp : Response) : Any {
    var language = req.queryParams("language")
    if(language.isNullOrEmpty())
        language = "de"

    val key = req.queryParams("key")
    val filter = if(key.isNullOrEmpty()) "" else "?key=$key"
    var infoTexts = (Unirest.get("${config.dbServiceEndpoint}/info$filter")
            .toModel(Infotext::class.java).second as List<Infotext>)
            .filter { it.language == language }
    resp.type("application/json")
    return infoTexts.map { SimpleInfotext(it.key, it.description, it.language) }.toJson()
}