package org.delcom.helpers

import kotlinx.coroutines.Dispatchers
import org.delcom.dao.RefreshTokenDAO
import org.delcom.dao.WatchlistDAO
import org.delcom.dao.UserDAO
import org.delcom.entities.RefreshToken
import org.delcom.entities.Watchlist
import org.delcom.entities.User
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

suspend fun <T> suspendTransaction(block: Transaction.() -> T): T =
    newSuspendedTransaction(Dispatchers.IO, statement = block)

fun userDAOToModel(dao: UserDAO) = User(
    id        = dao.id.value.toString(),
    name      = dao.name,
    username  = dao.username,
    password  = dao.password,
    photo     = dao.photo,
    about     = dao.about,
    createdAt = dao.createdAt,
    updatedAt = dao.updatedAt
)

fun refreshTokenDAOToModel(dao: RefreshTokenDAO) = RefreshToken(
    id           = dao.id.value.toString(),
    userId       = dao.userId.toString(),
    authToken    = dao.authToken,
    refreshToken = dao.refreshToken,
    createdAt    = dao.createdAt,
    expiresAt    = dao.expiresAt
)

fun watchlistDAOToModel(dao: WatchlistDAO) = Watchlist(
    id          = dao.id.value.toString(),
    userId      = dao.userId.toString(),
    title       = dao.title,
    description = dao.description,
    isDone      = dao.isDone,
    urgency     = dao.urgency,
    cover       = dao.cover,
    createdAt   = dao.createdAt,
    updatedAt   = dao.updatedAt
)
