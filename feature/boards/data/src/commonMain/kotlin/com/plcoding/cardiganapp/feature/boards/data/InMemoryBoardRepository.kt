package com.plcoding.cardiganapp.feature.boards.data

import com.plcoding.cardiganapp.core.domain.session.SessionScopedStore
import com.plcoding.cardiganapp.core.domain.util.DataError
import com.plcoding.cardiganapp.core.domain.util.EmptyResult
import com.plcoding.cardiganapp.core.domain.util.Result
import com.plcoding.cardiganapp.core.domain.util.asEmptyResult
import com.plcoding.cardiganapp.feature.boards.domain.BoardOrder
import com.plcoding.cardiganapp.feature.boards.domain.BoardRepository
import com.plcoding.cardiganapp.feature.boards.domain.BoardSaveError
import com.plcoding.cardiganapp.feature.boards.domain.BoardSummary
import com.plcoding.cardiganapp.feature.boards.domain.RemoteBoardDataSource
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The app's board store. Keeps the server's order and places a saved board by [BoardOrder].
 * Refreshes run in [scope], so a caller that leaves doesn't cancel a refresh others joined.
 *
 * [clear] ends the session's data: [boards] becomes null, and the result of every refresh or save
 * that started before it is dropped. The session number and the list live in one state that is only
 * changed by compare-and-set, so a result can't be written after a [clear] on any thread.
 */
class InMemoryBoardRepository(
    private val remote: RemoteBoardDataSource,
    private val scope: CoroutineScope,
) : BoardRepository, SessionScopedStore {

    private val store = MutableStateFlow(Snapshot(session = 0, boards = null))
    override val boards: StateFlow<List<BoardSummary>?> = BoardsView(store)

    private val lock = Mutex()
    private var running: RunningRefresh? = null

    override suspend fun refresh(): EmptyResult<DataError.Network> {
        val refresh = lock.withLock {
            val current = store.value.session
            running?.takeIf { it.session == current } ?: startRefresh(current).also { running = it }
        }
        return refresh.result.await()
    }

    override suspend fun ensureLoaded(): EmptyResult<DataError.Network> {
        if (store.value.boards != null) return Result.Success(Unit)
        return refresh()
    }

    override suspend fun createBoard(name: String, key: String): Result<BoardSummary, BoardSaveError> {
        val started = store.value.session
        val result = remote.createBoard(name, key)
        if (result is Result.Success) applySave(started, SaveOp.Upsert(result.data))
        return result
    }

    override suspend fun renameBoard(id: Uuid, name: String): Result<BoardSummary, BoardSaveError> {
        val started = store.value.session
        val result = remote.renameBoard(id, name)
        when {
            result is Result.Success -> applySave(started, SaveOp.Upsert(result.data))
            result is Result.Error && result.error == BoardSaveError.NotFound -> applySave(started, SaveOp.Remove(id))
        }
        return result
    }

    override suspend fun deleteBoard(id: Uuid): EmptyResult<BoardSaveError> {
        val started = store.value.session
        val result = remote.deleteBoard(id)
        if (result is Result.Error && result.error != BoardSaveError.NotFound) return result
        applySave(started, SaveOp.Remove(id))
        return Result.Success(Unit)
    }

    /** Resets [boards] to null and drops the results of requests that are still running. */
    override fun clear() {
        store.update { Snapshot(session = it.session + 1, boards = null) }
    }

    private fun startRefresh(started: Int): RunningRefresh {
        val savesDuring = mutableListOf<SaveOp>()
        val result = scope.async(start = CoroutineStart.LAZY) {
            val response = remote.getBoards()
            lock.withLock {
                if (response is Result.Success) {
                    val merged = savesDuring.fold(response.data) { boards, op -> op.applyTo(boards) }
                    setIfSession(started) { merged }
                }
                if (running?.session == started) running = null
            }
            response.asEmptyResult()
        }
        return RunningRefresh(started, result, savesDuring).also { result.start() }
    }

    private suspend fun applySave(started: Int, op: SaveOp) = lock.withLock {
        running?.takeIf { it.session == started }?.savesDuring?.add(op)
        setIfSession(started) { current -> current?.let(op::applyTo) }
    }

    /** Applies [transform] only while the session is still [started]; checked in the same CAS step as the write. */
    private inline fun setIfSession(started: Int, transform: (List<BoardSummary>?) -> List<BoardSummary>?) {
        store.update { current ->
            if (current.session != started) current else current.copy(boards = transform(current.boards))
        }
    }

    private data class Snapshot(val session: Int, val boards: List<BoardSummary>?)

    /** Read-only view of the list in [source], so readers never see the session number. */
    @OptIn(ExperimentalForInheritanceCoroutinesApi::class)
    private class BoardsView(private val source: StateFlow<Snapshot>) : StateFlow<List<BoardSummary>?> {
        override val value: List<BoardSummary>? get() = source.value.boards
        override val replayCache: List<List<BoardSummary>?> get() = listOf(value)

        override suspend fun collect(collector: FlowCollector<List<BoardSummary>?>): Nothing {
            source.map { it.boards }.distinctUntilChanged().collect(collector)
            awaitCancellation()
        }
    }

    private class RunningRefresh(
        val session: Int,
        val result: Deferred<EmptyResult<DataError.Network>>,
        val savesDuring: MutableList<SaveOp>,
    )

    private sealed interface SaveOp {
        fun applyTo(boards: List<BoardSummary>): List<BoardSummary>

        data class Upsert(val board: BoardSummary) : SaveOp {
            override fun applyTo(boards: List<BoardSummary>) = BoardOrder.replace(boards, board)
        }

        data class Remove(val id: Uuid) : SaveOp {
            override fun applyTo(boards: List<BoardSummary>) = boards.filterNot { it.id == id }
        }
    }
}
