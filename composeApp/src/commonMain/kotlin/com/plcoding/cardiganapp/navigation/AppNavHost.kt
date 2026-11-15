package com.plcoding.cardiganapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavDestination.Companion.hasRoute
import com.plcoding.cardiganapp.feature.transfer.presentation.navigation.ExportBoardRoute
import com.plcoding.cardiganapp.feature.transfer.presentation.navigation.ImportBoardRoute
import com.plcoding.cardiganapp.feature.transfer.presentation.navigation.transferGraph

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(navController = navController, startDestination = AppStartRoute, modifier = modifier) {
        composable<AppStartRoute> {}
        transferGraph(
            // TODO(boards): reload the board list once mob-board-list-ui exposes its reload hook.
            onImported = {},
            onBoardListChanged = {},
            onDone = { navController.popTransferRoute() },
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
