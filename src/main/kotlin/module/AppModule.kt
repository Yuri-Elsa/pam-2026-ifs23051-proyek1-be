package org.delcom.watchlist.module

import org.delcom.watchlist.repositories.*
import org.delcom.watchlist.services.AuthService
import org.delcom.watchlist.services.WatchlistService
import org.delcom.watchlist.services.UserService
import org.koin.dsl.module

fun appModule(jwtSecret: String, uploadDir: String, uploadMaxSizeMb: Long) = module {

    // ── Repositories ──────────────────────────────────────────────────────────
    single<IUserRepository>         { UserRepository() }
    single<IRefreshTokenRepository> { RefreshTokenRepository() }
    single<IWatchlistRepository>         { WatchlistRepository() }

    // ── Services ──────────────────────────────────────────────────────────────
    single { AuthService(jwtSecret, get(), get()) }
    single { UserService(get(), get(), uploadDir, uploadMaxSizeMb) }
    single { WatchlistService(get(), get(), uploadDir, uploadMaxSizeMb) }
}