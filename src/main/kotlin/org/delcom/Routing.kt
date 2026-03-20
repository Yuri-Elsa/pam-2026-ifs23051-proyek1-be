package org.delcom

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.delcom.data.AppException
import org.delcom.data.ErrorResponse
import org.delcom.helpers.JWTConstants
import org.delcom.helpers.configureStaticFiles
import org.delcom.helpers.parseMessageToMap
import org.delcom.services.AuthService
import org.delcom.services.WatchlistService
import org.delcom.services.UserService
import org.koin.ktor.ext.inject

fun Application.configureRouting() {
    val authService: AuthService by inject()
    val userService: UserService by inject()
    val watchlistService: WatchlistService by inject()

    val uploadDir = System.getenv("UPLOAD_DIR") ?: "uploads"

    install(StatusPages) {
        exception<AppException> { call, cause ->
            val dataMap: Map<String, List<String>> = parseMessageToMap(cause.message)

            call.respond(
                status = HttpStatusCode.fromValue(cause.code),
                message = ErrorResponse(
                    status  = "fail",
                    message = if (dataMap.isEmpty()) cause.message else "Data yang dikirimkan tidak valid!",
                    data    = if (dataMap.isEmpty()) null else dataMap.toString()
                )
            )
        }

        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled error", cause)
            call.respond(
                status = HttpStatusCode.InternalServerError,
                message = ErrorResponse(
                    status  = "error",
                    message = cause.message ?: "Terjadi kesalahan pada server",
                    data    = null
                )
            )
        }
    }

    routing {
        get("/") {
            call.respond(
                mapOf(
                    "status"  to "ok",
                    "app"     to "WatchList API",
                    "version" to "1.0.0"
                )
            )
        }

        route("/auth") {
            post("/register") { authService.postRegister(call) }
            post("/login") { authService.postLogin(call) }
            post("/refresh-token") { authService.postRefreshToken(call) }
            post("/logout") { authService.postLogout(call) }
        }

        authenticate(JWTConstants.NAME) {
            route("/users") {
                get("/me") { userService.getMe(call) }
                put("/me") { userService.putMe(call) }
                put("/me/password") { userService.putMyPassword(call) }
                put("/me/photo") { userService.putMyPhoto(call) }
                put("/me/about") { userService.putMyAbout(call) }
            }

            route("/watchlists") {
                get("/stats") { watchlistService.getStats(call) }
                get { watchlistService.getAll(call) }
                post { watchlistService.post(call) }
                get("/{id}") { watchlistService.getById(call) }
                put("/{id}") { watchlistService.put(call) }
                put("/{id}/cover") { watchlistService.putCover(call) }
                delete("/{id}") { watchlistService.delete(call) }
            }
        }

        route("/images") {
            get("/users/{id}") { userService.getPhoto(call) }
            get("/watchlists/{id}") { watchlistService.getCover(call) }
        }

        configureStaticFiles(uploadDir)
    }
}
