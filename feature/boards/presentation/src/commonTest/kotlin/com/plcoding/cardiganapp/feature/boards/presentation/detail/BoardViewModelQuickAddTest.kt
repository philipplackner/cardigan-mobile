package com.plcoding.cardiganapp.feature.boards.presentation.detail

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import cardiganapp.feature.boards.presentation.generated.resources.Res
import cardiganapp.feature.boards.presentation.generated.resources.board_view_add_card_failed
import cardiganapp.feature.boards.presentation.generated.resources.board_view_board_gone
import cardiganapp.feature.boards.presentation.generated.resources.board_view_column_gone
import com.plcoding.cardiganapp.core.domain.text.NameError
import com.plcoding.cardiganapp.core.domain.util.Result
import com.plcoding.cardiganapp.feature.boards.domain.detail.CardCreateError
import com.plcoding.cardiganapp.feature.boards.presentation.fakes.boardContent
import com.plcoding.cardiganapp.feature.boards.presentation.util.assertResource
import kotlin.test.Test
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest

/*
 * Break vectors:
 * - two cards in a row, each appended after its response; field stays open and empty
 * - whitespace only, empty: nothing sent, no error
 * - pasted line break, 201 code points (emoji count once), 200 is fine
 * - Enter and Cancel while saving
 * - Cancel drops the text; outside tap keeps text, closes when empty
 * - switching: empty field closes, field with text asks (both directions with Add column),
 *   Keep editing / back / Discard, switch while saving does nothing, a menu dialog doesn't ask
 * - failure keeps the text, toasts once and reloads (the lost card shows up)
 * - column gone, board gone, server title error
 * - refresh that removes the field's column closes it; a refresh keeps other text
 */
class BoardViewModelQuickAddTest : BoardViewModelTestBase() {

    private fun BoardViewModel.type(text: String, columnId: kotlin.uuid.Uuid = todo.id) {
        onAction(BoardAction.OpenQuickAdd(columnId))
        onAction(BoardAction.InlineTextChange(text))
    }

    private fun BoardViewModel.cardTitles(columnIndex: Int = 0) = board.columns[columnIndex].cards.map { it.title }

    @Test
    fun addTwoCardsInARow() {
        val viewModel = viewModel()
        viewModel.type("Write docs")
        viewModel.onAction(BoardAction.InlineSubmit)
        viewModel.onAction(BoardAction.InlineTextChange("Ship it"))
        viewModel.onAction(BoardAction.InlineSubmit)
        assertThat(viewModel.cardTitles()).containsExactly("Card 101", "Card 102", "Write docs", "Ship it")
        assertThat(viewModel.state.value.inlineField).isEqualTo(InlineField(InlineTarget.QuickAdd(todo.id)))
    }

    @Test
    fun cardAppearsOnlyAfterTheServerConfirms() {
        val viewModel = viewModel()
        val gate = CompletableDeferred<Unit>()
        cards.gates += gate
        viewModel.type("Write docs")
        viewModel.onAction(BoardAction.InlineSubmit)
        assertThat(viewModel.cardTitles()).containsExactly("Card 101", "Card 102")
        assertThat(viewModel.state.value.inlineField!!.isSaving).isTrue()
        gate.complete(Unit)
        assertThat(viewModel.cardTitles()).containsExactly("Card 101", "Card 102", "Write docs")
    }

    @Test
    fun whitespaceOnlyDoesNothing() {
        val viewModel = viewModel()
        viewModel.type("   ")
        viewModel.onAction(BoardAction.InlineSubmit)
        viewModel.onAction(BoardAction.InlineTextChange(""))
        viewModel.onAction(BoardAction.InlineSubmit)
        assertThat(cards.calls).containsExactly()
        assertThat(viewModel.state.value.inlineField!!.error).isNull()
        assertThat(viewModel.state.value.inlineField!!.showErrors).isFalse()
    }

    @Test
    fun pastedLineBreakShowsTheControlCharacterError() {
        val viewModel = viewModel()
        viewModel.type("Fix\nlogin")
        viewModel.onAction(BoardAction.InlineSubmit)
        assertThat(viewModel.state.value.inlineField!!.error).isEqualTo(NameError.InvalidCharacter)
        assertThat(cards.calls).containsExactly()
    }

    @Test
    fun titleLengthCountsCodePoints() {
        val viewModel = viewModel()
        viewModel.type("😀".repeat(200))
        viewModel.onAction(BoardAction.InlineSubmit)
        assertThat(cards.calls.size).isEqualTo(1)
        viewModel.onAction(BoardAction.InlineTextChange("a".repeat(201)))
        viewModel.onAction(BoardAction.InlineSubmit)
        assertThat(viewModel.state.value.inlineField!!.error).isEqualTo(NameError.TooLong(200))
        assertThat(cards.calls.size).isEqualTo(1)
    }

    @Test
    fun errorClearsLiveAfterTheFirstSubmit() {
        val viewModel = viewModel()
        viewModel.type("a".repeat(201))
        assertThat(viewModel.state.value.inlineField!!.canSubmit).isTrue()
        viewModel.onAction(BoardAction.InlineSubmit)
        assertThat(viewModel.state.value.inlineField!!.canSubmit).isFalse()
        viewModel.onAction(BoardAction.InlineTextChange("a".repeat(200)))
        assertThat(viewModel.state.value.inlineField!!.error).isNull()
    }

