package org.delcom.watchlist.helpers

import kotlinx.coroutines.Dispatchers
import org.delcom.watchlist.dao.AuthTokenDAO
import org.delcom.watchlist.dao.TodoDAO
import org.delcom.watchlist.dao.UserDAO
import org.delcom.watchlist.entities.AuthToken
import org.delcom.watchlist.entities.Todo
import org.delcom.watchlist.entities.User
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

fun authTokenDAOToModel(dao: AuthTokenDAO) = AuthToken(
    id           = dao.id.value.toString(),
    userId       = dao.userId.toString(),
    authToken    = dao.authToken,
    refreshToken = dao.refreshToken,
    createdAt    = dao.createdAt,
    expiresAt    = dao.expiresAt
)

fun todoDAOToModel(dao: TodoDAO) = Todo(
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
