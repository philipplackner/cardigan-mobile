package com.plcoding.cardiganapp.session

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasContentDescription
import androidx.navigation.toRoute
import com.plcoding.cardiganapp.core.domain.label.LabelSource
import com.plcoding.cardiganapp.fakes.FakeBoardDetailRepository
import com.plcoding.cardiganapp.fakes.FakeCardCreateRepository
import com.plcoding.cardiganapp.fakes.FakeLabelSource
import com.plcoding.cardiganapp.feature.boards.domain.detail.BoardDetailRepository
import com.plcoding.cardiganapp.feature.boards.domain.detail.CardCreateRepository
import com.plcoding.cardiganapp.feature.boards.presentation.navigation.BoardRoute
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.plcoding.cardiganapp.App
import com.plcoding.cardiganapp.core.domain.board.Board
import com.plcoding.cardiganapp.core.domain.session.SessionManager
import com.plcoding.cardiganapp.core.domain.session.SessionState
import com.plcoding.cardiganapp.core.domain.session.SignOutReason
import com.plcoding.cardiganapp.core.domain.util.Result
import com.plcoding.cardiganapp.di.appModules
import com.plcoding.cardiganapp.fakes.FakeAuthRepository
import com.plcoding.cardiganapp.fakes.FakeExportFileChecker
import com.plcoding.cardiganapp.fakes.FakeRemoteBoardDataSource
import com.plcoding.cardiganapp.fakes.FakeSessionManager
import com.plcoding.cardiganapp.fakes.summary
import com.plcoding.cardiganapp.feature.auth.domain.AuthRepository
import com.plcoding.cardiganapp.feature.auth.presentation.navigation.LoginRoute
import com.plcoding.cardiganapp.feature.auth.presentation.navigation.RegisterRoute
import com.plcoding.cardiganapp.feature.boards.domain.BoardRepository
import com.plcoding.cardiganapp.feature.boards.domain.RemoteBoardDataSource
import com.plcoding.cardiganapp.feature.boards.presentation.navigation.BoardListRoute
import com.plcoding.cardiganapp.feature.transfer.domain.BoardTransferRepository
import com.plcoding.cardiganapp.feature.transfer.domain.ExportError
import com.plcoding.cardiganapp.feature.transfer.domain.ExportedFile
import com.plcoding.cardiganapp.feature.transfer.domain.ImportError
import com.plcoding.cardiganapp.feature.transfer.domain.ExportFileChecker
import com.plcoding.cardiganapp.feature.transfer.presentation.navigation.ExportBoardRoute
import com.plcoding.cardiganapp.navigation.navigateToExport
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.uuid.Uuid
import kotlinx.coroutines.awaitCancellation
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

/** Keeps every export running, so the export dialog stays open until something closes it. */
private class PendingExportRepository : BoardTransferRepository {
    override suspend fun exportBoard(boardId: Uuid): Result<ExportedFile, ExportError> = awaitCancellation()

    override suspend fun importBoard(file: ByteArray, key: String?): Result<Board, ImportError> = awaitCancellation()
}

/** The real [App] and session gate, with a fake session and fakes at the network boundaries. */
@OptIn(ExperimentalTestApi::class)
class SessionGateUiTest {

    private val cardigan = summary("0b5c8a6e-1f00-4a3c-9a43-6f5e0e3c1d2a", "Cardigan v1", "CRD")
    private val sessionExpiredText = "Your session has expired. Log in again to continue."
    private val isTextField = SemanticsMatcher.keyIsDefined(SemanticsProperties.EditableText)
    private lateinit var session: FakeSessionManager
    private lateinit var koin: Koin
    private lateinit var navController: NavHostController
    private lateinit var appViewModel: AppViewModel

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    private fun ComposeUiTest.showApp(restoreTo: SessionState?) {
        session = FakeSessionManager(restoreTo = restoreTo)
        koin = startKoin {
            modules(
                appModules() + module {
                    single<SessionManager> { session }
                    single<AuthRepository> { FakeAuthRepository(session) }
                    single<RemoteBoardDataSource> { FakeRemoteBoardDataSource(listOf(cardigan)) }
                    single<BoardTransferRepository> { PendingExportRepository() }
                    single<ExportFileChecker> { FakeExportFileChecker(null) }
                    single<BoardDetailRepository> { FakeBoardDetailRepository() }
                    single<CardCreateRepository> { FakeCardCreateRepository() }
                    single<LabelSource> { FakeLabelSource() }
                },
            )
        }.koin
        appViewModel = koin.get<AppViewModel>()
        setContent {
            navController = rememberNavController()
            App(appViewModel = appViewModel, navController = navController)
        }
        waitForIdle()
    }

    private fun ComposeUiTest.screens(): List<NavDestination> = runOnIdle {
        navController.currentBackStack.value.map { it.destination }.filter { it !is NavGraph }
    }

    private fun ComposeUiTest.awaitScreen(check: (NavDestination) -> Boolean) {
        waitUntil(timeoutMillis = 5_000) { screens().lastOrNull()?.let(check) == true }
    }

    private fun ComposeUiTest.awaitBoardList() = awaitScreen { it.hasRoute<BoardListRoute>() }

    private fun ComposeUiTest.awaitLogin() {
        awaitScreen { it.hasRoute<LoginRoute>() }
        onNodeWithText("Log in to Cardigan").assertExists()
    }

