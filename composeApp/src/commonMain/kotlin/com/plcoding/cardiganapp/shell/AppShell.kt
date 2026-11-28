package com.plcoding.cardiganapp.shell

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
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
import com.plcoding.cardiganapp.feature.boards.presentation.navigation.BoardRoute
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
    val onBoardList =
        navController.previousNonDialogDestinationIsBoardList(
            backStackEntry?.destination?.hasRoute<BoardListRoute>() == true,
        )
    val startInset = WindowInsets.safeDrawing.only(WindowInsetsSides.Start)
    val sidebarWidth =
        if (windowClass ==
            WindowClass.Tablet
        ) {
            AppTheme.width.sidebarTablet
        } else {
            AppTheme.width.sidebarDesktop
        }
    Row(modifier = modifier.fillMaxSize()) {
        Sidebar(
            boardsHeading = stringResource(Res.string.shell_boards_heading),
            accountEmail = accountEmail,
            avatarInitial = accountEmail?.firstOrNull()?.uppercase(),
            onLogOutClick = onLogOut,
            logOutContentDescription = stringResource(Res.string.shell_log_out),
            modifier =
                Modifier
                    .width(
                        sidebarWidth + startInset.asPaddingValues().calculateStartPadding(LocalLayoutDirection.current),
                    ).fillMaxHeight(),
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
                    selectedBoardId = backStackEntry?.let { navController.openBoardId() },
                )
            },
        )
        // The sidebar pads its content by the start inset, so the screen next to it must not again.
        content(Modifier.weight(1f).fillMaxHeight().consumeWindowInsets(startInset))
    }
}

/** The board shown by the screen under any open dialog, or null when that screen isn't a board. */
private fun NavHostController.openBoardId(): String? {
    val screen =
        currentBackStack.value.lastOrNull {
            it.destination.navigatorName != "dialog" &&
                it.destination.navigatorName != "navigation"
        }
    return screen?.takeIf { it.destination.hasRoute<BoardRoute>() }?.toRoute<BoardRoute>()?.boardId
}

/**
 * True when the board list is the screen under any open dialog, so "All boards" stays selected
 * while a board dialog or the import dialog is shown on top of it.
 */
private fun NavHostController.previousNonDialogDestinationIsBoardList(currentIsBoardList: Boolean): Boolean {
    if (currentIsBoardList) return true
    val entries = currentBackStack.value
    val screen =
        entries.lastOrNull {
            it.destination.navigatorName != "dialog" &&
                it.destination.navigatorName != "navigation"
        }
    return screen?.destination?.hasRoute<BoardListRoute>() == true
}
