package com.plcoding.cardiganapp.core.presentation.toast

import com.plcoding.cardiganapp.core.presentation.util.UiText

/** Shows a toast in the app-wide toast host, from any feature. */
interface ToastController {
    fun show(message: ToastMessage)
}

data class ToastMessage(
    val text: UiText,
    val type: ToastType,
)

enum class ToastType {
    Success,
    Error,
}
