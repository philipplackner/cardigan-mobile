package com.plcoding.cardiganapp.feature.boards.presentation.detail

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import assertk.assertions.isTrue
import cardiganapp.feature.boards.presentation.generated.resources.Res
import cardiganapp.feature.boards.presentation.generated.resources.board_view_add_column_failed
import cardiganapp.feature.boards.presentation.generated.resources.board_view_board_gone
import cardiganapp.feature.boards.presentation.generated.resources.board_view_column_gone
import cardiganapp.feature.boards.presentation.generated.resources.board_view_delete_column_failed
import cardiganapp.feature.boards.presentation.generated.resources.board_view_move_column_failed
import cardiganapp.feature.boards.presentation.generated.resources.board_view_move_stale
import cardiganapp.feature.boards.presentation.generated.resources.board_view_rename_column_failed
import com.plcoding.cardiganapp.core.domain.text.NameError
import com.plcoding.cardiganapp.core.domain.util.Result
import com.plcoding.cardiganapp.feature.boards.domain.detail.BoardLoadError
import com.plcoding.cardiganapp.feature.boards.domain.detail.ColumnWriteError
import com.plcoding.cardiganapp.feature.boards.presentation.fakes.boardContent
import com.plcoding.cardiganapp.feature.boards.presentation.util.assertResource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

/*
 * Break vectors:
 * - add column: success closes and scrolls, too long / empty / pasted line break, double submit and
 *   Enter while saving, server field error, failure keeps text and reloads in the background
 *   (and a failing background reload shows no second toast), board missing
 * - move: first, last and only column (no request), second column to the front, two moves on one
 *   column before the first response, a delete queued before a move, stale (D40), column gone, a
 *   name error the server shouldn't send, failure reloads, response IDs that don't match the board
 * - rename: unchanged and only surrounding spaces (D19), missing column, failure keeps the dialog,
 *   server name error, dismiss and edits while saving
 * - delete: success without a reload, ColumnNotFound counts as done, failure keeps the dialog, board
 *   missing, dismiss, dismiss and a second confirm while deleting
 * - a refresh closes a menu dialog whose column is gone and keeps one whose column stays
 * - back-to-back writes apply in the order they arrive; a write after a refresh still runs
 * - outside tap on the add column field: empty closes, text stays, ignored while saving (D42)
 */
class BoardViewModelColumnTest : BoardViewModelTestBase() {
    private fun BoardViewModel.addColumn(name: String) {
        onAction(BoardAction.OpenAddColumn)
        onAction(BoardAction.InlineTextChange(name))
        onAction(BoardAction.InlineSubmit)
    }

    @Test
    fun addColumnAppendsClosesAndScrolls() =
        runTest {
            val viewModel = viewModel()
            val events = events(viewModel)
            viewModel.addColumn("Blocked")
            assertThat(viewModel.columnNames).containsExactly("To do", "In progress", "Review", "Done", "Blocked")
            assertThat(viewModel.state.value.inlineField).isNull()
            val added = viewModel.board.columns.last()
            assertThat(events.last()).isEqualTo(BoardEvent.ScrollToColumn(added.id))
            assertThat(repository.calls).containsExactly("load", "addColumn:Blocked")
        }

    @Test
    fun addColumnSendsTheNormalizedName() {
        val viewModel = viewModel()
        viewModel.addColumn("  Blocked \r\n")
        assertThat(repository.calls.last()).isEqualTo("addColumn:Blocked")
    }

    @Test
    fun addColumnOnABoardWithoutColumns() {
        repository.server = boardContent()
        val viewModel = viewModel()
        viewModel.addColumn("To do")
        assertThat(viewModel.columnNames).containsExactly("To do")
    }