    @Test
    fun enterAndCancelWhileSavingAreIgnored() {
        val viewModel = viewModel()
        val gate = CompletableDeferred<Unit>()
        cards.gates += gate
        viewModel.type("Write docs")
        viewModel.onAction(BoardAction.InlineSubmit)
        viewModel.onAction(BoardAction.InlineSubmit)
        viewModel.onAction(BoardAction.InlineCancel)
        viewModel.onAction(BoardAction.InlineOutsideTap)
        assertThat(viewModel.state.value.inlineField!!.isSaving).isTrue()
        gate.complete(Unit)
        assertThat(cards.calls).containsExactly("Write docs")
    }

    @Test
    fun cancelDropsTheTextWithoutAsking() {
        val viewModel = viewModel()
        viewModel.type("Write docs")
        viewModel.onAction(BoardAction.InlineCancel)
        assertThat(viewModel.state.value.inlineField).isNull()
        assertThat(viewModel.state.value.dialog).isNull()
    }

    @Test
    fun outsideTapKeepsTextAndClosesEmpty() {
        val viewModel = viewModel()
        viewModel.type("Write docs")
        viewModel.onAction(BoardAction.InlineOutsideTap)
        assertThat(viewModel.state.value.inlineField!!.text).isEqualTo("Write docs")
        viewModel.onAction(BoardAction.InlineTextChange("  "))
        viewModel.onAction(BoardAction.InlineOutsideTap)
        assertThat(viewModel.state.value.inlineField).isNull()
    }

    @Test
    fun openingAnotherColumnClosesAnEmptyField() = runTest {
        val viewModel = viewModel()
        val events = events(viewModel)
        viewModel.onAction(BoardAction.OpenQuickAdd(todo.id))
        viewModel.onAction(BoardAction.OpenQuickAdd(done.id))
        assertThat(viewModel.state.value.inlineField).isEqualTo(InlineField(InlineTarget.QuickAdd(done.id)))
        assertThat(events.last()).isEqualTo(BoardEvent.FocusInlineField(InlineTarget.QuickAdd(done.id)))
    }

    @Test
    fun openingAnotherColumnWithTextAsks() {
        val viewModel = viewModel()
        viewModel.type("Write docs")
        viewModel.onAction(BoardAction.OpenQuickAdd(done.id))
        assertThat(viewModel.state.value.dialog).isEqualTo(BoardDialog.DiscardInlineField(InlineTarget.QuickAdd(done.id)))
        assertThat(viewModel.state.value.inlineField!!.target).isEqualTo(InlineTarget.QuickAdd(todo.id))
    }

    @Test
    fun keepEditingRefocusesTheOpenField() = runTest {
        val viewModel = viewModel()
        viewModel.type("Write docs")
        viewModel.onAction(BoardAction.OpenQuickAdd(done.id))
        val events = events(viewModel)
        viewModel.onAction(BoardAction.DiscardKeepEditing)
        assertThat(viewModel.state.value.dialog).isNull()
        assertThat(viewModel.state.value.inlineField).isEqualTo(InlineField(InlineTarget.QuickAdd(todo.id), "Write docs"))
        assertThat(events.last()).isEqualTo(BoardEvent.FocusInlineField(InlineTarget.QuickAdd(todo.id)))
    }

    @Test
    fun backOnTheDiscardDialogKeepsEditing() {
        val viewModel = viewModel()
        viewModel.type("Write docs")
        viewModel.onAction(BoardAction.OpenQuickAdd(done.id))
        viewModel.onAction(BoardAction.DialogDismiss)
        assertThat(viewModel.state.value.dialog).isNull()
        assertThat(viewModel.state.value.inlineField!!.text).isEqualTo("Write docs")
    }

    @Test
    fun discardOpensTheNewField() = runTest {
        val viewModel = viewModel()
        viewModel.type("Write docs")
        viewModel.onAction(BoardAction.OpenQuickAdd(done.id))
        val events = events(viewModel)
        viewModel.onAction(BoardAction.DiscardConfirm)
        assertThat(viewModel.state.value.inlineField).isEqualTo(InlineField(InlineTarget.QuickAdd(done.id)))
        assertThat(events.last()).isEqualTo(BoardEvent.FocusInlineField(InlineTarget.QuickAdd(done.id)))
    }

    @Test
    fun quickAddWithTextToAddColumnAsks() {
        val viewModel = viewModel()
        viewModel.type("Write docs")
        viewModel.onAction(BoardAction.OpenAddColumn)
        assertThat(viewModel.state.value.dialog).isEqualTo(BoardDialog.DiscardInlineField(InlineTarget.AddColumn))
    }

    @Test
    fun addColumnWithTextToQuickAddAsks() {
        val viewModel = viewModel()
        viewModel.onAction(BoardAction.OpenAddColumn)
        viewModel.onAction(BoardAction.InlineTextChange("Blocked"))
        viewModel.onAction(BoardAction.OpenQuickAdd(todo.id))
        assertThat(viewModel.state.value.dialog).isEqualTo(BoardDialog.DiscardInlineField(InlineTarget.QuickAdd(todo.id)))
    }

