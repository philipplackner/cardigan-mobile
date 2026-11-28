package com.plcoding.cardiganapp.di

import com.plcoding.cardiganapp.core.data.di.coreDataModule
import com.plcoding.cardiganapp.feature.auth.data.fixture.fixtureAuthModule
import com.plcoding.cardiganapp.feature.auth.presentation.di.authPresentationModule
import com.plcoding.cardiganapp.feature.boards.data.di.boardsDataModule
import com.plcoding.cardiganapp.feature.boards.presentation.di.boardsPresentationModule
import com.plcoding.cardiganapp.feature.transfer.data.di.transferDataModule
import com.plcoding.cardiganapp.feature.transfer.presentation.di.transferPresentationModule
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/** Build facts the UI needs. */
data class AppBuildInfo(
    val isDebug: Boolean,
)

/**
 * Starts Koin. Call exactly once per process, from the Android `Application` and the iOS app init.
 * [isDebug] binds the temporary auth fixture (D3, removed in add-auth-ui task 9.1).
 */
fun initKoin(
    isDebug: Boolean = false,
    config: KoinAppDeclaration? = null,
) {
    startKoin {
        config?.invoke(this)
        modules(appModules(isDebug))
    }
}

fun appModules(isDebug: Boolean = false): List<Module> =
    listOf(
        module { single { AppBuildInfo(isDebug) } },
        coreDataModule,
        appModule,
        authPresentationModule,
        boardsDataModule,
        boardsPresentationModule,
        transferDataModule,
        transferPresentationModule,
    ) + if (isDebug) listOf(fixtureAuthModule) else emptyList()
