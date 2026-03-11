package org.delcom.watchlist.repositories

import org.delcom.watchlist.dao.TodoDAO
import org.delcom.watchlist.entities.Todo
import org.delcom.watchlist.helpers.suspendTransaction
import org.delcom.watchlist.helpers.todoDAOToModel
import org.delcom.watchlist.tables.TodoTable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.util.UUID

class TodoRepository : ITodoRepository {

    override suspend fun getAll(
        userId: String,
        search: String,
        page: Int,
        perPage: Int,
        isDone: Boolean?,
        urgency: String?
    ): Pair<List<Todo>, Long> = suspendTransaction {
        val skip = ((page - 1) * perPage).toLong()

        val query = TodoTable.selectAll()
            .where { TodoTable.userId eq UUID.fromString(userId) }

        if (search.isNotBlank()) {
            query.andWhere { TodoTable.title.lowerCase() like "%${search.lowercase()}%" }
        }
        if (isDone != null) {
            query.andWhere { TodoTable.isDone eq isDone }
        }
        if (!urgency.isNullOrBlank()) {
            query.andWhere { TodoTable.urgency eq urgency }
        }

        val total = query.count()
        val todos = TodoDAO
            .wrapRows(
                query
                    .orderBy(TodoTable.updatedAt to SortOrder.DESC)
                    .limit(perPage, skip)
            )
            .map(::todoDAOToModel)

        Pair(todos, total)
    }

    override suspend fun getStats(userId: String): Triple<Long, Long, Long> = suspendTransaction {
        val uid   = UUID.fromString(userId)
        val total = TodoTable.selectAll().where { TodoTable.userId eq uid }.count()
        val done  = TodoTable.selectAll().where { (TodoTable.userId eq uid) and (TodoTable.isDone eq true) }.count()
        Triple(total, done, total - done)
    }

    override suspend fun getById(todoId: String): Todo? = suspendTransaction {
        TodoDAO
            .find { TodoTable.id eq UUID.fromString(todoId) }
            .limit(1)
            .map(::todoDAOToModel)
            .firstOrNull()
    }

    override suspend fun create(todo: Todo): String = suspendTransaction {
        val dao = TodoDAO.new {
            userId      = UUID.fromString(todo.userId)
            title       = todo.title
            description = todo.description
            isDone      = todo.isDone
            urgency     = todo.urgency
            cover       = todo.cover
            createdAt   = todo.createdAt
            updatedAt   = todo.updatedAt
        }
        dao.id.value.toString()
    }

    override suspend fun update(userId: String, todoId: String, newTodo: Todo): Boolean = suspendTransaction {
        val dao = TodoDAO
            .find {
                (TodoTable.id eq UUID.fromString(todoId)) and
                        (TodoTable.userId eq UUID.fromString(userId))
            }
            .limit(1)
            .firstOrNull() ?: return@suspendTransaction false

        dao.title       = newTodo.title
        dao.description = newTodo.description
        dao.isDone      = newTodo.isDone
        dao.urgency     = newTodo.urgency
        dao.updatedAt   = newTodo.updatedAt
        true
    }

    override suspend fun updateCover(userId: String, todoId: String, cover: String?): Boolean = suspendTransaction {
        val rows = TodoTable.update({
            (TodoTable.id eq UUID.fromString(todoId)) and
                    (TodoTable.userId eq UUID.fromString(userId))
        }) {
            it[TodoTable.cover] = cover
        }
        rows >= 1
    }

    override suspend fun delete(userId: String, todoId: String): Boolean = suspendTransaction {
        TodoTable.deleteWhere {
            (TodoTable.id eq UUID.fromString(todoId)) and
                    (TodoTable.userId eq UUID.fromString(userId))
        } >= 1
    }
}