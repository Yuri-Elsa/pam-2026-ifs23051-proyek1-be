package org.delcom.watchlist.helpers

import org.mindrot.jbcrypt.BCrypt
import java.io.File

// ── Validation ────────────────────────────────────────────────────────────────

fun parseMessageToMap(rawMessage: String): Map<String, List<String>> {
    return rawMessage.split("|").mapNotNull { part ->
        val split = part.split(":", limit = 2)
        if (split.size == 2) {
            val key   = split[0].trim()
            val value = split[1].trim()
            key to listOf(value)
        } else null
    }.toMap()
}

// ── Password ──────────────────────────────────────────────────────────────────

fun hashPassword(password: String): String =
    BCrypt.hashpw(password, BCrypt.gensalt(12))

fun verifyPassword(password: String, hashed: String): Boolean =
    BCrypt.checkpw(password, hashed)

// ── Image / file ──────────────────────────────────────────────────────────────

private val ALLOWED_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp")

/**
 * Simpan file gambar dari multipart ke disk.
 * Mengembalikan path relatif file yang disimpan.
 */
fun saveImage(
    bytes: ByteArray,
    originalFileName: String?,
    baseDir: String,
    subDir: String,
    nameWithoutExt: String,
    maxMb: Long = 5L
): String {
    val ext = (originalFileName ?: "file")
        .substringAfterLast('.', "jpg")
        .lowercase()

    if (ext !in ALLOWED_EXTENSIONS)
        throw IllegalArgumentException("Format tidak didukung: $ext. Gunakan: ${ALLOWED_EXTENSIONS.joinToString()}")

    if (bytes.size > maxMb * 1024 * 1024)
        throw IllegalArgumentException("File terlalu besar. Maksimum ${maxMb}MB.")

    val dir = File("$baseDir/$subDir").also { it.mkdirs() }

    // Hapus file lama dengan nama yang sama (ekstensi berbeda)
    ALLOWED_EXTENSIONS.forEach {
        File(dir, "$nameWithoutExt.$it").takeIf { f -> f.exists() }?.delete()
    }

    val filePath = "$baseDir/$subDir/$nameWithoutExt.$ext"
    File(filePath).writeBytes(bytes)
    return filePath
}

/**
 * Cari file gambar berdasarkan nama (tanpa ekstensi).
 */
fun findImage(baseDir: String, subDir: String, name: String): File? =
    ALLOWED_EXTENSIONS
        .map { File("$baseDir/$subDir/$name.$it") }
        .firstOrNull { it.exists() }

/**
 * Hapus file gambar berdasarkan path.
 */
fun deleteImageByPath(path: String) {
    val file = File(path)
    if (file.exists()) file.delete()
}