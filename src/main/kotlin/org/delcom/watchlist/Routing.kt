package org.delcom.watchlist

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.delcom.watchlist.data.AppException
import org.delcom.watchlist.data.ErrorResponse
import org.delcom.watchlist.helpers.JWTConstants
import org.delcom.watchlist.helpers.parseMessageToMap
import org.delcom.watchlist.services.AuthService
import org.delcom.watchlist.services.TodoService
import org.delcom.watchlist.services.UserService
import org.koin.ktor.ext.inject

fun Application.configureRouting() {
    val authService: AuthService by inject()
    val userService: UserService by inject()
    val todoService: TodoService by inject()

    install(StatusPages) {
        exception<AppException> { call, cause ->
            val dataMap: Map<String, List<String>> = parseMessageToMap(cause.message)

            call.respond(
                status = HttpStatusCode.fromValue(cause.code),
                message = ErrorResponse(
                    status = "fail",
                    message = if (dataMap.isEmpty()) cause.message else "Data yang dikirimkan tidak valid!",
                    data = if (dataMap.isEmpty()) null else dataMap.toString()
                )
            )
        }

        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled error", cause)
            call.respond(
                status = HttpStatusCode.InternalServerError,
                message = ErrorResponse(
                    status = "error",
                    message = cause.message ?: "Terjadi kesalahan pada server",
                    data = null
                )
            )
        }
    }

    routing {
        get("/") {
            call.respond(
                mapOf(
                    "status" to "ok",
                    "app" to "WatchList API",
                    "version" to "1.0.0"
                )
            )
        }

        // ── Auth ──────────────────────────────────────────────────────────────
        route("/auth") {
            post("/register") {
                authService.postRegister(call)
            }
            post("/login") {
                authService.postLogin(call)
            }
            post("/refresh-token") {
                authService.postRefreshToken(call)
            }
            post("/logout") {
                authService.postLogout(call)
            }
        }

        authenticate(JWTConstants.NAME) {
            // ── Users ─────────────────────────────────────────────────────────
            route("/users") {
                get("/me") {
                    userService.getMe(call)
                }
                put("/me") {
                    userService.putMe(call)
                }
                put("/me/password") {
                    userService.putMyPassword(call)
                }
                put("/me/photo") {
                    userService.putMyPhoto(call)
                }
                put("/me/about") {
                    userService.putMyAbout(call)
                }
            }

            // ── Todos (Film) ──────────────────────────────────────────────────
            route("/todos") {
                get("/stats") {
                    todoService.getStats(call)
                }
                get {
                    todoService.getAll(call)
                }
                post {
                    todoService.post(call)
                }
                get("/{id}") {
                    todoService.getById(call)
                }
                put("/{id}") {
                    todoService.put(call)
                }
                put("/{id}/cover") {
                    todoService.putCover(call)
                }
                delete("/{id}") {
                    todoService.delete(call)
                }
            }
        }

        // ── Public image endpoints ─────────────────────────────────────────
        route("/images") {
            get("/users/{id}") {
                userService.getPhoto(call)
            }
            get("/todos/{id}") {
                todoService.getCover(call)
            }
        }
    }
}
