package com.plcoding.cardiganapp.feature.boards.presentation.navigation

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.plcoding.cardiganapp.core.domain.util.DataError
import com.plcoding.cardiganapp.core.domain.util.Result
import com.plcoding.cardiganapp.feature.boards.presentation.fakes.FakeBoardRepository
import kotlin.test.Test
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

/*
 * Break vectors:
 * - two overlapping calls: one refresh, both get the same failure
 * - a caller cancelled mid-reload: the request keeps running and isReloading still returns to false
 * - isReloading: true from the first call until the shared reload ends, also when it fails
 * - a call after the previous reload ended starts a new refresh
 * - the reloader has no toast dependency (constructor takes only the repository and a scope)
 */
class BoardListReloaderTest {

    private val repository = FakeBoardRepository(emptyList())

    private fun TestScope.reloader() = BoardListReloader(repository, backgroundScope)

    @Test
    fun overlappingCallsShareOneRefreshAndItsFailure() = runTest {
        repository.gate = CompletableDeferred()
        repository.refreshResult = Result.Error(DataError.Network.NO_INTERNET)
        val reloader = reloader()

        val first = async { reloader.reload() }
        val second = async { reloader.reload() }
        runCurrent()
        assertThat(reloader.isReloading.value).isTrue()
        repository.gate!!.complete(Unit)

        assertThat(first.await()).isEqualTo(Result.Error(DataError.Network.NO_INTERNET))
        assertThat(second.await()).isEqualTo(Result.Error(DataError.Network.NO_INTERNET))
        assertThat(repository.refreshCalls).isEqualTo(1)
        assertThat(reloader.isReloading.value).isFalse()
    }

    @Test
    fun cancelledCallerDoesNotCancelTheRequest() = runTest {
        repository.gate = CompletableDeferred()
        repository.serverBoards = listOf()
        val reloader = reloader()

        val caller = async { reloader.reload() }
        runCurrent()
        caller.cancel()
        runCurrent()
        assertThat(reloader.isReloading.value).isTrue()

        repository.gate!!.complete(Unit)
        runCurrent()

        assertThat(repository.refreshCalls).isEqualTo(1)
        assertThat(repository.state.value).isEqualTo(emptyList())
        assertThat(reloader.isReloading.value).isFalse()
    }

    @Test
    fun aCallAfterTheReloadEndedRefreshesAgain() = runTest {
        val reloader = reloader()
        assertThat(reloader.reload()).isEqualTo(Result.Success(Unit))
        assertThat(reloader.reload()).isEqualTo(Result.Success(Unit))
        assertThat(repository.refreshCalls).isEqualTo(2)
        assertThat(reloader.isReloading.value).isFalse()
    }

    @Test
    fun aLateJoinerGetsTheRunningResult() = runTest {
        repository.gate = CompletableDeferred()
        val reloader = reloader()
        val first = async { reloader.reload() }
        runCurrent()
        repository.refreshResult = Result.Error(DataError.Network.SERVER_ERROR)
        val late = async { reloader.reload() }
        runCurrent()
        repository.gate!!.complete(Unit)
        assertThat(late.await()).isEqualTo(first.await())
        assertThat(repository.refreshCalls).isEqualTo(1)
    }
}
