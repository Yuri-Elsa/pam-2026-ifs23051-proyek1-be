package org.delcom.watchlist.repositories

import org.delcom.watchlist.entities.AuthToken

interface IAuthTokenRepository {
    suspend fun getByRefreshToken(refreshToken: String): AuthToken?
    suspend fun getByAuthAndRefreshToken(authToken: String, refreshToken: String): AuthToken?
    suspend fun create(authToken: AuthToken): String
    suspend fun deleteByAuthToken(authToken: String): Boolean
    suspend fun deleteByUserId(userId: String): Boolean
}
