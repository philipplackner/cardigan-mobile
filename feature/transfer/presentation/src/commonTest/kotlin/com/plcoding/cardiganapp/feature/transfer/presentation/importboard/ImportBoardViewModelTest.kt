package com.plcoding.cardiganapp.feature.transfer.presentation.importboard

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import assertk.assertions.isTrue
import cardiganapp.feature.transfer.presentation.generated.resources.Res
import cardiganapp.feature.transfer.presentation.generated.resources.import_success
import com.plcoding.cardiganapp.core.domain.board.BoardKeyError
import com.plcoding.cardiganapp.core.domain.util.Result
import com.plcoding.cardiganapp.core.presentation.toast.ToastType
import com.plcoding.cardiganapp.core.presentation.util.UiText
import com.plcoding.cardiganapp.feature.transfer.domain.CheckedFile
import com.plcoding.cardiganapp.feature.transfer.domain.ImportError
import com.plcoding.cardiganapp.feature.transfer.presentation.fakes.FakeBoardKeyOwnerLookup
import com.plcoding.cardiganapp.feature.transfer.presentation.fakes.FakeBoardTransferRepository
import com.plcoding.cardiganapp.feature.transfer.presentation.fakes.FakeExportFileChecker
import com.plcoding.cardiganapp.feature.transfer.presentation.fakes.FakeToastController
import com.plcoding.cardiganapp.feature.transfer.presentation.fakes.oversizedPick
import com.plcoding.cardiganapp.feature.transfer.presentation.fakes.validPick
import com.plcoding.cardiganapp.feature.transfer.presentation.platform.PickedFile
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

