package com.plcoding.cardiganapp.feature.boards.data.fixture

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import com.plcoding.cardiganapp.core.domain.util.DataError
import com.plcoding.cardiganapp.core.domain.util.Result
import com.plcoding.cardiganapp.feature.boards.domain.BoardOrder
import com.plcoding.cardiganapp.feature.boards.domain.BoardSaveError
import com.plcoding.cardiganapp.feature.boards.domain.BoardSummary
import com.plcoding.cardiganapp.feature.boards.domain.KeyFieldError
import kotlin.test.Test
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest

class FixtureBoardDataSourceTest {

    private class FakeClock(var now: Instant = Instant.parse("2026-11-21T10:00:00Z")) : Clock {
        override fun now(): Instant = now
    }

    private val clock = FakeClock()
    private val source = FixtureBoardDataSource(clock, latency = Duration.ZERO)

    private suspend fun boards(): List<BoardSummary> = (source.getBoards() as Result.Success).data

    @Test
    fun startsWithTheSixFigmaBoardsInServerOrder() = runTest {
        val boards = boards()
        assertThat(boards.map { it.key }).isEqualTo(listOf("CRD", "RAY", "TALK", "HOME", "DOT", "READ"))
        assertThat(boards.sortedWith(BoardOrder)).isEqualTo(boards)
    }

    @Test
    fun createWithAFailNameFailsWithANetworkError() = runTest {
        assertThat(source.createBoard("Fail on purpose", "FAIL"))
            .isEqualTo(Result.Error(BoardSaveError.Network(DataError.Network.NO_INTERNET)))
    }

    @Test
    fun renameWithAFailNameFailsWithANetworkError() = runTest {
        val board = boards().first()
        assertThat(source.renameBoard(board.id, "fail"))
            .isEqualTo(Result.Error(BoardSaveError.Network(DataError.Network.NO_INTERNET)))
    }

    @Test
    fun usedKeyFailsWithAlreadyUsed() = runTest {
        assertThat(source.createBoard("Something", "USED"))
            .isEqualTo(Result.Error(BoardSaveError.InvalidFields(key = KeyFieldError.AlreadyUsed)))
    }

    @Test
    fun goneBoardIsNotFoundOnRenameAndDelete() = runTest {
        val gone = (source.createBoard("Gone", "GONE") as Result.Success).data
        assertThat(source.renameBoard(gone.id, "Back")).isEqualTo(Result.Error(BoardSaveError.NotFound))
        assertThat(boards().none { it.id == gone.id }).isTrue()

        val goneAgain = (source.createBoard("Gone", "GONE") as Result.Success).data
        assertThat(source.deleteBoard(goneAgain.id)).isEqualTo(Result.Error(BoardSaveError.NotFound))
    }

    @Test
    fun refreshWithinTwoSecondsOfAFailedSaveFails() = runTest {
        source.createBoard("fail", "FAIL")
        clock.now += 2.seconds
        assertThat(source.getBoards()).isEqualTo(Result.Error(DataError.Network.NO_INTERNET))
        clock.now += 1.seconds
        assertThat(source.getBoards()).isInstanceOf<Result.Success<*>>()
    }

    @Test
    fun createdBoardIsListedFirst() = runTest {
        clock.now += 1.seconds
        val created = (source.createBoard("Garden irrigation", "GARD") as Result.Success).data
        val boards = boards()
        assertThat(boards).hasSize(7)
        assertThat(boards.first()).isEqualTo(created)
        assertThat(created.id).isNotNull()
    }
}
