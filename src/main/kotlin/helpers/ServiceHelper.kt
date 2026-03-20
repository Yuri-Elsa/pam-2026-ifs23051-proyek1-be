package org.delcom.watchlist.helpers

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import org.delcom.watchlist.data.AppException
import org.delcom.watchlist.entities.User
import org.delcom.watchlist.repositories.IUserRepository

object ServiceHelper {
    suspend fun getAuthUser(call: ApplicationCall, userRepository: IUserRepository): User {
        val principal = call.principal<JWTPrincipal>()
            ?: throw AppException(401, "Unauthorized")

        val userId = principal
            .payload
            .getClaim("userId")
            .asString()
            ?: throw AppException(401, "Token tidak valid")

        return userRepository.getById(userId)
            ?: throw AppException(401, "User tidak valid")
    }
}