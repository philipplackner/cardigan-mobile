package com.plcoding.cardiganapp.feature.boards.presentation.detail

import androidx.lifecycle.SavedStateHandle
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.uuid.Uuid

/*
 * Break vectors: actions that arrive when their target isn't there.
 * - no board ID in the route
 * - every field and dialog action with nothing open, and the actions the ViewModel leaves to the Root
 * - column menu on a column that isn't on the board
 * - a second menu dialog while one shows; an inline field while a menu dialog shows
 * - opening a field or moving a column before the board loaded
 * - discard dialog actions while a menu dialog shows, and Keep editing after the field was cancelled
 */
class BoardViewModelGuardTest : BoardViewModelTestBase() {
    @Test
    fun missingBoardIdFailsFast() {
        assertFailure { BoardViewModel(repository, cards, labels, toasts, SavedStateHandle()) }
            .isInstanceOf<IllegalArgumentException>()
    }

    @Test
    fun actionsWithoutTheirTargetDoNothing() =
        runTest {
            val viewModel = viewModel()
            val events = events(viewModel)
            val before = viewModel.state.value
            listOf(
                BoardAction.InlineTextChange("Blocked"),
                BoardAction.InlineSubmit,
                BoardAction.InlineCancel,
                BoardAction.InlineOutsideTap,
                BoardAction.DiscardKeepEditing,
                BoardAction.DiscardConfirm,
                BoardAction.RenameTextChange("QA"),
                BoardAction.RenameSave,
                BoardAction.DeleteConfirm,
                BoardAction.DialogDismiss,
                BoardAction.BackClick,
                BoardAction.CardClick(todo.cards.first().id),
                BoardAction.RenameBoardClick,
                BoardAction.DeleteBoardClick,
                BoardAction.ExportClick,
            ).forEach { action ->
                viewModel.onAction(action)
                assertThat(viewModel.state.value, name = "$action").isEqualTo(before)
            }
            assertThat(events).containsExactly()
            assertThat(repository.calls).containsExactly("load")
            assertThat(cards.calls).containsExactly()
        }

    @Test
    fun columnMenuOnAnUnknownColumnDoesNothing() {
        val viewModel = viewModel()
        viewModel.onAction(BoardAction.RenameColumnClick(Uuid.random()))
        viewModel.onAction(BoardAction.DeleteColumnClick(Uuid.random()))
        assertThat(viewModel.state.value.dialog).isNull()
    }

    @Test
    fun aSecondMenuDialogDoesNotReplaceTheOpenOne() {
        val viewModel = viewModel()
        viewModel.onAction(BoardAction.DeleteColumnClick(review.id))
        viewModel.onAction(BoardAction.RenameColumnClick(todo.id))
        assertThat(viewModel.state.value.dialog).isEqualTo(BoardDialog.DeleteColumn(review.id, "Review", 2))
        viewModel.onAction(BoardAction.DialogDismiss)
        viewModel.onAction(BoardAction.RenameColumnClick(review.id))
        viewModel.onAction(BoardAction.DeleteColumnClick(todo.id))
        assertThat(viewModel.state.value.dialog).isEqualTo(BoardDialog.RenameColumn(review.id, "Review", "Review"))
    }

    @Test
    fun inlineFieldDoesNotOpenWhileAMenuDialogShows() {
        val viewModel = viewModel()
        viewModel.onAction(BoardAction.RenameColumnClick(review.id))
        viewModel.onAction(BoardAction.OpenQuickAdd(todo.id))
        viewModel.onAction(BoardAction.OpenAddColumn)
        assertThat(viewModel.state.value.inlineField).isNull()
    }

    @Test
    fun nothingOpensOrMovesBeforeTheBoardLoaded() {
        val gate = CompletableDeferred<Unit>()
        repository.gates += gate
        val viewModel = viewModel()
        viewModel.onAction(BoardAction.OpenAddColumn)
        viewModel.onAction(BoardAction.MoveColumnRight(doing.id))
        gate.complete(Unit)
        assertThat(viewModel.state.value.inlineField).isNull()
        assertThat(repository.calls).containsExactly("load")
    }

    @Test
    fun discardActionsLeaveAMenuDialogAlone() {
        val viewModel = viewModel()
        viewModel.onAction(BoardAction.RenameColumnClick(review.id))
        viewModel.onAction(BoardAction.DiscardKeepEditing)
        viewModel.onAction(BoardAction.DiscardConfirm)
        assertThat(viewModel.state.value.dialog).isEqualTo(BoardDialog.RenameColumn(review.id, "Review", "Review"))
        assertThat(viewModel.state.value.inlineField).isNull()
    }

    @Test
    fun keepEditingAfterTheFieldWasCancelledOnlyCloses() =
        runTest {
            val viewModel = viewModel()
            val events = events(viewModel)
            viewModel.onAction(BoardAction.OpenQuickAdd(todo.id))
            viewModel.onAction(BoardAction.InlineTextChange("Write docs"))
            viewModel.onAction(BoardAction.OpenQuickAdd(done.id))
            viewModel.onAction(BoardAction.InlineCancel)
            viewModel.onAction(BoardAction.DiscardKeepEditing)
            assertThat(viewModel.state.value.dialog).isNull()
            assertThat(viewModel.state.value.inlineField).isNull()
            assertThat(events).containsExactly(BoardEvent.FocusInlineField(InlineTarget.QuickAdd(todo.id)))
        }
}
