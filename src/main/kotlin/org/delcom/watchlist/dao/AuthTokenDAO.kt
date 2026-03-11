package org.delcom.watchlist.dao

import org.delcom.watchlist.tables.AuthTokenTable
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.util.UUID

class AuthTokenDAO(id: EntityID<UUID>) : Entity<UUID>(id) {
    companion object : EntityClass<UUID, AuthTokenDAO>(AuthTokenTable)

    var userId       by AuthTokenTable.userId
    var authToken    by AuthTokenTable.authToken
    var refreshToken by AuthTokenTable.refreshToken
    var createdAt    by AuthTokenTable.createdAt
    var expiresAt    by AuthTokenTable.expiresAt
}
