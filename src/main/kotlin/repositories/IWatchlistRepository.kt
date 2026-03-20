package org.delcom.watchlist.repositories

import org.delcom.watchlist.entities.Watchlist

interface IWatchlistRepository {
    suspend fun getAll(
        userId: String,
        search: String,
        page: Int,
        perPage: Int,
        isDone: Boolean?,
        urgency: String?
    ): Pair<List<Watchlist>, Long>

    suspend fun getStats(userId: String): Triple<Long, Long, Long>
    suspend fun getById(watchlistId: String): Watchlist?
    suspend fun create(watchlist: Watchlist): String
    suspend fun update(userId: String, watchlistId: String, newWatchlist: Watchlist): Boolean
    suspend fun updateCover(userId: String, watchlistId: String, cover: String?): Boolean
    suspend fun delete(userId: String, watchlistId: String): Boolean
}