package com.plcoding.cardiganapp.core.domain.text

import kotlinx.cinterop.BetaInteropApi
import platform.Foundation.NSString
import platform.Foundation.create
import platform.Foundation.decomposedStringWithCanonicalMapping
import platform.Foundation.precomposedStringWithCanonicalMapping

@OptIn(BetaInteropApi::class)
actual fun String.toNfc(): String = NSString.create(string = this).precomposedStringWithCanonicalMapping()

@OptIn(BetaInteropApi::class)
actual fun String.toNfd(): String = NSString.create(string = this).decomposedStringWithCanonicalMapping()
