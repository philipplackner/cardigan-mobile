package com.plcoding.cardiganapp.feature.boards.data

import com.plcoding.cardiganapp.core.domain.util.DataError
import com.plcoding.cardiganapp.core.domain.util.EmptyResult
import com.plcoding.cardiganapp.core.domain.util.Result
import com.plcoding.cardiganapp.core.domain.util.asEmptyResult
import com.plcoding.cardiganapp.feature.boards.domain.BoardOrder
import com.plcoding.cardiganapp.feature.boards.domain.BoardRepository
import com.plcoding.cardiganapp.feature.boards.domain.BoardSaveError
import com.plcoding.cardiganapp.feature.boards.domain.BoardSummary
import com.plcoding.cardiganapp.feature.boards.domain.RemoteBoardDataSource
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The app's board store. Keeps the server's order and places a saved board by [BoardOrder].
 * Refreshes run in [scope], so a caller that leaves doesn't cancel a refresh others joined.
 *
 * [clear] ends the session's data: [boards] becomes null, and the result of every refresh or save
 * that started before it is dropped.
 */
@OptIn(ExperimentalAtomicApi::class)
class InMemoryBoardRepository(
    private val remote: RemoteBoardDataSource,
    private val scope: CoroutineScope,
) : BoardRepository {

    private val _boards = MutableStateFlow<List<BoardSummary>?>(null)
    override val boards: StateFlow<List<BoardSummary>?> = _boards.asStateFlow()

    private val session = AtomicInt(0)
    private val lock = Mutex()
    private var running: RunningRefresh? = null

    override suspend fun refresh(): EmptyResult<DataError.Network> {
        val refresh = lock.withLock {
            val current = session.load()
            running?.takeIf { it.session == current } ?: startRefresh(current).also { running = it }
        }
        return refresh.result.await()
    }

    override suspend fun ensureLoaded(): EmptyResult<DataError.Network> {
        if (_boards.value != null) return Result.Success(Unit)
        return refresh()
    }

    override suspend fun createBoard(name: String, key: String): Result<BoardSummary, BoardSaveError> {
        val started = session.load()
        val result = remote.createBoard(name, key)
        if (result is Result.Success) applySave(started, SaveOp.Upsert(result.data))
        return result
    }

    override suspend fun renameBoard(id: Uuid, name: String): Result<BoardSummary, BoardSaveError> {
        val started = session.load()
        val result = remote.renameBoard(id, name)
        when {
            result is Result.Success -> applySave(started, SaveOp.Upsert(result.data))
            result is Result.Error && result.error == BoardSaveError.NotFound -> applySave(started, SaveOp.Remove(id))
        }
        return result
    }

    override suspend fun deleteBoard(id: Uuid): EmptyResult<BoardSaveError> {
        val started = session.load()
        val result = remote.deleteBoard(id)
        if (result is Result.Error && result.error != BoardSaveError.NotFound) return result
        applySave(started, SaveOp.Remove(id))
        return Result.Success(Unit)
    }

    /** Resets [boards] to null and drops the results of requests that are still running. */
    fun clear() {
        session.incrementAndFetch()
        _boards.value = null
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

    private inline fun setIfSession(started: Int, transform: (List<BoardSummary>?) -> List<BoardSummary>?) {
        _boards.update { current -> if (session.load() == started) transform(current) else current }
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
