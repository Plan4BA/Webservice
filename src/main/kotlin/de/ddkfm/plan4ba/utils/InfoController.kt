package de.ddkfm.plan4ba.utils

import com.mashape.unirest.http.Unirest
import de.ddkfm.plan4ba.config
import de.ddkfm.plan4ba.models.Infotext
import de.ddkfm.plan4ba.models.SimpleInfotext
import spark.Request
import spark.Response

fun info(req : Request, resp : Response) : Any {
    val language = req.queryParams("language")
    val key = req.queryParams("key")
    val filter = if(key.isNullOrEmpty()) "" else "?key=$key"
    var infoTexts = Unirest.get("${config.dbServiceEndpoint}/info$filter")
            .toModel(Infotext::class.java).second as List<Infotext>
    if(language?.isNotEmpty() == true)
        infoTexts = infoTexts.filter { it.language == language }
    resp.type("application/json")
    return infoTexts.map { SimpleInfotext(it.key, it.description, it.language) }.toJson()
}