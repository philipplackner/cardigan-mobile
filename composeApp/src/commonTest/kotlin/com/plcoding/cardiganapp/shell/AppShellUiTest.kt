package com.plcoding.cardiganapp.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import assertk.assertThat
import assertk.assertions.isEqualTo
import cardiganapp.composeapp.generated.resources.Res
import cardiganapp.composeapp.generated.resources.shell_all_boards
import cardiganapp.composeapp.generated.resources.shell_labels
import cardiganapp.composeapp.generated.resources.shell_log_out
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import com.plcoding.cardiganapp.core.designsystem.theme.WindowClass
import com.plcoding.cardiganapp.core.presentation.toast.ToastController
import com.plcoding.cardiganapp.core.presentation.toast.ToastMessage
import com.plcoding.cardiganapp.fakes.FakeRemoteBoardDataSource
import com.plcoding.cardiganapp.fakes.summary
import com.plcoding.cardiganapp.feature.boards.data.di.boardsDataModule
import com.plcoding.cardiganapp.feature.boards.domain.RemoteBoardDataSource
import com.plcoding.cardiganapp.feature.boards.presentation.di.boardsPresentationModule
import com.plcoding.cardiganapp.feature.boards.presentation.navigation.BoardListRoute
import org.jetbrains.compose.resources.getString
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class AppShellUiTest {
    private val remote =
        FakeRemoteBoardDataSource(listOf(summary("0b5c8a6e-1f00-4a3c-9a43-6f5e0e3c1d2a", "Cardigan v1", "CRD")))
    private var logOuts = 0
    private var labelClicks = 0

    @BeforeTest
    fun setUp() {
        startKoin {
            modules(
                boardsDataModule,
                boardsPresentationModule,
                module {
                    single<RemoteBoardDataSource> { remote }
                    single<ToastController> {
                        object : ToastController {
                            override fun show(message: ToastMessage) = Unit
                        }
                    }
                },
            )
        }
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    private fun ComposeUiTest.show(
        windowClass: WindowClass,
        email: String?,
    ) {
        setContent {
            AppTheme(darkTheme = false, windowClass = windowClass) {
                Box(Modifier.requiredSize(834.dp, 1112.dp)) {
                    val navController = rememberNavController()
                    AppShell(
                        navController = navController,
                        accountEmail = email,
                        onLogOut = { logOuts++ },
                        onOpenBoard = { _, _, _ -> },
                        onLabelsClick = { labelClicks++ },
                    ) { modifier ->
                        NavHost(navController, startDestination = BoardListRoute, modifier = modifier) {
                            composable<BoardListRoute> { Text("Content") }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun phoneShowsOnlyTheContent() =
        runComposeUiTest {
            show(WindowClass.Phone, "ada@example.com")
            onNodeWithText("Content").assertExists()
            onAllNodes(hasText(getString(Res.string.shell_all_boards))).assertCountEquals(0)
            onAllNodes(hasText("ada@example.com")).assertCountEquals(0)
        }

    @Test
    fun tabletShowsTheSidebarWithTheAccount() =
        runComposeUiTest {
            show(WindowClass.Tablet, "ada@example.com")
            onNodeWithText("Content").assertExists()
            onNodeWithText(getString(Res.string.shell_all_boards)).assertExists()
            onNodeWithText("ada@example.com").assertExists()
            onNodeWithText("A").assertExists()
            waitUntil { onAllNodes(hasText("Cardigan v1")).fetchSemanticsNodes().isNotEmpty() }

            onNodeWithText(getString(Res.string.shell_labels)).performClick()
            onNodeWithContentDescription(getString(Res.string.shell_log_out)).performClick()
            assertThat(labelClicks).isEqualTo(1)
            assertThat(logOuts).isEqualTo(1)
        }

    @Test
    fun desktopWithoutEmailLeavesTheAccountOutButKeepsLogOut() =
        runComposeUiTest {
            show(WindowClass.Desktop, null)
            onNodeWithText(getString(Res.string.shell_all_boards)).assertExists()
            onAllNodes(hasText("@", substring = true)).assertCountEquals(0)
            onNodeWithContentDescription(getString(Res.string.shell_log_out)).performClick()
            assertThat(logOuts).isEqualTo(1)
        }
}
