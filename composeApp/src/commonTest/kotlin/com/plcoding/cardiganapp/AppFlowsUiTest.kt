package com.plcoding.cardiganapp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.toRoute
import com.plcoding.cardiganapp.core.domain.label.LabelSource
import com.plcoding.cardiganapp.fakes.FakeBoardDetailRepository
import com.plcoding.cardiganapp.fakes.FakeCardCreateRepository
import com.plcoding.cardiganapp.fakes.FakeLabelSource
import com.plcoding.cardiganapp.feature.boards.domain.detail.BoardDetailRepository
import com.plcoding.cardiganapp.feature.boards.domain.detail.CardCreateRepository
import com.plcoding.cardiganapp.feature.boards.presentation.navigation.navigateToBoard
import com.plcoding.cardiganapp.feature.transfer.presentation.navigation.ExportBoardRoute
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import com.plcoding.cardiganapp.core.designsystem.theme.WindowClass
import com.plcoding.cardiganapp.core.domain.board.Board
import com.plcoding.cardiganapp.core.domain.util.Result
import com.plcoding.cardiganapp.core.presentation.toast.ToastController
import com.plcoding.cardiganapp.core.presentation.toast.ToastType
import com.plcoding.cardiganapp.fakes.RecordingToastController
import com.plcoding.cardiganapp.di.appModules
import com.plcoding.cardiganapp.fakes.FakeBoardTransferRepository
import com.plcoding.cardiganapp.fakes.FakeExportFileChecker
import com.plcoding.cardiganapp.fakes.FakeFilePicker
import com.plcoding.cardiganapp.fakes.FakeRemoteBoardDataSource
import com.plcoding.cardiganapp.fakes.summary
import com.plcoding.cardiganapp.feature.boards.domain.RemoteBoardDataSource
import com.plcoding.cardiganapp.feature.boards.presentation.navigation.BoardListRoute
import com.plcoding.cardiganapp.feature.transfer.domain.BoardTransferRepository
import com.plcoding.cardiganapp.feature.transfer.domain.ExportFileChecker
import com.plcoding.cardiganapp.feature.transfer.domain.ImportError
import com.plcoding.cardiganapp.navigation.AppNavHost
import kotlin.test.AfterTest
import kotlin.test.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

/** The app's wiring of the board list and the import flow, with fakes only at the boundaries. */
@OptIn(ExperimentalTestApi::class)
class AppFlowsUiTest {

    private val cardigan = summary("0b5c8a6e-1f00-4a3c-9a43-6f5e0e3c1d2a", "Cardigan v1", "CRD")
    private val garden = summary("1c6d9b7f-2a11-4b4d-8b54-7a6f1f4d2e3b", "Garden irrigation", "GARD", updated = "2026-11-21T10:00:00Z")
    private val picker = FakeFilePicker()
    private val toasts = RecordingToastController()

