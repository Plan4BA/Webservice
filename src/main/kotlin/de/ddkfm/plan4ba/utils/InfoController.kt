package de.ddkfm.plan4ba.utils

import de.ddkfm.plan4ba.models.Infotext
import de.ddkfm.plan4ba.models.SimpleInfotext
import spark.Request
import spark.Response

fun info(req : Request, resp : Response) : Any {
    var language = req.queryParams("language")
    if(language.isNullOrEmpty())
        language = "de"

    val key = req.queryParams("key")
    val filter = if(key.isNullOrEmpty())
        arrayOf<Pair<String, Any>>()
    else
        arrayOf("key" to key)

    val infoTexts = DBService.all<Infotext>(*filter).getOrThrow().filter { it.language == language }
    resp.type("application/json")
    return infoTexts.map { SimpleInfotext(it.key, it.description, it.language) }.toJson()
}