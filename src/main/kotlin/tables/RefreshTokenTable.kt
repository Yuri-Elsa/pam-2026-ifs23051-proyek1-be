package org.delcom.watchlist.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object RefreshTokenTable : UUIDTable("auth_tokens") {
    val userId       = uuid("user_id")
    val authToken    = text("auth_token").uniqueIndex()
    val refreshToken = text("refresh_token").uniqueIndex()
    val createdAt    = timestamp("created_at")
    val expiresAt    = timestamp("expires_at")
}