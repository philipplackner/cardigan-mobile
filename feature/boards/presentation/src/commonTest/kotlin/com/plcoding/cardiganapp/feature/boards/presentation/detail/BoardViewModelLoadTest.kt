package com.plcoding.cardiganapp.feature.boards.presentation.detail

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import cardiganapp.feature.boards.presentation.generated.resources.Res
import cardiganapp.feature.boards.presentation.generated.resources.board_view_board_gone
import cardiganapp.feature.boards.presentation.generated.resources.board_view_refresh_failed
import com.plcoding.cardiganapp.core.domain.util.DataError
import com.plcoding.cardiganapp.core.domain.util.Result
import com.plcoding.cardiganapp.feature.boards.domain.detail.BoardLoadError
import com.plcoding.cardiganapp.feature.boards.presentation.fakes.card
import com.plcoding.cardiganapp.feature.boards.presentation.fakes.label
import com.plcoding.cardiganapp.feature.boards.presentation.util.assertResource
import kotlin.test.Test
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest

/*
 * Break vectors:
 * - header with and without the board list's hint, while loading, after a failure, after load
 * - board fails, labels fail, both fail, board missing (404) on first load and on refresh
 * - retry only from the error state, and it shows the skeleton again
 * - refresh: picks up changes, failure keeps the board with one toast, a second pull while one runs
 * - labels arrive in a map; a card with an unknown label ID keeps its ID (the tile skips it)
 * - board renamed from the board menu updates the header; board deleted emits BoardDeleted
 */
class BoardViewModelLoadTest : BoardViewModelTestBase() {

    @Test
    fun loadsBoardAndLabels() {
        val api = label("api")
        labels.labels = listOf(api)
        val viewModel = viewModel()
        assertThat(viewModel.columnNames).containsExactly("To do", "In progress", "Review", "Done")
        val loaded = viewModel.state.value.loaded!!
        assertThat(loaded.labelsById).isEqualTo(mapOf(api.id to api))
        assertThat(viewModel.board.cardCount).isEqualTo(5)
    }

    @Test
    fun headerUsesTheHintWhileLoading() {
        repository.gates += CompletableDeferred()
        val viewModel = viewModel(handle(name = "Cardigan", key = "CRD"))
        val state = viewModel.state.value
        assertThat(state.content).isEqualTo(BoardLoadState.Loading)
        assertThat(state.headerName).isEqualTo("Cardigan")
        assertThat(state.headerKey).isEqualTo("CRD")
    }

    @Test
    fun headerWithoutHintHasNoTitleUntilLoaded() {
        repository.loadResults += Result.Error(BoardLoadError.Failed)
        val viewModel = viewModel()
        assertThat(viewModel.state.value.content).isEqualTo(BoardLoadState.Error)
        assertThat(viewModel.state.value.headerName).isNull()
        assertThat(viewModel.state.value.headerKey).isNull()
    }

    @Test
    fun loadedBoardWinsOverTheHint() {
        val viewModel = viewModel(handle(name = "Old name", key = "OLD"))
        assertThat(viewModel.state.value.headerName).isEqualTo("Cardigan v1")
        assertThat(viewModel.state.value.headerKey).isEqualTo("CRD")
    }

    @Test
    fun hintNeedsBothNameAndKey() {
        repository.gates += CompletableDeferred()
        val viewModel = viewModel(handle(name = "Cardigan"))
        assertThat(viewModel.state.value.headerHint).isNull()
    }

    @Test
    fun labelsFailingFailsTheLoad() {
        labels.results += Result.Error(DataError.Network.NO_INTERNET)
        val viewModel = viewModel()
        assertThat(viewModel.state.value.content).isEqualTo(BoardLoadState.Error)
        assertThat(toasts.messages).containsExactly()
    }

    @Test
    fun retryLoadsAgainWithTheSkeleton() {
        repository.loadResults += Result.Error(BoardLoadError.Failed)
        val viewModel = viewModel()
        val gate = CompletableDeferred<Unit>()
        repository.gates += gate
        viewModel.onAction(BoardAction.Retry)
        assertThat(viewModel.state.value.content).isEqualTo(BoardLoadState.Loading)
        gate.complete(Unit)
        assertThat(viewModel.columnNames).containsExactly("To do", "In progress", "Review", "Done")
    }

    @Test
    fun retryIsIgnoredOnceLoaded() {
        val viewModel = viewModel()
        viewModel.onAction(BoardAction.Retry)
        assertThat(repository.calls).containsExactly("load")
    }

    @Test
    fun missingBoardOnFirstLoad() = runTest {
        repository.loadResults += Result.Error(BoardLoadError.BoardNotFound)
        val viewModel = viewModel()
        val events = events(viewModel)
        assertThat(events).containsExactly(BoardEvent.BoardMissing)
        assertThat(toasts.messages.size).isEqualTo(1)
        assertResource(toasts.messages.single().text, Res.string.board_view_board_gone)
    }

