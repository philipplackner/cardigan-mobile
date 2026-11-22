package com.plcoding.cardiganapp.feature.boards.domain

import com.plcoding.cardiganapp.core.domain.util.DataError
import com.plcoding.cardiganapp.core.domain.util.EmptyResult
import com.plcoding.cardiganapp.core.domain.util.Result
import kotlinx.coroutines.flow.StateFlow
import kotlin.uuid.Uuid

/** The one board store that the board list, the sidebar, the board dialogs and the key lookup read. */
interface BoardRepository {
    /** The loaded boards in the server's order, or null before the first successful load. */
    val boards: StateFlow<List<BoardSummary>?>

    /** Reloads the list. Concurrent calls share one request. A failure keeps the current list. */
    suspend fun refresh(): EmptyResult<DataError.Network>

    /** Returns at once when [boards] is loaded, otherwise joins the running load or starts one. */
    suspend fun ensureLoaded(): EmptyResult<DataError.Network>

    suspend fun createBoard(name: String, key: String): Result<BoardSummary, BoardSaveError>

    /** A [BoardSaveError.NotFound] also removes the board from [boards]. */
    suspend fun renameBoard(id: Uuid, name: String): Result<BoardSummary, BoardSaveError>

    /** A board that no longer exists on the server counts as deleted. */
    suspend fun deleteBoard(id: Uuid): EmptyResult<BoardSaveError>
}
