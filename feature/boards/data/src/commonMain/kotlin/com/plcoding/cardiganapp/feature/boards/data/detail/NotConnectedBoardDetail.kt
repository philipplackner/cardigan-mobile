package com.plcoding.cardiganapp.feature.boards.data.detail

import com.plcoding.cardiganapp.core.domain.board.BoardContent
import com.plcoding.cardiganapp.core.domain.board.Card
import com.plcoding.cardiganapp.core.domain.board.Column
import com.plcoding.cardiganapp.core.domain.label.Label
import com.plcoding.cardiganapp.core.domain.label.LabelSource
import com.plcoding.cardiganapp.core.domain.util.DataError
import com.plcoding.cardiganapp.core.domain.util.EmptyResult
import com.plcoding.cardiganapp.core.domain.util.Result
import com.plcoding.cardiganapp.feature.boards.domain.detail.BoardDetailRepository
import com.plcoding.cardiganapp.feature.boards.domain.detail.BoardLoadError
import com.plcoding.cardiganapp.feature.boards.domain.detail.CardCreateError
import com.plcoding.cardiganapp.feature.boards.domain.detail.CardCreateRepository
import com.plcoding.cardiganapp.feature.boards.domain.detail.ColumnWriteError
import kotlin.uuid.Uuid

/**
 * Stands in for the board endpoints until `mob-boards-connect` binds the Ktor repository. Every
 * call fails, so the board view shows its load error state.
 */
class NotConnectedBoardDetailRepository : BoardDetailRepository {
    override suspend fun loadBoard(boardId: Uuid): Result<BoardContent, BoardLoadError> = Result.Error(BoardLoadError.Failed)

    override suspend fun addColumn(boardId: Uuid, name: String): Result<Column, ColumnWriteError> = Result.Error(ColumnWriteError.Failed)

    override suspend fun renameColumn(boardId: Uuid, columnId: Uuid, name: String): Result<Column, ColumnWriteError> =
        Result.Error(ColumnWriteError.Failed)

    override suspend fun moveColumn(boardId: Uuid, columnId: Uuid, position: Int): Result<List<Uuid>, ColumnWriteError> =
        Result.Error(ColumnWriteError.Failed)

    override suspend fun deleteColumn(boardId: Uuid, columnId: Uuid): EmptyResult<ColumnWriteError> = Result.Error(ColumnWriteError.Failed)
}

/** Stands in for card create until `mob-cards-connect` binds the Ktor repository. Every call fails. */
class NotConnectedCardCreateRepository : CardCreateRepository {
    override suspend fun createCard(boardId: Uuid, columnId: Uuid, title: String): Result<Card, CardCreateError> =
        Result.Error(CardCreateError.Failed)
}

/** Stands in for the user's labels until the labels feature implements [LabelSource]. Every call fails. */
class NotConnectedLabelSource : LabelSource {
    override suspend fun labels(): Result<List<Label>, DataError> = Result.Error(DataError.Network.UNKNOWN)
}
