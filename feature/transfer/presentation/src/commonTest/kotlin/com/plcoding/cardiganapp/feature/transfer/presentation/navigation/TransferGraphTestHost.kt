package com.plcoding.cardiganapp.feature.transfer.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import com.plcoding.cardiganapp.core.presentation.toast.ToastController
import com.plcoding.cardiganapp.feature.transfer.domain.BoardKeyOwnerLookup
import com.plcoding.cardiganapp.feature.transfer.domain.BoardTransferRepository
import com.plcoding.cardiganapp.feature.transfer.domain.ExportFileChecker
import com.plcoding.cardiganapp.feature.transfer.presentation.di.transferPresentationModule
import com.plcoding.cardiganapp.feature.transfer.presentation.fakes.FakeBoardKeyOwnerLookup
import com.plcoding.cardiganapp.feature.transfer.presentation.fakes.FakeBoardTransferRepository
import com.plcoding.cardiganapp.feature.transfer.presentation.fakes.FakeExportFileChecker
import com.plcoding.cardiganapp.feature.transfer.presentation.fakes.FakeFilePicker
import com.plcoding.cardiganapp.feature.transfer.presentation.fakes.FakeToastController
import kotlinx.serialization.Serializable
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

@Serializable
private data object TestHostRoute

/** Hosts the real [transferGraph] in a NavHost with fakes at the boundaries. */
class TransferGraphTestHost {
    val repository = FakeBoardTransferRepository()
    val checker = FakeExportFileChecker()
    val picker = FakeFilePicker()
    var doneCount = 0
        private set

    fun start() {
        startKoin {
            modules(
                transferPresentationModule,
                module {
                    single<BoardTransferRepository> { repository }
                    single<ExportFileChecker> { checker }
                    single<BoardKeyOwnerLookup> { FakeBoardKeyOwnerLookup() }
                    single<ToastController> { FakeToastController() }
                },
            )
        }
    }

    fun stop() {
        stopKoin()
    }

    @OptIn(ExperimentalTestApi::class)
    fun openImport(test: ComposeUiTest) =
        with(test) {
            setContent {
                AppTheme(darkTheme = false) {
                    val navController = rememberNavController()
                    NavHost(navController, startDestination = TestHostRoute) {
                        composable<TestHostRoute> { Box {} }
                        transferGraph(
                            onImported = {},
                            onBoardListChanged = {},
                            onDone = {
                                doneCount++
                                navController.popBackStack()
                            },
                            filePicker = { onPicked, onCancelled -> picker.bind(onPicked, onCancelled) },
                        )
                    }
                    LaunchedEffect(Unit) { navController.navigate(ImportBoardRoute) }
                }
            }
            waitUntil { picker.openCount == 1 }
        }

    @OptIn(ExperimentalTestApi::class)
    fun awaitText(
        test: ComposeUiTest,
        text: String,
    ) = with(test) {
        waitUntil { onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty() }
    }
}
