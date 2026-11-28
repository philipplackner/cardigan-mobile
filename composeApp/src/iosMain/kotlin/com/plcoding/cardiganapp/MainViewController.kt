package com.plcoding.cardiganapp

import androidx.compose.ui.window.ComposeUIViewController

// iosApp calls this by name from Swift.
@Suppress("ktlint:standard:function-naming")
fun MainViewController() = ComposeUIViewController { App() }
