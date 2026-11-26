package com.plcoding.cardiganapp.feature.boards.data

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.plcoding.cardiganapp.core.domain.util.DataError
import com.plcoding.cardiganapp.core.domain.util.Result
import com.plcoding.cardiganapp.feature.boards.domain.BoardSaveError
import kotlin.test.Test
import kotlin.uuid.Uuid
import kotlinx.coroutines.test.runTest

/* Break vectors: every endpoint, since the board list must show its error state for each one. */
class NotConnectedRemoteBoardDataSourceTest {

    private val source = NotConnectedRemoteBoardDataSource()
    private val networkFailure = Result.Error(BoardSaveError.Network(DataError.Network.UNKNOWN))

    @Test
    fun loadingBoardsFailsWithANetworkError() = runTest {
        assertThat(source.getBoards()).isEqualTo(Result.Error(DataError.Network.UNKNOWN))
    }

    @Test
    fun everySaveFailsWithANetworkError() = runTest {
        assertThat(source.createBoard("Garden", "GARD")).isEqualTo(networkFailure)
        assertThat(source.renameBoard(Uuid.random(), "Garden")).isEqualTo(networkFailure)
        assertThat(source.deleteBoard(Uuid.random())).isEqualTo(networkFailure)
    }
}
