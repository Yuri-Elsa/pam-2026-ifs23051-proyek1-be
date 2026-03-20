package org.delcom.helpers

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.File

fun Routing.configureStaticFiles(uploadDir: String) {
    get("/uploads/{subDir}/{filename}") {
        val subDir   = call.parameters["subDir"]   ?: return@get call.respond(HttpStatusCode.BadRequest)
        val filename = call.parameters["filename"] ?: return@get call.respond(HttpStatusCode.BadRequest)

        val file = File("$uploadDir/$subDir/$filename")
        if (!file.exists() || !file.isFile) {
            call.respond(HttpStatusCode.NotFound)
            return@get
        }

        call.respondFile(file)
    }
}
