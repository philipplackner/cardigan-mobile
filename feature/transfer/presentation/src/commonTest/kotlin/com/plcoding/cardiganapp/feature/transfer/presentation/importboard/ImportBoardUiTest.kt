package com.plcoding.cardiganapp.feature.transfer.presentation.importboard

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import androidx.lifecycle.SavedStateHandle
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import cardiganapp.feature.transfer.presentation.generated.resources.Res
import cardiganapp.feature.transfer.presentation.generated.resources.import_dialog_cancel
import cardiganapp.feature.transfer.presentation.generated.resources.import_dialog_title
import cardiganapp.feature.transfer.presentation.generated.resources.import_drop_zone_description
import cardiganapp.feature.transfer.presentation.generated.resources.import_error_failed
import cardiganapp.feature.transfer.presentation.generated.resources.import_error_invalid_file
import cardiganapp.feature.transfer.presentation.generated.resources.import_error_too_large
import cardiganapp.feature.transfer.presentation.generated.resources.import_key_cancel
import cardiganapp.feature.transfer.presentation.generated.resources.import_key_confirm
import cardiganapp.feature.transfer.presentation.generated.resources.import_key_description_known
import cardiganapp.feature.transfer.presentation.generated.resources.import_key_description_unknown
import cardiganapp.feature.transfer.presentation.generated.resources.importing_description
import cardiganapp.feature.transfer.presentation.generated.resources.importing_title
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import com.plcoding.cardiganapp.core.domain.board.Board
import com.plcoding.cardiganapp.core.domain.util.Result
import com.plcoding.cardiganapp.core.presentation.toast.ToastType
import com.plcoding.cardiganapp.feature.transfer.domain.CheckedFile
import com.plcoding.cardiganapp.feature.transfer.domain.ImportError
import com.plcoding.cardiganapp.feature.transfer.presentation.fakes.FakeBoardKeyOwnerLookup
import com.plcoding.cardiganapp.feature.transfer.presentation.fakes.FakeBoardTransferRepository
import com.plcoding.cardiganapp.feature.transfer.presentation.fakes.FakeExportFileChecker
import com.plcoding.cardiganapp.feature.transfer.presentation.fakes.FakeFilePicker
import com.plcoding.cardiganapp.feature.transfer.presentation.fakes.FakeToastController
import com.plcoding.cardiganapp.feature.transfer.presentation.fakes.oversizedPick
import com.plcoding.cardiganapp.feature.transfer.presentation.fakes.validPick
import kotlinx.coroutines.CompletableDeferred
import org.jetbrains.compose.resources.getString
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class ImportBoardUiTest {
    private val repository = FakeBoardTransferRepository()
    private val checker = FakeExportFileChecker()
    private val lookup = FakeBoardKeyOwnerLookup(mapOf("CRD" to "Cardigan v1"))
    private val toasts = FakeToastController()
    private val picker = FakeFilePicker()
    private val imported = mutableListOf<Board>()
    private var listChanged = 0
    private var doneCount = 0

    private fun ComposeUiTest.showImport() {
        val viewModel = ImportBoardViewModel(repository, checker, lookup, toasts, SavedStateHandle())
        setContent {
            AppTheme(darkTheme = false) {
                ImportBoardRoot(
                    onImported = { imported += it },
                    onBoardListChanged = { listChanged++ },
                    onDone = { doneCount++ },
                    viewModel = viewModel,
                    filePicker = { onPicked, onCancelled -> picker.bind(onPicked, onCancelled) },
                )
            }
        }
        waitUntil { picker.openCount == 1 }
    }

    private fun ComposeUiTest.awaitText(text: String) {
        waitUntil { onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun pickerOpensFirstWithNoDialogAndCancelEndsTheImport() =
        runComposeUiTest {
            showImport()
            onAllNodes(isDialog()).assertCountEquals(0)

            picker.cancel()
            waitUntil { doneCount == 1 }
            onAllNodes(isDialog()).assertCountEquals(0)
            assertThat(toasts.messages).isEmpty()
        }

    @Test
    fun oversizedFileShowsTheSizeErrorWithItsRow() =
        runComposeUiTest {
            showImport()
            picker.pick(oversizedPick())

            awaitText(getString(Res.string.import_error_too_large, "7.2 MB"))
            onNodeWithText("board-backup.json").assertIsDisplayed()
            onNodeWithText("7.2 MB").assertIsDisplayed()
            assertThat(repository.importCalls).isEmpty()
        }

    @Test
    fun invalidFileShowsTheInvalidFileError() =
        runComposeUiTest {
            checker.result = null
            showImport()
            picker.pick(validPick(name = "notes.json"))

            awaitText(getString(Res.string.import_error_invalid_file))
            onNodeWithText("notes.json").assertIsDisplayed()
            assertThat(repository.importCalls).isEmpty()
        }

    @Test
    fun dropZoneIsAButtonThatReopensThePickerAndCancelKeepsTheError() =
        runComposeUiTest {
            checker.result = null
            showImport()
            picker.pick(validPick())
            val label = getString(Res.string.import_drop_zone_description)
            waitUntil { onAllNodes(hasContentDescription(label)).fetchSemanticsNodes().isNotEmpty() }

            onNodeWithContentDescription(label)
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
                .performClick()
            waitUntil { picker.openCount == 2 }

            picker.cancel()
            waitForIdle()
            onNodeWithText(getString(Res.string.import_error_invalid_file)).assertIsDisplayed()
            assertThat(doneCount).isEqualTo(0)
        }

    @Test
    fun cancelInTheErrorDialogEndsTheImport() =
        runComposeUiTest {
            checker.result = null
            showImport()
            picker.pick(validPick())
            val cancel = getString(Res.string.import_dialog_cancel)
            awaitText(cancel)

            onNodeWithText(cancel).performClick()
            waitUntil { doneCount == 1 }
        }

    @Test
    fun progressDialogShowsWhileUploading() =
        runComposeUiTest {
            repository.gate = CompletableDeferred()
            showImport()
            picker.pick(validPick())

            awaitText(getString(Res.string.importing_title, "board-backup.json"))
            onNodeWithText(getString(Res.string.importing_description)).assertIsDisplayed()
            assertThat(doneCount).isEqualTo(0)
        }

    @Test
    fun successSendsTheToastAndCallsOnImported() =
        runComposeUiTest {
            showImport()
            picker.pick(validPick())

            waitUntil { doneCount == 1 }
            assertThat(imported).containsExactly(repository.importedBoard)
            assertThat(toasts.messages.single().type).isEqualTo(ToastType.Success)
            onAllNodes(isDialog()).assertCountEquals(0)
        }

    @Test
    fun clashWithAKnownBoardOpensTheKeyDialogWithImportDisabled() =
        runComposeUiTest {
            repository.importResults += Result.Error(ImportError.KeyAlreadyUsed)
            showImport()
            picker.pick(validPick())

            awaitText(getString(Res.string.import_key_description_known, "Cardigan v1", "CRD"))
            onNodeWithText("You already have a board with the key CRD.").assertIsDisplayed()
            val import = getString(Res.string.import_key_confirm)
            onNodeWithText(import).assertIsNotEnabled()

            onNode(hasSetTextAction()).performTextReplacement("CRDB")
            waitForIdle()
            onNodeWithText(import).assertIsEnabled()
        }

    @Test
    fun clashWithAnUnknownBoardUsesTheSentenceWithoutAName() =
        runComposeUiTest {
            lookup.owners = emptyMap()
            checker.result = CheckedFile(key = "crd")
            repository.importResults += Result.Error(ImportError.KeyAlreadyUsed)
            showImport()
            picker.pick(validPick())

            awaitText(getString(Res.string.import_key_description_unknown, "CRD"))
            onNodeWithText(getString(Res.string.import_key_confirm)).assertIsNotEnabled()
        }

    @Test
    fun invalidKeyShowsTheFieldErrorWithoutUploading() =
        runComposeUiTest {
            repository.importResults += Result.Error(ImportError.KeyAlreadyUsed)
            showImport()
            picker.pick(validPick())
            val import = getString(Res.string.import_key_confirm)
            awaitText(import)

            onNode(hasSetTextAction()).performTextReplacement("CR1")
            onNodeWithText(import).performClick()
            awaitText("Use only the letters A–Z.")
            assertThat(repository.importCalls.size).isEqualTo(1)
        }

    @Test
    fun cancelImportClosesEverything() =
        runComposeUiTest {
            repository.importResults += Result.Error(ImportError.KeyAlreadyUsed)
            showImport()
            picker.pick(validPick())
            val cancel = getString(Res.string.import_key_cancel)
            awaitText(cancel)

            onNodeWithText(cancel).performClick()
            waitUntil { doneCount == 1 }
            assertThat(repository.importCalls.size).isEqualTo(1)
            assertThat(toasts.messages).isEmpty()
        }

    @Test
    fun failedImportShowsTheErrorAndReloadsTheList() =
        runComposeUiTest {
            repository.importResults += Result.Error(ImportError.Failed)
            showImport()
            picker.pick(validPick())

            awaitText(getString(Res.string.import_error_failed))
            assertThat(listChanged).isEqualTo(1)
            onNodeWithText(getString(Res.string.import_dialog_title)).assertIsDisplayed()
        }
}
