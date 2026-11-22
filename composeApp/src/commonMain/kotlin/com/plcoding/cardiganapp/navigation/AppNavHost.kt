package com.plcoding.cardiganapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.plcoding.cardiganapp.feature.boards.presentation.navigation.BoardListReloader
import com.plcoding.cardiganapp.feature.boards.presentation.navigation.BoardListRoute
import com.plcoding.cardiganapp.feature.boards.presentation.navigation.boardsGraph
import com.plcoding.cardiganapp.feature.transfer.presentation.navigation.ExportBoardRoute
import com.plcoding.cardiganapp.feature.transfer.presentation.navigation.ImportBoardRoute
import com.plcoding.cardiganapp.feature.transfer.presentation.navigation.transferGraph
import com.plcoding.cardiganapp.feature.transfer.presentation.platform.FilePicker
import com.plcoding.cardiganapp.feature.transfer.presentation.platform.PickedFile
import com.plcoding.cardiganapp.feature.transfer.presentation.platform.rememberFilePicker
import org.koin.compose.koinInject

/**
 * The app's navigation. [accountEmail] and [onLogOut] come from the session; [onOpenBoard] and
 * [onLabelsClick] lead to screens other features own. [filePicker] is replaced in tests.
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    accountEmail: String?,
    onLogOut: () -> Unit,
    onOpenBoard: (boardId: String, name: String, key: String) -> Unit,
    onLabelsClick: () -> Unit,
    modifier: Modifier = Modifier,
    reloader: BoardListReloader = koinInject(),
    filePicker: @Composable (onPicked: (PickedFile) -> Unit, onCancelled: () -> Unit) -> FilePicker = { onPicked, onCancelled ->
        rememberFilePicker(onPicked, onCancelled)
    },
) {
    val currentEmail by rememberUpdatedState(accountEmail)
    NavHost(navController = navController, startDestination = BoardListRoute, modifier = modifier) {
        boardsGraph(
            navController = navController,
            accountEmail = { currentEmail },
            onOpenBoard = onOpenBoard,
            onImportClick = navController::navigateToImport,
            onLabelsClick = onLabelsClick,
            onLogOutClick = onLogOut,
        )
        transferGraph(
            onImported = { reloader.reload() },
            onBoardListChanged = reloader::reload,
            onDone = { navController.popTransferRoute() },
            filePicker = filePicker,
        )
    }
}

/** Pops the import or export dialog, and nothing else, so a late second call can't pop a screen. */
private fun NavHostController.popTransferRoute() {
    val destination = currentBackStackEntry?.destination ?: return
    if (destination.hasRoute<ImportBoardRoute>() || destination.hasRoute<ExportBoardRoute>()) popBackStack()
}

/** Opens the import flow. A second tap while it is open doesn't start another one. */
fun NavHostController.navigateToImport() {
    navigate(ImportBoardRoute) { launchSingleTop = true }
}

/** Opens the export flow for one board. A second tap while it is open doesn't start another one. */
fun NavHostController.navigateToExport(boardId: String, boardName: String) {
    navigate(ExportBoardRoute(boardId, boardName)) { launchSingleTop = true }
}
