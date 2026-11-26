package com.plcoding.cardiganapp.feature.boards.data.detail

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.plcoding.cardiganapp.core.domain.util.DataError
import com.plcoding.cardiganapp.core.domain.util.Result
import com.plcoding.cardiganapp.feature.boards.domain.detail.BoardLoadError
import com.plcoding.cardiganapp.feature.boards.domain.detail.CardCreateError
import com.plcoding.cardiganapp.feature.boards.domain.detail.ColumnWriteError
import kotlin.test.Test
import kotlin.uuid.Uuid
import kotlinx.coroutines.test.runTest

/* Break vectors: every call of every stand-in, since the board view must show its error state for each one. */
class NotConnectedBoardDetailTest {

    private val boardId = Uuid.random()
    private val columnId = Uuid.random()

    @Test
    fun boardDetailCallsAllFail() = runTest {
        val repository = NotConnectedBoardDetailRepository()

        assertThat(repository.loadBoard(boardId)).isEqualTo(Result.Error(BoardLoadError.Failed))
        assertThat(repository.addColumn(boardId, "To do")).isEqualTo(Result.Error(ColumnWriteError.Failed))
        assertThat(repository.renameColumn(boardId, columnId, "Done")).isEqualTo(Result.Error(ColumnWriteError.Failed))
        assertThat(repository.moveColumn(boardId, columnId, 0)).isEqualTo(Result.Error(ColumnWriteError.Failed))
        assertThat(repository.deleteColumn(boardId, columnId)).isEqualTo(Result.Error(ColumnWriteError.Failed))
    }

    @Test
    fun cardCreateFails() = runTest {
        assertThat(NotConnectedCardCreateRepository().createCard(boardId, columnId, "Fix login"))
            .isEqualTo(Result.Error(CardCreateError.Failed))
    }

    @Test
    fun labelsFailWithANetworkError() = runTest {
        assertThat(NotConnectedLabelSource().labels()).isEqualTo(Result.Error(DataError.Network.UNKNOWN))
    }
}
