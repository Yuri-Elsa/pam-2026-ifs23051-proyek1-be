package org.delcom.watchlist.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object WatchlistTable : UUIDTable("watchlists") {
    val userId      = uuid("user_id")
    val title       = varchar("title", 255)
    val description = text("description")
    val isDone      = bool("is_done").default(false)
    val urgency     = varchar("urgency", 10).default("medium")
    val cover       = text("cover").nullable()
    val createdAt   = timestamp("created_at")
    val updatedAt   = timestamp("updated_at")
}