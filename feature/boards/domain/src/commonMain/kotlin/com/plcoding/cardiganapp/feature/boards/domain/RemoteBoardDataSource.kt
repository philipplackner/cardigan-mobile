package com.plcoding.cardiganapp.feature.boards.domain

import com.plcoding.cardiganapp.core.domain.util.DataError
import com.plcoding.cardiganapp.core.domain.util.EmptyResult
import com.plcoding.cardiganapp.core.domain.util.Result
import kotlin.uuid.Uuid

/** The board endpoints. Errors arrive already typed; mapping ProblemDetail codes is the data source's job. */
interface RemoteBoardDataSource {
    suspend fun getBoards(): Result<List<BoardSummary>, DataError.Network>

    suspend fun createBoard(name: String, key: String): Result<BoardSummary, BoardSaveError>

    suspend fun renameBoard(id: Uuid, name: String): Result<BoardSummary, BoardSaveError>

    suspend fun deleteBoard(id: Uuid): EmptyResult<BoardSaveError>
}
