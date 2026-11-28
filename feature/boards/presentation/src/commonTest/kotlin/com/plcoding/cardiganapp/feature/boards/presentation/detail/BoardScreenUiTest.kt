package com.plcoding.cardiganapp.feature.boards.presentation.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isCloseTo
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isLessThan
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import com.plcoding.cardiganapp.core.designsystem.theme.WindowClass
import com.plcoding.cardiganapp.core.domain.label.LabelColor
import com.plcoding.cardiganapp.feature.boards.presentation.fakes.boardContent
import com.plcoding.cardiganapp.feature.boards.presentation.fakes.card
import com.plcoding.cardiganapp.feature.boards.presentation.fakes.column
import com.plcoding.cardiganapp.feature.boards.presentation.fakes.label
import kotlin.test.Test

/*
 * Each state on phone and desktop, the main board on tablet: header text per window class and
 * card count, columns and tiles, label order on a tile, the D38 labels, touch targets, skeleton,
 * load error, no columns, no cards, quick add and add column fields, and pull to refresh.
 */
@OptIn(ExperimentalTestApi::class)
class BoardScreenUiTest {
    private val actions = mutableListOf<BoardAction>()

    private val api = label("api", LabelColor.Blue)
    private val infra = label("infra", LabelColor.Amber)
    private val web = label("web", LabelColor.Teal)
    private val todo =
        column(
            "To do",
            card(118, "Decide card ordering", listOf(api)),
            card(121, "Define export file format v1", listOf(infra, api)),
        )
    private val doing = column("In progress", card(109, "Single-flight token refresh", listOf(web)))
    private val review = column("Review")
    private val board = boardContent(todo, doing, review)
    private val labelsById = listOf(api, infra, web).associateBy { it.id }

    private fun loaded(
        content: com.plcoding.cardiganapp.core.domain.board.BoardContent = board,
        field: InlineField? = null,
    ) = BoardState(
        content = BoardLoadState.Loaded(content, labelsById),
        headerHint = BoardHeaderHint("Cardigan v1", "CRD"),
        inlineField = field,
    )

    private fun ComposeUiTest.show(
        state: BoardState,
        windowClass: WindowClass = WindowClass.Phone,
    ) {
        val (width, height) =
            when (windowClass) {
                WindowClass.Phone -> 390.dp to 844.dp
                WindowClass.Tablet -> 834.dp to 1112.dp
                WindowClass.Desktop -> 1440.dp to 900.dp
            }
        setContent {
            AppTheme(darkTheme = false, windowClass = windowClass) {
                // The test window can be smaller than the frame; keep the frame's top start on screen.
                Box(Modifier.wrapContentSize(Alignment.TopStart, unbounded = true).requiredSize(width, height)) {
                    BoardScreen(state = state, onAction = { actions += it })
                }
            }
        }
    }

    private fun ComposeUiTest.assertTouchTarget(description: String) {
        val bounds = onNodeWithContentDescription(description).fetchSemanticsNode().touchBoundsInRoot
        with(density) {
            assertThat(bounds.width.toDp()).isGreaterThanOrEqualTo(48.dp)
            assertThat(bounds.height.toDp()).isGreaterThanOrEqualTo(48.dp)
        }
    }

    @Test
    fun phoneBoard() =
        runComposeUiTest {
            show(loaded())
            onNodeWithText("Cardigan v1").assertIsDisplayed()
            onNodeWithText("CRD · 3 cards").assertIsDisplayed()
            onNodeWithText("To do").assertIsDisplayed()
            onNodeWithContentDescription("CRD-121, Define export file format v1, labels api, infra").assertIsDisplayed()
            onNodeWithContentDescription("CRD-109, Single-flight token refresh, labels web").assertExists()
            assertTouchTarget("Back")
            assertTouchTarget("Board actions")
            assertTouchTarget("Actions for column “To do”")
            onNodeWithContentDescription("Add card to “To do”").assertIsDisplayed()
        }

    @Test
    fun phoneNextColumnPeeksIn() =
        runComposeUiTest {
            show(loaded())
            val second = onNodeWithTag(BoardTestTags.column("In progress")).fetchSemanticsNode().boundsInRoot
            with(density) {
                assertThat(second.left.toDp()).isLessThan(390.dp)
                assertThat(second.left.toDp()).isGreaterThan(300.dp)
            }
        }

    @Test
    fun phoneColumnsAre300Wide() =
        runComposeUiTest {
            show(loaded())
            val bounds = onNodeWithTag(BoardTestTags.column("To do")).fetchSemanticsNode().boundsInRoot
            with(density) { assertThat(bounds.width.toDp()).isEqualTo(300.dp) }
        }

