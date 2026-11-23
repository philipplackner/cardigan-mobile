package com.plcoding.cardiganapp.feature.boards.data

import com.plcoding.cardiganapp.core.domain.util.DataError
import com.plcoding.cardiganapp.core.domain.util.EmptyResult
import com.plcoding.cardiganapp.core.domain.util.Result
import com.plcoding.cardiganapp.feature.boards.domain.BoardSaveError
import com.plcoding.cardiganapp.feature.boards.domain.BoardSummary
import com.plcoding.cardiganapp.feature.boards.domain.RemoteBoardDataSource
import kotlin.uuid.Uuid

/**
 * Stands in for the board endpoints until `mob-boards-connect` binds the Ktor data source. Every
 * call fails with a network error, so the board list shows its load error state.
 */
class NotConnectedRemoteBoardDataSource : RemoteBoardDataSource {
    override suspend fun getBoards(): Result<List<BoardSummary>, DataError.Network> = Result.Error(DataError.Network.UNKNOWN)

    override suspend fun createBoard(name: String, key: String): Result<BoardSummary, BoardSaveError> = failed()

    override suspend fun renameBoard(id: Uuid, name: String): Result<BoardSummary, BoardSaveError> = failed()

    override suspend fun deleteBoard(id: Uuid): EmptyResult<BoardSaveError> = failed()

    private fun <T> failed(): Result<T, BoardSaveError> = Result.Error(BoardSaveError.Network(DataError.Network.UNKNOWN))
}
