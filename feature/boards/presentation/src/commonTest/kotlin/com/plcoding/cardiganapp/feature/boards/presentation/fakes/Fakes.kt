package com.plcoding.cardiganapp.feature.boards.presentation.fakes

import com.plcoding.cardiganapp.core.domain.util.DataError
import com.plcoding.cardiganapp.core.domain.util.EmptyResult
import com.plcoding.cardiganapp.core.domain.util.Result
import com.plcoding.cardiganapp.core.presentation.toast.ToastController
import com.plcoding.cardiganapp.core.presentation.toast.ToastMessage
import com.plcoding.cardiganapp.feature.boards.domain.BoardOrder
import com.plcoding.cardiganapp.feature.boards.domain.BoardRepository
import com.plcoding.cardiganapp.feature.boards.domain.BoardSaveError
import com.plcoding.cardiganapp.feature.boards.domain.BoardSummary
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

/** A board store with scripted results. Successful saves update [boards] like the real store. */
class FakeBoardRepository(
    initial: List<BoardSummary>? = null,
) : BoardRepository {
    val state = MutableStateFlow(initial)
    override val boards: StateFlow<List<BoardSummary>?> = state

    var refreshCalls = 0
        private set
    var ensureLoadedCalls = 0
        private set
    val createCalls = mutableListOf<Pair<String, String>>()
    val renameCalls = mutableListOf<Pair<Uuid, String>>()
    val deleteCalls = mutableListOf<Uuid>()

    /** The list a successful refresh loads. Null keeps the current one. */
    var serverBoards: List<BoardSummary>? = null
    var refreshResult: EmptyResult<DataError.Network> = Result.Success(Unit)
    var createResult: Result<BoardSummary, BoardSaveError>? = null
    var renameResult: Result<BoardSummary, BoardSaveError>? = null
    var deleteResult: EmptyResult<BoardSaveError> = Result.Success(Unit)

    /** When set, refresh and every save suspend until it completes. */
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun refresh(): EmptyResult<DataError.Network> {
        refreshCalls++
        gate?.await()
        if (refreshResult is Result.Success) state.value = serverBoards ?: state.value ?: emptyList()
        return refreshResult
    }

    override suspend fun ensureLoaded(): EmptyResult<DataError.Network> {
        ensureLoadedCalls++
        if (state.value != null) return Result.Success(Unit)
        return refresh()
    }

    override suspend fun createBoard(
        name: String,
        key: String,
    ): Result<BoardSummary, BoardSaveError> {
        createCalls += name to key
        gate?.await()
        val result = createResult ?: Result.Success(board(name = name, key = key, updated = "2026-11-21T09:59:00Z"))
        if (result is Result.Success) state.value = state.value?.let { BoardOrder.replace(it, result.data) }
        return result
    }

    override suspend fun renameBoard(
        id: Uuid,
        name: String,
    ): Result<BoardSummary, BoardSaveError> {
        renameCalls += id to name
        gate?.await()
        val current = state.value?.firstOrNull { it.id == id }
        val result =
            renameResult ?: current?.let { Result.Success(it.copy(name = name)) }
                ?: Result.Error(BoardSaveError.NotFound)
        when {
            result is Result.Success -> {
                state.value = state.value?.let { BoardOrder.replace(it, result.data) }
            }

            result is Result.Error && result.error == BoardSaveError.NotFound -> {
                state.value =
                    state.value?.filterNot { it.id == id }
            }
        }
        return result
    }

    override suspend fun deleteBoard(id: Uuid): EmptyResult<BoardSaveError> {
        deleteCalls += id
        gate?.await()
        val result = deleteResult
        if (result is Result.Error && result.error != BoardSaveError.NotFound) return result
        state.value = state.value?.filterNot { it.id == id }
        return Result.Success(Unit)
    }
}

class FakeToastController : ToastController {
    val messages = mutableListOf<ToastMessage>()

    override fun show(message: ToastMessage) {
        messages += message
    }
}

class FakeClock(
    var now: Instant = Instant.parse("2026-11-21T10:00:00Z"),
) : Clock {
    override fun now(): Instant = now
}

fun board(
    id: String = Uuid.random().toString(),
    name: String = "Board",
    key: String = "BRD",
    cardCount: Int = 0,
    updated: String = "2026-11-21T08:00:00Z",
    created: String = "2026-11-01T10:00:00Z",
) = BoardSummary(
    id = Uuid.parse(id),
    name = name,
    key = key,
    cardCount = cardCount,
    createdAt = Instant.parse(created),
    updatedAt = Instant.parse(updated),
)
