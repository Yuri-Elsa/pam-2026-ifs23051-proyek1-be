package org.delcom.helpers

import io.ktor.server.application.*
import org.delcom.tables.RefreshTokenTable
import org.delcom.tables.WatchlistTable
import org.delcom.tables.UserTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureDatabases() {
    val dbHost     = getEnv("DB_HOST",     "127.0.0.1")
    val dbPort     = getEnv("DB_PORT",     "5432")
    val dbName     = getEnv("DB_NAME",     "watchlist")
    val dbUser     = getEnv("DB_USER",     "postgres")
    val dbPassword = getEnv("DB_PASSWORD", "postgres")

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
    log.info("DB_HOST=$dbHost, DB_PORT=$dbPort, DB_NAME=$dbName, DB_USER=$dbUser")
}