    @Test
    fun addColumnValidatesOnSubmitThenLive() {
        val viewModel = viewModel()
        viewModel.onAction(BoardAction.OpenAddColumn)
        viewModel.onAction(BoardAction.InlineTextChange("x".repeat(31)))
        assertThat(
            viewModel.state.value.inlineField!!
                .error,
        ).isNull()
        viewModel.onAction(BoardAction.InlineSubmit)
        assertThat(
            viewModel.state.value.inlineField!!
                .error,
        ).isEqualTo(NameError.TooLong(30))
        viewModel.onAction(BoardAction.InlineTextChange("x".repeat(30)))
        assertThat(
            viewModel.state.value.inlineField!!
                .error,
        ).isNull()
        assertThat(repository.calls).containsExactly("load")
    }

    @Test
    fun addColumnEmptyShowsRequired() {
        val viewModel = viewModel()
        viewModel.addColumn("   ")
        assertThat(
            viewModel.state.value.inlineField!!
                .error,
        ).isEqualTo(NameError.Required)
    }

    @Test
    fun addColumnWithPastedLineBreak() {
        val viewModel = viewModel()
        viewModel.addColumn("To\ndo")
        assertThat(
            viewModel.state.value.inlineField!!
                .error,
        ).isEqualTo(NameError.InvalidCharacter)
    }

    @Test
    fun doubleSubmitAndEditsWhileSavingAreIgnored() {
        val viewModel = viewModel()
        val gate = CompletableDeferred<Unit>()
        repository.gates += gate
        viewModel.addColumn("Blocked")
        viewModel.onAction(BoardAction.InlineSubmit)
        viewModel.onAction(BoardAction.InlineTextChange("Other"))
        viewModel.onAction(BoardAction.InlineCancel)
        assertThat(
            viewModel.state.value.inlineField!!
                .isSaving,
        ).isTrue()
        assertThat(
            viewModel.state.value.inlineField!!
                .text,
        ).isEqualTo("Blocked")
        gate.complete(Unit)
        assertThat(repository.calls).containsExactly("load", "addColumn:Blocked")
    }

    @Test
    fun addColumnServerFieldError() {
        val viewModel = viewModel()
        repository.writeResults += Result.Error(ColumnWriteError.InvalidName(NameError.TooLong(30)))
        viewModel.addColumn("Blocked")
        val field = viewModel.state.value.inlineField!!
        assertThat(field.error).isEqualTo(NameError.TooLong(30))
        assertThat(field.isSaving).isEqualTo(false)
    }

    @Test
    fun addColumnFailureKeepsTextAndReloadsInTheBackground() {
        val viewModel = viewModel()
        repository.writeResults += Result.Error(ColumnWriteError.Failed)
        viewModel.addColumn("Blocked")
        assertThat(viewModel.state.value.inlineField).isEqualTo(
            InlineField(InlineTarget.AddColumn, "Blocked", showErrors = true),
        )
        assertResource(toasts.messages.single().text, Res.string.board_view_add_column_failed)
        assertThat(repository.calls).containsExactly("load", "addColumn:Blocked", "load")
    }

    @Test
    fun failingBackgroundReloadShowsNoSecondToast() {
        val viewModel = viewModel()
        repository.writeResults += Result.Error(ColumnWriteError.Failed)
        repository.loadResults += Result.Error(BoardLoadError.Failed)
        viewModel.addColumn("Blocked")
        assertThat(toasts.messages.size).isEqualTo(1)
        assertThat(viewModel.columnNames).containsExactly("To do", "In progress", "Review", "Done")
    }

    @Test
    fun addColumnOnAMissingBoard() =
        runTest {
            val viewModel = viewModel()
            val events = events(viewModel)
            repository.writeResults += Result.Error(ColumnWriteError.BoardNotFound)
            viewModel.addColumn("Blocked")
            assertThat(events.withoutFocus).containsExactly(BoardEvent.BoardMissing)
            assertResource(toasts.messages.single().text, Res.string.board_view_board_gone)
        }

    @Test
    fun moveRightSendsTheNextIndexAndAppliesTheResponse() {
        val viewModel = viewModel()
        viewModel.onAction(BoardAction.MoveColumnRight(doing.id))
        assertThat(repository.moves).containsExactly(doing.id to 2)
        assertThat(viewModel.columnNames).containsExactly("To do", "Review", "In progress", "Done")
        assertThat(repository.calls).containsExactly("load", "moveColumn:2")
    }

