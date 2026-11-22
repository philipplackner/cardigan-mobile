package com.plcoding.cardiganapp.di

import com.plcoding.cardiganapp.core.data.di.coreDataModule
import com.plcoding.cardiganapp.feature.boards.data.di.boardsDataModule
import com.plcoding.cardiganapp.feature.boards.data.fixture.fixtureBoardsModule
import com.plcoding.cardiganapp.feature.boards.presentation.di.boardsPresentationModule
import com.plcoding.cardiganapp.feature.transfer.data.di.transferDataModule
import com.plcoding.cardiganapp.feature.transfer.presentation.di.transferPresentationModule
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration

/** Starts Koin. Call exactly once per process, from the Android `Application` and the iOS app init. */
fun initKoin(config: KoinAppDeclaration? = null) {
    startKoin {
        config?.invoke(this)
        modules(appModules())
    }
}

fun appModules(): List<Module> = listOf(
    coreDataModule,
    appModule,
    boardsDataModule,
    // TEMPORARY: the in-memory board fixture (D3) until mob-boards-connect binds the Ktor data source.
    fixtureBoardsModule,
    boardsPresentationModule,
    transferDataModule,
    transferPresentationModule,
)
