package org.delcom.watchlist.repositories

import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import org.delcom.watchlist.dao.AuthTokenDAO
import org.delcom.watchlist.entities.AuthToken
import org.delcom.watchlist.helpers.authTokenDAOToModel
import org.delcom.watchlist.helpers.suspendTransaction
import org.delcom.watchlist.tables.AuthTokenTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import java.util.UUID

class AuthTokenRepository : IAuthTokenRepository {

    override suspend fun getByRefreshToken(refreshToken: String): AuthToken? = suspendTransaction {
        AuthTokenDAO
            .find { AuthTokenTable.refreshToken eq refreshToken }
            .limit(1)
            .map(::authTokenDAOToModel)
            .firstOrNull()
    }

    override suspend fun getByAuthAndRefreshToken(authToken: String, refreshToken: String): AuthToken? = suspendTransaction {
        AuthTokenDAO
            .find {
                (AuthTokenTable.authToken eq authToken) and
                (AuthTokenTable.refreshToken eq refreshToken)
            }
            .limit(1)
            .map(::authTokenDAOToModel)
            .firstOrNull()
    }

    override suspend fun create(authToken: AuthToken): String = suspendTransaction {
        val dao = AuthTokenDAO.new {
            userId           = UUID.fromString(authToken.userId)
            this.authToken    = authToken.authToken
            this.refreshToken = authToken.refreshToken
            createdAt        = authToken.createdAt
            expiresAt        = authToken.expiresAt
        }
        dao.id.value.toString()
    }

    override suspend fun deleteByAuthToken(authToken: String): Boolean = suspendTransaction {
        AuthTokenTable.deleteWhere { AuthTokenTable.authToken eq authToken } >= 1
    }

    override suspend fun deleteByUserId(userId: String): Boolean = suspendTransaction {
        AuthTokenTable.deleteWhere { AuthTokenTable.userId eq UUID.fromString(userId) } >= 1
    }
}
