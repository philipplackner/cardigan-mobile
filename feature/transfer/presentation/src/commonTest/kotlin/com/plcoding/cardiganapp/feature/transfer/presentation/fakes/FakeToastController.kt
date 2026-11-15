package com.plcoding.cardiganapp.feature.transfer.presentation.fakes

import com.plcoding.cardiganapp.core.presentation.toast.ToastController
import com.plcoding.cardiganapp.core.presentation.toast.ToastMessage

class FakeToastController : ToastController {
    val messages = mutableListOf<ToastMessage>()

    override fun show(message: ToastMessage) {
        messages += message
    }
}