/*
 * Break vectors:
 * - first picker cancelled; picker cancelled from the error dialog, then the drop zone again
 * - key change and Import with key outside the key dialog
 * - key change to the same text must keep the field error
 * - double pick; pick while uploading
 * - oversized (null bytes, size over the limit), at the limit, invalid file
 * - server 413, 422, 409 (first upload and retry), 400 on retry, Failed on first upload and retry
 * - cancel paths are navigation only (see ImportBoardUiTest)
 * - Import disabled for every used key, including lowercase input of a used key
 * - lowercase file key that clashes
 * - invalid key input (too short, not letters), a new key that is accepted
 * - process death restore in each phase, and in Picking with a delivered result (no second picker)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ImportBoardViewModelTest {
    private lateinit var repository: FakeBoardTransferRepository
    private lateinit var checker: FakeExportFileChecker
    private lateinit var lookup: FakeBoardKeyOwnerLookup
    private lateinit var toasts: FakeToastController

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repository = FakeBoardTransferRepository()
        checker = FakeExportFileChecker()
        lookup = FakeBoardKeyOwnerLookup(mapOf("CRD" to "Cardigan v1"))
        toasts = FakeToastController()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(savedStateHandle: SavedStateHandle = SavedStateHandle()) =
        ImportBoardViewModel(repository, checker, lookup, toasts, savedStateHandle)

    private val ImportBoardViewModel.phase get() = state.value.phase

    private suspend fun ReceiveTurbine<ImportBoardEvent>.skipOpenPicker() {
        assertThat(awaitItem()).isEqualTo(ImportBoardEvent.OpenPicker)
    }

    private fun ImportBoardViewModel.clash(fileKey: String = "CRD") {
        checker.result = CheckedFile(key = fileKey)
        repository.importResults += Result.Error(ImportError.KeyAlreadyUsed)
        onAction(ImportBoardAction.FilePicked(validPick()))
    }

    @Test
    fun opensThePickerOnceWhenTheRouteOpens() =
        runTest {
            val vm = viewModel()
            vm.events.test {
                assertThat(awaitItem()).isEqualTo(ImportBoardEvent.OpenPicker)
                expectNoEvents()
            }
            assertThat(vm.phase).isEqualTo(ImportPhase.Picking)
        }

    @Test
    fun firstPickerCancelledFinishesWithNothingShown() =
        runTest {
            val vm = viewModel()
            vm.events.test {
                skipOpenPicker()
                vm.onAction(ImportBoardAction.PickerCancelled)
                assertThat(awaitItem()).isEqualTo(ImportBoardEvent.Finished)
            }
            assertThat(repository.importCalls).isEmpty()
            assertThat(toasts.messages).isEmpty()
            assertThat(vm.phase).isEqualTo(ImportPhase.Done)
        }

    @Test
    fun dropZoneOpensThePickerAgainAfterItWasCancelled() =
        runTest {
            checker.result = null
            val vm = viewModel()
            vm.events.test {
                skipOpenPicker()
                vm.onAction(ImportBoardAction.FilePicked(validPick()))
                vm.onAction(ImportBoardAction.DropZoneClick)
                assertThat(awaitItem()).isEqualTo(ImportBoardEvent.OpenPicker)
                vm.onAction(ImportBoardAction.PickerCancelled)
                vm.onAction(ImportBoardAction.DropZoneClick)
                assertThat(awaitItem()).isEqualTo(ImportBoardEvent.OpenPicker)
            }
        }

    @Test
    fun keyActionsOutsideTheKeyDialogAreIgnored() =
        runTest {
            repository.gate = CompletableDeferred()
            val vm = viewModel()
            vm.onAction(ImportBoardAction.KeyChange("CRDB"))
            vm.onAction(ImportBoardAction.ImportWithKey)
            assertThat(vm.phase).isEqualTo(ImportPhase.Picking)
            vm.onAction(ImportBoardAction.FilePicked(validPick()))
            vm.onAction(ImportBoardAction.KeyChange("CRDB"))
            vm.onAction(ImportBoardAction.ImportWithKey)
            assertThat(vm.phase).isEqualTo(ImportPhase.Uploading("board-backup.json"))
            assertThat(repository.importCalls).hasSize(1)
        }

    @Test
    fun validPickUploadsAtOnceAndShowsProgress() =
        runTest {
            repository.gate = CompletableDeferred()
            val vm = viewModel()
            vm.onAction(ImportBoardAction.FilePicked(validPick()))
            assertThat(vm.phase).isEqualTo(ImportPhase.Uploading("board-backup.json"))
            assertThat(repository.importCalls).hasSize(1)
            assertThat(repository.importCalls.single().second).isNull()
        }

    @Test
    fun successSendsToastThenImported() =
        runTest {
            val vm = viewModel()
            vm.events.test {
                skipOpenPicker()
                vm.onAction(ImportBoardAction.FilePicked(validPick()))
                assertThat(awaitItem()).isEqualTo(ImportBoardEvent.Imported(repository.importedBoard))
            }
            val toast = toasts.messages.single()
            assertThat(toast.type).isEqualTo(ToastType.Success)
            val text = toast.text as UiText.Resource
            assertThat(text.id).isEqualTo(Res.string.import_success)
            assertThat(text.args.toList()).containsExactly("Cardigan v1", "CRD")
            assertThat(vm.phase).isEqualTo(ImportPhase.Done)
        }

    @Test
    fun oversizedFileIsRejectedWithoutUpload() =
        runTest {
            val vm = viewModel()
            vm.onAction(ImportBoardAction.FilePicked(oversizedPick()))
            assertThat(vm.phase).isEqualTo(
                ImportPhase.ErrorShown(PickedFileInfo("board-backup.json", 7_549_747), ImportDialogError.TooLarge),
            )
            assertThat(repository.importCalls).isEmpty()
            assertThat(checker.checked).isEmpty()
        }

    @Test
    fun sizeOverLimitWithBytesIsStillRejected() =
        runTest {
            val vm = viewModel()
            vm.onAction(ImportBoardAction.FilePicked(PickedFile("a.json", 5_242_881, byteArrayOf(1))))
            assertThat((vm.phase as ImportPhase.ErrorShown).error).isEqualTo(ImportDialogError.TooLarge)
        }

    @Test
    fun fileAtTheLimitIsUploaded() =
        runTest {
            val vm = viewModel()
            vm.onAction(ImportBoardAction.FilePicked(PickedFile("a.json", 5_242_880, byteArrayOf(1))))
            assertThat(repository.importCalls).hasSize(1)
        }

    @Test
    fun invalidFileIsRejectedWithoutUpload() =
        runTest {
            checker.result = null
            val vm = viewModel()
            vm.onAction(ImportBoardAction.FilePicked(validPick(name = "notes.json")))
            assertThat(vm.phase).isEqualTo(
                ImportPhase.ErrorShown(PickedFileInfo("notes.json", 2), ImportDialogError.InvalidFile),
            )
            assertThat(repository.importCalls).isEmpty()
        }

    @Test
    fun dropZoneReopensThePickerAndCancelKeepsTheError() =
        runTest {
            checker.result = null
            val vm = viewModel()
            vm.events.test {
                skipOpenPicker()
                vm.onAction(ImportBoardAction.FilePicked(validPick()))
                val error = vm.phase
                vm.onAction(ImportBoardAction.DropZoneClick)
                assertThat(awaitItem()).isEqualTo(ImportBoardEvent.OpenPicker)
                vm.onAction(ImportBoardAction.PickerCancelled)
                expectNoEvents()
                assertThat(vm.phase).isEqualTo(error)
            }
        }

    @Test
    fun dropZoneWhilePickerIsOpenDoesNotOpenASecondPicker() =
        runTest {
            checker.result = null
            val vm = viewModel()
            vm.events.test {
                skipOpenPicker()
                vm.onAction(ImportBoardAction.FilePicked(validPick()))
                vm.onAction(ImportBoardAction.DropZoneClick)
                assertThat(awaitItem()).isEqualTo(ImportBoardEvent.OpenPicker)
                vm.onAction(ImportBoardAction.DropZoneClick)
                expectNoEvents()
            }
        }

    @Test
    fun validPickFromTheErrorDialogUploads() =
        runTest {
            checker.result = null
            val vm = viewModel()
            vm.onAction(ImportBoardAction.FilePicked(validPick()))
            checker.result = CheckedFile("CRD")
            vm.onAction(ImportBoardAction.DropZoneClick)
            vm.onAction(ImportBoardAction.FilePicked(validPick()))
            assertThat(repository.importCalls).hasSize(1)
            assertThat(vm.phase).isEqualTo(ImportPhase.Done)
        }

    @Test
    fun pickWhileUploadingIsIgnored() =
        runTest {
            repository.gate = CompletableDeferred()
            val vm = viewModel()
            vm.onAction(ImportBoardAction.FilePicked(validPick()))
            vm.onAction(ImportBoardAction.FilePicked(validPick(name = "other.json")))
            assertThat(repository.importCalls).hasSize(1)
            assertThat(vm.phase).isEqualTo(ImportPhase.Uploading("board-backup.json"))
        }

    @Test
    fun doublePickUploadsOnce() =
        runTest {
            val vm = viewModel()
            vm.onAction(ImportBoardAction.FilePicked(validPick()))
            vm.onAction(ImportBoardAction.FilePicked(validPick()))
            assertThat(repository.importCalls).hasSize(1)
        }

    @Test
    fun server413ShowsTheSizeErrorWithThePickedSize() =
        runTest {
            repository.importResults += Result.Error(ImportError.FileTooLarge)
            val vm = viewModel()
            vm.onAction(ImportBoardAction.FilePicked(validPick()))
            assertThat(vm.phase).isEqualTo(
                ImportPhase.ErrorShown(PickedFileInfo("board-backup.json", 2), ImportDialogError.TooLarge),
            )
        }

    @Test
    fun server422ShowsTheInvalidFileError() =
        runTest {
            repository.importResults += Result.Error(ImportError.InvalidFile)
            val vm = viewModel()
            vm.onAction(ImportBoardAction.FilePicked(validPick()))
            assertThat((vm.phase as ImportPhase.ErrorShown).error).isEqualTo(ImportDialogError.InvalidFile)
        }

    @Test
    fun failedShowsTheImportErrorAndReloadsTheList() =
        runTest {
            repository.importResults += Result.Error(ImportError.Failed)
            val vm = viewModel()
            vm.events.test {
                skipOpenPicker()
                vm.onAction(ImportBoardAction.FilePicked(validPick()))
                assertThat(awaitItem()).isEqualTo(ImportBoardEvent.BoardListChanged)
            }
            assertThat((vm.phase as ImportPhase.ErrorShown).error).isEqualTo(ImportDialogError.Failed)
            assertThat(repository.importCalls).hasSize(1)
        }

    @Test
    fun clashOpensTheKeyDialogWithTheUsedError() =
        runTest {
            val vm = viewModel()
            vm.clash()
            assertThat(vm.phase).isEqualTo(
                ImportPhase.ChoosingKey(
                    key = "CRD",
                    clashKey = "CRD",
                    boardName = "Cardigan v1",
                    usedKeys = setOf("CRD"),
                    fieldError = KeyFieldError.AlreadyUsed("CRD"),
                ),
            )
            assertThat((vm.phase as ImportPhase.ChoosingKey).canImport).isFalse()
        }

    @Test
    fun clashWithAnUnknownBoardHasNoName() =
        runTest {
            lookup.owners = emptyMap()
            val vm = viewModel()
            vm.clash()
            assertThat((vm.phase as ImportPhase.ChoosingKey).boardName).isNull()
        }

    @Test
    fun lowercaseFileKeyClashUsesTheNormalizedKey() =
        runTest {
            val vm = viewModel()
            vm.clash(fileKey = "crd")
            val phase = vm.phase as ImportPhase.ChoosingKey
            assertThat(phase.key).isEqualTo("CRD")
            assertThat(phase.boardName).isEqualTo("Cardigan v1")
            assertThat(phase.canImport).isFalse()
            assertThat(lookup.lookups).containsExactly("CRD")
        }

    @Test
    fun changingTheKeyClearsTheErrorAndEnablesImport() =
        runTest {
            val vm = viewModel()
            vm.clash()
            vm.onAction(ImportBoardAction.KeyChange("CRDB"))
            val phase = vm.phase as ImportPhase.ChoosingKey
            assertThat(phase.fieldError).isNull()
            assertThat(phase.canImport).isTrue()
        }

    @Test
    fun retypingTheSameKeyKeepsTheUsedError() =
        runTest {
            val vm = viewModel()
            vm.clash()
            vm.onAction(ImportBoardAction.KeyChange("CRD"))
            assertThat((vm.phase as ImportPhase.ChoosingKey).fieldError).isEqualTo(KeyFieldError.AlreadyUsed("CRD"))
        }

    @Test
    fun importStaysDisabledForEveryUsedKeyIncludingLowercase() =
        runTest {
            val vm = viewModel()
            vm.clash()
            repository.importResults += Result.Error(ImportError.KeyAlreadyUsed)
            vm.onAction(ImportBoardAction.KeyChange("crdb"))
            vm.onAction(ImportBoardAction.ImportWithKey)
            assertThat((vm.phase as ImportPhase.ChoosingKey).usedKeys).isEqualTo(setOf("CRD", "CRDB"))
            vm.onAction(ImportBoardAction.KeyChange("crd"))
            assertThat((vm.phase as ImportPhase.ChoosingKey).canImport).isFalse()
            vm.onAction(ImportBoardAction.KeyChange("cRdB"))
            assertThat((vm.phase as ImportPhase.ChoosingKey).canImport).isFalse()
            vm.onAction(ImportBoardAction.ImportWithKey)
            assertThat(repository.importCalls).hasSize(2)
        }

    @Test
    fun validNewKeyUploadsTheSameBytesWithTheNormalizedKey() =
        runTest {
            val vm = viewModel()
            vm.clash()
            vm.onAction(ImportBoardAction.KeyChange("crdb"))
            repository.gate = CompletableDeferred()
            vm.onAction(ImportBoardAction.ImportWithKey)
            assertThat(vm.phase).isEqualTo(ImportPhase.Uploading("board-backup.json"))
            assertThat(repository.importCalls[1].second).isEqualTo("CRDB")
            assertThat(repository.importCalls[1].first).isEqualTo(repository.importCalls[0].first)
        }

    @Test
    fun tooShortKeyShowsTheRuleErrorWithoutUpload() =
        runTest {
            val vm = viewModel()
            vm.clash()
            vm.onAction(ImportBoardAction.KeyChange("C"))
            vm.onAction(ImportBoardAction.ImportWithKey)
            assertThat(
                (vm.phase as ImportPhase.ChoosingKey).fieldError,
            ).isEqualTo(KeyFieldError.Rule(BoardKeyError.TooShort))
            assertThat(repository.importCalls).hasSize(1)
        }

    @Test
    fun emptyKeyShowsTheRequiredErrorWithoutUpload() =
        runTest {
            val vm = viewModel()
            vm.clash()
            vm.onAction(ImportBoardAction.KeyChange(""))
            vm.onAction(ImportBoardAction.ImportWithKey)
            assertThat(
                (vm.phase as ImportPhase.ChoosingKey).fieldError,
            ).isEqualTo(KeyFieldError.Rule(BoardKeyError.Required))
            assertThat(repository.importCalls).hasSize(1)
        }

    @Test
    fun nonLetterKeyShowsTheFormatError() =
        runTest {
            val vm = viewModel()
            vm.clash()
            vm.onAction(ImportBoardAction.KeyChange("CR1"))
            vm.onAction(ImportBoardAction.ImportWithKey)
            assertThat(
                (vm.phase as ImportPhase.ChoosingKey).fieldError,
            ).isEqualTo(KeyFieldError.Rule(BoardKeyError.InvalidFormat))
        }

    @Test
    fun retryClashShowsTheNewKeyAsUsed() =
        runTest {
            val vm = viewModel()
            vm.clash()
            repository.importResults += Result.Error(ImportError.KeyAlreadyUsed)
            vm.onAction(ImportBoardAction.KeyChange("CRDB"))
            vm.onAction(ImportBoardAction.ImportWithKey)
            val phase = vm.phase as ImportPhase.ChoosingKey
            assertThat(phase.key).isEqualTo("CRDB")
            assertThat(phase.clashKey).isEqualTo("CRDB")
            assertThat(phase.fieldError).isEqualTo(KeyFieldError.AlreadyUsed("CRDB"))
        }

    @Test
    fun badRequestOnRetryKeepsTheDialogWithTheRuleError() =
        runTest {
            val vm = viewModel()
            vm.clash()
            repository.importResults += Result.Error(ImportError.KeyInvalid(BoardKeyError.InvalidFormat))
            vm.onAction(ImportBoardAction.KeyChange("CRDB"))
            vm.onAction(ImportBoardAction.ImportWithKey)
            val phase = vm.phase as ImportPhase.ChoosingKey
            assertThat(phase.key).isEqualTo("CRDB")
            assertThat(phase.clashKey).isEqualTo("CRD")
            assertThat(phase.fieldError).isEqualTo(KeyFieldError.Rule(BoardKeyError.InvalidFormat))
        }

    @Test
    fun failedOnRetryBehavesLikeTheFirstUpload() =
        runTest {
            val vm = viewModel()
            vm.events.test {
                skipOpenPicker()
                vm.clash()
                repository.importResults += Result.Error(ImportError.Failed)
                vm.onAction(ImportBoardAction.KeyChange("CRDB"))
                vm.onAction(ImportBoardAction.ImportWithKey)
                assertThat(awaitItem()).isEqualTo(ImportBoardEvent.BoardListChanged)
            }
            assertThat((vm.phase as ImportPhase.ErrorShown).error).isEqualTo(ImportDialogError.Failed)
        }

    @Test
    fun keyInvalidOnTheFirstUploadIsAFailure() =
        runTest {
            repository.importResults += Result.Error(ImportError.KeyInvalid(BoardKeyError.TooLong))
            val vm = viewModel()
            vm.onAction(ImportBoardAction.FilePicked(validPick()))
            assertThat((vm.phase as ImportPhase.ErrorShown).error).isEqualTo(ImportDialogError.Failed)
        }

    @Test
    fun restoreInPickingWaitsForTheDeliveredResult() =
        runTest {
            val handle = SavedStateHandle()
            viewModel(handle)
            val restored = viewModel(handle)
            restored.events.test {
                expectNoEvents()
                restored.onAction(ImportBoardAction.FilePicked(validPick()))
                assertThat(awaitItem()).isInstanceOf(ImportBoardEvent.Imported::class)
            }
        }

    @Test
    fun restoreInPickingWithACancelFinishes() =
        runTest {
            val handle = SavedStateHandle()
            viewModel(handle)
            val restored = viewModel(handle)
            restored.events.test {
                restored.onAction(ImportBoardAction.PickerCancelled)
                assertThat(awaitItem()).isEqualTo(ImportBoardEvent.Finished)
            }
        }

    @Test
    fun restoreInErrorShownKeepsTheError() =
        runTest {
            checker.result = null
            val handle = SavedStateHandle()
            viewModel(handle).onAction(ImportBoardAction.FilePicked(validPick()))
            val restored = viewModel(handle)
            assertThat(restored.phase).isEqualTo(
                ImportPhase.ErrorShown(PickedFileInfo("board-backup.json", 2), ImportDialogError.InvalidFile),
            )
            restored.events.test { expectNoEvents() }
        }

    @Test
    fun restoreInUploadingShowsFailedAndReloads() =
        runTest {
            repository.gate = CompletableDeferred()
            val handle = SavedStateHandle()
            viewModel(handle).onAction(ImportBoardAction.FilePicked(validPick()))
            val restored = viewModel(handle)
            assertThat(restored.phase).isEqualTo(ImportPhase.ErrorShown(file = null, error = ImportDialogError.Failed))
            restored.events.test {
                assertThat(awaitItem()).isEqualTo(ImportBoardEvent.BoardListChanged)
                expectNoEvents()
            }
        }

    @Test
    fun restoreInChoosingKeyShowsFailedAndReloads() =
        runTest {
            val handle = SavedStateHandle()
            viewModel(handle).clash()
            val restored = viewModel(handle)
            assertThat(restored.phase).isEqualTo(ImportPhase.ErrorShown(file = null, error = ImportDialogError.Failed))
            restored.events.test {
                assertThat(awaitItem()).isEqualTo(ImportBoardEvent.BoardListChanged)
            }
        }

    @Test
    fun restoreAfterTheFailedRestoreDoesNotReloadAgain() =
        runTest {
            val handle = SavedStateHandle()
            viewModel(handle).clash()
            viewModel(handle)
            val again = viewModel(handle)
            again.events.test { expectNoEvents() }
        }
}
