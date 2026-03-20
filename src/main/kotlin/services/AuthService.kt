package org.delcom.watchlist.services

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.datetime.Clock
import org.delcom.watchlist.data.AppException
import org.delcom.watchlist.data.AuthRequest
import org.delcom.watchlist.data.DataResponse
import org.delcom.watchlist.data.RefreshTokenRequest
import org.delcom.watchlist.entities.RefreshToken
import org.delcom.watchlist.helpers.JWTConstants
import org.delcom.watchlist.helpers.ValidatorHelper
import org.delcom.watchlist.helpers.hashPassword
import org.delcom.watchlist.helpers.verifyPassword
import org.delcom.watchlist.repositories.IRefreshTokenRepository
import org.delcom.watchlist.repositories.IUserRepository
import java.util.Date
import java.util.UUID

class AuthService(
    private val jwtSecret: String,
    private val userRepository: IUserRepository,
    private val refreshTokenRepository: IRefreshTokenRepository,
) {

    private fun generateAuthToken(userId: String): String {
        if (jwtSecret.isBlank()) throw AppException(500, "JWT secret tidak dikonfigurasi!")
        return JWT.create()
            .withAudience(JWTConstants.AUDIENCE)
            .withIssuer(JWTConstants.ISSUER)
            .withClaim("userId", userId)
            .withExpiresAt(Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L)) // 7 hari
            .sign(Algorithm.HMAC256(jwtSecret))
    }

    // ── Register ──────────────────────────────────────────────────────────────
    suspend fun postRegister(call: ApplicationCall) {
        val request = call.receive<AuthRequest>()

        val validator = ValidatorHelper(request.toMap())
        validator.required("name",     "Nama tidak boleh kosong")
        validator.required("username", "Username tidak boleh kosong")
        validator.required("password", "Password tidak boleh kosong")
        validator.minLength("password", 6, "Password minimal 6 karakter")
        validator.validate()

        if (userRepository.getByUsername(request.username) != null) {
            throw AppException(409, "Username sudah digunakan")
        }

        request.password = hashPassword(request.password)
        val userId = userRepository.create(request.toEntity())

        call.respond(
            DataResponse(
                status  = "success",
                message = "Registrasi berhasil",
                data    = mapOf("userId" to userId)
            )
        )
    }

    // ── Login ─────────────────────────────────────────────────────────────────
    suspend fun postLogin(call: ApplicationCall) {
        val request = call.receive<AuthRequest>()

        val validator = ValidatorHelper(request.toMap())
        validator.required("username", "Username tidak boleh kosong")
        validator.required("password", "Password tidak boleh kosong")
        validator.validate()

        val existUser = userRepository.getByUsername(request.username)
            ?: throw AppException(401, "Username atau password salah")

        if (!verifyPassword(request.password, existUser.password)) {
            throw AppException(401, "Username atau password salah")
        }

        // Hapus token lama milik user ini
        refreshTokenRepository.deleteByUserId(existUser.id)

        val authToken    = generateAuthToken(existUser.id)
        val refreshToken = UUID.randomUUID().toString()

        refreshTokenRepository.create(
            RefreshToken(
                userId       = existUser.id,
                authToken    = authToken,
                refreshToken = refreshToken,
                expiresAt    = Clock.System.now().plus(kotlin.time.Duration.parse("720h"))
            )
        )

        call.respond(
            DataResponse(
                status  = "success",
                message = "Login berhasil",
                data    = mapOf(
                    "authToken"    to authToken,
                    "refreshToken" to refreshToken
                )
            )
        )
    }

    // ── Refresh Token ─────────────────────────────────────────────────────────
    suspend fun postRefreshToken(call: ApplicationCall) {
        val request = call.receive<RefreshTokenRequest>()

        val validator = ValidatorHelper(request.toMap())
        validator.required("authToken",    "Auth token tidak boleh kosong")
        validator.required("refreshToken", "Refresh token tidak boleh kosong")
        validator.validate()

        val existing = refreshTokenRepository.getByAuthAndRefreshToken(
            request.authToken, request.refreshToken
        ) ?: throw AppException(401, "Token tidak valid!")

        // Hapus token lama
        refreshTokenRepository.deleteByAuthToken(request.authToken)

        val user = userRepository.getById(existing.userId)
            ?: throw AppException(401, "User tidak valid!")

        val newAuthToken    = generateAuthToken(user.id)
        val newRefreshToken = UUID.randomUUID().toString()

        refreshTokenRepository.create(
            RefreshToken(
                userId       = user.id,
                authToken    = newAuthToken,
                refreshToken = newRefreshToken,
                expiresAt    = Clock.System.now().plus(kotlin.time.Duration.parse("720h"))
            )
        )

        call.respond(
            DataResponse(
                status  = "success",
                message = "Token berhasil diperbarui",
                data    = mapOf(
                    "authToken"    to newAuthToken,
                    "refreshToken" to newRefreshToken
                )
            )
        )
    }

    // ── Logout ────────────────────────────────────────────────────────────────
    suspend fun postLogout(call: ApplicationCall) {
        val request = call.receive<RefreshTokenRequest>()

        val validator = ValidatorHelper(request.toMap())
        validator.required("authToken", "Auth token tidak boleh kosong")
        validator.validate()

        try {
            val decoded = JWT.require(Algorithm.HMAC256(jwtSecret)).build().verify(request.authToken)
            val userId  = decoded.getClaim("userId").asString()
            if (!userId.isNullOrBlank()) {
                refreshTokenRepository.deleteByUserId(userId)
            }
        } catch (e: JWTVerificationException) {
            // Token expired/invalid — tetap lanjut hapus dari DB
        } catch (e: Exception) {
            // Abaikan error lain
        }

        refreshTokenRepository.deleteByAuthToken(request.authToken)

        call.respond(
            DataResponse<Unit>(
                status  = "success",
                message = "Logout berhasil"
            )
        )
    }
}