    @Test
    fun switchWhileSavingDoesNothing() {
        val viewModel = viewModel()
        cards.gates += CompletableDeferred()
        viewModel.type("Write docs")
        viewModel.onAction(BoardAction.InlineSubmit)
        viewModel.onAction(BoardAction.OpenQuickAdd(done.id))
        assertThat(viewModel.state.value.dialog).isNull()
        assertThat(viewModel.state.value.inlineField!!.target).isEqualTo(InlineTarget.QuickAdd(todo.id))
    }

    @Test
    fun columnMenuDialogKeepsTheField() {
        val viewModel = viewModel()
        viewModel.type("Write docs")
        viewModel.onAction(BoardAction.RenameColumnClick(review.id))
        viewModel.onAction(BoardAction.DialogDismiss)
        assertThat(viewModel.state.value.inlineField!!.text).isEqualTo("Write docs")
    }

    @Test
    fun failureKeepsTextToastsOnceAndReloads() {
        val viewModel = viewModel()
        cards.results += Result.Error(CardCreateError.Failed)
        viewModel.type("Write docs")
        viewModel.onAction(BoardAction.InlineSubmit)
        val field = viewModel.state.value.inlineField!!
        assertThat(field.text).isEqualTo("Write docs")
        assertThat(field.isSaving).isFalse()
        assertResource(toasts.messages.single().text, Res.string.board_view_add_card_failed)
        // The fake created the card before the response got lost; the reload shows it once.
        assertThat(viewModel.cardTitles()).containsExactly("Card 101", "Card 102", "Write docs")
        assertThat(repository.calls).containsExactly("load", "load")
    }

    @Test
    fun noAutomaticRepeatAfterAFailure() {
        val viewModel = viewModel()
        cards.results += Result.Error(CardCreateError.Failed)
        viewModel.type("Write docs")
        viewModel.onAction(BoardAction.InlineSubmit)
        assertThat(cards.calls).containsExactly("Write docs")
    }

    @Test
    fun serverTitleError() {
        val viewModel = viewModel()
        cards.results += Result.Error(CardCreateError.InvalidTitle(NameError.InvalidCharacter))
        viewModel.type("Write docs")
        viewModel.onAction(BoardAction.InlineSubmit)
        assertThat(viewModel.state.value.inlineField!!.error).isEqualTo(NameError.InvalidCharacter)
    }

    @Test
    fun columnGoneClosesReloadsAndToasts() {
        val viewModel = viewModel()
        repository.server = boardContent(doing, review, done)
        viewModel.type("Write docs")
        viewModel.onAction(BoardAction.InlineSubmit)
        assertThat(viewModel.state.value.inlineField).isNull()
        assertThat(viewModel.columnNames).containsExactly("In progress", "Review", "Done")
        assertResource(toasts.messages.single().text, Res.string.board_view_column_gone, "To do")
    }

    @Test
    fun boardGone() = runTest {
        val viewModel = viewModel()
        cards.results += Result.Error(CardCreateError.BoardNotFound)
        viewModel.type("Write docs")
        val events = events(viewModel)
        viewModel.onAction(BoardAction.InlineSubmit)
        assertThat(events.withoutFocus).containsExactly(BoardEvent.BoardMissing)
        assertResource(toasts.messages.single().text, Res.string.board_view_board_gone)
    }

    @Test
    fun refreshKeepsTheFieldText() {
        val viewModel = viewModel()
        viewModel.type("Write docs")
        viewModel.onAction(BoardAction.Refresh(todo.id))
        assertThat(viewModel.state.value.inlineField!!.text).isEqualTo("Write docs")
    }

    @Test
    fun refreshThatRemovesTheColumnClosesTheField() {
        val viewModel = viewModel()
        viewModel.type("Write docs")
        repository.server = boardContent(doing, review, done)
        viewModel.onAction(BoardAction.Refresh(doing.id))
        assertThat(viewModel.state.value.inlineField).isNull()
    }

    @Test
    fun refreshThatRemovesThePendingColumnClosesTheDiscardDialog() {
        val viewModel = viewModel()
        viewModel.type("Write docs")
        viewModel.onAction(BoardAction.OpenQuickAdd(done.id))
        repository.server = boardContent(todo, doing, review)
        viewModel.onAction(BoardAction.Refresh(todo.id))
        assertThat(viewModel.state.value.dialog).isNull()
        assertThat(viewModel.state.value.inlineField!!.text).isEqualTo("Write docs")
    }

    @Test
    fun addColumnFieldSurvivesARefresh() {
        val viewModel = viewModel()
        viewModel.onAction(BoardAction.OpenAddColumn)
        viewModel.onAction(BoardAction.InlineTextChange("Blocked"))
        repository.server = boardContent()
        viewModel.onAction(BoardAction.Refresh(todo.id))
        assertThat(viewModel.state.value.inlineField!!.text).isEqualTo("Blocked")
    }
}
