package com.plcoding.cardiganapp.feature.transfer.presentation.fakes

import assertk.assertThat
import assertk.assertions.containsExactly
import com.plcoding.cardiganapp.core.presentation.toast.ToastMessage
import com.plcoding.cardiganapp.core.presentation.toast.ToastType
import com.plcoding.cardiganapp.core.presentation.util.UiText
import kotlin.test.Test

class FakeToastControllerTest {
    @Test
    fun recordsMessagesInOrder() {
        val controller = FakeToastController()
        val first = ToastMessage(UiText.DynamicString("first"), ToastType.Success)
        val second = ToastMessage(UiText.DynamicString("second"), ToastType.Error)

        controller.show(first)
        controller.show(second)

        assertThat(controller.messages).containsExactly(first, second)
    }
}
