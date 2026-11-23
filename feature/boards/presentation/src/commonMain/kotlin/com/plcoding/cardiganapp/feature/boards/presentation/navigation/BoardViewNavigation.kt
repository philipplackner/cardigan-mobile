package com.plcoding.cardiganapp.feature.boards.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.plcoding.cardiganapp.feature.boards.presentation.detail.BoardAction
import com.plcoding.cardiganapp.feature.boards.presentation.detail.BoardRoot
import com.plcoding.cardiganapp.feature.boards.presentation.detail.BoardViewModel
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

/**
 * One board. [name] and [key] are what the caller already knows about it; the header shows them
 * while the board loads (D39). Their property names are the ViewModel's argument keys.
 */
@Serializable
data class BoardRoute(
    val boardId: String,
    val name: String? = null,
    val key: String? = null,
)

/**
 * Registers the board view. Rename board and Delete board open the board list's dialogs through
 * [navController]; their results come back to this entry. [onBoardMissing] runs after the
 * "This board no longer exists." toast, [onBoardDeleted] after a delete from the board menu.
 */
fun NavGraphBuilder.boardViewDestination(
    navController: NavController,
    onBack: () -> Unit,
    onCardClick: (boardId: String, cardId: String) -> Unit,
    onExportClick: (boardId: String, boardName: String) -> Unit,
    onBoardMissing: (boardId: String) -> Unit,
    onBoardDeleted: (boardId: String) -> Unit,
) {
    composable<BoardRoute> { entry ->
        val viewModel = koinViewModel<BoardViewModel>()
        val boardId = entry.toRoute<BoardRoute>().boardId
        ObserveBoardDialogResults(
            entry = entry,
            onRenamed = { viewModel.onAction(BoardAction.BoardRenamed(it)) },
            onDeleted = { viewModel.onAction(BoardAction.BoardDeleted) },
        )
        BoardRoot(
            viewModel = viewModel,
            onBack = onBack,
            onCardClick = { cardId -> onCardClick(boardId, cardId) },
            onExportClick = onExportClick,
            onRenameBoardClick = navController::navigateToRenameBoard,
            onDeleteBoardClick = navController::navigateToDeleteBoard,
            onBoardMissing = { onBoardMissing(boardId) },
            onBoardDeleted = { onBoardDeleted(boardId) },
        )
    }
}

/** Opens a board. [name] and [key] fill the header while it loads. */
fun NavController.navigateToBoard(boardId: String, name: String? = null, key: String? = null) {
    navigate(BoardRoute(boardId, name, key))
}
