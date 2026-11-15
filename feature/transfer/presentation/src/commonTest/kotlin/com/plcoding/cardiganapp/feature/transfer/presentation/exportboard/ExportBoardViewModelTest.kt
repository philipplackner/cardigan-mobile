package com.plcoding.cardiganapp.feature.transfer.presentation.exportboard

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isSameInstanceAs
import cardiganapp.feature.transfer.presentation.generated.resources.Res
import cardiganapp.feature.transfer.presentation.generated.resources.export_error_failed
import cardiganapp.feature.transfer.presentation.generated.resources.export_error_not_found
import com.plcoding.cardiganapp.core.domain.util.Result
import com.plcoding.cardiganapp.core.presentation.toast.ToastType
import com.plcoding.cardiganapp.core.presentation.util.UiText
import com.plcoding.cardiganapp.feature.transfer.domain.ExportError
import com.plcoding.cardiganapp.feature.transfer.presentation.fakes.FakeBoardTransferRepository
import com.plcoding.cardiganapp.feature.transfer.presentation.fakes.FakeToastController
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.uuid.Uuid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class ExportBoardViewModelTest {

    private val boardId = "8f14e45f-ceea-4e7a-9e2f-5b1d6c3a7b21"
    private lateinit var repository: FakeBoardTransferRepository
    private lateinit var toasts: FakeToastController

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repository = FakeBoardTransferRepository()
        toasts = FakeToastController()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(id: String = boardId) = ExportBoardViewModel(id, "Cardigan v1", repository, toasts)

    @Test
    fun successSharesTheExportedFileWithoutAToast() = runTest {
        val file = (repository.exportResult as Result.Success).data
        viewModel().events.test {
            val event = awaitItem()
            assertThat(event).isInstanceOf(ExportBoardEvent.Share::class)
            assertThat((event as ExportBoardEvent.Share).file).isSameInstanceAs(file)
            expectNoEvents()
        }
        assertThat(repository.exportCalls).containsExactly(Uuid.parse(boardId))
        assertThat(toasts.messages).isEmpty()
    }

    @Test
    fun notFoundShowsTheNoLongerExistsToastAndFinishes() = runTest {
        repository.exportResult = Result.Error(ExportError.BoardNotFound)
        viewModel().events.test {
            assertThat(awaitItem()).isEqualTo(ExportBoardEvent.Finished)
        }
        val toast = toasts.messages.single()
        assertThat(toast.type).isEqualTo(ToastType.Error)
        assertThat((toast.text as UiText.Resource).id).isEqualTo(Res.string.export_error_not_found)
        assertThat((toast.text as UiText.Resource).args.toList()).containsExactly("Cardigan v1")
    }

    @Test
    fun failedShowsTheGenericToastAndFinishes() = runTest {
        repository.exportResult = Result.Error(ExportError.Failed)
        viewModel().events.test {
            assertThat(awaitItem()).isEqualTo(ExportBoardEvent.Finished)
        }
        assertThat((toasts.messages.single().text as UiText.Resource).id).isEqualTo(Res.string.export_error_failed)
    }

    @Test
    fun malformedBoardIdFailsWithoutARequest() = runTest {
        viewModel(id = "not-a-uuid").events.test {
            assertThat(awaitItem()).isEqualTo(ExportBoardEvent.Finished)
        }
        assertThat(repository.exportCalls).isEmpty()
        assertThat((toasts.messages.single().text as UiText.Resource).id).isEqualTo(Res.string.export_error_failed)
    }
}