    @Test
    fun moveLeft() {
        val viewModel = viewModel()
        viewModel.onAction(BoardAction.MoveColumnLeft(done.id))
        assertThat(viewModel.columnNames).containsExactly("To do", "In progress", "Done", "Review")
    }

    @Test
    fun moveSecondColumnToTheFront() {
        val viewModel = viewModel()
        viewModel.onAction(BoardAction.MoveColumnLeft(doing.id))
        assertThat(repository.moves).containsExactly(doing.id to 0)
        assertThat(viewModel.columnNames).containsExactly("In progress", "To do", "Review", "Done")
    }

    @Test
    fun noMoveBeyondTheEnds() {
        val viewModel = viewModel()
        viewModel.onAction(BoardAction.MoveColumnLeft(todo.id))
        viewModel.onAction(BoardAction.MoveColumnRight(done.id))
        assertThat(repository.calls).containsExactly("load")
    }

    @Test
    fun onlyColumnDoesNotMove() {
        repository.server = boardContent(todo)
        val viewModel = viewModel()
        viewModel.onAction(BoardAction.MoveColumnLeft(todo.id))
        viewModel.onAction(BoardAction.MoveColumnRight(todo.id))
        assertThat(repository.calls).containsExactly("load")
    }

    @Test
    fun columnStaysUntilTheServerConfirms() {
        val viewModel = viewModel()
        val gate = CompletableDeferred<Unit>()
        repository.gates += gate
        viewModel.onAction(BoardAction.MoveColumnRight(doing.id))
        assertThat(viewModel.columnNames).containsExactly("To do", "In progress", "Review", "Done")
        gate.complete(Unit)
        assertThat(viewModel.columnNames).containsExactly("To do", "Review", "In progress", "Done")
    }

    @Test
    fun twoMovesBeforeTheFirstResponseMoveTwice() {
        val viewModel = viewModel()
        val gate = CompletableDeferred<Unit>()
        repository.gates += gate
        viewModel.onAction(BoardAction.MoveColumnRight(todo.id))
        viewModel.onAction(BoardAction.MoveColumnRight(todo.id))
        gate.complete(Unit)
        assertThat(repository.moves.map { it.second }).containsExactly(1, 2)
        assertThat(viewModel.columnNames).containsExactly("In progress", "Review", "To do", "Done")
    }

    @Test
    fun moveQueuedBehindADeleteUsesTheBoardAfterTheDelete() {
        val viewModel = viewModel()
        val gate = CompletableDeferred<Unit>()
        repository.gates += gate
        viewModel.onAction(BoardAction.DeleteColumnClick(todo.id))
        viewModel.onAction(BoardAction.DeleteConfirm)
        viewModel.onAction(BoardAction.MoveColumnRight(review.id))
        gate.complete(Unit)
        assertThat(repository.moves).containsExactly(review.id to 2)
        assertThat(viewModel.columnNames).containsExactly("In progress", "Done", "Review")
        assertThat(toasts.messages).containsExactly()
    }

    @Test
    fun moveQueuedBehindADeleteThatMakesItTheLastColumnIsDropped() {
        val viewModel = viewModel()
        val gate = CompletableDeferred<Unit>()
        repository.gates += gate
        viewModel.onAction(BoardAction.DeleteColumnClick(done.id))
        viewModel.onAction(BoardAction.DeleteConfirm)
        viewModel.onAction(BoardAction.MoveColumnRight(review.id))
        gate.complete(Unit)
        assertThat(repository.moves).containsExactly()
    }

    @Test
    fun staleMoveReloadsWithTheD40Toast() {
        val viewModel = viewModel()
        repository.writeResults += Result.Error(ColumnWriteError.Stale)
        repository.server = boardContent(review, todo, doing, done)
        viewModel.onAction(BoardAction.MoveColumnRight(review.id))
        assertResource(toasts.messages.single().text, Res.string.board_view_move_stale, "Review")
        assertThat(viewModel.columnNames).containsExactly("Review", "To do", "In progress", "Done")
    }

