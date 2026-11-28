package com.plcoding.cardiganapp.feature.boards.presentation.newboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import androidx.lifecycle.SavedStateHandle
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import cardiganapp.feature.boards.presentation.generated.resources.Res
import cardiganapp.feature.boards.presentation.generated.resources.board_dialog_cancel
import cardiganapp.feature.boards.presentation.generated.resources.delete_board_body_empty
import cardiganapp.feature.boards.presentation.generated.resources.delete_board_confirm
import cardiganapp.feature.boards.presentation.generated.resources.delete_board_title
import cardiganapp.feature.boards.presentation.generated.resources.new_board_columns_note
import cardiganapp.feature.boards.presentation.generated.resources.new_board_create
import cardiganapp.feature.boards.presentation.generated.resources.new_board_key_hint
import cardiganapp.feature.boards.presentation.generated.resources.rename_board_key_hint
import cardiganapp.feature.boards.presentation.generated.resources.rename_board_save
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import com.plcoding.cardiganapp.core.designsystem.theme.WindowClass
import com.plcoding.cardiganapp.feature.boards.presentation.deleteboard.DeleteBoardRoot
import com.plcoding.cardiganapp.feature.boards.presentation.deleteboard.DeleteBoardViewModel
import com.plcoding.cardiganapp.feature.boards.presentation.fakes.FakeBoardRepository
import com.plcoding.cardiganapp.feature.boards.presentation.fakes.FakeToastController
import com.plcoding.cardiganapp.feature.boards.presentation.fakes.board
import com.plcoding.cardiganapp.feature.boards.presentation.renameboard.RenameBoardRoot
import com.plcoding.cardiganapp.feature.boards.presentation.renameboard.RenameBoardViewModel
import org.jetbrains.compose.resources.getString
import kotlin.test.Test

/** New board, Rename board and Delete board with their real ViewModels, on Phone and Desktop. */
@OptIn(ExperimentalTestApi::class)
class BoardDialogsUiTest {
    private val cardigan =
        board(id = "0b5c8a6e-1f00-4a3c-9a43-6f5e0e3c1d2a", name = "Cardigan v1", key = "CRD", cardCount = 12)
    private val empty = board(id = "1c6d9b7f-2a11-4b4d-8b54-7a6f1f4d2e3b", name = "Empty", key = "EMP", cardCount = 0)
    private val repository = FakeBoardRepository(listOf(cardigan, empty))
    private val toasts = FakeToastController()
    private val created = mutableListOf<String>()
    private val renamed = mutableListOf<String>()
    private var closed = 0

    private fun ComposeUiTest.show(
        windowClass: WindowClass,
        content: @Composable () -> Unit,
    ) {
        setContent { AppTheme(darkTheme = false, windowClass = windowClass) { content() } }
    }

    private fun ComposeUiTest.newBoard(windowClass: WindowClass) =
        show(windowClass) {
            NewBoardRoot(
                onClose = { closed++ },
                onCreated = { _, name, key -> created += "$name/$key" },
                viewModel = NewBoardViewModel(repository, toasts, SavedStateHandle()),
            )
        }

    private fun ComposeUiTest.fields() = onAllNodes(hasSetTextAction())

    private suspend fun ComposeUiTest.newBoardFlow(windowClass: WindowClass) {
        newBoard(windowClass)
        onNodeWithText(getString(Res.string.new_board_columns_note)).assertExists()
        onNodeWithText(getString(Res.string.new_board_create)).assertIsEnabled()

        onNodeWithText(getString(Res.string.new_board_create)).performClick()
        onNodeWithText("Enter a name.").assertExists()
        onNodeWithText("Enter a key.").assertExists()
        onNodeWithText(getString(Res.string.new_board_create)).assertIsNotEnabled()
        assertThat(repository.createCalls).isEmpty()

        fields()[0].performTextReplacement("Garden irrigation")
        fields()[1].assertTextEquals("GARD", includeEditableText = true)
        onNodeWithText(getString(Res.string.new_board_key_hint, "GARD")).assertExists()
        onAllNodes(hasText("Enter a name.")).assertCountEquals(0)

        fields()[1].performTextReplacement("crd")
        fields()[1].assertTextEquals("CRD", includeEditableText = true)
        onNodeWithText("You already have a board with the key CRD.").assertExists()

        fields()[1].performTextReplacement("gi")
        onNodeWithText(getString(Res.string.new_board_create)).performClick()
        waitUntil { created.isNotEmpty() }
        assertThat(created).containsExactly("Garden irrigation/GI")
    }

    @Test
    fun newBoardOnPhone() = runComposeUiTest { newBoardFlow(WindowClass.Phone) }

    @Test
    fun newBoardOnDesktop() = runComposeUiTest { newBoardFlow(WindowClass.Desktop) }

    private suspend fun ComposeUiTest.renameFlow(windowClass: WindowClass) {
        show(windowClass) {
            RenameBoardRoot(
                viewModel = RenameBoardViewModel(cardigan.id.toString(), repository, toasts, SavedStateHandle()),
                onClose = { closed++ },
                onRenamed = { renamed += it },
            )
        }
        onNodeWithText(getString(Res.string.rename_board_key_hint)).assertExists()
        onNodeWithText("CRD").assertExists()
        fields().assertCountEquals(1)

        fields()[0].performTextReplacement("Cardigan v1 ")
        onNodeWithText(getString(Res.string.rename_board_save)).performClick()
        waitUntil { closed == 1 }
        assertThat(repository.renameCalls).isEmpty()

        fields()[0].performTextReplacement("Cardigan v2")
        onNodeWithText(getString(Res.string.rename_board_save)).performClick()
        waitUntil { renamed.isNotEmpty() }
        assertThat(renamed).containsExactly("Cardigan v2")
    }

    @Test
    fun renameOnPhone() = runComposeUiTest { renameFlow(WindowClass.Phone) }

    @Test
    fun renameOnDesktop() = runComposeUiTest { renameFlow(WindowClass.Desktop) }

    @Test
    fun deleteWithCards() =
        runComposeUiTest {
            var deleted = 0
            show(WindowClass.Phone) {
                DeleteBoardRoot(
                    viewModel = DeleteBoardViewModel(cardigan.id.toString(), repository, toasts),
                    onClose = { closed++ },
                    onDeleted = { deleted++ },
                )
            }
            onNodeWithText(getString(Res.string.delete_board_title, "Cardigan v1")).assertExists()
            onNodeWithText("Its 12 cards will be deleted too. This can’t be undone.").assertExists()
            onNodeWithText(getString(Res.string.delete_board_confirm)).performClick()
            waitUntil { deleted == 1 }
        }

    @Test
    fun deleteWithoutCardsOnDesktop() =
        runComposeUiTest {
            show(WindowClass.Desktop) {
                DeleteBoardRoot(
                    viewModel = DeleteBoardViewModel(empty.id.toString(), repository, toasts),
                    onClose = { closed++ },
                    onDeleted = {},
                )
            }
            onNodeWithText(getString(Res.string.delete_board_body_empty)).assertExists()
            onNodeWithText(getString(Res.string.board_dialog_cancel)).performClick()
            waitUntil { closed == 1 }
            assertThat(repository.deleteCalls).isEmpty()
        }
}
