package com.plcoding.cardiganapp.feature.boards.presentation.boardlist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.plcoding.cardiganapp.core.presentation.util.ObserveAsEvents
import org.koin.compose.viewmodel.koinViewModel

/**
 * The board list. [accountEmail] is shown in the phone menu; null leaves the "Signed in as" line
 * out. Navigation goes through the callbacks.
 */
@Composable
fun BoardListRoot(
    accountEmail: String?,
    onOpenBoard: (boardId: String, name: String, key: String) -> Unit,
    onNewBoardClick: () -> Unit,
    onRenameClick: (boardId: String) -> Unit,
    onDeleteClick: (boardId: String) -> Unit,
    onImportClick: () -> Unit,
    onLabelsClick: () -> Unit,
    onLogOutClick: () -> Unit,
    viewModel: BoardListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is BoardListEvent.OpenBoard -> onOpenBoard(event.boardId, event.name, event.key)
        }
    }
    BoardListScreen(
        state = state.copy(accountEmail = accountEmail),
        onAction = { action ->
            viewModel.onAction(action)
            when (action) {
                BoardListAction.OnNewBoardClick -> onNewBoardClick()
                is BoardListAction.OnRenameClick -> onRenameClick(action.boardId)
                is BoardListAction.OnDeleteClick -> onDeleteClick(action.boardId)
                BoardListAction.OnImportClick -> onImportClick()
                BoardListAction.OnLabelsClick -> onLabelsClick()
                BoardListAction.OnLogOutClick -> onLogOutClick()
                else -> Unit
            }
        },
    )
}
