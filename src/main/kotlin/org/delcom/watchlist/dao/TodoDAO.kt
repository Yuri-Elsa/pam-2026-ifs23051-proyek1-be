package org.delcom.watchlist.dao

import org.delcom.watchlist.tables.TodoTable
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.util.UUID

class TodoDAO(id: EntityID<UUID>) : Entity<UUID>(id) {
    companion object : EntityClass<UUID, TodoDAO>(TodoTable)

    var userId      by TodoTable.userId
    var title       by TodoTable.title
    var description by TodoTable.description
    var isDone      by TodoTable.isDone
    var urgency     by TodoTable.urgency
    var cover       by TodoTable.cover
    var createdAt   by TodoTable.createdAt
    var updatedAt   by TodoTable.updatedAt
}
