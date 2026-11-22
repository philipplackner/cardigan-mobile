package com.plcoding.cardiganapp.feature.boards.data

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.plcoding.cardiganapp.core.domain.util.DataError
import com.plcoding.cardiganapp.core.domain.util.EmptyResult
import com.plcoding.cardiganapp.core.domain.util.Result
import com.plcoding.cardiganapp.feature.boards.domain.BoardSaveError
import com.plcoding.cardiganapp.feature.boards.domain.BoardSummary
import com.plcoding.cardiganapp.feature.boards.domain.RemoteBoardDataSource
import kotlin.test.Test
import kotlin.uuid.Uuid
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first

/*
 * Break vectors for clear() (a sign-out) against requests that started before it:
 * - the first load is in flight and nothing is loaded yet
 * - a refresh in flight over a loaded list, completing on another dispatcher (Dispatchers.Default)
 * - a create, a rename, a rename that gets 404 and a delete, each in flight
 * - ensureLoaded joins: a call after clear() must not join the old request, and must load for the new session
 * - a save that completes during a refresh started after clear() lands in the new session's list only if it
 *   started in the new session
 * Every request is held by its own CompletableDeferred, so the order is fixed, not timing-based.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InMemoryBoardRepositoryClearTest {

    /** Every call waits for the test to complete its deferred. */
    private class ScriptedRemote : RemoteBoardDataSource {
        val listCalls = mutableListOf<CompletableDeferred<Result<List<BoardSummary>, DataError.Network>>>()
        val createCalls = mutableListOf<CompletableDeferred<Result<BoardSummary, BoardSaveError>>>()
        val renameCalls = mutableListOf<CompletableDeferred<Result<BoardSummary, BoardSaveError>>>()
        val deleteCalls = mutableListOf<CompletableDeferred<EmptyResult<BoardSaveError>>>()
        val listCallCount = MutableStateFlow(0)

        override suspend fun getBoards() = CompletableDeferred<Result<List<BoardSummary>, DataError.Network>>()
            .also {
                listCalls += it
                listCallCount.value = listCalls.size
            }.await()

        override suspend fun createBoard(name: String, key: String) = CompletableDeferred<Result<BoardSummary, BoardSaveError>>()
            .also { createCalls += it }.await()

        override suspend fun renameBoard(id: Uuid, name: String) = CompletableDeferred<Result<BoardSummary, BoardSaveError>>()
            .also { renameCalls += it }.await()

        override suspend fun deleteBoard(id: Uuid) = CompletableDeferred<EmptyResult<BoardSaveError>>()
            .also { deleteCalls += it }.await()
    }

    private val remote = ScriptedRemote()

    private val oldA = summary(id = "a0000000-0000-0000-0000-000000000000", name = "Old A", key = "OLDA", updated = "2026-11-20T12:00:00.000Z")
    private val oldB = summary(id = "b0000000-0000-0000-0000-000000000000", name = "Old B", key = "OLDB", updated = "2026-11-20T10:00:00.000Z")
    private val newC = summary(id = "c0000000-0000-0000-0000-000000000000", name = "New C", key = "NEWC")

    private fun TestScope.repository() = InMemoryBoardRepository(remote, backgroundScope)

    private fun TestScope.loaded(vararg boards: BoardSummary): InMemoryBoardRepository {
        val repository = repository()
        val load = async { repository.refresh() }
        runCurrent()
        remote.listCalls.last().complete(Result.Success(boards.toList()))
        runCurrent()
        assertThat(load.isCompleted).isEqualTo(true)
        return repository
    }

    @Test
    fun clearDuringTheFirstLoadDropsItsResult() = runTest {
        val repository = repository()
        val load = async { repository.ensureLoaded() }
        runCurrent()

        repository.clear()
        remote.listCalls.single().complete(Result.Success(listOf(oldA)))
        load.await()

        assertThat(repository.boards.value).isNull()
    }

    @Test
    fun clearDuringARefreshOnAnotherDispatcherDropsItsResult() = runTest {
        val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val repository = InMemoryBoardRepository(remote, appScope)
            val first = appScope.async { repository.refresh() }
            remote.listCallCount.first { it >= 1 }
            remote.listCalls.single().complete(Result.Success(listOf(oldA)))
            first.await()
            assertThat(repository.boards.value).isEqualTo(listOf(oldA))

            val refresh = appScope.async { repository.refresh() }
            remote.listCallCount.first { it >= 2 }
            repository.clear()
            remote.listCalls[1].complete(Result.Success(listOf(oldA, oldB)))
            refresh.await()

            assertThat(repository.boards.value).isNull()
        } finally {
            appScope.cancel()
        }
    }

    @Test
    fun clearDuringACreateDropsIt() = runTest {
        val repository = loaded(oldA)
        val create = async { repository.createBoard("Old new", "OLDN") }
        runCurrent()
        repository.clear()
        loadNewSession(repository, newC)

        remote.createCalls.single().complete(Result.Success(summary(name = "Old new", key = "OLDN", updated = "2026-11-22T00:00:00.000Z")))
        create.await()

        assertThat(repository.boards.value).isEqualTo(listOf(newC))
    }

    @Test
    fun clearDuringARenameDropsIt() = runTest {
        val repository = loaded(oldA)
        val rename = async { repository.renameBoard(newC.id, "Renamed by the old session") }
        runCurrent()
        repository.clear()
        loadNewSession(repository, newC)

        remote.renameCalls.single().complete(Result.Success(newC.copy(name = "Renamed by the old session")))
        rename.await()

        assertThat(repository.boards.value).isEqualTo(listOf(newC))
    }

    @Test
    fun clearDuringARenameThatGetsNotFoundDropsTheRemoval() = runTest {
        val repository = loaded(oldA)
        val rename = async { repository.renameBoard(newC.id, "X") }
        runCurrent()
        repository.clear()
        loadNewSession(repository, newC)

        remote.renameCalls.single().complete(Result.Error(BoardSaveError.NotFound))
        rename.await()

        assertThat(repository.boards.value).isEqualTo(listOf(newC))
    }

    @Test
    fun clearDuringADeleteDropsIt() = runTest {
        val repository = loaded(oldA)
        val delete = async { repository.deleteBoard(newC.id) }
        runCurrent()
        repository.clear()
        loadNewSession(repository, newC)

        remote.deleteCalls.single().complete(Result.Success(Unit))
        delete.await()

        assertThat(repository.boards.value).isEqualTo(listOf(newC))
    }

    @Test
    fun clearDuringASaveWhileNothingIsLoadedKeepsNull() = runTest {
        val repository = loaded(oldA)
        val create = async { repository.createBoard("Old new", "OLDN") }
        runCurrent()
        repository.clear()

        remote.createCalls.single().complete(Result.Success(summary(name = "Old new", key = "OLDN")))
        create.await()

        assertThat(repository.boards.value).isNull()
    }

    @Test
    fun ensureLoadedAfterClearDoesNotJoinTheOldLoad() = runTest {
        val repository = repository()
        val before = async { repository.ensureLoaded() }
        runCurrent()
        repository.clear()

        val after = async { repository.ensureLoaded() }
        runCurrent()
        assertThat(remote.listCalls).hasSize(2)

        remote.listCalls[1].complete(Result.Success(listOf(newC)))
        assertThat(after.await()).isEqualTo(Result.Success(Unit))
        assertThat(repository.boards.value).isEqualTo(listOf(newC))

        remote.listCalls[0].complete(Result.Success(listOf(oldA)))
        before.await()
        assertThat(repository.boards.value).isEqualTo(listOf(newC))
    }

    @Test
    fun oldLoadFinishingFirstDoesNotSatisfyTheNewSession() = runTest {
        val repository = repository()
        val before = async { repository.ensureLoaded() }
        runCurrent()
        repository.clear()
        val after = async { repository.ensureLoaded() }
        runCurrent()

        remote.listCalls[0].complete(Result.Success(listOf(oldA)))
        before.await()
        assertThat(repository.boards.value).isNull()
        assertThat(after.isCompleted).isEqualTo(false)

        remote.listCalls[1].complete(Result.Error(DataError.Network.NO_INTERNET))
        assertThat(after.await()).isEqualTo(Result.Error(DataError.Network.NO_INTERNET))
        assertThat(repository.boards.value).isNull()
    }

    @Test
    fun oldSaveDuringTheNewSessionsRefreshIsNotReapplied() = runTest {
        val repository = loaded(oldA)
        val create = async { repository.createBoard("Old new", "OLDN") }
        runCurrent()
        repository.clear()

        val refresh = async { repository.refresh() }
        runCurrent()
        remote.createCalls.single().complete(Result.Success(summary(name = "Old new", key = "OLDN")))
        create.await()
        remote.listCalls.last().complete(Result.Success(listOf(newC)))
        refresh.await()

        assertThat(repository.boards.value).isEqualTo(listOf(newC))
    }

    private fun TestScope.loadNewSession(repository: InMemoryBoardRepository, vararg boards: BoardSummary) {
        val load = async { repository.refresh() }
        runCurrent()
        remote.listCalls.last().complete(Result.Success(boards.toList()))
        runCurrent()
        assertThat(load.isCompleted).isEqualTo(true)
        assertThat(repository.boards.value).isEqualTo(boards.toList())
    }
}
