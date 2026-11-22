package com.plcoding.cardiganapp.feature.boards.data

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import com.plcoding.cardiganapp.core.domain.util.DataError
import com.plcoding.cardiganapp.core.domain.util.Result
import com.plcoding.cardiganapp.feature.boards.domain.BoardSaveError
import kotlin.test.Test
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

/*
 * Break vectors:
 * - order: the server's order is kept, never re-sorted
 * - refresh failure keeps the shown list, and on first load keeps null
 * - single flight: concurrent refreshes and ensureLoaded calls share one request; a later refresh makes a new one
 * - ensureLoaded returns at once when loaded
 * - saves: create inserts by BoardOrder, rename moves, delete removes
 * - 404: rename removes the board and returns NotFound, delete counts as success
 * - failed saves leave the list as it is
 * - a save that completes while a refresh runs survives the refresh result
 * - clear(): resets to null and drops a refresh or save that completes after it
 */
class InMemoryBoardRepositoryTest {

    private val remote = FakeRemoteBoardDataSource()

    private val a = summary(id = "a0000000-0000-0000-0000-000000000000", name = "A", key = "AAA", updated = "2026-11-20T12:00:00.000Z")
    private val b = summary(id = "b0000000-0000-0000-0000-000000000000", name = "B", key = "BBB", updated = "2026-11-20T10:00:00.000Z")
    private val c = summary(id = "c0000000-0000-0000-0000-000000000000", name = "C", key = "CCC", updated = "2026-11-20T08:00:00.000Z")

    private fun TestScope.repository() = InMemoryBoardRepository(remote, backgroundScope)

    private suspend fun InMemoryBoardRepository.loaded(vararg boards: com.plcoding.cardiganapp.feature.boards.domain.BoardSummary) {
        remote.getBoardsResult = Result.Success(boards.toList())
        refresh()
    }

    @Test
    fun serverOrderIsKept() = runTest {
        val repository = repository()
        repository.loaded(b, a, c)
        assertThat(repository.boards.value).isEqualTo(listOf(b, a, c))
    }

    @Test
    fun failedRefreshKeepsTheList() = runTest {
        val repository = repository()
        repository.loaded(a, b)
        remote.getBoardsResult = Result.Error(DataError.Network.NO_INTERNET)

        val result = repository.refresh()

        assertThat(result).isEqualTo(Result.Error(DataError.Network.NO_INTERNET))
        assertThat(repository.boards.value).isEqualTo(listOf(a, b))
    }

    @Test
    fun failedFirstLoadStaysNull() = runTest {
        val repository = repository()
        remote.getBoardsResult = Result.Error(DataError.Network.SERVER_ERROR)
        repository.ensureLoaded()
        assertThat(repository.boards.value).isNull()
    }

    @Test
    fun concurrentRefreshesMakeOneCall() = runTest {
        val repository = repository()
        remote.getBoardsResult = Result.Success(listOf(a))
        remote.getBoardsGate = CompletableDeferred()

        val first = async { repository.refresh() }
        val second = async { repository.refresh() }
        val third = async { repository.ensureLoaded() }
        runCurrent()
        remote.getBoardsGate!!.complete(Unit)

        assertThat(first.await()).isEqualTo(Result.Success(Unit))
        assertThat(second.await()).isEqualTo(Result.Success(Unit))
        assertThat(third.await()).isEqualTo(Result.Success(Unit))
        assertThat(remote.getBoardsCalls).isEqualTo(1)
    }

    @Test
    fun refreshAfterACompletedOneMakesANewCall() = runTest {
        val repository = repository()
        repository.loaded(a)
        repository.refresh()
        assertThat(remote.getBoardsCalls).isEqualTo(2)
    }

    @Test
    fun ensureLoadedReturnsAtOnceWhenLoaded() = runTest {
        val repository = repository()
        repository.loaded(a)
        assertThat(repository.ensureLoaded()).isEqualTo(Result.Success(Unit))
        assertThat(remote.getBoardsCalls).isEqualTo(1)
    }

    @Test
    fun createInsertsByOrder() = runTest {
        val repository = repository()
        repository.loaded(a, c)
        val created = summary(name = "New", key = "NEW", updated = "2026-11-20T09:00:00.000Z")
        remote.createResult = Result.Success(created)

        repository.createBoard("New", "NEW")

        assertThat(repository.boards.value).isEqualTo(listOf(a, created, c))
    }

    @Test
    fun renameMovesTheBoard() = runTest {
        val repository = repository()
        repository.loaded(a, b, c)
        val renamed = c.copy(name = "C2", updatedAt = kotlin.time.Instant.parse("2026-11-21T00:00:00.000Z"))
        remote.renameResult = Result.Success(renamed)

        repository.renameBoard(c.id, "C2")

        assertThat(repository.boards.value).isEqualTo(listOf(renamed, a, b))
    }

