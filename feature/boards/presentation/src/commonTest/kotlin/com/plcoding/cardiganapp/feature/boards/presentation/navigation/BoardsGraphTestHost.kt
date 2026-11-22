package com.plcoding.cardiganapp.feature.boards.presentation.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import com.plcoding.cardiganapp.core.designsystem.theme.WindowClass
import com.plcoding.cardiganapp.core.presentation.toast.ToastController
import com.plcoding.cardiganapp.feature.boards.domain.BoardRepository
import com.plcoding.cardiganapp.feature.boards.presentation.di.boardsPresentationModule
import com.plcoding.cardiganapp.feature.boards.presentation.fakes.FakeBoardRepository
import com.plcoding.cardiganapp.feature.boards.presentation.fakes.FakeToastController
import com.plcoding.cardiganapp.feature.boards.presentation.fakes.board
import kotlinx.serialization.Serializable
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

@Serializable
data object TestCallerRoute

/**
 * Hosts the real [boardsGraph] in a NavHost with fakes at the boundaries. [TestCallerRoute] stands
 * in for another screen (like the board view) that opens the dialogs and receives their results.
 */
class BoardsGraphTestHost {
    val cardigan = board(id = "0b5c8a6e-1f00-4a3c-9a43-6f5e0e3c1d2a", name = "Cardigan v1", key = "CRD", cardCount = 12)
    val raytracer = board(id = "1c6d9b7f-2a11-4b4d-8b54-7a6f1f4d2e3b", name = "Raytracer", key = "RAY", cardCount = 1)
    val repository = FakeBoardRepository(listOf(cardigan, raytracer))
    val toasts = FakeToastController()
    val renamed = mutableListOf<String>()
    var deletedCount = 0
        private set
    val opened = mutableListOf<Triple<String, String, String>>()
    lateinit var navController: NavHostController
        private set

    fun start() {
        startKoin {
            modules(
                boardsPresentationModule,
                module {
                    single<BoardRepository> { repository }
                    single<ToastController> { toasts }
                },
            )
        }
    }

    fun stop() {
        stopKoin()
    }

    @OptIn(ExperimentalTestApi::class)
    fun show(test: ComposeUiTest, startAtList: Boolean, windowClass: WindowClass = WindowClass.Phone) = with(test) {
        setContent {
            AppTheme(darkTheme = false, windowClass = windowClass) {
                val controller = rememberNavController()
                LaunchedEffect(controller) { navController = controller }
                NavHost(controller, startDestination = if (startAtList) BoardListRoute else TestCallerRoute) {
                    composable<TestCallerRoute> { entry ->
                        ObserveBoardDialogResults(
                            entry = entry,
                            onRenamed = { renamed += it },
                            onDeleted = { deletedCount++ },
                        )
                        Column { Text("Caller") }
                    }
                    boardsGraph(
                        navController = controller,
                        accountEmail = { "ada@example.com" },
                        onOpenBoard = { id, name, key -> opened += Triple(id, name, key) },
                        onImportClick = {},
                        onLabelsClick = {},
                        onLogOutClick = {},
                    )
                }
            }
        }
        waitUntil { this@BoardsGraphTestHost::navController.isInitialized }
    }

    @OptIn(ExperimentalTestApi::class)
    fun awaitText(test: ComposeUiTest, text: String) = with(test) {
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty() }
    }

    /** Rows and tiles read as one accessible name, so their texts aren't separate nodes. */
    @OptIn(ExperimentalTestApi::class)
    fun awaitBoard(test: ComposeUiTest, name: String) = with(test) {
        waitUntil(timeoutMillis = 5_000) {
            onAllNodes(hasContentDescription("$name,", substring = true)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @OptIn(ExperimentalTestApi::class)
    fun awaitGone(test: ComposeUiTest, text: String) = with(test) {
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText(text)).fetchSemanticsNodes().isEmpty() }
    }
}
