package com.plcoding.cardiganapp.toast

import com.plcoding.cardiganapp.core.presentation.toast.ToastController
import com.plcoding.cardiganapp.core.presentation.toast.ToastMessage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/** Delivers toasts from every feature to the one [ToastHost][com.plcoding.cardiganapp.core.designsystem.components.feedback.ToastHost] in `App()`. */
class AppToastController : ToastController {

    // Conflated: a toast sent before the host collects is replaced by a newer one, like on screen.
    private val channel = Channel<ToastMessage>(Channel.CONFLATED)
    val messages: Flow<ToastMessage> = channel.receiveAsFlow()

    override fun show(message: ToastMessage) {
        channel.trySend(message)
    }
}
