package com.plcoding.cardiganapp.core.designsystem.components.feedback

import androidx.compose.material3.SnackbarDuration
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import androidx.compose.material3.SnackbarVisuals
import assertk.assertions.isNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

/*
 * Break vectors:
 * - success vs error duration (closes on its own vs stays)
 * - the close control is an action with the given close label, and there is no extra dismiss action
 * - a second toast replaces the first instead of queueing behind it
 */
class ToastHostStateTest {

    @Test
    fun successToastShowsTheMessageWithACloseActionAndClosesOnItsOwn() = runTest {
        val state = ToastHostState()
        backgroundScope.launch { state.show("Board created", ToastType.Success, closeLabel = "Close") }
        runCurrent()

        val visuals = state.current()
        assertThat(visuals).isNotNull()
        assertThat(visuals!!.message).isEqualTo("Board created")
        assertThat(visuals.actionLabel).isEqualTo("Close")
        assertThat(visuals.withDismissAction).isFalse()
        assertThat(visuals.duration).isEqualTo(SnackbarDuration.Short)
    }

    @Test
    fun errorToastStaysUntilClosed() = runTest {
        val state = ToastHostState()
        backgroundScope.launch { state.show("Couldn't save", ToastType.Error, closeLabel = "Close") }
        runCurrent()

        assertThat(state.current()?.duration).isEqualTo(SnackbarDuration.Indefinite)
    }

    @Test
    fun newToastReplacesTheOneShowing() = runTest {
        val state = ToastHostState()
        val first = backgroundScope.launch { state.show("First", ToastType.Error, closeLabel = "Close") }
        runCurrent()

        backgroundScope.launch { state.show("Second", ToastType.Success, closeLabel = "Close") }
        runCurrent()

        assertThat(state.current()?.message).isEqualTo("Second")
        assertThat(first.isCompleted).isTrue()
    }

    private fun ToastHostState.current(): SnackbarVisuals? = snackbarHostState.currentSnackbarData?.visuals
}
