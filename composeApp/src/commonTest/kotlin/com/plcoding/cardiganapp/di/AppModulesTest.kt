package com.plcoding.cardiganapp.di

import androidx.lifecycle.SavedStateHandle
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import assertk.assertions.isSameInstanceAs
import com.plcoding.cardiganapp.core.domain.board.Board
import com.plcoding.cardiganapp.core.domain.label.LabelSource
import com.plcoding.cardiganapp.core.domain.session.SessionManager
import com.plcoding.cardiganapp.core.domain.session.SessionScopedStore
import com.plcoding.cardiganapp.core.domain.util.DataError
import com.plcoding.cardiganapp.core.domain.util.Result
import com.plcoding.cardiganapp.core.presentation.toast.ToastController
import com.plcoding.cardiganapp.fakes.FakeAuthRepository
import com.plcoding.cardiganapp.fakes.FakeBoardDetailRepository
import com.plcoding.cardiganapp.fakes.FakeCardCreateRepository
import com.plcoding.cardiganapp.fakes.FakeLabelSource
import com.plcoding.cardiganapp.fakes.FakeRemoteBoardDataSource
import com.plcoding.cardiganapp.fakes.FakeSessionManager
import com.plcoding.cardiganapp.fakes.summary
import com.plcoding.cardiganapp.feature.auth.data.fixture.FixtureAuthRepository
import com.plcoding.cardiganapp.feature.auth.data.fixture.InMemorySessionManager
import com.plcoding.cardiganapp.feature.auth.domain.AuthRepository
import com.plcoding.cardiganapp.feature.auth.presentation.login.LoginViewModel
import com.plcoding.cardiganapp.feature.auth.presentation.register.RegisterViewModel
import com.plcoding.cardiganapp.feature.boards.data.InMemoryBoardRepository
import com.plcoding.cardiganapp.feature.boards.domain.BoardRepository
import com.plcoding.cardiganapp.feature.boards.domain.RemoteBoardDataSource
import com.plcoding.cardiganapp.feature.boards.domain.detail.BoardDetailRepository
import com.plcoding.cardiganapp.feature.boards.domain.detail.BoardLoadError
import com.plcoding.cardiganapp.feature.boards.domain.detail.CardCreateError
import com.plcoding.cardiganapp.feature.boards.domain.detail.CardCreateRepository
import com.plcoding.cardiganapp.feature.boards.presentation.boardlist.BoardListViewModel
import com.plcoding.cardiganapp.feature.boards.presentation.deleteboard.DeleteBoardViewModel
import com.plcoding.cardiganapp.feature.boards.presentation.detail.BoardViewModel
import com.plcoding.cardiganapp.feature.boards.presentation.navigation.BoardListReloader
import com.plcoding.cardiganapp.feature.boards.presentation.newboard.NewBoardViewModel
import com.plcoding.cardiganapp.feature.boards.presentation.renameboard.RenameBoardViewModel
import com.plcoding.cardiganapp.feature.boards.presentation.sidebar.BoardsSidebarViewModel
import com.plcoding.cardiganapp.feature.transfer.domain.BoardKeyOwnerLookup
import com.plcoding.cardiganapp.feature.transfer.domain.BoardTransferRepository
import com.plcoding.cardiganapp.feature.transfer.domain.ExportError
import com.plcoding.cardiganapp.feature.transfer.domain.ExportFileChecker
import com.plcoding.cardiganapp.feature.transfer.domain.ExportedFile
import com.plcoding.cardiganapp.feature.transfer.domain.ImportError
import com.plcoding.cardiganapp.feature.transfer.presentation.exportboard.ExportBoardViewModel
import com.plcoding.cardiganapp.feature.transfer.presentation.importboard.ImportBoardViewModel
import com.plcoding.cardiganapp.session.AppViewModel
import com.plcoding.cardiganapp.toast.AppToastController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.koin.core.Koin
import org.koin.core.KoinApplication
import org.koin.core.parameter.parametersOf
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.uuid.Uuid

