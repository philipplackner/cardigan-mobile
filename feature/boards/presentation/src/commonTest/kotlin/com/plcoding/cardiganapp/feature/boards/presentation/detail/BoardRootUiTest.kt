package com.plcoding.cardiganapp.feature.boards.presentation.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.ui.Alignment
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import cardiganapp.feature.boards.presentation.generated.resources.Res
import cardiganapp.feature.boards.presentation.generated.resources.board_view_board_gone
import cardiganapp.feature.boards.presentation.generated.resources.board_view_rename_column_failed
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import com.plcoding.cardiganapp.core.designsystem.theme.WindowClass
import com.plcoding.cardiganapp.core.domain.util.Result
import com.plcoding.cardiganapp.feature.boards.domain.detail.BoardLoadError
import com.plcoding.cardiganapp.feature.boards.domain.detail.ColumnWriteError
import com.plcoding.cardiganapp.feature.boards.presentation.fakes.FakeBoardDetailRepository
import com.plcoding.cardiganapp.feature.boards.presentation.fakes.FakeCardCreateRepository
import com.plcoding.cardiganapp.feature.boards.presentation.fakes.FakeLabelSource
import com.plcoding.cardiganapp.feature.boards.presentation.fakes.FakeToastController
import com.plcoding.cardiganapp.feature.boards.presentation.fakes.boardContent
import com.plcoding.cardiganapp.feature.boards.presentation.fakes.card
import com.plcoding.cardiganapp.feature.boards.presentation.fakes.column
import com.plcoding.cardiganapp.feature.boards.presentation.fakes.sampleBoardId
import com.plcoding.cardiganapp.feature.boards.presentation.util.assertResource
import kotlin.test.Test

/** Root + the real [BoardViewModel] with fakes at the repository boundary. */
@OptIn(ExperimentalTestApi::class)
class BoardRootUiTest {

    private val todo = column("To do", card(101, "Setup"))
    private val review = column("Review", card(104, "Import fails"), card(106, "Reverse proxy"))
    private val done = column("Done")
    private val repository = FakeBoardDetailRepository(boardContent(todo, review, done))
    private val cards = FakeCardCreateRepository(repository)
    private val toasts = FakeToastController()
    private var missing = 0
    private var deleted = 0

    private fun ComposeUiTest.show(windowClass: WindowClass = WindowClass.Desktop) {
        setContent {
            AppTheme(darkTheme = false, windowClass = windowClass) {
                Box(Modifier.wrapContentSize(Alignment.TopStart, unbounded = true).requiredSize(1440.dp, 900.dp)) {
                    val viewModel = remember {
                        BoardViewModel(
                            repository,
                            cards,
                            FakeLabelSource(),
                            toasts,
                            SavedStateHandle(mapOf(BoardViewModel.ARG_BOARD_ID to sampleBoardId.toString())),
                        )
                    }
                    BoardRoot(
                        viewModel = viewModel,
                        onBack = {},
                        onCardClick = {},
                        onExportClick = { _, _ -> },
                        onRenameBoardClick = {},
                        onDeleteBoardClick = {},
                        onBoardMissing = { missing++ },
                        onBoardDeleted = { deleted++ },
                    )
                }
            }
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("To do")).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun ComposeUiTest.awaitText(text: String) =
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty() }

