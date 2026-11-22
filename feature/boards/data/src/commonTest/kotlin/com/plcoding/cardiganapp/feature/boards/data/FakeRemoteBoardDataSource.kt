package com.plcoding.cardiganapp.feature.boards.data

import com.plcoding.cardiganapp.core.domain.util.DataError
import com.plcoding.cardiganapp.core.domain.util.EmptyResult
import com.plcoding.cardiganapp.core.domain.util.Result
import com.plcoding.cardiganapp.feature.boards.domain.BoardSaveError
import com.plcoding.cardiganapp.feature.boards.domain.BoardSummary
import com.plcoding.cardiganapp.feature.boards.domain.RemoteBoardDataSource
import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.coroutines.CompletableDeferred

class FakeRemoteBoardDataSource : RemoteBoardDataSource {
    var getBoardsCalls = 0
        private set
    var getBoardsResult: Result<List<BoardSummary>, DataError.Network> = Result.Success(emptyList())
    var createResult: Result<BoardSummary, BoardSaveError>? = null
    var renameResult: Result<BoardSummary, BoardSaveError>? = null
    var deleteResult: EmptyResult<BoardSaveError> = Result.Success(Unit)

    /** When set, the matching call suspends until it completes. */
    var getBoardsGate: CompletableDeferred<Unit>? = null
    var saveGate: CompletableDeferred<Unit>? = null

    override suspend fun getBoards(): Result<List<BoardSummary>, DataError.Network> {
        getBoardsCalls++
        getBoardsGate?.await()
        return getBoardsResult
    }

    override suspend fun createBoard(name: String, key: String): Result<BoardSummary, BoardSaveError> {
        saveGate?.await()
        return createResult ?: Result.Success(summary(name = name, key = key))
    }

    override suspend fun renameBoard(id: Uuid, name: String): Result<BoardSummary, BoardSaveError> {
        saveGate?.await()
        return renameResult ?: error("renameResult not set")
    }

    override suspend fun deleteBoard(id: Uuid): EmptyResult<BoardSaveError> {
        saveGate?.await()
        return deleteResult
    }
}

fun summary(
    id: String = Uuid.random().toString(),
    name: String = "Board",
    key: String = "BRD",
    cardCount: Int = 0,
    updated: String = "2026-11-20T10:00:00.000Z",
    created: String = "2026-11-01T10:00:00.000Z",
) = BoardSummary(
    id = Uuid.parse(id),
    name = name,
    key = key,
    cardCount = cardCount,
    createdAt = Instant.parse(created),
    updatedAt = Instant.parse(updated),
)
