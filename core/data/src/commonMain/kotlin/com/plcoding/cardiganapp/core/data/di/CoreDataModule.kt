package com.plcoding.cardiganapp.core.data.di

import com.plcoding.cardiganapp.core.data.networking.HttpClientFactory
import org.koin.core.module.Module
import org.koin.dsl.module

/** Provides the platform [io.ktor.client.engine.HttpClientEngine]. */
expect val coreDataPlatformModule: Module

val coreDataModule = module {
    includes(coreDataPlatformModule)
    single { HttpClientFactory.create(get()) }
}
