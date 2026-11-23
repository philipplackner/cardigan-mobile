package com.plcoding.cardiganapp.feature.boards.data.fixture

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.plcoding.cardiganapp.core.domain.util.Result
import com.plcoding.cardiganapp.feature.boards.data.InMemoryBoardRepository
import com.plcoding.cardiganapp.feature.boards.domain.detail.BoardLoadError
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.uuid.Uuid
import kotlinx.coroutines.test.runTest

/** The temporary board view fixture: the sample board for `CRD`, default columns elsewhere, a missing board. */
class FixtureBoardDetailTest {

    @Test
    fun sampleAndNewBoards() = runTest {
        val boards = InMemoryBoardRepository(FixtureBoardDataSource(latency = Duration.ZERO), backgroundScope)
        val repository = FixtureBoardDetailRepository(FixtureBoardStore(boards, latency = Duration.ZERO))
        boards.ensureLoaded()
        val all = boards.boards.value!!
        val sample = repository.loadBoard(all.first { it.key == "CRD" }.id) as Result.Success
        assertThat(sample.data.columns.map { it.name }).containsExactly("To do", "In progress", "Review", "Done")
        assertThat(sample.data.cardCount).isEqualTo(12)
        val other = repository.loadBoard(all.first { it.key == "RAY" }.id) as Result.Success
        assertThat(other.data.columns.map { it.name }).containsExactly("To do", "In progress", "Done")
        assertThat(repository.loadBoard(Uuid.random())).isInstanceOf<Result.Error<BoardLoadError>>()
    }
}
