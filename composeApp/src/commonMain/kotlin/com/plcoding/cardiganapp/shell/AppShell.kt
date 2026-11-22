package com.plcoding.cardiganapp.shell

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import cardiganapp.composeapp.generated.resources.Res
import cardiganapp.composeapp.generated.resources.shell_all_boards
import cardiganapp.composeapp.generated.resources.shell_boards_heading
import cardiganapp.composeapp.generated.resources.shell_labels
import cardiganapp.composeapp.generated.resources.shell_log_out
import com.plcoding.cardiganapp.core.designsystem.components.navigation.NavItem
import com.plcoding.cardiganapp.core.designsystem.components.navigation.NavItemState
import com.plcoding.cardiganapp.core.designsystem.components.navigation.Sidebar
import com.plcoding.cardiganapp.core.designsystem.icons.CardiganIcons
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import com.plcoding.cardiganapp.core.designsystem.theme.LocalWindowClass
import com.plcoding.cardiganapp.core.designsystem.theme.WindowClass
import com.plcoding.cardiganapp.feature.boards.presentation.navigation.BoardListRoute
import com.plcoding.cardiganapp.feature.boards.presentation.navigation.navigateToNewBoard
import com.plcoding.cardiganapp.feature.boards.presentation.sidebar.BoardsSidebarSection
import org.jetbrains.compose.resources.stringResource

/**
 * The frame around every top-level screen (D1). Phone: only [content], the screens draw their own
 * bars and menus. Tablet and desktop: the sidebar with All boards, Labels, the boards and the
 * account, next to [content]. A null [accountEmail] leaves the email and avatar out.
 */
@Composable
fun AppShell(
    navController: NavHostController,
    accountEmail: String?,
    onLogOut: () -> Unit,
    onOpenBoard: (boardId: String, name: String, key: String) -> Unit,
    onLabelsClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit,
) {
    val windowClass = LocalWindowClass.current
    if (windowClass == WindowClass.Phone) {
        content(modifier.fillMaxSize())
        return
    }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val onBoardList = navController.previousNonDialogDestinationIsBoardList(backStackEntry?.destination?.hasRoute<BoardListRoute>() == true)
    Row(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
    ) {
        Sidebar(
            boardsHeading = stringResource(Res.string.shell_boards_heading),
            accountEmail = accountEmail,
            avatarInitial = accountEmail?.firstOrNull()?.uppercase(),
            onLogOutClick = onLogOut,
            logOutContentDescription = stringResource(Res.string.shell_log_out),
            modifier = Modifier
                .width(if (windowClass == WindowClass.Tablet) AppTheme.width.sidebarTablet else AppTheme.width.sidebarDesktop)
                .fillMaxHeight()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical)),
            mainNav = {
                NavItem(
                    label = stringResource(Res.string.shell_all_boards),
                    icon = CardiganIcons.Boards,
                    state = if (onBoardList) NavItemState.Selected else NavItemState.Default,
                    onClick = { navController.popBackStack(BoardListRoute, inclusive = false) },
                )
                NavItem(
                    label = stringResource(Res.string.shell_labels),
                    icon = CardiganIcons.Label,
                    onClick = onLabelsClick,
                )
            },
            boards = {
                BoardsSidebarSection(
                    onOpenBoard = onOpenBoard,
                    onNewBoardClick = navController::navigateToNewBoard,
                )
            },
        )
        content(Modifier.weight(1f).fillMaxHeight())
    }
}

/**
 * True when the board list is the screen under any open dialog, so "All boards" stays selected
 * while a board dialog or the import dialog is shown on top of it.
 */
private fun NavHostController.previousNonDialogDestinationIsBoardList(currentIsBoardList: Boolean): Boolean {
    if (currentIsBoardList) return true
    val entries = currentBackStack.value
    val screen = entries.lastOrNull { it.destination.navigatorName != "dialog" && it.destination.navigatorName != "navigation" }
    return screen?.destination?.hasRoute<BoardListRoute>() == true
}
