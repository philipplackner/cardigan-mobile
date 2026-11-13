package com.plcoding.cardiganapp.core.data.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import org.koin.core.module.Module
import org.koin.dsl.module

actual val coreDataPlatformModule: Module = module {
    single<HttpClientEngine> { Darwin.create() }
}