    private fun ComposeUiTest.assertOnlyScreen(check: (NavDestination) -> Boolean) {
        val screens = screens()
        assertThat(screens.size).isEqualTo(1)
        assertThat(check(screens.single())).isEqualTo(true)
    }

    @Test
    fun signedInStartsOnTheBoardList() = runComposeUiTest {
        showApp(SessionState.SignedIn("ann@example.com"))
        awaitBoardList()
        assertOnlyScreen { it.hasRoute<BoardListRoute>() }
    }

    @Test
    fun restoreThatCouldNotReachTheBackendStillStartsOnTheBoardList() = runComposeUiTest {
        // D18: the session layer keeps the stored session and reports it as signed in.
        showApp(SessionState.SignedIn("ann@example.com"))
        awaitBoardList()
        assertThat(onAllNodes(hasText("Log in to Cardigan")).fetchSemanticsNodes()).isEmpty()
    }

    @Test
    fun noSessionStartsOnThePlainLogin() = runComposeUiTest {
        showApp(SessionState.SignedOut(SignOutReason.NoSession))
        awaitLogin()
        assertThat(onAllNodes(hasText(sessionExpiredText)).fetchSemanticsNodes()).isEmpty()
    }

    @Test
    fun expiredStartsOnTheLoginWithTheBanner() = runComposeUiTest {
        showApp(SessionState.SignedOut(SignOutReason.Expired))
        awaitLogin()
        onNodeWithText(sessionExpiredText).assertExists()
    }

    @Test
    fun nothingIsDrawnWhileRestoring() = runComposeUiTest {
        showApp(restoreTo = null)
        assertThat(screens()).isEmpty()
        assertThat(runOnIdle { navController.currentBackStackEntry }).isNull()
        assertThat(onAllNodes(hasText("Log in to Cardigan")).fetchSemanticsNodes()).isEmpty()
        assertThat(onAllNodes(isTextField).fetchSemanticsNodes()).isEmpty()
    }

    @Test
    fun loginLandsOnTheBoardListWithNoAuthScreenLeft() = runComposeUiTest {
        showApp(SessionState.SignedOut(SignOutReason.NoSession))
        awaitLogin()
        onNodeWithText("Create one").performClick()
        awaitScreen { it.hasRoute<RegisterRoute>() }
        runOnIdle { navController.popBackStack() }
        awaitLogin()

        onAllNodes(isTextField)[0].performTextReplacement("Ann@Example.com")
        onAllNodes(isTextField)[1].performTextReplacement("secret")
        onNodeWithText("Log in").performClick()

        awaitBoardList()
        assertOnlyScreen { it.hasRoute<BoardListRoute>() }
        assertThat(session.state.value).isEqualTo(SessionState.SignedIn("ann@example.com"))
        waitUntil(timeoutMillis = 5_000) { koin.get<BoardRepository>().boards.value != null }

        runOnIdle { navController.popBackStack() }
        assertThat(screens()).isEmpty()
    }

    @Test
    fun signedInUserOpensABoardFromTheList() = runComposeUiTest {
        showApp(SessionState.SignedIn("ann@example.com"))
        awaitBoardList()
        waitUntil(timeoutMillis = 5_000) {
            onAllNodes(hasContentDescription("Cardigan v1,", substring = true)).fetchSemanticsNodes().isNotEmpty()
        }
        onAllNodes(hasContentDescription("Cardigan v1,", substring = true))[0].performClick()

        awaitScreen { it.hasRoute<BoardRoute>() }
        val route = runOnIdle { navController.currentBackStackEntry!!.toRoute<BoardRoute>() }
        assertThat(route).isEqualTo(BoardRoute(cardigan.id.toString(), "Cardigan v1", "CRD"))
        assertThat(screens().map { it.hasRoute<BoardListRoute>() }).containsExactly(true, false)
    }

    @Test
    fun logoutLandsOnThePlainLoginWithAClearedBackStack() = runComposeUiTest {
        showApp(SessionState.SignedIn("ann@example.com"))
        awaitBoardList()
        waitUntil(timeoutMillis = 5_000) { koin.get<BoardRepository>().boards.value != null }

        appViewModel.onLogout()

        awaitLogin()
        assertOnlyScreen { it.hasRoute<LoginRoute>() }
        assertThat(onAllNodes(hasText(sessionExpiredText)).fetchSemanticsNodes()).isEmpty()
        assertThat(koin.get<BoardRepository>().boards.value).isNull()
        assertThat(session.logoutCalls).isEqualTo(1)
    }

    @Test
    fun expiryWithADialogOpenLandsOnTheLoginWithTheBanner() = runComposeUiTest {
        showApp(SessionState.SignedIn("ann@example.com"))
        awaitBoardList()
        runOnIdle { navController.navigateToExport(Uuid.random().toString(), "Cardigan v1") }
        awaitScreen { it.hasRoute<ExportBoardRoute>() }

        runOnIdle { session.expire() }

        awaitLogin()
        onNodeWithText(sessionExpiredText).assertExists()
        assertThat(screens().map { it.hasRoute<ExportBoardRoute>() || it.hasRoute<BoardListRoute>() })
            .containsExactly(false)
    }
}
