package org.delcom.helpers

/**
 * Membaca environment variable dari OS terlebih dahulu,
 * jika tidak ada maka fallback ke System Properties (yang di-set oleh dotenv-kotlin).
 * Ini diperlukan karena dotenv-kotlin menggunakan System.setProperty(),
 * bukan System.setenv(), sehingga tidak bisa dibaca langsung oleh System.getenv().
 */
fun getEnv(key: String, default: String = ""): String =
    System.getenv(key) ?: System.getProperty(key) ?: default