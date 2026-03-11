package org.delcom.watchlist.module

import org.delcom.watchlist.repositories.*
import org.delcom.watchlist.services.AuthService
import org.delcom.watchlist.services.TodoService
import org.delcom.watchlist.services.UserService
import org.koin.dsl.module

fun appModule(jwtSecret: String, uploadDir: String, uploadMaxSizeMb: Long) = module {

    // ── Repositories ──────────────────────────────────────────────────────────
    single<IUserRepository>      { UserRepository() }
    single<IAuthTokenRepository> { AuthTokenRepository() }
    single<ITodoRepository>      { TodoRepository() }

    // ── Services ──────────────────────────────────────────────────────────────
    single { AuthService(jwtSecret, get(), get()) }
    single { UserService(get(), get(), uploadDir, uploadMaxSizeMb) }
    single { TodoService(get(), get(), uploadDir, uploadMaxSizeMb) }
}
