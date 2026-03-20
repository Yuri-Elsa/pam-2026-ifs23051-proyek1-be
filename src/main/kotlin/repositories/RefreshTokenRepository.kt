package org.delcom.watchlist.repositories

import org.delcom.watchlist.dao.RefreshTokenDAO
import org.delcom.watchlist.entities.RefreshToken
import org.delcom.watchlist.helpers.refreshTokenDAOToModel
import org.delcom.watchlist.helpers.suspendTransaction
import org.delcom.watchlist.tables.RefreshTokenTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import java.util.UUID

class RefreshTokenRepository : IRefreshTokenRepository {

    override suspend fun getByRefreshToken(refreshToken: String): RefreshToken? = suspendTransaction {
        RefreshTokenDAO
            .find { RefreshTokenTable.refreshToken eq refreshToken }
            .limit(1)
            .map(::refreshTokenDAOToModel)
            .firstOrNull()
    }

    override suspend fun getByAuthAndRefreshToken(authToken: String, refreshToken: String): RefreshToken? = suspendTransaction {
        RefreshTokenDAO
            .find {
                (RefreshTokenTable.authToken eq authToken) and
                        (RefreshTokenTable.refreshToken eq refreshToken)
            }
            .limit(1)
            .map(::refreshTokenDAOToModel)
            .firstOrNull()
    }

    override suspend fun create(refreshToken: RefreshToken): String = suspendTransaction {
        val dao = RefreshTokenDAO.new {
            userId                = UUID.fromString(refreshToken.userId)
            this.authToken        = refreshToken.authToken
            this.refreshToken     = refreshToken.refreshToken
            createdAt             = refreshToken.createdAt
            expiresAt             = refreshToken.expiresAt
        }
        dao.id.value.toString()
    }

    override suspend fun deleteByAuthToken(authToken: String): Boolean = suspendTransaction {
        RefreshTokenTable.deleteWhere { RefreshTokenTable.authToken eq authToken } >= 1
    }

    override suspend fun deleteByUserId(userId: String): Boolean = suspendTransaction {
        RefreshTokenTable.deleteWhere { RefreshTokenTable.userId eq UUID.fromString(userId) } >= 1
    }
}