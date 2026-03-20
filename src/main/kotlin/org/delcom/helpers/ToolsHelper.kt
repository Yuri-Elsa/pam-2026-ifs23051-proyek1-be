package org.delcom.helpers

import org.mindrot.jbcrypt.BCrypt
import java.io.File

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

fun hashPassword(password: String): String =
    BCrypt.hashpw(password, BCrypt.gensalt(12))

fun verifyPassword(password: String, hashed: String): Boolean =
    BCrypt.checkpw(password, hashed)

private val ALLOWED_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp")

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

    ALLOWED_EXTENSIONS.forEach {
        File(dir, "$nameWithoutExt.$it").takeIf { f -> f.exists() }?.delete()
    }

    val filePath = "$baseDir/$subDir/$nameWithoutExt.$ext"
    File(filePath).writeBytes(bytes)
    return filePath
}

fun findImage(baseDir: String, subDir: String, name: String): File? =
    ALLOWED_EXTENSIONS
        .map { File("$baseDir/$subDir/$name.$it") }
        .firstOrNull { it.exists() }

fun deleteImageByPath(path: String) {
    val file = File(path)
    if (file.exists()) file.delete()
}