    @Test
    fun moveOfAGoneColumn() {
        val viewModel = viewModel()
        repository.writeResults += Result.Error(ColumnWriteError.ColumnNotFound)
        repository.server = boardContent(todo, doing, done)
        viewModel.onAction(BoardAction.MoveColumnRight(review.id))
        assertResource(toasts.messages.single().text, Res.string.board_view_column_gone, "Review")
        assertThat(viewModel.columnNames).containsExactly("To do", "In progress", "Done")
    }

    @Test
    fun moveFailureReloadsInTheBackground() {
        val viewModel = viewModel()
        repository.writeResults += Result.Error(ColumnWriteError.Failed)
        viewModel.onAction(BoardAction.MoveColumnRight(review.id))
        assertResource(toasts.messages.single().text, Res.string.board_view_move_column_failed, "Review")
        assertThat(repository.calls).containsExactly("load", "moveColumn:3", "load")
    }

    @Test
    fun moveWithANameErrorShowsTheMoveFailure() {
        val viewModel = viewModel()
        repository.writeResults += Result.Error(ColumnWriteError.InvalidName(NameError.Required))
        viewModel.onAction(BoardAction.MoveColumnRight(review.id))
        assertResource(toasts.messages.single().text, Res.string.board_view_move_column_failed, "Review")
        assertThat(repository.calls).containsExactly("load", "moveColumn:3", "load")
    }

    @Test
    fun moveResponseThatDoesNotMatchReloads() {
        val viewModel = viewModel()
        repository.writeResults += Result.Success(listOf(doing.id, todo.id))
        viewModel.onAction(BoardAction.MoveColumnRight(todo.id))
        assertThat(repository.calls).containsExactly("load", "moveColumn:1", "load")
    }

    @Test
    fun moveOnAMissingBoard() =
        runTest {
            val viewModel = viewModel()
            val events = events(viewModel)
            repository.writeResults += Result.Error(ColumnWriteError.BoardNotFound)
            viewModel.onAction(BoardAction.MoveColumnRight(todo.id))
            assertThat(events).containsExactly(BoardEvent.BoardMissing)
        }

    @Test
    fun renameReplacesTheNameAndKeepsCards() {
        val viewModel = viewModel()
        viewModel.onAction(BoardAction.RenameColumnClick(review.id))
        viewModel.onAction(BoardAction.RenameTextChange("QA"))
        viewModel.onAction(BoardAction.RenameSave)
        assertThat(viewModel.columnNames).containsExactly("To do", "In progress", "QA", "Done")
        assertThat(viewModel.board.columns[2].cards).isEqualTo(review.cards)
        assertThat(viewModel.state.value.dialog).isNull()
    }

    @Test
    fun unchangedRenameClosesWithoutRequest() {
        val viewModel = viewModel()
        viewModel.onAction(BoardAction.RenameColumnClick(review.id))
        viewModel.onAction(BoardAction.RenameSave)
        assertThat(viewModel.state.value.dialog).isNull()
        viewModel.onAction(BoardAction.RenameColumnClick(review.id))
        viewModel.onAction(BoardAction.RenameTextChange("  Review  "))
        viewModel.onAction(BoardAction.RenameSave)
        assertThat(viewModel.state.value.dialog).isNull()
        assertThat(repository.calls).containsExactly("load")
    }

    @Test
    fun renameValidates() {
        val viewModel = viewModel()
        viewModel.onAction(BoardAction.RenameColumnClick(review.id))
        assertThat((viewModel.state.value.dialog as BoardDialog.RenameColumn).canSave).isTrue()
        viewModel.onAction(BoardAction.RenameTextChange(""))
        viewModel.onAction(BoardAction.RenameSave)
        val dialog = viewModel.state.value.dialog as BoardDialog.RenameColumn
        assertThat(dialog.error).isEqualTo(NameError.Required)
        assertThat(dialog.canSave).isEqualTo(false)
    }

