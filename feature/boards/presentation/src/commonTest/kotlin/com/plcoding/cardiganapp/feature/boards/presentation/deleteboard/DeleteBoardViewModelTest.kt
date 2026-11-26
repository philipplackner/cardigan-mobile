package com.plcoding.cardiganapp.feature.boards.presentation.deleteboard

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import cardiganapp.feature.boards.presentation.generated.resources.Res
import cardiganapp.feature.boards.presentation.generated.resources.delete_board_body
import cardiganapp.feature.boards.presentation.generated.resources.delete_board_body_empty
import cardiganapp.feature.boards.presentation.generated.resources.delete_board_failed
import com.plcoding.cardiganapp.core.domain.util.DataError
import com.plcoding.cardiganapp.core.domain.util.Result
import com.plcoding.cardiganapp.feature.boards.domain.BoardSaveError
import com.plcoding.cardiganapp.feature.boards.presentation.fakes.FakeBoardRepository
import com.plcoding.cardiganapp.feature.boards.presentation.fakes.FakeToastController
import com.plcoding.cardiganapp.feature.boards.presentation.fakes.board
import com.plcoding.cardiganapp.feature.boards.presentation.util.assertPlural
import com.plcoding.cardiganapp.feature.boards.presentation.util.assertResource
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/*
 * Break vectors:
 * - name and card count shown; body text for zero, one and many cards
 * - success, already gone on the server (counts as deleted), network failure (toast, stays)
 * - deleting ignores Cancel and a second Delete
 * - board not in the list closes; actions before the board is found are ignored
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DeleteBoardViewModelTest {

    private val cardigan = board(id = "0b5c8a6e-1f00-4a3c-9a43-6f5e0e3c1d2a", name = "Cardigan v1", key = "CRD", cardCount = 12)
    private val repository = FakeBoardRepository(listOf(cardigan))
    private val toasts = FakeToastController()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(id: String = cardigan.id.toString()) = DeleteBoardViewModel(id, repository, toasts)

    @Test
    fun showsTheNameAndCardCount() {
        assertThat(viewModel().state.value).isEqualTo(DeleteBoardState("Cardigan v1", 12))
    }

    @Test
    fun bodyForZeroOneAndMany() {
        assertResource(deleteBoardBody(0), Res.string.delete_board_body_empty)
        assertPlural(deleteBoardBody(1), Res.plurals.delete_board_body, 1)
        assertPlural(deleteBoardBody(12), Res.plurals.delete_board_body, 12)
    }

    @Test
    fun successReportsDeleted() = runTest {
        val viewModel = viewModel()
        viewModel.events.test {
            viewModel.onAction(DeleteBoardAction.Delete)
            assertThat(awaitItem()).isEqualTo(DeleteBoardEvent.Deleted)
        }
        assertThat(repository.state.value).isEqualTo(emptyList())
        assertThat(toasts.messages).isEmpty()
    }

    @Test
    fun notFoundCountsAsDeleted() = runTest {
        repository.deleteResult = Result.Error(BoardSaveError.NotFound)
        val viewModel = viewModel()
        viewModel.events.test {
            viewModel.onAction(DeleteBoardAction.Delete)
            assertThat(awaitItem()).isEqualTo(DeleteBoardEvent.Deleted)
        }
        assertThat(toasts.messages).isEmpty()
    }

    @Test
    fun networkFailureShowsTheToastAndStays() = runTest {
        repository.deleteResult = Result.Error(BoardSaveError.Network(DataError.Network.NO_INTERNET))
        val viewModel = viewModel()
        viewModel.events.test {
            viewModel.onAction(DeleteBoardAction.Delete)
            expectNoEvents()
        }
        assertResource(toasts.messages.single().text, Res.string.delete_board_failed, "Cardigan v1")
        assertThat(viewModel.state.value.isDeleting).isFalse()
    }

    @Test
    fun deletingIgnoresCancelAndASecondDelete() = runTest {
        repository.gate = CompletableDeferred()
        val viewModel = viewModel()
        viewModel.events.test {
            viewModel.onAction(DeleteBoardAction.Delete)
            assertThat(viewModel.state.value.isDeleting).isTrue()
            viewModel.onAction(DeleteBoardAction.Cancel)
            viewModel.onAction(DeleteBoardAction.Delete)
            expectNoEvents()
            repository.gate!!.complete(Unit)
            assertThat(awaitItem()).isEqualTo(DeleteBoardEvent.Deleted)
        }
        assertThat(repository.deleteCalls).hasSize(1)
    }

    @Test
    fun missingBoardCloses() = runTest {
        val viewModel = viewModel(id = "00000000-0000-0000-0000-000000000000")
        viewModel.events.test { assertThat(awaitItem()).isEqualTo(DeleteBoardEvent.Close) }
    }

    @Test
    fun actionsBeforeTheBoardIsFoundAreIgnored() = runTest {
        repository.state.value = null
        repository.serverBoards = listOf(cardigan)
        repository.gate = CompletableDeferred()
        val viewModel = viewModel()
        viewModel.events.test {
            viewModel.onAction(DeleteBoardAction.Delete)
            viewModel.onAction(DeleteBoardAction.Cancel)
            expectNoEvents()
        }
        assertThat(repository.deleteCalls).isEmpty()
    }
}
