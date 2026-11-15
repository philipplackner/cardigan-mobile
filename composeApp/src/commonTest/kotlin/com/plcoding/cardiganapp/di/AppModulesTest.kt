package com.plcoding.cardiganapp.di

import androidx.lifecycle.SavedStateHandle
import com.plcoding.cardiganapp.core.domain.board.Board
import com.plcoding.cardiganapp.core.domain.util.Result
import com.plcoding.cardiganapp.core.presentation.toast.ToastController
import com.plcoding.cardiganapp.feature.transfer.domain.BoardTransferRepository
import com.plcoding.cardiganapp.feature.transfer.domain.ExportError
import com.plcoding.cardiganapp.feature.transfer.domain.ExportFileChecker
import com.plcoding.cardiganapp.feature.transfer.domain.ExportedFile
import com.plcoding.cardiganapp.feature.transfer.domain.ImportError
import com.plcoding.cardiganapp.feature.transfer.presentation.exportboard.ExportBoardViewModel
import com.plcoding.cardiganapp.feature.transfer.presentation.importboard.ImportBoardViewModel
import com.plcoding.cardiganapp.toast.AppToastController
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.uuid.Uuid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.koin.core.Koin
import org.koin.core.parameter.parametersOf
import org.koin.dsl.koinApplication
import org.koin.dsl.module

/** Resolves the transfer definitions of [appModules]. */
@OptIn(ExperimentalCoroutinesApi::class)
class AppModulesTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val testRepository = module {
        single<BoardTransferRepository> {
            object : BoardTransferRepository {
                override suspend fun exportBoard(boardId: Uuid): Result<ExportedFile, ExportError> = Result.Error(ExportError.Failed)
                override suspend fun importBoard(file: ByteArray, key: String?): Result<Board, ImportError> = Result.Error(ImportError.Failed)
            }
        }
    }

    private fun Koin.resolveTransfer() {
        get<ToastController>()
        get<AppToastController>()
        get<ExportFileChecker>()
        get<ImportBoardViewModel> { parametersOf(SavedStateHandle()) }
        get<ExportBoardViewModel> { parametersOf(Uuid.random().toString(), "Board") }
    }

    @Test
    fun transferDefinitionsResolveWithTheRepositoryFromATestModule() {
        // No module binds BoardTransferRepository until mob-import-export-connect adds the Ktor one.
        val koin = koinApplication { modules(appModules() + testRepository) }.koin
        koin.resolveTransfer()
    }
}
