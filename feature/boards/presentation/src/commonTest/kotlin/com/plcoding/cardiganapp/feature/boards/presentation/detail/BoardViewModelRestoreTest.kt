package com.plcoding.cardiganapp.feature.boards.presentation.detail

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import com.plcoding.cardiganapp.core.domain.text.NameError
import kotlin.test.Test
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest

/*
 * Break vectors: process death with each field and dialog open (quick add, add column, rename,
 * delete), with errors already showing, and with the field's or dialog's column gone after the
 * load, and a second process death before the restored dialog could reopen. Saving flags and the
 * discard dialog (D44) are not restored.
 */
class BoardViewModelRestoreTest : BoardViewModelTestBase() {

    /** Runs [setup] on a first ViewModel, then builds a second one from the same saved state. */
    private fun restored(setup: BoardViewModel.() -> Unit): BoardViewModel {
        val handle = handle()
        viewModel(handle).setup()
        return viewModel(handle)
    }

    @Test
    fun quickAddFieldAndTextSurvive() {
        val viewModel = restored {
            onAction(BoardAction.OpenQuickAdd(review.id))
            onAction(BoardAction.InlineTextChange("Write docs"))
        }
        assertThat(viewModel.state.value.inlineField).isEqualTo(InlineField(InlineTarget.QuickAdd(review.id), "Write docs"))
    }

    @Test
    fun addColumnFieldWithShownErrorSurvives() {
        val viewModel = restored {
            onAction(BoardAction.OpenAddColumn)
            onAction(BoardAction.InlineTextChange("x".repeat(31)))
            onAction(BoardAction.InlineSubmit)
        }
        val field = viewModel.state.value.inlineField!!
        assertThat(field.target).isEqualTo(InlineTarget.AddColumn)
        assertThat(field.error).isEqualTo(NameError.TooLong(30))
    }

    @Test
    fun savingIsNotRestored() {
        val viewModel = restored {
            repository.gates += CompletableDeferred()
            onAction(BoardAction.OpenAddColumn)
            onAction(BoardAction.InlineTextChange("Blocked"))
            onAction(BoardAction.InlineSubmit)
        }
        val field = viewModel.state.value.inlineField!!
        assertThat(field.text).isEqualTo("Blocked")
        assertThat(field.isSaving).isFalse()
    }

    @Test
    fun renameDialogSurvivesWithItsText() {
        val viewModel = restored {
            onAction(BoardAction.RenameColumnClick(review.id))
            onAction(BoardAction.RenameTextChange("QA"))
        }
        assertThat(viewModel.state.value.dialog).isEqualTo(BoardDialog.RenameColumn(review.id, "Review", "QA"))
    }

    @Test
    fun renameDialogWithShownErrorSurvives() {
        val viewModel = restored {
            onAction(BoardAction.RenameColumnClick(review.id))
            onAction(BoardAction.RenameTextChange(""))
            onAction(BoardAction.RenameSave)
        }
        assertThat(viewModel.state.value.dialog).isEqualTo(
            BoardDialog.RenameColumn(review.id, "Review", "", showErrors = true, error = NameError.Required),
        )
    }

    @Test
    fun dialogSurvivesASecondDeathBeforeTheLoad() {
        val handle = handle()
        viewModel(handle).onAction(BoardAction.RenameColumnClick(review.id))
        repository.gates += CompletableDeferred()
        viewModel(handle)
        assertThat(viewModel(handle).state.value.dialog).isEqualTo(BoardDialog.RenameColumn(review.id, "Review", "Review"))
    }

    @Test
    fun deleteDialogSurvives() {
        val viewModel = restored { onAction(BoardAction.DeleteColumnClick(review.id)) }
        assertThat(viewModel.state.value.dialog).isEqualTo(BoardDialog.DeleteColumn(review.id, "Review", 2))
    }

    @Test
    fun fieldOfAGoneColumnClosesAfterRestore() {
        val handle = handle()
        viewModel(handle).apply {
            onAction(BoardAction.OpenQuickAdd(review.id))
            onAction(BoardAction.InlineTextChange("Write docs"))
        }
        repository.server = repository.server.copy(columns = repository.server.columns.filterNot { it.id == review.id })
        assertThat(viewModel(handle).state.value.inlineField).isNull()
    }

    @Test
    fun dialogOfAGoneColumnClosesAfterRestore() = runTest {
        val handle = handle()
        viewModel(handle).onAction(BoardAction.RenameColumnClick(review.id))
        repository.server = repository.server.copy(columns = repository.server.columns.filterNot { it.id == review.id })
        assertThat(viewModel(handle).state.value.dialog).isNull()
    }

    @Test
    fun closedDialogIsNotRestored() {
        val viewModel = restored {
            onAction(BoardAction.RenameColumnClick(review.id))
            onAction(BoardAction.DialogDismiss)
        }
        assertThat(viewModel.state.value.dialog).isNull()
    }

    @Test
    fun discardDialogIsNotRestoredButTheFieldIs() {
        val viewModel = restored {
            onAction(BoardAction.OpenQuickAdd(todo.id))
            onAction(BoardAction.InlineTextChange("Write docs"))
            onAction(BoardAction.OpenQuickAdd(done.id))
        }
        assertThat(viewModel.state.value.dialog).isNull()
        assertThat(viewModel.state.value.inlineField).isEqualTo(InlineField(InlineTarget.QuickAdd(todo.id), "Write docs"))
    }
}
