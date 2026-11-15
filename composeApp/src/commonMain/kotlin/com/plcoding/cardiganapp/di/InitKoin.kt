package com.plcoding.cardiganapp.di

import com.plcoding.cardiganapp.core.data.di.coreDataModule
import com.plcoding.cardiganapp.feature.transfer.data.di.transferDataModule
import com.plcoding.cardiganapp.feature.transfer.data.di.transferFixtureDataModule
import com.plcoding.cardiganapp.feature.transfer.presentation.di.transferPresentationModule
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/** Build facts the UI needs. */
data class AppBuildInfo(val isDebug: Boolean)

/**
 * Starts Koin. Call exactly once per process, from the Android `Application` and the iOS app init.
 * [isDebug] binds the temporary transfer fixture repository (removed in add-transfer-ui group 10).
 */
fun initKoin(isDebug: Boolean, config: KoinAppDeclaration? = null) {
    startKoin {
        config?.invoke(this)
        modules(appModules(isDebug))
    }
}

fun appModules(isDebug: Boolean): List<Module> = buildList {
    add(module { single { AppBuildInfo(isDebug) } })
    add(coreDataModule)
    add(appModule)
    add(transferDataModule)
    add(transferPresentationModule)
    if (isDebug) add(transferFixtureDataModule)
}