    @Test
    fun missingBoardWinsOverFailingLabels() = runTest {
        repository.loadResults += Result.Error(BoardLoadError.BoardNotFound)
        labels.results += Result.Error(DataError.Network.NO_INTERNET)
        val viewModel = viewModel()
        assertThat(events(viewModel)).containsExactly(BoardEvent.BoardMissing)
    }

    @Test
    fun refreshPicksUpChanges() {
        val viewModel = viewModel()
        repository.server = repository.server.copy(
            columns = repository.server.columns.map { if (it.id == done.id) it.copy(cards = listOf(card(106))) else it },
        )
        viewModel.onAction(BoardAction.Refresh(todo.id))
        assertThat(viewModel.board.cardCount).isEqualTo(6)
        assertThat(viewModel.state.value.isRefreshing).isFalse()
    }

    @Test
    fun refreshShowsTheIndicatorInThePulledColumn() {
        val viewModel = viewModel()
        val gate = CompletableDeferred<Unit>()
        repository.gates += gate
        viewModel.onAction(BoardAction.Refresh(review.id))
        assertThat(viewModel.state.value.isRefreshing).isTrue()
        assertThat(viewModel.state.value.refreshingColumnId).isEqualTo(review.id)
        gate.complete(Unit)
        assertThat(viewModel.state.value.refreshingColumnId).isNull()
    }

    @Test
    fun secondPullWhileRefreshingIsIgnored() {
        val viewModel = viewModel()
        val gate = CompletableDeferred<Unit>()
        repository.gates += gate
        viewModel.onAction(BoardAction.Refresh(todo.id))
        viewModel.onAction(BoardAction.Refresh(done.id))
        gate.complete(Unit)
        assertThat(repository.calls).containsExactly("load", "load")
    }

    @Test
    fun refreshFailureKeepsTheBoardAndToastsOnce() {
        val viewModel = viewModel()
        repository.loadResults += Result.Error(BoardLoadError.Failed)
        viewModel.onAction(BoardAction.Refresh(todo.id))
        assertThat(viewModel.columnNames).containsExactly("To do", "In progress", "Review", "Done")
        assertThat(toasts.messages.size).isEqualTo(1)
        assertResource(toasts.messages.single().text, Res.string.board_view_refresh_failed)
    }

    @Test
    fun refreshWithFailingLabelsKeepsTheBoard() {
        val viewModel = viewModel()
        labels.results += Result.Error(DataError.Network.SERVER_ERROR)
        viewModel.onAction(BoardAction.Refresh(null))
        assertThat(viewModel.columnNames).containsExactly("To do", "In progress", "Review", "Done")
        assertResource(toasts.messages.single().text, Res.string.board_view_refresh_failed)
    }

    @Test
    fun refreshFindsTheBoardMissing() = runTest {
        val viewModel = viewModel()
        val events = events(viewModel)
        repository.loadResults += Result.Error(BoardLoadError.BoardNotFound)
        viewModel.onAction(BoardAction.Refresh(todo.id))
        assertThat(events).containsExactly(BoardEvent.BoardMissing)
        assertResource(toasts.messages.single().text, Res.string.board_view_board_gone)
    }

    @Test
    fun refreshBeforeTheFirstLoadIsIgnored() {
        repository.loadResults += Result.Error(BoardLoadError.Failed)
        val viewModel = viewModel()
        viewModel.onAction(BoardAction.Refresh(null))
        assertThat(repository.calls).containsExactly("load")
    }

    @Test
    fun unknownLabelIdStaysOnTheCard() {
        val ghost = label("ghost")
        repository.server = repository.server.copy(columns = listOf(todo.copy(cards = listOf(card(1, labels = listOf(ghost))))))
        val viewModel = viewModel()
        val loaded = viewModel.state.value.loaded!!
        assertThat(loaded.labelsById[ghost.id]).isNull()
    }

    @Test
    fun boardRenamedUpdatesTheHeader() {
        val viewModel = viewModel(handle(name = "Cardigan v1", key = "CRD"))
        viewModel.onAction(BoardAction.BoardRenamed("Cardigan v2"))
        assertThat(viewModel.state.value.headerName).isEqualTo("Cardigan v2")
        assertThat(viewModel.board.board.name).isEqualTo("Cardigan v2")
    }

    @Test
    fun boardDeletedLeavesWithoutToast() = runTest {
        val viewModel = viewModel()
        val events = events(viewModel)
        viewModel.onAction(BoardAction.BoardDeleted)
        assertThat(events).containsExactly(BoardEvent.BoardDeleted)
        assertThat(toasts.messages).containsExactly()
    }
}
