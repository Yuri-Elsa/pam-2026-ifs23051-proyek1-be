package org.delcom.watchlist.data

import kotlinx.serialization.Serializable
import org.delcom.watchlist.entities.AuthToken

@Serializable
data class AuthTokenRequest(
    var userId: String = "",
    var authToken: String = "",
    var refreshToken: String = "",
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "authToken"    to authToken,
        "refreshToken" to refreshToken,
    )

    fun toEntity(): AuthToken = AuthToken(
        userId       = userId,
        authToken    = authToken,
        refreshToken = refreshToken,
    )
}