/** Resolves the auth, session, board and transfer definitions of [appModules]. */
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

    private val testRepository =
        module {
            // No module binds the session or the auth repository until mob-auth-connect adds the real ones.
            single<SessionManager> { FakeSessionManager() }
            single<AuthRepository> { FakeAuthRepository(get<SessionManager>() as FakeSessionManager) }
            single<BoardDetailRepository> { FakeBoardDetailRepository() }
            single<CardCreateRepository> { FakeCardCreateRepository() }
            single<LabelSource> { FakeLabelSource() }
            single<RemoteBoardDataSource> {
                FakeRemoteBoardDataSource(listOf(summary("0b5c8a6e-1f00-4a3c-9a43-6f5e0e3c1d2a", "Cardigan v1", "CRD")))
            }
            single<BoardTransferRepository> {
                object : BoardTransferRepository {
                    override suspend fun exportBoard(boardId: Uuid): Result<ExportedFile, ExportError> =
                        Result.Error(ExportError.Failed)

                    override suspend fun importBoard(
                        file: ByteArray,
                        key: String?,
                    ): Result<Board, ImportError> = Result.Error(ImportError.Failed)
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

    private fun get(koin: Koin) =
        with(koin) {
            get<BoardRepository>()
            get<InMemoryBoardRepository>()
            get<BoardListReloader>()
            get<BoardListViewModel>()
            get<BoardsSidebarViewModel>()
            get<NewBoardViewModel> { parametersOf(SavedStateHandle()) }
            get<RenameBoardViewModel> { parametersOf(Uuid.random().toString(), SavedStateHandle()) }
            get<DeleteBoardViewModel> { parametersOf(Uuid.random().toString()) }
            get<BoardViewModel> {
                parametersOf(
                    SavedStateHandle(
                        mapOf(BoardViewModel.ARG_BOARD_ID to Uuid.random().toString()),
                    ),
                )
            }
        }

    @Test
    fun authAndSessionDefinitionsResolveWithTheSessionFromATestModule() {
        val koin = koin()
        koin.get<AppViewModel>()
        koin.get<LoginViewModel> { parametersOf(true, SavedStateHandle()) }
        koin.get<RegisterViewModel> { parametersOf(SavedStateHandle()) }
        assertThat(koin.getAll<SessionScopedStore>()).containsExactly(koin.get<InMemoryBoardRepository>())
    }

    @Test
    fun debugBuildBindsTheTemporaryAuthFixture() {
        val koin = koinApplication { modules(appModules(isDebug = true)) }.also { apps += it }.koin
        assertThat(koin.get<AppBuildInfo>().isDebug).isEqualTo(true)
        assertThat(koin.get<SessionManager>()).isSameInstanceAs(koin.get<InMemorySessionManager>())
        assertThat(koin.get<AuthRepository>()).isInstanceOf<FixtureAuthRepository>()
        koin.get<AppViewModel>()
        koin.get<LoginViewModel> { parametersOf(false, SavedStateHandle()) }
        koin.get<RegisterViewModel> { parametersOf(SavedStateHandle()) }
    }

    @Test
    fun releaseBuildHasNoAuthFixture() {
        val koin = koinApplication { modules(appModules(isDebug = false)) }.also { apps += it }.koin
        assertThat(koin.get<AppBuildInfo>().isDebug).isEqualTo(false)
        assertThat(koin.getOrNull<SessionManager>()).isNull()
        assertThat(koin.getOrNull<AuthRepository>()).isNull()
        assertThat(koin.getOrNull<InMemorySessionManager>()).isNull()
    }

    @Test
    fun keyOwnerLookupReadsTheLoadedBoards() =
        runTest {
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
    fun boardSourcesFailUntilTheConnectChanges() =
        runTest {
            // Without the test overrides, appModules binds sources that fail until the connect changes.
            val koin = koinApplication { modules(appModules()) }.also { apps += it }.koin
            assertThat(koin.get<BoardRepository>().refresh()).isEqualTo(Result.Error(DataError.Network.UNKNOWN))
            get(koin)
            assertThat(
                koin.get<BoardDetailRepository>().loadBoard(Uuid.random()),
            ).isEqualTo(Result.Error(BoardLoadError.Failed))
            assertThat(koin.get<CardCreateRepository>().createCard(Uuid.random(), Uuid.random(), "Card"))
                .isEqualTo(Result.Error(CardCreateError.Failed))
            assertThat(koin.get<LabelSource>().labels()).isInstanceOf<Result.Error<*>>()
        }
}
