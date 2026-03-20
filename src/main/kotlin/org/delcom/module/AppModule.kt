package org.delcom.module

import org.delcom.repositories.*
import org.delcom.services.AuthService
import org.delcom.services.WatchlistService
import org.delcom.services.UserService
import org.koin.dsl.module

fun appModule(jwtSecret: String, uploadDir: String, uploadMaxSizeMb: Long) = module {

    single<IUserRepository>         { UserRepository() }
    single<IRefreshTokenRepository> { RefreshTokenRepository() }
    single<IWatchlistRepository>    { WatchlistRepository() }

    single { AuthService(jwtSecret, get(), get()) }
    single { UserService(get(), get(), uploadDir, uploadMaxSizeMb) }
    single { WatchlistService(get(), get(), uploadDir, uploadMaxSizeMb) }
}