    @Test
    fun renameOfAGoneColumnClosesReloadsAndToasts() {
        val viewModel = viewModel()
        repository.server = boardContent(todo, doing, done)
        viewModel.onAction(BoardAction.RenameColumnClick(review.id))
        viewModel.onAction(BoardAction.RenameTextChange("QA"))
        viewModel.onAction(BoardAction.RenameSave)
        assertThat(viewModel.state.value.dialog).isNull()
        assertThat(viewModel.columnNames).containsExactly("To do", "In progress", "Done")
        assertResource(toasts.messages.single().text, Res.string.board_view_column_gone, "Review")
    }

    @Test
    fun renameFailureKeepsTheDialog() {
        val viewModel = viewModel()
        repository.writeResults += Result.Error(ColumnWriteError.Failed)
        viewModel.onAction(BoardAction.RenameColumnClick(review.id))
        viewModel.onAction(BoardAction.RenameTextChange("QA"))
        viewModel.onAction(BoardAction.RenameSave)
        assertThat(
            viewModel.state.value.dialog,
        ).isEqualTo(BoardDialog.RenameColumn(review.id, "Review", "QA", showErrors = true))
        assertResource(toasts.messages.single().text, Res.string.board_view_rename_column_failed, "Review")
        assertThat(repository.calls).containsExactly("load", "renameColumn:QA")
    }

    @Test
    fun renameServerNameErrorShowsInTheDialog() {
        val viewModel = viewModel()
        repository.writeResults += Result.Error(ColumnWriteError.InvalidName(NameError.TooLong(30)))
        viewModel.onAction(BoardAction.RenameColumnClick(review.id))
        viewModel.onAction(BoardAction.RenameTextChange("QA"))
        viewModel.onAction(BoardAction.RenameSave)
        assertThat(viewModel.state.value.dialog).isEqualTo(
            BoardDialog.RenameColumn(review.id, "Review", "QA", showErrors = true, error = NameError.TooLong(30)),
        )
        assertThat(toasts.messages).containsExactly()
    }

    @Test
    fun renameDialogCantBeDismissedOrEditedWhileSaving() {
        val viewModel = viewModel()
        val gate = CompletableDeferred<Unit>()
        repository.gates += gate
        viewModel.onAction(BoardAction.RenameColumnClick(review.id))
        viewModel.onAction(BoardAction.RenameTextChange("QA"))
        viewModel.onAction(BoardAction.RenameSave)
        viewModel.onAction(BoardAction.DialogDismiss)
        viewModel.onAction(BoardAction.RenameTextChange("Other"))
        viewModel.onAction(BoardAction.RenameSave)
        val dialog = viewModel.state.value.dialog as BoardDialog.RenameColumn
        assertThat(dialog.text).isEqualTo("QA")
        assertThat(dialog.isSaving).isTrue()
        assertThat(dialog.canSave).isFalse()
        gate.complete(Unit)
        assertThat(repository.calls).containsExactly("load", "renameColumn:QA")
    }

    @Test
    fun deleteRemovesTheColumn() {
        val viewModel = viewModel()
        viewModel.onAction(BoardAction.DeleteColumnClick(review.id))
        assertThat((viewModel.state.value.dialog as BoardDialog.DeleteColumn).cardCount).isEqualTo(2)
        viewModel.onAction(BoardAction.DeleteConfirm)
        assertThat(viewModel.columnNames).containsExactly("To do", "In progress", "Done")
        assertThat(viewModel.state.value.dialog).isNull()
        assertThat(repository.calls).containsExactly("load", "deleteColumn")
    }

    @Test
    fun dismissClosesTheDeleteDialog() {
        val viewModel = viewModel()
        viewModel.onAction(BoardAction.DeleteColumnClick(review.id))
        viewModel.onAction(BoardAction.DialogDismiss)
        assertThat(viewModel.state.value.dialog).isNull()
        assertThat(repository.calls).containsExactly("load")
    }