    @Test
    fun tabletBoardUsesTheDesktopLayout() =
        runComposeUiTest {
            show(loaded(), WindowClass.Tablet)
            onNodeWithText("Cardigan v1").assertIsDisplayed()
            onNodeWithText("CRD").assertIsDisplayed()
            onNodeWithText("3 cards").assertIsDisplayed()
            onNodeWithContentDescription("Back").assertDoesNotExist()
            val bounds = onNodeWithTag(BoardTestTags.column("To do")).fetchSemanticsNode().boundsInRoot
            with(density) { assertThat(bounds.width.toDp()).isEqualTo(276.dp) }
        }

    @Test
    fun desktopBoard() =
        runComposeUiTest {
            show(loaded(), WindowClass.Desktop)
            onNodeWithText("3 cards").assertIsDisplayed()
            onNodeWithText("Review").assertIsDisplayed()
            onNodeWithText("No cards yet").assertIsDisplayed()
            onNodeWithTag(BoardTestTags.ADD_COLUMN).assertIsDisplayed()
            assertTouchTarget("Board actions")
        }

    @Test
    fun singleCardSaysOneCard() =
        runComposeUiTest {
            show(loaded(boardContent(column("To do", card(1, "Setup")))), WindowClass.Desktop)
            onNodeWithText("1 card").assertIsDisplayed()
            onNodeWithContentDescription("CRD-1, Setup").assertIsDisplayed()
        }

    @Test
    fun boardWithoutCardsShowsNoCount() =
        runComposeUiTest {
            show(loaded(boardContent(column("To do"), column("Done"))))
            onNodeWithText("CRD").assertIsDisplayed()
            onNodeWithText("CRD · 0 cards").assertDoesNotExist()
            onAllNodes(hasText("No cards yet")).fetchSemanticsNodes().let { assertThat(it.size).isEqualTo(2) }
        }

    @Test
    fun loadingPhoneShowsHintAndSkeletonWithoutBoardMenu() =
        runComposeUiTest {
            show(BoardState(headerHint = BoardHeaderHint("Cardigan v1", "CRD")))
            onNodeWithText("Cardigan v1").assertIsDisplayed()
            onNodeWithText("CRD").assertIsDisplayed()
            onNodeWithTag(BoardTestTags.SKELETON).assertIsDisplayed()
            onNodeWithContentDescription("Board actions").assertDoesNotExist()
        }

    @Test
    fun loadingDesktopWithoutHint() =
        runComposeUiTest {
            show(BoardState(), WindowClass.Desktop)
            onNodeWithTag(BoardTestTags.SKELETON).assertIsDisplayed()
            onNodeWithContentDescription("Board actions").assertDoesNotExist()
        }

    @Test
    fun errorPhone() =
        runComposeUiTest {
            show(BoardState(content = BoardLoadState.Error, headerHint = BoardHeaderHint("Cardigan v1", "CRD")))
            onNodeWithText("Couldn’t load this board").assertIsDisplayed()
            onNodeWithText("Check your connection and try again.").assertIsDisplayed()
            onNodeWithContentDescription("Board actions").assertDoesNotExist()
            onNodeWithText("Try again").performClick()
            assertThat(actions).contains(BoardAction.Retry)
        }

    @Test
    fun errorDesktopWithoutHint() =
        runComposeUiTest {
            show(BoardState(content = BoardLoadState.Error), WindowClass.Desktop)
            onNodeWithText("Couldn’t load this board").assertIsDisplayed()
            onNodeWithText("Cardigan v1").assertDoesNotExist()
        }

    @Test
    fun noColumnsPhoneAndAddColumn() =
        runComposeUiTest {
            show(loaded(boardContent()))
            onNodeWithText("This board has no columns").assertIsDisplayed()
            onNodeWithText("Add a column to start adding cards.").assertIsDisplayed()
            onNodeWithText("Add column").performClick()
            assertThat(actions).contains(BoardAction.OpenAddColumn)
        }

    @Test
    fun noColumnsDesktop() =
        runComposeUiTest {
            show(loaded(boardContent()), WindowClass.Desktop)
            onNodeWithText("This board has no columns").assertIsDisplayed()
        }

    @Test
    fun addColumnFieldReplacesTheEmptyState() =
        runComposeUiTest {
            show(loaded(boardContent(), InlineField(InlineTarget.AddColumn, "Blocked")), WindowClass.Desktop)
            onNodeWithText("This board has no columns").assertDoesNotExist()
            onNodeWithText("Blocked").assertIsDisplayed()
        }

