package com.plcoding.cardiganapp.feature.boards.domain.detail

import com.plcoding.cardiganapp.core.domain.board.BoardContent
import com.plcoding.cardiganapp.core.domain.board.Card
import com.plcoding.cardiganapp.core.domain.board.Column
import com.plcoding.cardiganapp.core.domain.util.EmptyResult
import com.plcoding.cardiganapp.core.domain.util.Result
import kotlin.uuid.Uuid

/** One board's content and its column writes. Every call waits for the server. */
interface BoardDetailRepository {
    suspend fun loadBoard(boardId: Uuid): Result<BoardContent, BoardLoadError>

    /** Adds the column as the board's last column. */
    suspend fun addColumn(boardId: Uuid, name: String): Result<Column, ColumnWriteError>

    suspend fun renameColumn(boardId: Uuid, columnId: Uuid, name: String): Result<Column, ColumnWriteError>

    /** Moves the column to the final [position] and returns all column IDs in their new order. */
    suspend fun moveColumn(boardId: Uuid, columnId: Uuid, position: Int): Result<List<Uuid>, ColumnWriteError>

    /** A column that no longer exists counts as deleted. */
    suspend fun deleteColumn(boardId: Uuid, columnId: Uuid): EmptyResult<ColumnWriteError>
}

/** Creates cards. Owned by boards until card drag & drop adds the move. */
interface CardCreateRepository {
    /** Adds the card at the bottom of the column. */
    suspend fun createCard(boardId: Uuid, columnId: Uuid, title: String): Result<Card, CardCreateError>
}
