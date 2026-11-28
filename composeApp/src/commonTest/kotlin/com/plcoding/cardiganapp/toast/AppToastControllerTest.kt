package com.plcoding.cardiganapp.toast

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.plcoding.cardiganapp.core.presentation.toast.ToastMessage
import com.plcoding.cardiganapp.core.presentation.toast.ToastType
import com.plcoding.cardiganapp.core.presentation.util.UiText
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class AppToastControllerTest {
    private val first = ToastMessage(UiText.DynamicString("first"), ToastType.Success)
    private val second = ToastMessage(UiText.DynamicString("second"), ToastType.Error)

    @Test
    fun shownMessageReachesTheCollector() =
        runTest {
            val controller = AppToastController()
            controller.messages.test {
                controller.show(first)
                assertThat(awaitItem()).isEqualTo(first)
            }
        }

    @Test
    fun secondMessageReplacesAnUncollectedFirst() =
        runTest {
            val controller = AppToastController()
            controller.show(first)
            controller.show(second)
            controller.messages.test {
                assertThat(awaitItem()).isEqualTo(second)
                expectNoEvents()
            }
        }
}
