package com.plcoding.cardiganapp.feature.boards.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.toRoute
import com.plcoding.cardiganapp.feature.boards.presentation.boardlist.BoardListRoot
import com.plcoding.cardiganapp.feature.boards.presentation.deleteboard.DeleteBoardRoot
import com.plcoding.cardiganapp.feature.boards.presentation.deleteboard.DeleteBoardViewModel
import com.plcoding.cardiganapp.feature.boards.presentation.newboard.NewBoardRoot
import com.plcoding.cardiganapp.feature.boards.presentation.renameboard.RenameBoardRoot
import com.plcoding.cardiganapp.feature.boards.presentation.renameboard.RenameBoardViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.reflect.KClass

/**
 * Registers the board list and the New board, Rename board and Delete board dialogs.
 * [accountEmail] is read while the list is shown; null leaves the phone menu's account line out.
 * Rename and Delete report their result to the entry that opened them, see [ObserveBoardDialogResults].
 */
fun NavGraphBuilder.boardsGraph(
    navController: NavController,
    accountEmail: () -> String?,
    onOpenBoard: (boardId: String, name: String, key: String) -> Unit,
    onImportClick: () -> Unit,
    onLabelsClick: () -> Unit,
    onLogOutClick: () -> Unit,
) {
    composable<BoardListRoute> {
        BoardListRoot(
            accountEmail = accountEmail(),
            onOpenBoard = onOpenBoard,
            onNewBoardClick = navController::navigateToNewBoard,
            onRenameClick = navController::navigateToRenameBoard,
            onDeleteClick = navController::navigateToDeleteBoard,
            onImportClick = onImportClick,
            onLabelsClick = onLabelsClick,
            onLogOutClick = onLogOutClick,
        )
    }
    dialog<NewBoardRoute>(dialogProperties = boardDialogDestinationProperties()) {
        BoardDialogDestinationNoDimEffect()
        NewBoardRoot(
            onClose = { navController.popBoardDialog(NewBoardRoute::class) },
            onCreated = onOpenBoard,
        )
    }
    dialog<RenameBoardRoute>(dialogProperties = boardDialogDestinationProperties()) { entry ->
        BoardDialogDestinationNoDimEffect()
        val boardId = entry.toRoute<RenameBoardRoute>().boardId
        val caller = rememberCaller(navController)
        RenameBoardRoot(
            viewModel = koinViewModel<RenameBoardViewModel> { parametersOf(boardId) },
            onClose = { navController.popBoardDialog(RenameBoardRoute::class) },
            onRenamed = { newName -> caller?.savedStateHandle?.set(RESULT_RENAMED, newName) },
        )
    }
    dialog<DeleteBoardRoute>(dialogProperties = boardDialogDestinationProperties()) { entry ->
        BoardDialogDestinationNoDimEffect()
        val boardId = entry.toRoute<DeleteBoardRoute>().boardId
        val caller = rememberCaller(navController)
        DeleteBoardRoot(
            viewModel = koinViewModel<DeleteBoardViewModel> { parametersOf(boardId) },
            onClose = { navController.popBoardDialog(DeleteBoardRoute::class) },
            onDeleted = { caller?.savedStateHandle?.set(RESULT_DELETED, true) },
        )
    }
}

/** Opens the New board dialog. A second tap while it is open doesn't open another one. */
fun NavController.navigateToNewBoard() {
    navigate(NewBoardRoute) { launchSingleTop = true }
}

/** Opens Rename board. The result goes to the current entry, see [ObserveBoardDialogResults]. */
fun NavController.navigateToRenameBoard(boardId: String) {
    navigate(RenameBoardRoute(boardId)) { launchSingleTop = true }
}

/** Opens Delete board. The result goes to the current entry, see [ObserveBoardDialogResults]. */
fun NavController.navigateToDeleteBoard(boardId: String) {
    navigate(DeleteBoardRoute(boardId)) { launchSingleTop = true }
}

/**
 * Delivers the Rename board and Delete board results to the screen of [entry] that opened them:
 * [onRenamed] gets the name the server returned. Cancel and a rename of a missing board report nothing.
 */
@Composable
fun ObserveBoardDialogResults(
    entry: NavBackStackEntry,
    onRenamed: (newName: String) -> Unit,
    onDeleted: () -> Unit,
) {
    val currentOnRenamed by rememberUpdatedState(onRenamed)
    val currentOnDeleted by rememberUpdatedState(onDeleted)
    val handle = entry.savedStateHandle
    LaunchedEffect(handle) {
        handle.getStateFlow<String?>(RESULT_RENAMED, null).collect { name ->
            if (name != null) {
                handle.remove<String>(RESULT_RENAMED)
                currentOnRenamed(name)
            }
        }
    }
    LaunchedEffect(handle) {
        handle.getStateFlow(RESULT_DELETED, false).collect { deleted ->
            if (deleted) {
                handle.remove<Boolean>(RESULT_DELETED)
                currentOnDeleted()
            }
        }
    }
}

@Composable
private fun rememberCaller(navController: NavController): NavBackStackEntry? =
    remember { navController.previousBackStackEntry }

/** Pops the board dialog of [route], and nothing else, so a late second call can't pop a screen. */
private fun NavController.popBoardDialog(route: KClass<*>) {
    val destination = currentBackStackEntry?.destination ?: return
    if (destination.hasRoute(route)) popBackStack()
}

private const val RESULT_RENAMED = "board_dialog_renamed"
private const val RESULT_DELETED = "board_dialog_deleted"
