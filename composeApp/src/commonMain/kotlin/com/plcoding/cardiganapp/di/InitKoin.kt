package com.plcoding.cardiganapp.di

import com.plcoding.cardiganapp.core.data.di.coreDataModule
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

/** Starts Koin. Call exactly once per process, from the Android `Application` and the iOS app init. */
fun initKoin(config: KoinAppDeclaration? = null) {
    startKoin {
        config?.invoke(this)
        modules(coreDataModule)
    }
}