    private fun ComposeUiTest.awaitGone(text: String) =
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText(text)).fetchSemanticsNodes().isEmpty() }

    private fun ComposeUiTest.openColumnMenu(name: String) =
        onNodeWithContentDescription("Actions for column “$name”").performClick()

    private fun ComposeUiTest.columnNames(): List<String> = repository.server.columns.map { it.name }

    @Test
    fun addCardTwiceInARow() = runComposeUiTest {
        show()
        onNodeWithContentDescription("Add card to “Done”").performClick()
        onNodeWithTag(BoardTestTags.InlineField).performTextReplacement("Write docs")
        onNodeWithTag(BoardTestTags.InlineField).performImeAction()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasContentDescription("CRD-500, Write docs")).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag(BoardTestTags.InlineField).performTextReplacement("Ship it")
        onNodeWithText("Add card").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasContentDescription("CRD-501, Ship it")).fetchSemanticsNodes().isNotEmpty() }
        assertThat(cards.calls).containsExactly("Write docs", "Ship it")
        assertThat(repository.server.columns.last().cards.map { it.title }).containsExactly("Write docs", "Ship it")
        onNodeWithTag(BoardTestTags.InlineField).assertExists()
    }

    @Test
    fun moveRight() = runComposeUiTest {
        show()
        openColumnMenu("To do")
        onNodeWithText("Move right").performClick()
        waitUntil(timeoutMillis = 5_000) { repository.moves.isNotEmpty() }
        assertThat(columnNames()).containsExactly("Review", "To do", "Done")
    }

    @Test
    fun moveLeftIsDisabledOnTheFirstColumn() = runComposeUiTest {
        show()
        openColumnMenu("To do")
        onNodeWithText("Move left").assertIsNotEnabled()
        onNodeWithText("Move right").assertIsEnabled()
    }

    @Test
    fun renameUnchangedSendsNothing() = runComposeUiTest {
        show()
        openColumnMenu("Review")
        onNodeWithText("Rename").performClick()
        onAllNodes(hasSetTextAction())[0].performTextReplacement(" Review ")
        onNodeWithText("Save").performClick()
        awaitGone("Rename column")
        assertThat(repository.calls).containsExactly("load")
    }

    @Test
    fun renameFailureShowsTheToastAndKeepsTheDialog() = runComposeUiTest {
        repository.writeResults += Result.Error(ColumnWriteError.Failed)
        show()
        openColumnMenu("Review")
        onNodeWithText("Rename").performClick()
        onAllNodes(hasSetTextAction())[0].performTextReplacement("QA")
        onNodeWithText("Save").performClick()
        waitUntil(timeoutMillis = 5_000) { toasts.messages.isNotEmpty() }
        assertResource(toasts.messages.single().text, Res.string.board_view_rename_column_failed, "Review")
        onNodeWithText("Rename column").assertExists()
    }

    @Test
    fun discardDialogKeepEditingAndDiscard() = runComposeUiTest {
        show()
        onNodeWithContentDescription("Add card to “To do”").performClick()
        onNodeWithTag(BoardTestTags.InlineField).performTextReplacement("Write docs")
        onNodeWithContentDescription("Add card to “Done”").performClick()
        onNodeWithText("The card you started in “To do” hasn’t been added.").assertExists()

        onNodeWithText("Keep editing").performClick()
        awaitGone("Discard changes?")
        onNodeWithText("Write docs").assertExists()

        onNodeWithContentDescription("Add card to “Done”").performClick()
        onNodeWithText("Discard").performClick()
        awaitGone("Discard changes?")
        onNodeWithText("Write docs").assertDoesNotExist()
        onNodeWithContentDescription("Add card to “To do”").assertExists()
        onNodeWithContentDescription("Add card to “Done”").assertDoesNotExist()
    }

    @Test
    fun deleteColumnWithCards() = runComposeUiTest {
        show()
        openColumnMenu("Review")
        onNodeWithText("Delete column").performClick()
        onNodeWithText("Delete column “Review”?").assertExists()
        onNodeWithText("Its 2 cards will be deleted too. This can’t be undone.").assertExists()
        onNodeWithText("Delete column and 2 cards").performClick()
        awaitGone("Review")
        assertThat(columnNames()).containsExactly("To do", "Done")
    }

    @Test
    fun deleteEmptyColumn() = runComposeUiTest {
        show()
        openColumnMenu("Done")
        onAllNodes(hasText("Delete column"))[0].performClick()
        onNodeWithText("This can't be undone.").assertExists()
        onAllNodes(hasText("Delete column")).fetchSemanticsNodes().let { assertThat(it.size).isEqualTo(1) }
        onNodeWithText("Delete column").performClick()
        waitUntil(timeoutMillis = 5_000) { columnNames() == listOf("To do", "Review") }
        assertThat(toasts.messages).isEmpty()
    }

    @Test
    fun missingBoardLeaves() = runComposeUiTest {
        repository.loadResults += Result.Error(BoardLoadError.BoardNotFound)
        setContent {
            AppTheme(darkTheme = false, windowClass = WindowClass.Phone) {
                val viewModel = remember {
                    BoardViewModel(
                        repository,
                        cards,
                        FakeLabelSource(),
                        toasts,
                        SavedStateHandle(mapOf(BoardViewModel.ARG_BOARD_ID to sampleBoardId.toString())),
                    )
                }
                BoardRoot(
                    viewModel = viewModel,
                    onBack = {},
                    onCardClick = {},
                    onExportClick = { _, _ -> },
                    onRenameBoardClick = {},
                    onDeleteBoardClick = {},
                    onBoardMissing = { missing++ },
                    onBoardDeleted = { deleted++ },
                )
            }
        }
        waitUntil(timeoutMillis = 5_000) { missing == 1 }
        assertResource(toasts.messages.single().text, Res.string.board_view_board_gone)
    }
}
