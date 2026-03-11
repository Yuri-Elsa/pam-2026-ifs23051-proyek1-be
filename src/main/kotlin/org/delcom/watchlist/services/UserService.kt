package org.delcom.watchlist.services

import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import org.delcom.watchlist.data.AppException
import org.delcom.watchlist.data.AuthRequest
import org.delcom.watchlist.data.DataResponse
import org.delcom.watchlist.data.UserResponse
import org.delcom.watchlist.helpers.*
import org.delcom.watchlist.repositories.IAuthTokenRepository
import org.delcom.watchlist.repositories.IUserRepository

class UserService(
    private val userRepository: IUserRepository,
    private val authTokenRepository: IAuthTokenRepository,
    private val uploadDir: String,
    private val uploadMaxSizeMb: Long,
) {

    // ── GET /users/me ─────────────────────────────────────────────────────────
    suspend fun getMe(call: ApplicationCall) {
        val user = ServiceHelper.getAuthUser(call, userRepository)

        call.respond(
            DataResponse(
                status  = "success",
                message = "Berhasil mengambil informasi akun",
                data    = mapOf(
                    "user" to UserResponse(
                        id        = user.id,
                        name      = user.name,
                        username  = user.username,
                        about     = user.about,
                        createdAt = user.createdAt,
                        updatedAt = user.updatedAt,
                    )
                )
            )
        )
    }

    // ── PUT /users/me ─────────────────────────────────────────────────────────
    suspend fun putMe(call: ApplicationCall) {
        val user    = ServiceHelper.getAuthUser(call, userRepository)
        val request = call.receive<AuthRequest>()

        val validator = ValidatorHelper(request.toMap())
        validator.required("name",     "Nama tidak boleh kosong")
        validator.required("username", "Username tidak boleh kosong")
        validator.validate()

        val existUser = userRepository.getByUsername(request.username)
        if (existUser != null && existUser.id != user.id) {
            throw AppException(409, "Username sudah digunakan")
        }

        user.name     = request.name
        user.username = request.username

        if (!userRepository.update(user.id, user)) {
            throw AppException(400, "Gagal memperbarui profil!")
        }

        call.respond(DataResponse<Unit>(status = "success", message = "Profil berhasil diperbarui"))
    }

    // ── PUT /users/me/password ────────────────────────────────────────────────
    suspend fun putMyPassword(call: ApplicationCall) {
        val user    = ServiceHelper.getAuthUser(call, userRepository)
        val request = call.receive<AuthRequest>()

        val validator = ValidatorHelper(request.toMap())
        validator.required("password",    "Password lama tidak boleh kosong")
        validator.required("newPassword", "Password baru tidak boleh kosong")
        validator.minLength("newPassword", 6, "Password baru minimal 6 karakter")
        validator.validate()

        if (!verifyPassword(request.password, user.password)) {
            throw AppException(401, "Password lama tidak valid!")
        }

        user.password = hashPassword(request.newPassword)

        if (!userRepository.update(user.id, user)) {
            throw AppException(400, "Gagal mengubah password!")
        }

        // Invalidate semua token setelah ganti password
        authTokenRepository.deleteByUserId(user.id)

        call.respond(DataResponse<Unit>(status = "success", message = "Password berhasil diubah"))
    }

    // ── PUT /users/me/about ───────────────────────────────────────────────────
    suspend fun putMyAbout(call: ApplicationCall) {
        val user    = ServiceHelper.getAuthUser(call, userRepository)
        val request = call.receive<AuthRequest>()

        if (!userRepository.updateAbout(user.id, request.about ?: "")) {
            throw AppException(400, "Gagal memperbarui bio!")
        }

        call.respond(DataResponse<Unit>(status = "success", message = "Bio berhasil diperbarui"))
    }

    // ── PUT /users/me/photo ───────────────────────────────────────────────────
    suspend fun putMyPhoto(call: ApplicationCall) {
        val user = ServiceHelper.getAuthUser(call, userRepository)

        var newPhotoPath: String? = null
        var fileBytes: ByteArray? = null
        var originalFileName: String? = null

        call.receiveMultipart(formFieldLimit = uploadMaxSizeMb * 1024 * 1024).forEachPart { part ->
            if (part is PartData.FileItem && part.name == "file") {
                fileBytes        = part.streamProvider().readBytes()
                originalFileName = part.originalFileName
            }
            part.dispose()
        }

        val bytes = fileBytes ?: throw AppException(400, "File gambar tidak ditemukan")

        newPhotoPath = saveImage(
            bytes            = bytes,
            originalFileName = originalFileName,
            baseDir          = uploadDir,
            subDir           = "users",
            nameWithoutExt   = user.id,
            maxMb            = uploadMaxSizeMb
        )

        val oldPhoto = user.photo
        if (!userRepository.updatePhoto(user.id, newPhotoPath)) {
            throw AppException(400, "Gagal memperbarui foto profil!")
        }

        // Hapus foto lama jika berbeda path
        if (oldPhoto != null && oldPhoto != newPhotoPath) {
            deleteImageByPath(oldPhoto)
        }

        call.respond(DataResponse<Unit>(status = "success", message = "Foto profil berhasil diperbarui"))
    }

    // ── GET /images/users/{id} (public) ──────────────────────────────────────
    suspend fun getPhoto(call: ApplicationCall) {
        val userId = call.parameters["id"]
            ?: throw AppException(400, "userId diperlukan")

        val user = userRepository.getById(userId)
            ?: throw AppException(404, "User tidak ditemukan")

        val file = if (user.photo != null) {
            java.io.File(user.photo!!)
        } else {
            // Fallback: cari berdasarkan konvensi nama
            findImage(uploadDir, "users", userId)
        } ?: throw AppException(404, "Foto profil tidak tersedia")

        if (!file.exists()) throw AppException(404, "Foto profil tidak tersedia")

        call.respondFile(file)
    }
}
