package com.plcoding.cardiganapp.feature.transfer.presentation.di

import androidx.lifecycle.SavedStateHandle
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.plcoding.cardiganapp.core.presentation.toast.ToastController
import com.plcoding.cardiganapp.feature.transfer.domain.BoardKeyOwnerLookup
import com.plcoding.cardiganapp.feature.transfer.domain.BoardTransferRepository
import com.plcoding.cardiganapp.feature.transfer.domain.ExportFileChecker
import com.plcoding.cardiganapp.feature.transfer.presentation.exportboard.ExportBoardViewModel
import com.plcoding.cardiganapp.feature.transfer.presentation.fakes.FakeBoardKeyOwnerLookup
import com.plcoding.cardiganapp.feature.transfer.presentation.fakes.FakeBoardTransferRepository
import com.plcoding.cardiganapp.feature.transfer.presentation.fakes.FakeExportFileChecker
import com.plcoding.cardiganapp.feature.transfer.presentation.fakes.FakeToastController
import com.plcoding.cardiganapp.feature.transfer.presentation.importboard.ImportBoardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.koin.core.parameter.parametersOf
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

/** Resolves every definition of [transferPresentationModule], with the app-provided ones from a test module. */
@OptIn(ExperimentalCoroutinesApi::class)
class TransferPresentationModuleTest {
    private val appDependencies =
        module {
            single<BoardTransferRepository> { FakeBoardTransferRepository() }
            single<ExportFileChecker> { FakeExportFileChecker() }
            single<BoardKeyOwnerLookup> { FakeBoardKeyOwnerLookup() }
            single<ToastController> { FakeToastController() }
        }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun everyDefinitionResolves() {
        val koin = koinApplication { modules(transferPresentationModule, appDependencies) }.koin

        koin.get<ImportBoardViewModel> { parametersOf(SavedStateHandle()) }
        val export =
            koin.get<ExportBoardViewModel> {
                parametersOf(
                    "8f14e45f-ceea-4e7a-9e2f-5b1d6c3a7b21",
                    "Cardigan v1",
                )
            }
        assertThat(export.boardName).isEqualTo("Cardigan v1")
    }
}
