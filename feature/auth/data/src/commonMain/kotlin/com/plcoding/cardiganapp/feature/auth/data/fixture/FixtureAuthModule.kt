package com.plcoding.cardiganapp.feature.auth.data.fixture

import com.plcoding.cardiganapp.core.domain.session.SessionManager
import com.plcoding.cardiganapp.feature.auth.domain.AuthRepository
import org.koin.dsl.binds
import org.koin.dsl.module

/** TEMPORARY (D3): the fixture session and repository, bound only in debug builds. Removed in task 9.1. */
val fixtureAuthModule =
    module {
        single { InMemorySessionManager() } binds arrayOf(InMemorySessionManager::class, SessionManager::class)
        single<AuthRepository> { FixtureAuthRepository(get()) }
    }