    @Test
    fun newBoardPhone() =
        runComposeUiTest {
            show(loaded(boardContent(column("To do"), column("In progress"), column("Done"))))
            onAllNodes(
                hasText("No cards yet"),
            ).fetchSemanticsNodes().let { assertThat(it.size).isGreaterThanOrEqualTo(1) }
        }

    @Test
    fun quickAddFieldReplacesAddCard() =
        runComposeUiTest {
            show(loaded(field = InlineField(InlineTarget.QuickAdd(todo.id), "Write docs")))
            onNodeWithText("Write docs").assertIsDisplayed()
            onNodeWithContentDescription("Add card to “To do”").assertDoesNotExist()
            onNodeWithText("Cancel").assertIsDisplayed()
        }

    @Test
    fun quickAddWithErrorDisablesAddCard() =
        runComposeUiTest {
            show(
                loaded(
                    field =
                        InlineField(
                            InlineTarget.QuickAdd(review.id),
                            "Fix\nlogin",
                            showErrors = true,
                            error = com.plcoding.cardiganapp.core.domain.text.NameError.InvalidCharacter,
                        ),
                ),
                WindowClass.Desktop,
            )
            onNodeWithText("Use a single line without control characters.").assertIsDisplayed()
            onAllNodes(hasText("Add card")).fetchSemanticsNodes().let { assertThat(it.size).isEqualTo(1) }
            onNode(hasText("Add card")).assertIsNotEnabled()
        }

    @Test
    fun cardTapReportsTheCard() =
        runComposeUiTest {
            show(loaded())
            onNodeWithContentDescription("CRD-118, Decide card ordering, labels api").performClick()
            assertThat(actions).contains(BoardAction.CardClick(todo.cards.first().id))
        }

    @Test
    fun pullDownOnAColumnRefreshesThatColumn() =
        runComposeUiTest {
            show(loaded(), WindowClass.Desktop)
            onNodeWithTag(BoardTestTags.columnCards("In progress")).performTouchInput {
                swipeDown(startY = top + 1f, endY = top + 600f, durationMillis = 400)
            }
            waitUntil(timeoutMillis = 5_000) { actions.any { it is BoardAction.Refresh } }
            assertThat(actions.filterIsInstance<BoardAction.Refresh>().first()).isEqualTo(BoardAction.Refresh(doing.id))
        }

    @Test
    fun addColumnButtonOpensTheField() =
        runComposeUiTest {
            show(loaded(), WindowClass.Desktop)
            onNodeWithText("Add column").performClick()
            assertThat(actions).contains(BoardAction.OpenAddColumn)
        }

    @Test
    fun noCardsYetIsHiddenWhileQuickAddIsOpenInThatColumn() =
        runComposeUiTest {
            show(loaded(field = InlineField(InlineTarget.QuickAdd(review.id))), WindowClass.Desktop)
            onNodeWithText("No cards yet").assertDoesNotExist()
            onNodeWithTag(BoardTestTags.INLINE_FIELD).assertExists()
        }

    /**
     * Figma column header: the 28dp menu button sits 2dp inside the header's end and top, and the
     * header sits in the column's 8dp padding. Figma also counts the 1dp border, which `BoardColumn` draws on top.
     */
    private fun ComposeUiTest.assertMenuButtonAtHeaderEnd(columnName: String) {
        val column = onNodeWithTag(BoardTestTags.column(columnName)).fetchSemanticsNode().boundsInRoot
        val button = onNodeWithContentDescription("Actions for column “$columnName”").fetchSemanticsNode().boundsInRoot
        with(density) {
            assertThat((column.right - button.right).toDp().value).isCloseTo(10f, 0.5f)
            assertThat((button.top - column.top).toDp().value).isCloseTo(10f, 0.5f)
            assertThat(button.width.toDp().value).isCloseTo(28f, 0.5f)
        }
    }

    @Test
    fun columnMenuButtonSitsAtTheHeaderEndOnPhone() =
        runComposeUiTest {
            show(loaded())
            assertMenuButtonAtHeaderEnd("To do")
            assertTouchTarget("Actions for column “To do”")
        }

    @Test
    fun columnMenuButtonSitsAtTheHeaderEndOnDesktop() =
        runComposeUiTest {
            show(loaded(boardContent(column("To do"), column("A rather long column name here"))), WindowClass.Desktop)
            assertMenuButtonAtHeaderEnd("To do")
            assertMenuButtonAtHeaderEnd("A rather long column name here")
        }
}
