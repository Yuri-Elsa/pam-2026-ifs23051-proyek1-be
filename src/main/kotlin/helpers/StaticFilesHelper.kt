package org.delcom.watchlist.helpers

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.File

/**
 * Konfigurasi route publik untuk melayani file yang diunggah (foto, cover, dll).
 * Dipanggil sekali dari Routing.kt agar semua static-file route terpusat di sini.
 */
fun Routing.configureStaticFiles(uploadDir: String) {
    // GET /uploads/{subDir}/{filename}  →  serve file langsung
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