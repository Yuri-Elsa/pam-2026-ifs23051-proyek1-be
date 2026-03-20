package org.delcom.repositories

import org.delcom.dao.UserDAO
import org.delcom.entities.User
import org.delcom.helpers.suspendTransaction
import org.delcom.helpers.userDAOToModel
import org.delcom.tables.UserTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.update
import java.util.UUID

class UserRepository : IUserRepository {

    override suspend fun getById(userId: String): User? = suspendTransaction {
        UserDAO
            .find { UserTable.id eq UUID.fromString(userId) }
            .limit(1)
            .map(::userDAOToModel)
            .firstOrNull()
    }

    override suspend fun getByUsername(username: String): User? = suspendTransaction {
        UserDAO
            .find { UserTable.username eq username }
            .limit(1)
            .map(::userDAOToModel)
            .firstOrNull()
    }

    override suspend fun create(user: User): String = suspendTransaction {
        val dao = UserDAO.new {
            name      = user.name
            username  = user.username
            password  = user.password
            photo     = user.photo
            about     = user.about
            createdAt = user.createdAt
            updatedAt = user.updatedAt
        }
        dao.id.value.toString()
    }

    override suspend fun update(id: String, newUser: User): Boolean = suspendTransaction {
        val dao = UserDAO
            .find { UserTable.id eq UUID.fromString(id) }
            .limit(1)
            .firstOrNull() ?: return@suspendTransaction false

        dao.name      = newUser.name
        dao.username  = newUser.username
        dao.password  = newUser.password
        dao.photo     = newUser.photo
        dao.about     = newUser.about
        dao.updatedAt = newUser.updatedAt
        true
    }

    override suspend fun updateAbout(id: String, about: String): Boolean = suspendTransaction {
        val rows = UserTable.update({ UserTable.id eq UUID.fromString(id) }) {
            it[UserTable.about] = about
        }
        rows >= 1
    }

    override suspend fun updatePhoto(id: String, photo: String?): Boolean = suspendTransaction {
        val rows = UserTable.update({ UserTable.id eq UUID.fromString(id) }) {
            it[UserTable.photo] = photo
        }
        rows >= 1
    }

    override suspend fun delete(id: String): Boolean = suspendTransaction {
        UserTable.deleteWhere { UserTable.id eq UUID.fromString(id) } >= 1
    }
}
