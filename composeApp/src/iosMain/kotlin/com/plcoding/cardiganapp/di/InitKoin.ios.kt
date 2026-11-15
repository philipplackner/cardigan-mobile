package com.plcoding.cardiganapp.di

import kotlin.experimental.ExperimentalNativeApi

/** Starts Koin from the iOS app, which can't read the Kotlin debug flag itself. */
@OptIn(ExperimentalNativeApi::class)
fun initKoinIos() = initKoin(isDebug = Platform.isDebugBinary)
