package com.plcoding.cardiganapp.feature.boards.presentation.boardlist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isGreaterThanOrEqualTo
import cardiganapp.feature.boards.presentation.generated.resources.Res
import cardiganapp.feature.boards.presentation.generated.resources.board_actions_description
import cardiganapp.feature.boards.presentation.generated.resources.board_menu_delete
import cardiganapp.feature.boards.presentation.generated.resources.board_menu_rename
import cardiganapp.feature.boards.presentation.generated.resources.boards_empty_title
import cardiganapp.feature.boards.presentation.generated.resources.boards_import
import cardiganapp.feature.boards.presentation.generated.resources.boards_import_board
import cardiganapp.feature.boards.presentation.generated.resources.boards_labels
import cardiganapp.feature.boards.presentation.generated.resources.boards_load_error_title
import cardiganapp.feature.boards.presentation.generated.resources.boards_log_out
import cardiganapp.feature.boards.presentation.generated.resources.boards_menu
import cardiganapp.feature.boards.presentation.generated.resources.boards_new_board
import cardiganapp.feature.boards.presentation.generated.resources.boards_refreshing
import cardiganapp.feature.boards.presentation.generated.resources.boards_signed_in_as
import cardiganapp.feature.boards.presentation.generated.resources.boards_try_again
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import com.plcoding.cardiganapp.core.designsystem.theme.WindowClass
import com.plcoding.cardiganapp.core.presentation.util.UiText
import kotlin.test.Test
import org.jetbrains.compose.resources.getString

@OptIn(ExperimentalTestApi::class)
class BoardListUiTest {

    private val actions = mutableListOf<BoardListAction>()

    private fun item(id: String, name: String, key: String, cards: String = "12 cards", updated: String = "2 hours ago") =
        BoardItemUi(id, name, key, UiText.DynamicString(cards), UiText.DynamicString(updated))

    private val boards = listOf(
        item("1", "Cardigan v1", "CRD"),
        item("2", "Raytracer", "RAY", cards = "1 card", updated = "Yesterday"),
    )

    private fun loaded(items: List<BoardItemUi> = boards) =
        BoardListState(content = BoardListContent.Loaded(items), boardCount = items.size)

    private fun ComposeUiTest.show(state: BoardListState, windowClass: WindowClass = WindowClass.Phone) {
        val (width, height) = when (windowClass) {
            WindowClass.Phone -> 390.dp to 844.dp
            WindowClass.Tablet -> 834.dp to 1112.dp
            WindowClass.Desktop -> 1440.dp to 900.dp
        }
        setContent {
            AppTheme(darkTheme = false, windowClass = windowClass) {
                Box(Modifier.requiredSize(width, height)) {
                    BoardListScreen(state = state, onAction = { actions += it })
                }
            }
        }
    }

    private fun ComposeUiTest.assertTouchTarget(node: SemanticsNodeInteraction, min: Dp = 48.dp) {
        val bounds = node.fetchSemanticsNode().touchBoundsInRoot
        val minPx = with(density) { min.toPx() }
        assertThat(bounds.width).isGreaterThanOrEqualTo(minPx)
        assertThat(bounds.height).isGreaterThanOrEqualTo(minPx)
    }

    @Test
    fun phoneRowsReadAsOneAccessibleName() = runComposeUiTest {
        show(loaded())
        onNode(hasContentDescription("Cardigan v1, CRD, 12 cards, updated 2 hours ago")).assertIsDisplayed()
        onNode(hasContentDescription("Raytracer, RAY, 1 card, updated Yesterday")).assertIsDisplayed()
    }

    @Test
    fun phoneShowsTheCountInTheTitleAndAtTheEnd() = runComposeUiTest {
        show(loaded())
        onAllNodes(hasText("2 boards")).assertCountEquals(2)
    }

    @Test
    fun oneBoardIsSingular() = runComposeUiTest {
        show(loaded(boards.take(1)))
        onAllNodes(hasText("1 board")).assertCountEquals(2)
    }

    @Test
    fun tapOnARowOpensTheBoard() = runComposeUiTest {
        show(loaded())
        onNode(hasContentDescription("Raytracer, RAY, 1 card, updated Yesterday")).performClick()
        assertThat(actions).containsExactly(BoardListAction.OnBoardClick("2"))
    }

    @Test
    fun loadingShowsNoBoardsAndNoCount() = runComposeUiTest {
        show(BoardListState(content = BoardListContent.Loading))
        onAllNodes(hasText("boards", substring = true)).assertCountEquals(0)
        onAllNodes(hasText("Cardigan v1")).assertCountEquals(0)
    }

    @Test
    fun phoneEmptyStateHasNewBoardAndImport() = runComposeUiTest {
        show(loaded(emptyList()))
        onNodeWithText(getString(Res.string.boards_empty_title)).assertIsDisplayed()
        onNodeWithText(getString(Res.string.boards_import)).performClick()
        onNodeWithText(getString(Res.string.boards_new_board)).performClick()
        assertThat(actions).containsExactly(BoardListAction.OnImportClick, BoardListAction.OnNewBoardClick)
    }

    @Test
    fun errorStateRetries() = runComposeUiTest {
        show(BoardListState(content = BoardListContent.Error))
        onNodeWithText(getString(Res.string.boards_load_error_title)).assertIsDisplayed()
        onNodeWithText(getString(Res.string.boards_try_again)).performClick()
        assertThat(actions).containsExactly(BoardListAction.OnRetry)
    }

