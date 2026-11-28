package com.plcoding.cardiganapp.feature.boards.presentation.navigation

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import cardiganapp.feature.boards.presentation.generated.resources.Res
import cardiganapp.feature.boards.presentation.generated.resources.board_actions_description
import cardiganapp.feature.boards.presentation.generated.resources.board_dialog_cancel
import cardiganapp.feature.boards.presentation.generated.resources.board_menu_delete
import cardiganapp.feature.boards.presentation.generated.resources.board_menu_rename
import cardiganapp.feature.boards.presentation.generated.resources.boards_new_board
import cardiganapp.feature.boards.presentation.generated.resources.delete_board_confirm
import cardiganapp.feature.boards.presentation.generated.resources.delete_board_title
import cardiganapp.feature.boards.presentation.generated.resources.new_board_create
import cardiganapp.feature.boards.presentation.generated.resources.new_board_title
import cardiganapp.feature.boards.presentation.generated.resources.rename_board_save
import cardiganapp.feature.boards.presentation.generated.resources.rename_board_title
import kotlinx.coroutines.CompletableDeferred
import org.jetbrains.compose.resources.getString
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

/** The real board dialog destinations. Back-press checks are Android-only, in `BoardsGraphBackUiTest`. */
@OptIn(ExperimentalTestApi::class)
class BoardsGraphUiTest {
    private val host = BoardsGraphTestHost()

    @BeforeTest
    fun setUp() = host.start()

    @AfterTest
    fun tearDown() = host.stop()

    @Test
    fun listMenuOpensRenameAndCancelClosesIt() =
        runComposeUiTest {
            host.show(this, startAtList = true)
            host.awaitBoard(this, "Cardigan v1")
            onNodeWithContentDescription(getString(Res.string.board_actions_description, "Cardigan v1")).performClick()
            onNodeWithText(getString(Res.string.board_menu_rename)).performClick()

            val title = getString(Res.string.rename_board_title)
            host.awaitText(this, title)
            onNodeWithText(getString(Res.string.board_dialog_cancel)).performClick()
            host.awaitGone(this, title)
        }

    @Test
    fun listMenuOpensDelete() =
        runComposeUiTest {
            host.show(this, startAtList = true)
            host.awaitBoard(this, "Raytracer")
            onNodeWithContentDescription(getString(Res.string.board_actions_description, "Raytracer")).performClick()
            onNodeWithText(getString(Res.string.board_menu_delete)).performClick()
            host.awaitText(this, getString(Res.string.delete_board_title, "Raytracer"))
            onNodeWithText("Its 1 card will be deleted too. This can’t be undone.").assertExists()
        }

    @Test
    fun newBoardFromTheBarCreatesAndOpensTheBoard() =
        runComposeUiTest {
            host.show(this, startAtList = true)
            host.awaitBoard(this, "Cardigan v1")
            onNodeWithContentDescription(getString(Res.string.boards_new_board)).performClick()
            val title = getString(Res.string.new_board_title)
            host.awaitText(this, title)

            onAllNodes(hasSetTextAction())[0].performTextReplacement("Garden irrigation")
            host.awaitText(this, "GARD")
            onNodeWithText(getString(Res.string.new_board_create)).performClick()

            host.awaitGone(this, title)
            waitUntil { host.opened.isNotEmpty() }
            assertThat(host.opened.single().let { it.second to it.third }).isEqualTo("Garden irrigation" to "GARD")
        }

    @Test
    fun callerReceivesRenamed() =
        runComposeUiTest {
            host.show(this, startAtList = false)
            runOnIdle { host.navController.navigateToRenameBoard(host.raytracer.id.toString()) }
            host.awaitText(this, getString(Res.string.rename_board_title))
            onAllNodes(hasSetTextAction())[0].performTextReplacement("Path tracer")
            onNodeWithText(getString(Res.string.rename_board_save)).performClick()

            waitUntil { host.renamed.isNotEmpty() }
            assertThat(host.renamed).containsExactly("Path tracer")
            assertThat(host.deletedCount).isEqualTo(0)
        }

    @Test
    fun callerReceivesDeleted() =
        runComposeUiTest {
            host.show(this, startAtList = false)
            runOnIdle { host.navController.navigateToDeleteBoard(host.cardigan.id.toString()) }
            host.awaitText(this, getString(Res.string.delete_board_title, "Cardigan v1"))
            onNodeWithText(getString(Res.string.delete_board_confirm)).performClick()

            waitUntil { host.deletedCount == 1 }
            assertThat(host.renamed).isEmpty()
            host.awaitText(this, "Caller")
        }

    @Test
    fun cancelReportsNothing() =
        runComposeUiTest {
            host.show(this, startAtList = false)
            runOnIdle { host.navController.navigateToRenameBoard(host.raytracer.id.toString()) }
            val title = getString(Res.string.rename_board_title)
            host.awaitText(this, title)
            onNodeWithText(getString(Res.string.board_dialog_cancel)).performClick()
            host.awaitGone(this, title)
            assertThat(host.renamed).isEmpty()
            assertThat(host.deletedCount).isEqualTo(0)
        }

    @Test
    fun savingKeepsTheDialogWithCancelDisabled() =
        runComposeUiTest {
            host.repository.gate = CompletableDeferred()
            host.show(this, startAtList = false)
            runOnIdle { host.navController.navigateToRenameBoard(host.raytracer.id.toString()) }
            val title = getString(Res.string.rename_board_title)
            host.awaitText(this, title)
            onAllNodes(hasSetTextAction())[0].performTextReplacement("Path tracer")
            onNodeWithText(getString(Res.string.rename_board_save)).performClick()
            waitForIdle()

            onNodeWithText(title).assertExists()
            onNodeWithText(getString(Res.string.board_dialog_cancel)).assertIsNotEnabled()
            onAllNodes(hasText("Path tracer"))[0].assertExists()
            host.repository.gate!!.complete(Unit)
            waitUntil { host.renamed.isNotEmpty() }
        }
}
