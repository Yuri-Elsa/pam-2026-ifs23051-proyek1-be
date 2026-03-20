package org.delcom.helpers

import io.ktor.server.application.*
import org.delcom.tables.RefreshTokenTable
import org.delcom.tables.WatchlistTable
import org.delcom.tables.UserTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureDatabases() {
    val dbHost     = System.getenv("DB_HOST")     ?: "127.0.0.1"
    val dbPort     = System.getenv("DB_PORT")     ?: "5432"
    val dbName     = System.getenv("DB_NAME")     ?: "watchlist"
    val dbUser     = System.getenv("DB_USER")     ?: "postgres"
    val dbPassword = System.getenv("DB_PASSWORD") ?: "postgres"

    Database.connect(
        url      = "jdbc:postgresql://$dbHost:$dbPort/$dbName",
        driver   = "org.postgresql.Driver",
        user     = dbUser,
        password = dbPassword
    )

    transaction {
        SchemaUtils.createMissingTablesAndColumns(
            UserTable,
            RefreshTokenTable,
            WatchlistTable
        )
    }

    log.info("Database connected and schema ready.")
}
