package com.plcoding.cardiganapp.di

import androidx.lifecycle.SavedStateHandle
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import com.plcoding.cardiganapp.feature.boards.data.InMemoryBoardRepository
import com.plcoding.cardiganapp.feature.boards.data.fixture.FixtureBoardDataSource
import com.plcoding.cardiganapp.feature.boards.data.fixture.FixtureBoardDetailRepository
import com.plcoding.cardiganapp.feature.boards.data.fixture.FixtureCardCreateRepository
import com.plcoding.cardiganapp.feature.boards.data.fixture.FixtureLabelSource
import com.plcoding.cardiganapp.feature.boards.domain.BoardRepository
import com.plcoding.cardiganapp.feature.boards.domain.RemoteBoardDataSource
import com.plcoding.cardiganapp.feature.boards.presentation.boardlist.BoardListViewModel
import com.plcoding.cardiganapp.feature.boards.presentation.deleteboard.DeleteBoardViewModel
import com.plcoding.cardiganapp.feature.boards.presentation.detail.BoardViewModel
import com.plcoding.cardiganapp.core.domain.label.LabelSource
import com.plcoding.cardiganapp.feature.boards.domain.detail.BoardDetailRepository
import com.plcoding.cardiganapp.feature.boards.domain.detail.CardCreateRepository
import com.plcoding.cardiganapp.fakes.FakeBoardDetailRepository
import com.plcoding.cardiganapp.fakes.FakeCardCreateRepository
import com.plcoding.cardiganapp.fakes.FakeLabelSource
import com.plcoding.cardiganapp.feature.boards.presentation.navigation.BoardListReloader
import com.plcoding.cardiganapp.feature.boards.presentation.newboard.NewBoardViewModel
import com.plcoding.cardiganapp.feature.boards.presentation.renameboard.RenameBoardViewModel
import com.plcoding.cardiganapp.feature.boards.presentation.sidebar.BoardsSidebarViewModel
import com.plcoding.cardiganapp.feature.transfer.domain.BoardKeyOwnerLookup
import kotlin.time.Duration
import kotlinx.coroutines.test.runTest
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
import org.koin.core.KoinApplication
import org.koin.core.parameter.parametersOf
import org.koin.dsl.koinApplication
import org.koin.dsl.module

/** Resolves the board and transfer definitions of [appModules]. */
@OptIn(ExperimentalCoroutinesApi::class)
class AppModulesTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    // Module definitions cache their singles until the app closes, so every test closes its own.
    private val apps = mutableListOf<KoinApplication>()

    private fun koin(): Koin = koinApplication { modules(appModules() + testRepository) }.also { apps += it }.koin

    @AfterTest
    fun tearDown() {
        apps.forEach { it.close() }
        Dispatchers.resetMain()
    }

    private val testRepository = module {
        single<BoardDetailRepository> { FakeBoardDetailRepository() }
        single<CardCreateRepository> { FakeCardCreateRepository() }
        single<LabelSource> { FakeLabelSource() }
        single<RemoteBoardDataSource> { FixtureBoardDataSource(latency = Duration.ZERO) }
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
    fun boardDefinitionsResolve() {
        val koin = koin()
        get(koin)
    }

    private fun get(koin: Koin) = with(koin) {
        get<BoardRepository>()
        get<InMemoryBoardRepository>()
        get<BoardListReloader>()
        get<BoardListViewModel>()
        get<BoardsSidebarViewModel>()
        get<NewBoardViewModel> { parametersOf(SavedStateHandle()) }
        get<RenameBoardViewModel> { parametersOf(Uuid.random().toString(), SavedStateHandle()) }
        get<DeleteBoardViewModel> { parametersOf(Uuid.random().toString()) }
        get<BoardViewModel> { parametersOf(SavedStateHandle(mapOf(BoardViewModel.ARG_BOARD_ID to Uuid.random().toString()))) }
    }

    @Test
    fun keyOwnerLookupReadsTheLoadedBoards() = runTest {
        val koin = koin()
        val lookup = koin.get<BoardKeyOwnerLookup>()
        assertThat(lookup.boardNameForKey("CRD")).isNull()

        koin.get<BoardRepository>().refresh()

        assertThat(lookup.boardNameForKey("CRD")).isEqualTo("Cardigan v1")
        assertThat(lookup.boardNameForKey("NOPE")).isNull()
    }

    @Test
    fun transferDefinitionsResolveWithTheRepositoryFromATestModule() {
        // No module binds BoardTransferRepository until mob-import-export-connect adds the Ktor one.
        val koin = koin()
        koin.resolveTransfer()
    }

    @Test
    fun boardViewFixtureIsBoundAndLoadsTheSampleBoard() = runTest {
        // TEMPORARY with the board view fixture (D6): without the test overrides, appModules binds it.
        val koin = koinApplication {
            modules(appModules() + module { single<RemoteBoardDataSource> { FixtureBoardDataSource(latency = Duration.ZERO) } })
        }.also { apps += it }.koin
        // No BoardViewModel here: its first load would outlive the test on the fixture's delay.
        assertThat(koin.get<BoardDetailRepository>()).isInstanceOf<FixtureBoardDetailRepository>()
        assertThat(koin.get<CardCreateRepository>()).isInstanceOf<FixtureCardCreateRepository>()
        assertThat(koin.get<LabelSource>()).isInstanceOf<FixtureLabelSource>()
    }
}
