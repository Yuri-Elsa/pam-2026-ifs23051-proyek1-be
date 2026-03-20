package org.delcom.watchlist.data

import kotlinx.serialization.Serializable
import org.delcom.watchlist.entities.RefreshToken

@Serializable
data class RefreshTokenRequest(
    var userId: String = "",
    var authToken: String = "",
    var refreshToken: String = "",
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "authToken"    to authToken,
        "refreshToken" to refreshToken,
    )

    fun toEntity(): RefreshToken = RefreshToken(
        userId       = userId,
        authToken    = authToken,
        refreshToken = refreshToken,
    )
}