    @Test
    fun deleteRemovesTheBoard() = runTest {
        val repository = repository()
        repository.loaded(a, b, c)
        assertThat(repository.deleteBoard(b.id)).isEqualTo(Result.Success(Unit))
        assertThat(repository.boards.value).isEqualTo(listOf(a, c))
    }

    @Test
    fun renameNotFoundRemovesTheBoardAndReturnsNotFound() = runTest {
        val repository = repository()
        repository.loaded(a, b)
        remote.renameResult = Result.Error(BoardSaveError.NotFound)

        val result = repository.renameBoard(b.id, "X")

        assertThat(result).isEqualTo(Result.Error(BoardSaveError.NotFound))
        assertThat(repository.boards.value).isEqualTo(listOf(a))
    }

    @Test
    fun deleteNotFoundCountsAsDeleted() = runTest {
        val repository = repository()
        repository.loaded(a, b)
        remote.deleteResult = Result.Error(BoardSaveError.NotFound)

        assertThat(repository.deleteBoard(b.id)).isEqualTo(Result.Success(Unit))
        assertThat(repository.boards.value).isEqualTo(listOf(a))
    }

    @Test
    fun failedSavesLeaveTheList() = runTest {
        val repository = repository()
        repository.loaded(a, b)
        val network = Result.Error(BoardSaveError.Network(DataError.Network.NO_INTERNET))
        remote.createResult = network
        remote.renameResult = network
        remote.deleteResult = network

        assertThat(repository.createBoard("X", "XX")).isEqualTo(network)
        assertThat(repository.renameBoard(a.id, "X")).isEqualTo(network)
        assertThat(repository.deleteBoard(a.id)).isEqualTo(network)
        assertThat(repository.boards.value).isEqualTo(listOf(a, b))
    }

    @Test
    fun savesDuringARunningRefreshSurviveItsResult() = runTest {
        val repository = repository()
        repository.loaded(a, b, c)
        remote.getBoardsGate = CompletableDeferred()
        val refresh = async { repository.refresh() }
        runCurrent()

        val created = summary(name = "New", key = "NEW", updated = "2026-11-21T00:00:00.000Z")
        remote.createResult = Result.Success(created)
        repository.createBoard("New", "NEW")
        val renamed = c.copy(name = "C2", updatedAt = kotlin.time.Instant.parse("2026-11-20T11:00:00.000Z"))
        remote.renameResult = Result.Success(renamed)
        repository.renameBoard(c.id, "C2")
        repository.deleteBoard(b.id)

        remote.getBoardsResult = Result.Success(listOf(a, b, c))
        remote.getBoardsGate!!.complete(Unit)
        refresh.await()

        assertThat(repository.boards.value).isEqualTo(listOf(created, a, renamed))
    }

    @Test
    fun clearResetsToNull() = runTest {
        val repository = repository()
        repository.loaded(a)
        repository.clear()
        assertThat(repository.boards.value).isNull()
    }

    @Test
    fun refreshThatReturnsAfterClearIsDropped() = runTest {
        val repository = repository()
        remote.getBoardsResult = Result.Success(listOf(a))
        remote.getBoardsGate = CompletableDeferred()
        val refresh = async { repository.refresh() }
        runCurrent()

        repository.clear()
        remote.getBoardsGate!!.complete(Unit)
        refresh.await()

        assertThat(repository.boards.value).isNull()
    }

    @Test
    fun refreshAfterClearDoesNotJoinTheOldRequest() = runTest {
        val repository = repository()
        remote.getBoardsGate = CompletableDeferred()
        val old = async { repository.refresh() }
        runCurrent()
        repository.clear()

        remote.getBoardsResult = Result.Success(listOf(b))
        val fresh = async { repository.refresh() }
        runCurrent()
        remote.getBoardsGate!!.complete(Unit)
        old.await()
        fresh.await()

        assertThat(remote.getBoardsCalls).isEqualTo(2)
        assertThat(repository.boards.value).isEqualTo(listOf(b))
    }

    @Test
    fun saveThatReturnsAfterClearIsDropped() = runTest {
        val repository = repository()
        repository.loaded(a)
        remote.saveGate = CompletableDeferred()
        val create = async { repository.createBoard("New", "NEW") }
        runCurrent()

        repository.clear()
        remote.getBoardsResult = Result.Success(listOf(b))
        repository.refresh()
        remote.saveGate!!.complete(Unit)
        assertThat(create.await()).isInstanceOf<Result.Success<*>>()

        assertThat(repository.boards.value).isEqualTo(listOf(b))
    }

    @Test
    fun createBeforeTheFirstLoadKeepsNull() = runTest {
        val repository = repository()
        repository.createBoard("New", "NEW")
        assertThat(repository.boards.value).isNull()
    }

    @Test
    fun boardsListOrderIsUnchangedByEnsureLoaded() = runTest {
        val repository = repository()
        remote.getBoardsResult = Result.Success(listOf(c, a))
        repository.ensureLoaded()
        assertThat(repository.boards.value!!).containsExactly(c, a)
    }
}