    @Test
    fun refreshingShowsTheIndicatorAboveTheList() = runComposeUiTest {
        show(loaded().copy(isRefreshing = true))
        onNodeWithText(getString(Res.string.boards_refreshing)).assertIsDisplayed()
        onNodeWithText("Cardigan v1", useUnmergedTree = true).assertExists()
    }

    @Test
    fun titleCollapsesIntoTheBarOnScroll() = runComposeUiTest {
        val many = (1..14).map { item("$it", "Board $it", "B${'A' + it}") }
        show(loaded(many))
        onAllNodes(androidx.compose.ui.test.hasTestTag(BoardListTestTags.CollapsedBar)).assertCountEquals(0)

        onNodeWithTag(BoardListTestTags.List).performScrollToIndex(1)

        onNodeWithTag(BoardListTestTags.CollapsedBar).assertExists()
        onNodeWithContentDescription(getString(Res.string.boards_menu)).assertIsDisplayed()
        onNodeWithContentDescription(getString(Res.string.boards_new_board)).assertIsDisplayed()
    }

    @Test
    fun phoneMenuShowsTheAccountAndItsActions() = runComposeUiTest {
        show(loaded().copy(isMenuOpen = true, accountEmail = "ada@example.com"))
        onNodeWithText(getString(Res.string.boards_signed_in_as, "ada@example.com")).assertIsDisplayed()
        onNodeWithText(getString(Res.string.boards_import_board)).assertIsDisplayed()
        onNodeWithText(getString(Res.string.boards_labels)).assertIsDisplayed()
        onNodeWithText(getString(Res.string.boards_log_out)).performClick()
        assertThat(actions).containsExactly(BoardListAction.OnLogOutClick)
    }

    @Test
    fun phoneMenuWithoutEmailLeavesTheAccountLineOut() = runComposeUiTest {
        show(loaded().copy(isMenuOpen = true, accountEmail = null))
        onAllNodes(hasText("Signed in as", substring = true)).assertCountEquals(0)
        onNodeWithText(getString(Res.string.boards_import_board)).assertIsDisplayed()
    }

    @Test
    fun menuButtonOpensTheMenu() = runComposeUiTest {
        show(loaded())
        onNodeWithContentDescription(getString(Res.string.boards_menu)).performClick()
        assertThat(actions).containsExactly(BoardListAction.OnMenuClick)
    }

    @Test
    fun rowMenuOffersRenameAndDelete() = runComposeUiTest {
        show(loaded())
        val menuLabel = getString(Res.string.board_actions_description, "Cardigan v1")
        onNodeWithContentDescription(menuLabel).performClick()
        onNodeWithText(getString(Res.string.board_menu_delete)).assertIsDisplayed()
        onNodeWithText(getString(Res.string.board_menu_rename)).performClick()

        onNodeWithContentDescription(menuLabel).performClick()
        onNodeWithText(getString(Res.string.board_menu_delete)).performClick()

        assertThat(actions).containsExactly(BoardListAction.OnRenameClick("1"), BoardListAction.OnDeleteClick("1"))
    }

    @Test
    fun touchTargetsAreAtLeast48dp() = runComposeUiTest {
        show(loaded())
        assertTouchTarget(onNodeWithContentDescription(getString(Res.string.board_actions_description, "Cardigan v1")))
        assertTouchTarget(onNodeWithContentDescription(getString(Res.string.boards_menu)))
        assertTouchTarget(onNodeWithContentDescription(getString(Res.string.boards_new_board)))
        assertTouchTarget(onNode(hasContentDescription("Cardigan v1, CRD, 12 cards, updated 2 hours ago")))
    }

    @Test
    fun desktopShowsTilesWithUpdatedPrefixAndHeaderActions() = runComposeUiTest {
        show(loaded(), WindowClass.Desktop)
        onNode(hasContentDescription("Cardigan v1, CRD, 12 cards, updated 2 hours ago")).assertExists()
        onNodeWithText("Updated Yesterday", useUnmergedTree = true).assertExists()
        onNodeWithText("2 boards").assertExists()
        onNodeWithText(getString(Res.string.boards_import)).performClick()
        onNodeWithText(getString(Res.string.boards_new_board)).performClick()
        assertThat(actions).containsExactly(BoardListAction.OnImportClick, BoardListAction.OnNewBoardClick)
    }

    @Test
    fun tabletTileMenuOffersRenameAndDelete() = runComposeUiTest {
        show(loaded(), WindowClass.Tablet)
        onNodeWithContentDescription(getString(Res.string.board_actions_description, "Raytracer")).performClick()
        onNodeWithText(getString(Res.string.board_menu_delete)).performClick()
        assertThat(actions).containsExactly(BoardListAction.OnDeleteClick("2"))
    }

    @Test
    fun desktopEmptyAndErrorStates() = runComposeUiTest {
        var state by mutableStateOf(loaded(emptyList()))
        setContent {
            AppTheme(darkTheme = false, windowClass = WindowClass.Desktop) {
                Box(Modifier.requiredSize(1440.dp, 900.dp)) { BoardListScreen(state = state, onAction = { actions += it }) }
            }
        }
        onNodeWithText(getString(Res.string.boards_empty_title)).assertExists()
        onAllNodes(hasText(getString(Res.string.boards_import))).assertCountEquals(2)
        state = BoardListState(content = BoardListContent.Error)
        onNodeWithText(getString(Res.string.boards_load_error_title)).assertExists()
        onAllNodes(hasText("0 boards")).assertCountEquals(0)
    }

    @Test
    fun desktopLoadingShowsNoCount() = runComposeUiTest {
        show(BoardListState(content = BoardListContent.Loading), WindowClass.Desktop)
        onAllNodes(hasText("0 boards")).assertCountEquals(0)
    }
}
