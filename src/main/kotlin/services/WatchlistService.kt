package org.delcom.watchlist.services

import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable
import org.delcom.watchlist.data.AppException
import org.delcom.watchlist.data.DataResponse
import org.delcom.watchlist.data.WatchlistRequest
import org.delcom.watchlist.entities.Watchlist
import org.delcom.watchlist.helpers.*
import org.delcom.watchlist.repositories.IWatchlistRepository
import org.delcom.watchlist.repositories.IUserRepository

// ── Response wrappers ─────────────────────────────────────────────────────────

@Serializable
data class StatsData(val total: Long, val done: Long, val pending: Long)

@Serializable
data class StatsResponse(val stats: StatsData)

@Serializable
data class PaginationData(
    val currentPage: Int,
    val perPage: Int,
    val total: Long,
    val totalPages: Int,
    val hasNextPage: Boolean,
    val hasPrevPage: Boolean,
)

@Serializable
data class WatchlistsResponse(val watchlists: List<Watchlist>, val pagination: PaginationData)

@Serializable
data class WatchlistResponse(val watchlist: Watchlist)

@Serializable
data class WatchlistAddResponse(val watchlistId: String)

// ─────────────────────────────────────────────────────────────────────────────

class WatchlistService(
    private val userRepository: IUserRepository,
    private val watchlistRepository: IWatchlistRepository,
    private val uploadDir: String,
    private val uploadMaxSizeMb: Long,
) {

    // ── GET /watchlists/stats ──────────────────────────────────────────────────────
    suspend fun getStats(call: ApplicationCall) {
        val user = ServiceHelper.getAuthUser(call, userRepository)
        val (total, done, pending) = watchlistRepository.getStats(user.id)

        call.respond(
            DataResponse(
                status  = "success",
                message = "Berhasil mengambil statistik watchlist",
                data    = StatsResponse(StatsData(total, done, pending))
            )
        )
    }

    // ── GET /watchlists ────────────────────────────────────────────────────────────
    suspend fun getAll(call: ApplicationCall) {
        val user    = ServiceHelper.getAuthUser(call, userRepository)
        val params  = call.request.queryParameters

        val search  = params["search"] ?: ""
        val page    = params["page"]?.toIntOrNull() ?: 1
        val perPage = params["perPage"]?.toIntOrNull()?.coerceIn(1, 100) ?: 10
        val isDone: Boolean? = when (params["isDone"]?.lowercase()) {
            "true"  -> true
            "false" -> false
            else    -> null
        }
        val urgency = params["urgency"]

        val (watchlists, total) = watchlistRepository.getAll(user.id, search, page, perPage, isDone, urgency)
        val totalPages = if (total == 0L) 1 else Math.ceil(total.toDouble() / perPage).toInt()

        call.respond(
            DataResponse(
                status  = "success",
                message = "Berhasil mengambil daftar watchlist",
                data    = WatchlistsResponse(
                    watchlists = watchlists,
                    pagination = PaginationData(
                        currentPage = page,
                        perPage     = perPage,
                        total       = total,
                        totalPages  = totalPages,
                        hasNextPage = page < totalPages,
                        hasPrevPage = page > 1
                    )
                )
            )
        )
    }

    // ── POST /watchlists ───────────────────────────────────────────────────────────
    suspend fun post(call: ApplicationCall) {
        val user    = ServiceHelper.getAuthUser(call, userRepository)
        val request = call.receive<WatchlistRequest>()
        request.userId = user.id

        if (request.urgency !in listOf("low", "medium", "high")) request.urgency = "medium"

        val validator = ValidatorHelper(request.toMap())
        validator.required("title", "Judul tidak boleh kosong")
        validator.validate()

        val watchlistId = watchlistRepository.create(request.toEntity())

        call.respond(
            DataResponse(
                status  = "success",
                message = "Film berhasil ditambahkan",
                data    = WatchlistAddResponse(watchlistId)
            )
        )
    }

    // ── GET /watchlists/{id} ───────────────────────────────────────────────────────
    suspend fun getById(call: ApplicationCall) {
        val user   = ServiceHelper.getAuthUser(call, userRepository)
        val watchlistId = call.parameters["id"] ?: throw AppException(400, "ID watchlist tidak valid!")

        val watchlist = watchlistRepository.getById(watchlistId)
        if (watchlist == null || watchlist.userId != user.id) {
            throw AppException(404, "Film tidak ditemukan")
        }

        call.respond(DataResponse(status = "success", message = "Berhasil", data = WatchlistResponse(watchlist)))
    }

    // ── PUT /watchlists/{id} ───────────────────────────────────────────────────────
    suspend fun put(call: ApplicationCall) {
        val user    = ServiceHelper.getAuthUser(call, userRepository)
        val watchlistId  = call.parameters["id"] ?: throw AppException(400, "ID watchlist tidak valid!")
        val request = call.receive<WatchlistRequest>()
        request.userId = user.id

        if (request.urgency !in listOf("low", "medium", "high")) request.urgency = "medium"

        val validator = ValidatorHelper(request.toMap())
        validator.required("title", "Judul tidak boleh kosong")
        validator.validate()

        val oldWatchlist = watchlistRepository.getById(watchlistId)
        if (oldWatchlist == null || oldWatchlist.userId != user.id) {
            throw AppException(404, "Film tidak ditemukan")
        }

        // Pertahankan cover yang ada
        request.cover = oldWatchlist.cover

        if (!watchlistRepository.update(user.id, watchlistId, request.toEntity())) {
            throw AppException(400, "Gagal memperbarui film!")
        }

        call.respond(DataResponse<Unit>(status = "success", message = "Film berhasil diperbarui"))
    }

    // ── DELETE /watchlists/{id} ────────────────────────────────────────────────────
    suspend fun delete(call: ApplicationCall) {
        val user   = ServiceHelper.getAuthUser(call, userRepository)
        val watchlistId = call.parameters["id"] ?: throw AppException(400, "ID watchlist tidak valid!")

        val watchlist = watchlistRepository.getById(watchlistId)
        if (watchlist == null || watchlist.userId != user.id) {
            throw AppException(404, "Film tidak ditemukan")
        }

        if (!watchlistRepository.delete(user.id, watchlistId)) {
            throw AppException(400, "Gagal menghapus film!")
        }

        // Hapus file cover jika ada
        watchlist.cover?.let { deleteImageByPath(it) }

        call.respond(DataResponse<Unit>(status = "success", message = "Film berhasil dihapus"))
    }

    // ── PUT /watchlists/{id}/cover ─────────────────────────────────────────────────
    suspend fun putCover(call: ApplicationCall) {
        val user   = ServiceHelper.getAuthUser(call, userRepository)
        val watchlistId = call.parameters["id"] ?: throw AppException(400, "ID watchlist tidak valid!")

        val watchlist = watchlistRepository.getById(watchlistId)
        if (watchlist == null || watchlist.userId != user.id) {
            throw AppException(404, "Film tidak ditemukan")
        }

        var fileBytes: ByteArray?     = null
        var originalFileName: String? = null

        call.receiveMultipart(formFieldLimit = uploadMaxSizeMb * 1024 * 1024).forEachPart { part ->
            if (part is PartData.FileItem && part.name == "file") {
                fileBytes        = part.streamProvider().readBytes()
                originalFileName = part.originalFileName
            }
            part.dispose()
        }

        val bytes = fileBytes ?: throw AppException(400, "File gambar tidak ditemukan")

        val newCoverPath = saveImage(
            bytes            = bytes,
            originalFileName = originalFileName,
            baseDir          = uploadDir,
            subDir           = "watchlists",
            nameWithoutExt   = watchlistId,
            maxMb            = uploadMaxSizeMb
        )

        watchlistRepository.updateCover(user.id, watchlistId, newCoverPath)

        // Hapus cover lama jika berbeda
        if (watchlist.cover != null && watchlist.cover != newCoverPath) {
            deleteImageByPath(watchlist.cover!!)
        }

        call.respond(DataResponse<Unit>(status = "success", message = "Poster berhasil diperbarui"))
    }

    // ── GET /images/watchlists/{id} (public) ──────────────────────────────────────
    suspend fun getCover(call: ApplicationCall) {
        val watchlistId = call.parameters["id"] ?: throw AppException(400, "ID watchlist tidak valid!")

        val watchlist = watchlistRepository.getById(watchlistId)
            ?: throw AppException(404, "Film tidak ditemukan")

        val file = if (watchlist.cover != null) {
            java.io.File(watchlist.cover!!)
        } else {
            findImage(uploadDir, "watchlists", watchlistId)
        } ?: throw AppException(404, "Poster tidak tersedia")

        if (!file.exists()) throw AppException(404, "Poster tidak tersedia")

        call.respondFile(file)
    }
}