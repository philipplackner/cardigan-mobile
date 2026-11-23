package com.plcoding.cardiganapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.plcoding.cardiganapp.feature.auth.presentation.navigation.authGraph
import com.plcoding.cardiganapp.feature.boards.presentation.navigation.BoardListReloader
import com.plcoding.cardiganapp.feature.boards.presentation.navigation.BoardListRoute
import com.plcoding.cardiganapp.feature.boards.presentation.navigation.boardViewDestination
import com.plcoding.cardiganapp.feature.boards.presentation.navigation.boardsGraph
import com.plcoding.cardiganapp.feature.boards.presentation.util.boardsReloadFailedToast
import com.plcoding.cardiganapp.core.domain.util.onFailure
import com.plcoding.cardiganapp.core.presentation.toast.ToastController
import com.plcoding.cardiganapp.feature.transfer.presentation.navigation.ExportBoardRoute
import com.plcoding.cardiganapp.feature.transfer.presentation.navigation.ImportBoardRoute
import com.plcoding.cardiganapp.feature.transfer.presentation.navigation.transferGraph
import com.plcoding.cardiganapp.feature.transfer.presentation.platform.FilePicker
import com.plcoding.cardiganapp.feature.transfer.presentation.platform.PickedFile
import com.plcoding.cardiganapp.feature.transfer.presentation.platform.rememberFilePicker
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * The app's navigation. [startDestination] comes from the session gate. [accountEmail] and
 * [onLogOut] come from the session; [onOpenBoard] and [onLabelsClick] lead to screens other
 * features own. [filePicker] is replaced in tests.
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: Any,
    accountEmail: String?,
    onLogOut: () -> Unit,
    onOpenBoard: (boardId: String, name: String, key: String) -> Unit,
    onLabelsClick: () -> Unit,
    modifier: Modifier = Modifier,
    reloader: BoardListReloader = koinInject(),
    toastController: ToastController = koinInject(),
    filePicker: @Composable (onPicked: (PickedFile) -> Unit, onCancelled: () -> Unit) -> FilePicker = { onPicked, onCancelled ->
        rememberFilePicker(onPicked, onCancelled)
    },
) {
    val currentEmail by rememberUpdatedState(accountEmail)
    // Lives as long as the app UI; the reload itself runs in the reloader's app scope either way.
    val scope = rememberCoroutineScope()
    val reloadBoards = {
        scope.launch { reloader.reload().onFailure { toastController.show(boardsReloadFailedToast()) } }
        Unit
    }
    NavHost(navController = navController, startDestination = startDestination, modifier = modifier) {
        authGraph(navController)
        boardsGraph(
            navController = navController,
            accountEmail = { currentEmail },
            onOpenBoard = onOpenBoard,
            onImportClick = navController::navigateToImport,
            onLabelsClick = onLabelsClick,
            onLogOutClick = onLogOut,
        )
        boardViewDestination(
            navController = navController,
            onBack = { navController.popBackStack() },
            // TODO(card-detail): open the card once mob-card-detail-ui lands.
            onCardClick = { _, _ -> },
            onExportClick = navController::navigateToExport,
            onBoardMissing = {
                reloadBoards()
                navController.popBackStack(BoardListRoute, inclusive = false)
            },
            onBoardDeleted = { navController.popBackStack(BoardListRoute, inclusive = false) },
        )
        transferGraph(
            onImported = { reloadBoards() },
            onBoardListChanged = reloadBoards,
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