    private fun start(remote: FakeRemoteBoardDataSource, transfer: FakeBoardTransferRepository, fileKey: String?) {
        startKoin {
            modules(
                appModules() + module {
                    single<RemoteBoardDataSource> { remote }
                    single<BoardTransferRepository> { transfer }
                    single<ExportFileChecker> { FakeExportFileChecker(fileKey) }
                    single<ToastController> { toasts }
                    single<BoardDetailRepository> { FakeBoardDetailRepository() }
                    single<CardCreateRepository> { FakeCardCreateRepository() }
                    single<LabelSource> { FakeLabelSource() }
                },
            )
        }
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    private var navController: NavHostController? = null

    private fun ComposeUiTest.showApp() {
        setContent {
            AppTheme(darkTheme = false, windowClass = WindowClass.Phone) {
                Box(Modifier.requiredSize(390.dp, 844.dp)) {
                    AppNavHost(
                        navController = rememberNavController().also { navController = it },
                        startDestination = BoardListRoute,
                        accountEmail = null,
                        onLogOut = {},
                        onOpenBoard = { _, _, _ -> },
                        onLabelsClick = {},
                        filePicker = { onPicked, _ -> picker.bind(onPicked) },
                    )
                }
            }
        }
    }

    private fun ComposeUiTest.awaitBoard(name: String) {
        waitUntil(timeoutMillis = 5_000) {
            onAllNodes(hasContentDescription("$name,", substring = true)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun ComposeUiTest.openImportFromTheMenu() {
        onNodeWithContentDescription("Menu").performClick()
        onNodeWithText("Import board").performClick()
        waitUntil { picker.openCount == 1 }
    }

    @Test
    fun successfulImportReloadsTheList() = runComposeUiTest {
        val remote = FakeRemoteBoardDataSource(listOf(cardigan), listOf(garden, cardigan))
        start(remote, FakeBoardTransferRepository(Result.Success(Board(garden.id, garden.name, garden.key))), fileKey = "GARD")
        showApp()
        awaitBoard("Cardigan v1")
        assertThat(remote.getBoardsCalls).isEqualTo(1)

        openImportFromTheMenu()
        picker.pick()

        awaitBoard("Garden irrigation")
        assertThat(remote.getBoardsCalls).isEqualTo(2)
    }

    @Test
    fun importWhoseReloadFailsShowsTheReloadToastOnce() = runComposeUiTest {
        val remote = FakeRemoteBoardDataSource(listOf(cardigan)).apply { failFromCall = 2 }
        start(remote, FakeBoardTransferRepository(Result.Success(Board(garden.id, garden.name, garden.key))), fileKey = "GARD")
        showApp()
        awaitBoard("Cardigan v1")

        openImportFromTheMenu()
        picker.pick()

        waitUntil(timeoutMillis = 5_000) { toasts.messages.any { it.type == ToastType.Error } }
        waitForIdle()
        val errors = toasts.messages.filter { it.type == ToastType.Error }
        assertThat(errors.size).isEqualTo(1)
        assertThat(errors.single().text.asStringAsync()).isEqualTo("Couldn't refresh your boards. Check your connection and try again.")
        assertThat(remote.getBoardsCalls).isEqualTo(2)
        awaitBoard("Cardigan v1")
    }

    @Test
    fun keyClashNamesTheLoadedBoard() = runComposeUiTest {
        val remote = FakeRemoteBoardDataSource(listOf(cardigan))
        start(remote, FakeBoardTransferRepository(Result.Error(ImportError.KeyAlreadyUsed)), fileKey = "CRD")
        showApp()
        awaitBoard("Cardigan v1")

        openImportFromTheMenu()
        picker.pick()

        val text = "Your board “Cardigan v1” already uses the key CRD. Enter another key for the imported board."
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun exportFromTheBoardMenuOpensTheExportForThatBoard() = runComposeUiTest {
        val remote = FakeRemoteBoardDataSource(listOf(cardigan))
        start(remote, FakeBoardTransferRepository(Result.Error(ImportError.Failed)), fileKey = null)
        showApp()
        awaitBoard("Cardigan v1")
        val exports = mutableListOf<ExportBoardRoute>()
        runOnIdle {
            val controller = navController!!
            // The export dialog may close again on its own, so record it as it opens.
            controller.addOnDestinationChangedListener { _, destination, _ ->
                if (destination.hasRoute<ExportBoardRoute>()) exports += controller.currentBackStackEntry!!.toRoute<ExportBoardRoute>()
            }
            controller.navigateToBoard(cardigan.id.toString(), cardigan.name, cardigan.key)
        }

        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasContentDescription("Board actions")).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithContentDescription("Board actions").performClick()
        onNodeWithText("Export as JSON").performClick()

        waitUntil(timeoutMillis = 5_000) { exports.isNotEmpty() }
        assertThat(exports).containsExactly(ExportBoardRoute(cardigan.id.toString(), "Cardigan v1"))
    }
}