    @Test
    fun deleteDialogCantBeDismissedOrConfirmedAgainWhileDeleting() {
        val viewModel = viewModel()
        val gate = CompletableDeferred<Unit>()
        repository.gates += gate
        viewModel.onAction(BoardAction.DeleteColumnClick(review.id))
        viewModel.onAction(BoardAction.DeleteConfirm)
        viewModel.onAction(BoardAction.DialogDismiss)
        viewModel.onAction(BoardAction.DeleteConfirm)
        assertThat(
            viewModel.state.value.dialog,
        ).isEqualTo(BoardDialog.DeleteColumn(review.id, "Review", 2, isSaving = true))
        assertThat((viewModel.state.value.dialog as BoardDialog.DeleteColumn).isSaving).isTrue()
        gate.complete(Unit)
        assertThat(repository.calls).containsExactly("load", "deleteColumn")
        assertThat(viewModel.state.value.dialog).isNull()
    }

    @Test
    fun deletingTheLastColumnLeavesAnEmptyBoard() {
        repository.server = boardContent(todo)
        val viewModel = viewModel()
        viewModel.onAction(BoardAction.DeleteColumnClick(todo.id))
        viewModel.onAction(BoardAction.DeleteConfirm)
        assertThat(viewModel.board.columns).isEqualTo(emptyList())
    }

    @Test
    fun deleteOfAMissingColumnCountsAsDone() {
        val viewModel = viewModel()
        repository.writeResults += Result.Error(ColumnWriteError.ColumnNotFound)
        viewModel.onAction(BoardAction.DeleteColumnClick(review.id))
        viewModel.onAction(BoardAction.DeleteConfirm)
        assertThat(viewModel.state.value.dialog).isNull()
        assertThat(toasts.messages).containsExactly()
        assertThat(viewModel.columnNames).containsExactly("To do", "In progress", "Review", "Done")
        assertThat(repository.calls).containsExactly("load", "deleteColumn", "load")
    }

    @Test
    fun deleteFailureKeepsTheDialog() {
        val viewModel = viewModel()
        repository.writeResults += Result.Error(ColumnWriteError.Failed)
        viewModel.onAction(BoardAction.DeleteColumnClick(review.id))
        viewModel.onAction(BoardAction.DeleteConfirm)
        assertThat(viewModel.state.value.dialog).isEqualTo(BoardDialog.DeleteColumn(review.id, "Review", 2))
        assertResource(toasts.messages.single().text, Res.string.board_view_delete_column_failed, "Review")
        assertThat(repository.calls).containsExactly("load", "deleteColumn")
    }

    @Test
    fun deleteOnAMissingBoard() =
        runTest {
            val viewModel = viewModel()
            val events = events(viewModel)
            repository.writeResults += Result.Error(ColumnWriteError.BoardNotFound)
            viewModel.onAction(BoardAction.DeleteColumnClick(review.id))
            viewModel.onAction(BoardAction.DeleteConfirm)
            assertThat(events).containsExactly(BoardEvent.BoardMissing)
            assertResource(toasts.messages.single().text, Res.string.board_view_board_gone)
            assertThat(viewModel.state.value.dialog).isEqualTo(BoardDialog.DeleteColumn(review.id, "Review", 2))
        }

    @Test
    fun refreshClosesAMenuDialogWhoseColumnIsGone() {
        val viewModel = viewModel()
        repository.server = boardContent(todo, doing, done)
        viewModel.onAction(BoardAction.RenameColumnClick(review.id))
        viewModel.onAction(BoardAction.Refresh(null))
        assertThat(viewModel.state.value.dialog).isNull()
        viewModel.onAction(BoardAction.DeleteColumnClick(todo.id))
        repository.server = boardContent(doing, done)
        viewModel.onAction(BoardAction.Refresh(null))
        assertThat(viewModel.state.value.dialog).isNull()
    }

    @Test
    fun refreshKeepsAMenuDialogWhoseColumnStays() {
        val viewModel = viewModel()
        viewModel.onAction(BoardAction.RenameColumnClick(review.id))
        viewModel.onAction(BoardAction.RenameTextChange("QA"))
        viewModel.onAction(BoardAction.Refresh(null))
        assertThat(viewModel.state.value.dialog).isEqualTo(BoardDialog.RenameColumn(review.id, "Review", "QA"))
        viewModel.onAction(BoardAction.DialogDismiss)
        viewModel.onAction(BoardAction.DeleteColumnClick(review.id))
        viewModel.onAction(BoardAction.Refresh(null))
        assertThat(viewModel.state.value.dialog).isEqualTo(BoardDialog.DeleteColumn(review.id, "Review", 2))
    }

