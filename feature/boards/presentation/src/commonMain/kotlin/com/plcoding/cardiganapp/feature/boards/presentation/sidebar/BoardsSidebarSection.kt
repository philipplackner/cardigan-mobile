package com.plcoding.cardiganapp.feature.boards.presentation.sidebar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cardiganapp.feature.boards.presentation.generated.resources.Res
import cardiganapp.feature.boards.presentation.generated.resources.boards_new_board
import com.plcoding.cardiganapp.core.designsystem.components.navigation.NavItem
import com.plcoding.cardiganapp.core.designsystem.components.navigation.NavItemState
import com.plcoding.cardiganapp.core.designsystem.icons.CardiganIcons
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * The boards slot of the design-system `Sidebar`: one entry per loaded board and "New board".
 * [selectedBoardId] marks the open board, if any.
 */
@Composable
fun BoardsSidebarSection(
    onOpenBoard: (boardId: String, name: String, key: String) -> Unit,
    onNewBoardClick: () -> Unit,
    selectedBoardId: String? = null,
    viewModel: BoardsSidebarViewModel = koinViewModel(),
) {
    val boards by viewModel.boards.collectAsStateWithLifecycle()
    BoardsSidebarItems(boards, selectedBoardId, onOpenBoard, onNewBoardClick)
}

@Composable
fun BoardsSidebarItems(
    boards: List<SidebarBoardUi>,
    selectedBoardId: String?,
    onOpenBoard: (boardId: String, name: String, key: String) -> Unit,
    onNewBoardClick: () -> Unit,
) {
    boards.forEach { board ->
        NavItem(
            label = board.name,
            icon = CardiganIcons.Board,
            boardKey = board.key,
            state = if (board.id == selectedBoardId) NavItemState.Selected else NavItemState.Default,
            onClick = { onOpenBoard(board.id, board.name, board.key) },
        )
    }
    NavItem(
        label = stringResource(Res.string.boards_new_board),
        icon = CardiganIcons.Plus,
        state = NavItemState.Dim,
        onClick = onNewBoardClick,
    )
}

@Preview(name = "Sidebar boards · Light")
@Composable
private fun BoardsSidebarItemsPreview() {
    AppTheme(darkTheme = false) {
        Column(modifier = Modifier.width(AppTheme.width.sidebarDesktop)) {
            BoardsSidebarItems(
                boards =
                    listOf(
                        SidebarBoardUi("1", "Cardigan v1", "CRD"),
                        SidebarBoardUi("2", "Raytracer", "RAY"),
                    ),
                selectedBoardId = "2",
                onOpenBoard = { _, _, _ -> },
                onNewBoardClick = {},
            )
        }
    }
}
