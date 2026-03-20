package org.delcom.data

import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import org.delcom.entities.Watchlist

@Serializable
data class WatchlistRequest(
    var userId: String = "",
    var title: String = "",
    var description: String = "",
    var cover: String? = null,
    var isDone: Boolean = false,
    var urgency: String = "medium",
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "userId"      to userId,
        "title"       to title,
        "description" to description,
        "cover"       to cover,
        "isDone"      to isDone,
        "urgency"     to urgency,
    )

    fun toEntity(): Watchlist = Watchlist(
        userId      = userId,
        title       = title,
        description = description,
        cover       = cover,
        isDone      = isDone,
        urgency     = urgency,
        updatedAt   = Clock.System.now()
    )
}
