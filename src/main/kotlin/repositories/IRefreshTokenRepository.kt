package org.delcom.watchlist.repositories

import org.delcom.watchlist.entities.RefreshToken

interface IRefreshTokenRepository {
    suspend fun getByRefreshToken(refreshToken: String): RefreshToken?
    suspend fun getByAuthAndRefreshToken(authToken: String, refreshToken: String): RefreshToken?
    suspend fun create(refreshToken: RefreshToken): String
    suspend fun deleteByAuthToken(authToken: String): Boolean
    suspend fun deleteByUserId(userId: String): Boolean
}