    @Test
    fun boardMissingIsReportedOnce() =
        runTest {
            val viewModel = viewModel()
            val events = events(viewModel)
            repository.writeResults += Result.Error(ColumnWriteError.BoardNotFound)
            repository.writeResults += Result.Error(ColumnWriteError.BoardNotFound)
            viewModel.onAction(BoardAction.MoveColumnRight(todo.id))
            viewModel.onAction(BoardAction.MoveColumnRight(doing.id))
            assertThat(events).containsExactly(BoardEvent.BoardMissing)
            assertThat(toasts.messages.size).isEqualTo(1)
        }

    @Test
    fun backToBackWritesApplyInOrder() {
        val viewModel = viewModel()
        val first = CompletableDeferred<Unit>()
        repository.gates += first
        viewModel.addColumn("Blocked")
        viewModel.onAction(BoardAction.RenameColumnClick(todo.id))
        viewModel.onAction(BoardAction.RenameTextChange("Backlog"))
        viewModel.onAction(BoardAction.RenameSave)
        viewModel.onAction(BoardAction.MoveColumnRight(todo.id))
        first.complete(Unit)
        assertThat(
            repository.calls,
        ).containsExactly("load", "addColumn:Blocked", "renameColumn:Backlog", "moveColumn:1")
        assertThat(viewModel.columnNames).containsExactly("In progress", "Backlog", "Review", "Done", "Blocked")
    }

    @Test
    fun refreshDuringAWriteWaitsForIt() {
        val viewModel = viewModel()
        val gate = CompletableDeferred<Unit>()
        repository.gates += gate
        viewModel.addColumn("Blocked")
        viewModel.onAction(BoardAction.Refresh(todo.id))
        assertThat(repository.calls).containsExactly("load", "addColumn:Blocked")
        gate.complete(Unit)
        assertThat(repository.calls).containsExactly("load", "addColumn:Blocked", "load")
        assertThat(viewModel.columnNames).containsExactly("To do", "In progress", "Review", "Done", "Blocked")
    }

    @Test
    fun writeAfterARefreshStillRuns() {
        val viewModel = viewModel()
        viewModel.onAction(BoardAction.Refresh(null))
        viewModel.onAction(BoardAction.MoveColumnRight(todo.id))
        assertThat(viewModel.columnNames).containsExactly("In progress", "To do", "Review", "Done")
    }

    @Test
    fun outsideTapClosesAnEmptyAddColumnField() {
        val viewModel = viewModel()
        viewModel.onAction(BoardAction.OpenAddColumn)
        viewModel.onAction(BoardAction.InlineTextChange("   "))
        viewModel.onAction(BoardAction.InlineOutsideTap)
        assertThat(viewModel.state.value.inlineField).isNull()
        assertThat(viewModel.state.value.dialog).isNull()
    }

    @Test
    fun outsideTapKeepsAnAddColumnFieldWithText() {
        val viewModel = viewModel()
        viewModel.onAction(BoardAction.OpenAddColumn)
        viewModel.onAction(BoardAction.InlineTextChange("Blocked"))
        viewModel.onAction(BoardAction.InlineOutsideTap)
        assertThat(viewModel.state.value.inlineField).isEqualTo(InlineField(InlineTarget.AddColumn, "Blocked"))
        assertThat(viewModel.state.value.dialog).isNull()
    }

    @Test
    fun outsideTapWhileAddingAColumnIsIgnored() {
        val viewModel = viewModel()
        repository.gates += CompletableDeferred()
        viewModel.addColumn("Blocked")
        viewModel.onAction(BoardAction.InlineOutsideTap)
        assertThat(
            viewModel.state.value.inlineField!!
                .isSaving,
        ).isTrue()
    }
}
