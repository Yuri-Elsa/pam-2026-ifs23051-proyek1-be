package org.delcom.watchlist.services

import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable
import org.delcom.watchlist.data.AppException
import org.delcom.watchlist.data.DataResponse
import org.delcom.watchlist.data.TodoRequest
import org.delcom.watchlist.entities.Todo
import org.delcom.watchlist.helpers.*
import org.delcom.watchlist.repositories.ITodoRepository
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
data class TodosResponse(val todos: List<Todo>, val pagination: PaginationData)

@Serializable
data class TodoResponse(val todo: Todo)

@Serializable
data class TodoAddResponse(val todoId: String)

// ─────────────────────────────────────────────────────────────────────────────

class TodoService(
    private val userRepository: IUserRepository,
    private val todoRepository: ITodoRepository,
    private val uploadDir: String,
    private val uploadMaxSizeMb: Long,
) {

    // ── GET /todos/stats ──────────────────────────────────────────────────────
    suspend fun getStats(call: ApplicationCall) {
        val user = ServiceHelper.getAuthUser(call, userRepository)
        val (total, done, pending) = todoRepository.getStats(user.id)

        call.respond(
            DataResponse(
                status  = "success",
                message = "Berhasil mengambil statistik todo",
                data    = StatsResponse(StatsData(total, done, pending))
            )
        )
    }

    // ── GET /todos ────────────────────────────────────────────────────────────
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

        val (todos, total) = todoRepository.getAll(user.id, search, page, perPage, isDone, urgency)
        val totalPages = if (total == 0L) 1 else Math.ceil(total.toDouble() / perPage).toInt()

        call.respond(
            DataResponse(
                status  = "success",
                message = "Berhasil mengambil daftar todo",
                data    = TodosResponse(
                    todos      = todos,
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

    // ── POST /todos ───────────────────────────────────────────────────────────
    suspend fun post(call: ApplicationCall) {
        val user    = ServiceHelper.getAuthUser(call, userRepository)
        val request = call.receive<TodoRequest>()
        request.userId = user.id

        if (request.urgency !in listOf("low", "medium", "high")) request.urgency = "medium"

        val validator = ValidatorHelper(request.toMap())
        validator.required("title", "Judul tidak boleh kosong")
        validator.validate()

        val todoId = todoRepository.create(request.toEntity())

        call.respond(
            DataResponse(
                status  = "success",
                message = "Film berhasil ditambahkan",
                data    = TodoAddResponse(todoId)
            )
        )
    }

    // ── GET /todos/{id} ───────────────────────────────────────────────────────
    suspend fun getById(call: ApplicationCall) {
        val user   = ServiceHelper.getAuthUser(call, userRepository)
        val todoId = call.parameters["id"] ?: throw AppException(400, "ID todo tidak valid!")

        val todo = todoRepository.getById(todoId)
        if (todo == null || todo.userId != user.id) {
            throw AppException(404, "Film tidak ditemukan")
        }

        call.respond(DataResponse(status = "success", message = "Berhasil", data = TodoResponse(todo)))
    }

    // ── PUT /todos/{id} ───────────────────────────────────────────────────────
    suspend fun put(call: ApplicationCall) {
        val user    = ServiceHelper.getAuthUser(call, userRepository)
        val todoId  = call.parameters["id"] ?: throw AppException(400, "ID todo tidak valid!")
        val request = call.receive<TodoRequest>()
        request.userId = user.id

        if (request.urgency !in listOf("low", "medium", "high")) request.urgency = "medium"

        val validator = ValidatorHelper(request.toMap())
        validator.required("title", "Judul tidak boleh kosong")
        validator.validate()

        val oldTodo = todoRepository.getById(todoId)
        if (oldTodo == null || oldTodo.userId != user.id) {
            throw AppException(404, "Film tidak ditemukan")
        }

        // Pertahankan cover yang ada
        request.cover = oldTodo.cover

        if (!todoRepository.update(user.id, todoId, request.toEntity())) {
            throw AppException(400, "Gagal memperbarui film!")
        }

        call.respond(DataResponse<Unit>(status = "success", message = "Film berhasil diperbarui"))
    }

    // ── DELETE /todos/{id} ────────────────────────────────────────────────────
    suspend fun delete(call: ApplicationCall) {
        val user   = ServiceHelper.getAuthUser(call, userRepository)
        val todoId = call.parameters["id"] ?: throw AppException(400, "ID todo tidak valid!")

        val todo = todoRepository.getById(todoId)
        if (todo == null || todo.userId != user.id) {
            throw AppException(404, "Film tidak ditemukan")
        }

        if (!todoRepository.delete(user.id, todoId)) {
            throw AppException(400, "Gagal menghapus film!")
        }

        // Hapus file cover jika ada
        todo.cover?.let { deleteImageByPath(it) }

        call.respond(DataResponse<Unit>(status = "success", message = "Film berhasil dihapus"))
    }

    // ── PUT /todos/{id}/cover ─────────────────────────────────────────────────
    suspend fun putCover(call: ApplicationCall) {
        val user   = ServiceHelper.getAuthUser(call, userRepository)
        val todoId = call.parameters["id"] ?: throw AppException(400, "ID todo tidak valid!")

        val todo = todoRepository.getById(todoId)
        if (todo == null || todo.userId != user.id) {
            throw AppException(404, "Film tidak ditemukan")
        }

        var fileBytes: ByteArray?        = null
        var originalFileName: String?    = null

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
            subDir           = "todos",
            nameWithoutExt   = todoId,
            maxMb            = uploadMaxSizeMb
        )

        todoRepository.updateCover(user.id, todoId, newCoverPath)

        // Hapus cover lama jika berbeda
        if (todo.cover != null && todo.cover != newCoverPath) {
            deleteImageByPath(todo.cover!!)
        }

        call.respond(DataResponse<Unit>(status = "success", message = "Poster berhasil diperbarui"))
    }

    // ── GET /images/todos/{id} (public) ──────────────────────────────────────
    suspend fun getCover(call: ApplicationCall) {
        val todoId = call.parameters["id"] ?: throw AppException(400, "ID todo tidak valid!")

        val todo = todoRepository.getById(todoId)
            ?: throw AppException(404, "Film tidak ditemukan")

        val file = if (todo.cover != null) {
            java.io.File(todo.cover!!)
        } else {
            findImage(uploadDir, "todos", todoId)
        } ?: throw AppException(404, "Poster tidak tersedia")

        if (!file.exists()) throw AppException(404, "Poster tidak tersedia")

        call.respondFile(file)
    }
}
