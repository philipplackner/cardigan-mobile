package com.plcoding.cardiganapp.feature.transfer.presentation.exportboard

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import cardiganapp.feature.transfer.presentation.generated.resources.Res
import cardiganapp.feature.transfer.presentation.generated.resources.export_error_failed
import cardiganapp.feature.transfer.presentation.generated.resources.export_error_not_found
import cardiganapp.feature.transfer.presentation.generated.resources.exporting_description
import cardiganapp.feature.transfer.presentation.generated.resources.exporting_title
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import com.plcoding.cardiganapp.core.domain.util.Result
import com.plcoding.cardiganapp.core.presentation.toast.ToastType
import com.plcoding.cardiganapp.core.presentation.util.UiText
import com.plcoding.cardiganapp.feature.transfer.domain.ExportError
import com.plcoding.cardiganapp.feature.transfer.domain.ExportedFile
import com.plcoding.cardiganapp.feature.transfer.presentation.fakes.FakeBoardTransferRepository
import com.plcoding.cardiganapp.feature.transfer.presentation.fakes.FakeFileSharer
import com.plcoding.cardiganapp.feature.transfer.presentation.fakes.FakeToastController
import kotlin.test.Test
import kotlinx.coroutines.CompletableDeferred
import org.jetbrains.compose.resources.getString

@OptIn(ExperimentalTestApi::class)
class ExportBoardUiTest {

    private val repository = FakeBoardTransferRepository()
    private val toasts = FakeToastController()
    private val sharer = FakeFileSharer()
    private var doneCount = 0

    private fun androidx.compose.ui.test.ComposeUiTest.showExport() {
        val viewModel = ExportBoardViewModel("8f14e45f-ceea-4e7a-9e2f-5b1d6c3a7b21", "Cardigan v1", repository, toasts)
        setContent {
            AppTheme(darkTheme = false) {
                ExportBoardRoot(
                    boardId = "8f14e45f-ceea-4e7a-9e2f-5b1d6c3a7b21",
                    boardName = "Cardigan v1",
                    onDone = { doneCount++ },
                    viewModel = viewModel,
                    fileSharer = sharer,
                )
            }
        }
    }

    @Test
    fun showsTheExportingDialog() = runComposeUiTest {
        repository.gate = CompletableDeferred()
        showExport()
        val title = getString(Res.string.exporting_title, "Cardigan v1")
        waitUntil { onAllNodes(hasText(title)).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText(getString(Res.string.exporting_description)).assertIsDisplayed()
    }

    @Test
    fun sharesTheFileUnchangedAndShowsNoToast() = runComposeUiTest {
        val bytes = "{\"format\":\"cardigan.board\"}".encodeToByteArray()
        repository.exportResult = Result.Success(ExportedFile("CRD-2026-11-04.json", bytes))
        showExport()

        waitUntil { doneCount == 1 }
        val shared = sharer.shared.single()
        assertThat(shared.fileName).isEqualTo("CRD-2026-11-04.json")
        assertThat(shared.bytes.contentEquals(bytes)).isTrue()
        assertThat(toasts.messages).isEmpty()
    }

    @Test
    fun notFoundShowsTheErrorToast() = runComposeUiTest {
        repository.exportResult = Result.Error(ExportError.BoardNotFound)
        showExport()

        waitUntil { doneCount == 1 }
        val toast = toasts.messages.single()
        assertThat(toast.type).isEqualTo(ToastType.Error)
        assertThat((toast.text as UiText.Resource).id).isEqualTo(Res.string.export_error_not_found)
        assertThat(sharer.shared).isEmpty()
    }

    @Test
    fun failureShowsTheGenericErrorToast() = runComposeUiTest {
        repository.exportResult = Result.Error(ExportError.Failed)
        showExport()

        waitUntil { doneCount == 1 }
        assertThat((toasts.messages.single().text as UiText.Resource).id).isEqualTo(Res.string.export_error_failed)
        assertThat(sharer.shared).isEmpty()
    }
}
