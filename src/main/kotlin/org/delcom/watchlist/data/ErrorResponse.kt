package org.delcom.watchlist.data

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val status: String,
    val message: String,
    val data: String? = null
)
