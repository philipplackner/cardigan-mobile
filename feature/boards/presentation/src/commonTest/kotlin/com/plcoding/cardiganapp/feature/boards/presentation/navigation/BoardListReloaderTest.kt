package com.plcoding.cardiganapp.feature.boards.presentation.navigation

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import cardiganapp.feature.boards.presentation.generated.resources.Res
import cardiganapp.feature.boards.presentation.generated.resources.boards_refresh_failed
import com.plcoding.cardiganapp.core.domain.util.DataError
import com.plcoding.cardiganapp.core.domain.util.Result
import com.plcoding.cardiganapp.core.presentation.toast.ToastType
import com.plcoding.cardiganapp.feature.boards.presentation.fakes.FakeBoardRepository
import com.plcoding.cardiganapp.feature.boards.presentation.fakes.FakeToastController
import com.plcoding.cardiganapp.feature.boards.presentation.util.assertResource
import kotlin.test.Test
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

class BoardListReloaderTest {

    private val repository = FakeBoardRepository(emptyList())
    private val toasts = FakeToastController()

    private fun TestScope.reloader() = BoardListReloader(repository, toasts, backgroundScope)

    @Test
    fun failureShowsTheRefreshToastOnce() = runTest {
        repository.refreshResult = Result.Error(DataError.Network.NO_INTERNET)
        val reloader = reloader()

        reloader.reload()
        runCurrent()

        assertThat(toasts.messages).hasSize(1)
        assertResource(toasts.messages.single().text, Res.string.boards_refresh_failed)
        assertThat(toasts.messages.single().type).isEqualTo(ToastType.Error)
        assertThat(reloader.isReloading.value).isFalse()
    }

    @Test
    fun successShowsNoToast() = runTest {
        val reloader = reloader()
        reloader.reload()
        runCurrent()
        assertThat(toasts.messages).isEmpty()
    }

    @Test
    fun overlappingCallsMakeOneRefresh() = runTest {
        repository.gate = CompletableDeferred()
        repository.refreshResult = Result.Error(DataError.Network.NO_INTERNET)
        val reloader = reloader()

        reloader.reload()
        runCurrent()
        reloader.reload()
        runCurrent()
        assertThat(reloader.isReloading.value).isTrue()
        repository.gate!!.complete(Unit)
        runCurrent()

        assertThat(repository.refreshCalls).isEqualTo(1)
        assertThat(toasts.messages).hasSize(1)
        assertThat(reloader.isReloading.value).isFalse()
    }

    @Test
    fun aLaterCallRefreshesAgain() = runTest {
        val reloader = reloader()
        reloader.reload()
        runCurrent()
        reloader.reload()
        runCurrent()
        assertThat(repository.refreshCalls).isEqualTo(2)
    }
}
