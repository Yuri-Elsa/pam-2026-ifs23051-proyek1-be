package org.delcom.watchlist.dao

import org.delcom.watchlist.tables.WatchlistTable
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.util.UUID

class WatchlistDAO(id: EntityID<UUID>) : Entity<UUID>(id) {
    companion object : EntityClass<UUID, WatchlistDAO>(WatchlistTable)

    var userId      by WatchlistTable.userId
    var title       by WatchlistTable.title
    var description by WatchlistTable.description
    var isDone      by WatchlistTable.isDone
    var urgency     by WatchlistTable.urgency
    var cover       by WatchlistTable.cover
    var createdAt   by WatchlistTable.createdAt
    var updatedAt   by WatchlistTable.updatedAt
}