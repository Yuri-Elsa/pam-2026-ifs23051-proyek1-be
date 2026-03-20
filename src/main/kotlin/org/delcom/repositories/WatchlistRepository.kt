package org.delcom.repositories

import org.delcom.dao.WatchlistDAO
import org.delcom.entities.Watchlist
import org.delcom.helpers.suspendTransaction
import org.delcom.helpers.watchlistDAOToModel
import org.delcom.tables.WatchlistTable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.util.UUID

class WatchlistRepository : IWatchlistRepository {

    override suspend fun getAll(
        userId: String,
        search: String,
        page: Int,
        perPage: Int,
        isDone: Boolean?,
        urgency: String?
    ): Pair<List<Watchlist>, Long> = suspendTransaction {
        val skip = ((page - 1) * perPage).toLong()

        val query = WatchlistTable.selectAll()
            .where { WatchlistTable.userId eq UUID.fromString(userId) }

        if (search.isNotBlank()) {
            query.andWhere { WatchlistTable.title.lowerCase() like "%${search.lowercase()}%" }
        }
        if (isDone != null) {
            query.andWhere { WatchlistTable.isDone eq isDone }
        }
        if (!urgency.isNullOrBlank()) {
            query.andWhere { WatchlistTable.urgency eq urgency }
        }

        val total = query.count()
        val watchlists = WatchlistDAO
            .wrapRows(
                query
                    .orderBy(WatchlistTable.updatedAt to SortOrder.DESC)
                    .limit(perPage)
                    .offset(skip)
            )
            .map(::watchlistDAOToModel)

        Pair(watchlists, total)
    }

    override suspend fun getStats(userId: String): Triple<Long, Long, Long> = suspendTransaction {
        val uid   = UUID.fromString(userId)
        val total = WatchlistTable.selectAll().where { WatchlistTable.userId eq uid }.count()
        val done  = WatchlistTable.selectAll().where { (WatchlistTable.userId eq uid) and (WatchlistTable.isDone eq true) }.count()
        Triple(total, done, total - done)
    }

    override suspend fun getById(watchlistId: String): Watchlist? = suspendTransaction {
        WatchlistDAO
            .find { WatchlistTable.id eq UUID.fromString(watchlistId) }
            .limit(1)
            .map(::watchlistDAOToModel)
            .firstOrNull()
    }

    override suspend fun create(watchlist: Watchlist): String = suspendTransaction {
        val dao = WatchlistDAO.new {
            userId      = UUID.fromString(watchlist.userId)
            title       = watchlist.title
            description = watchlist.description
            isDone      = watchlist.isDone
            urgency     = watchlist.urgency
            cover       = watchlist.cover
            createdAt   = watchlist.createdAt
            updatedAt   = watchlist.updatedAt
        }
        dao.id.value.toString()
    }

    override suspend fun update(userId: String, watchlistId: String, newWatchlist: Watchlist): Boolean = suspendTransaction {
        val dao = WatchlistDAO
            .find {
                (WatchlistTable.id eq UUID.fromString(watchlistId)) and
                        (WatchlistTable.userId eq UUID.fromString(userId))
            }
            .limit(1)
            .firstOrNull() ?: return@suspendTransaction false

        dao.title       = newWatchlist.title
        dao.description = newWatchlist.description
        dao.isDone      = newWatchlist.isDone
        dao.urgency     = newWatchlist.urgency
        dao.updatedAt   = newWatchlist.updatedAt
        true
    }

    override suspend fun updateCover(userId: String, watchlistId: String, cover: String?): Boolean = suspendTransaction {
        val rows = WatchlistTable.update({
            (WatchlistTable.id eq UUID.fromString(watchlistId)) and
                    (WatchlistTable.userId eq UUID.fromString(userId))
        }) {
            it[WatchlistTable.cover] = cover
        }
        rows >= 1
    }

    override suspend fun delete(userId: String, watchlistId: String): Boolean = suspendTransaction {
        WatchlistTable.deleteWhere {
            (WatchlistTable.id eq UUID.fromString(watchlistId)) and
                    (WatchlistTable.userId eq UUID.fromString(userId))
        } >= 1
    }
}
