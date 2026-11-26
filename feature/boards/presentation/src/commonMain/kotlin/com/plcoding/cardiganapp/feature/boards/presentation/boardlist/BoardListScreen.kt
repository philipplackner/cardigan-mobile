package com.plcoding.cardiganapp.feature.boards.presentation.boardlist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cardiganapp.feature.boards.presentation.generated.resources.Res
import cardiganapp.feature.boards.presentation.generated.resources.board_actions_description
import cardiganapp.feature.boards.presentation.generated.resources.board_item_description
import cardiganapp.feature.boards.presentation.generated.resources.board_menu_delete
import cardiganapp.feature.boards.presentation.generated.resources.board_menu_rename
import cardiganapp.feature.boards.presentation.generated.resources.board_updated
import cardiganapp.feature.boards.presentation.generated.resources.boards_empty_description
import cardiganapp.feature.boards.presentation.generated.resources.boards_empty_title
import cardiganapp.feature.boards.presentation.generated.resources.boards_import
import cardiganapp.feature.boards.presentation.generated.resources.boards_import_board
import cardiganapp.feature.boards.presentation.generated.resources.boards_labels
import cardiganapp.feature.boards.presentation.generated.resources.boards_load_error_description
import cardiganapp.feature.boards.presentation.generated.resources.boards_load_error_title
import cardiganapp.feature.boards.presentation.generated.resources.boards_log_out
import cardiganapp.feature.boards.presentation.generated.resources.boards_menu
import cardiganapp.feature.boards.presentation.generated.resources.boards_new_board
import cardiganapp.feature.boards.presentation.generated.resources.boards_refreshing
import cardiganapp.feature.boards.presentation.generated.resources.boards_signed_in_as
import cardiganapp.feature.boards.presentation.generated.resources.boards_title
import cardiganapp.feature.boards.presentation.generated.resources.boards_try_again
import com.plcoding.cardiganapp.core.designsystem.components.actions.ButtonStyle
import com.plcoding.cardiganapp.core.designsystem.components.actions.CardiganButton
import com.plcoding.cardiganapp.core.designsystem.components.actions.CardiganIconButton
import com.plcoding.cardiganapp.core.designsystem.components.actions.IconButtonSize
import com.plcoding.cardiganapp.core.designsystem.components.boards.BoardRow
import com.plcoding.cardiganapp.core.designsystem.components.boards.BoardTile
import com.plcoding.cardiganapp.core.designsystem.components.feedback.EmptyState
import com.plcoding.cardiganapp.core.designsystem.components.feedback.EmptyStateType
import com.plcoding.cardiganapp.core.designsystem.components.feedback.RefreshBox
import com.plcoding.cardiganapp.core.designsystem.components.feedback.Skeleton
import com.plcoding.cardiganapp.core.designsystem.components.feedback.Spinner
import com.plcoding.cardiganapp.core.designsystem.components.navigation.BrandAppBar
import com.plcoding.cardiganapp.core.designsystem.components.navigation.PageHeader
import com.plcoding.cardiganapp.core.designsystem.components.navigation.TitleAppBar
import com.plcoding.cardiganapp.core.designsystem.components.overlays.Menu
import com.plcoding.cardiganapp.core.designsystem.components.overlays.MenuDivider
import com.plcoding.cardiganapp.core.designsystem.components.overlays.MenuHeader
import com.plcoding.cardiganapp.core.designsystem.components.overlays.MenuItem
import com.plcoding.cardiganapp.core.designsystem.icons.CardiganIcons
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import com.plcoding.cardiganapp.core.designsystem.theme.LocalWindowClass
import com.plcoding.cardiganapp.core.designsystem.theme.WindowClass
import com.plcoding.cardiganapp.feature.boards.presentation.components.ProvideConsumedInsets
import com.plcoding.cardiganapp.feature.boards.presentation.components.contentInsets
import com.plcoding.cardiganapp.feature.boards.presentation.components.plus
import com.plcoding.cardiganapp.feature.boards.presentation.util.boardCountText
import org.jetbrains.compose.resources.stringResource

/** Minimum Board tile width from Figma. The grid fits as many columns as this allows (D19). */
val BoardTileMinWidth = 260.dp

private const val PhoneSkeletonRows = 5
private const val GridSkeletonTiles = 6

@Composable
fun BoardListScreen(
    state: BoardListState,
    onAction: (BoardListAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background.app),
    ) {
        ProvideConsumedInsets {
            if (LocalWindowClass.current == WindowClass.Phone) {
                PhoneBoardList(state, onAction)
            } else {
                GridBoardList(state, onAction)
            }
        }
    }
}

@Composable
private fun PhoneBoardList(state: BoardListState, onAction: (BoardListAction) -> Unit) {
    val listState = rememberLazyListState()
    val collapsed by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    Column(modifier = Modifier.fillMaxSize()) {
        val actions: @Composable RowScope.() -> Unit = { PhoneBarActions(state, onAction) }
        if (collapsed) {
            TitleAppBar(
                title = stringResource(Res.string.boards_title),
                raised = true,
                actions = actions,
                modifier = Modifier.testTag(BoardListTestTags.CollapsedBar),
            )
        } else {
            BrandAppBar(actions = actions)
        }
        RefreshBox(
            isRefreshing = false,
            onRefresh = { onAction(BoardListAction.OnRefresh) },
            modifier = Modifier.fillMaxSize(),
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val viewportHeight = maxHeight
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().testTag(BoardListTestTags.List),
                    contentPadding = PaddingValues(
                        start = AppTheme.space.s16,
                        end = AppTheme.space.s16,
                        top = AppTheme.space.s4,
                        bottom = AppTheme.space.s24,
                    ) + contentInsets(),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.space.s18),
                ) {
                    item(key = "title") { LargeTitle(state) }
                    if (state.isRefreshing) item(key = "refreshing") { RefreshingRow() }
                    when (val content = state.content) {
                        BoardListContent.Loading -> item(key = "loading") { PhoneSkeletonList() }
                        BoardListContent.Error -> item(key = "error") {
                            LoadErrorState(onAction, Modifier.fillMaxWidth().heightIn(min = viewportHeight * 0.75f))
                        }
                        is BoardListContent.Loaded -> if (content.boards.isEmpty()) {
                            item(key = "empty") {
                                NoBoardsState(onAction, Modifier.fillMaxWidth().heightIn(min = viewportHeight * 0.75f))
                            }
                        } else {
                            item(key = "boards") { PhoneBoardCard(content.boards, onAction) }
                            item(key = "count") { ListEndCount(state.boardCount) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhoneBarActions(state: BoardListState, onAction: (BoardListAction) -> Unit) {
    CardiganIconButton(
        icon = CardiganIcons.Plus,
        contentDescription = stringResource(Res.string.boards_new_board),
        onClick = { onAction(BoardListAction.OnNewBoardClick) },
        size = IconButtonSize.Large,
    )
    Box {
        CardiganIconButton(
            icon = CardiganIcons.More,
            contentDescription = stringResource(Res.string.boards_menu),
            onClick = { onAction(BoardListAction.OnMenuClick) },
            size = IconButtonSize.Large,
            selected = state.isMenuOpen,
        )
        Menu(expanded = state.isMenuOpen, onDismissRequest = { onAction(BoardListAction.OnMenuDismiss) }) {
            state.accountEmail?.let { email ->
                MenuHeader(text = stringResource(Res.string.boards_signed_in_as, email))
            }
            MenuItem(
                text = stringResource(Res.string.boards_import_board),
                icon = CardiganIcons.Upload,
                onClick = { onAction(BoardListAction.OnImportClick) },
            )
            MenuItem(
                text = stringResource(Res.string.boards_labels),
                icon = CardiganIcons.Label,
                onClick = { onAction(BoardListAction.OnLabelsClick) },
            )
            MenuDivider()
            MenuItem(
                text = stringResource(Res.string.boards_log_out),
                icon = CardiganIcons.LogOut,
                onClick = { onAction(BoardListAction.OnLogOutClick) },
            )
        }
    }
}

@Composable
private fun LargeTitle(state: BoardListState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = AppTheme.space.s2, bottom = AppTheme.space.s4),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.space.s10),
    ) {
        Text(
            text = stringResource(Res.string.boards_title),
            modifier = Modifier
                .alignByBaseline()
                .semantics { heading() },
            style = AppTheme.textStyles.sans26SemiBold110LsMinus2_5,
            color = AppTheme.colors.text.primary,
        )
        if (state.content is BoardListContent.Loaded) {
            Text(
                text = boardCountText(state.boardCount).asString(),
                modifier = Modifier.alignByBaseline(),
                style = AppTheme.textStyles.mono12Regular100,
                color = AppTheme.colors.text.muted,
            )
        }
    }
}

@Composable
private fun RefreshingRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(AppTheme.space.s10),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.space.s8, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spinner(color = AppTheme.colors.text.muted)
        Text(
            text = stringResource(Res.string.boards_refreshing),
            style = AppTheme.textStyles.sans13_5Regular145,
            color = AppTheme.colors.text.muted,
        )
    }
}

@Composable
private fun ListCard(content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(AppTheme.radius.r10)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppTheme.colors.background.surface, shape)
            .border(AppTheme.space.s1, AppTheme.colors.border.default, shape)
            .padding(AppTheme.space.s1),
    ) {
        content()
    }
}

@Composable
private fun PhoneBoardCard(boards: List<BoardItemUi>, onAction: (BoardListAction) -> Unit) {
    ListCard {
        boards.forEachIndexed { index, board ->
            BoardRow(
                name = board.name,
                boardKey = board.key,
                cardCount = board.cardCount.asString(),
                updated = board.updated.asString(),
                onClick = { onAction(BoardListAction.OnBoardClick(board.id)) },
                showDivider = index < boards.lastIndex,
                contentDescription = boardDescription(board),
                menuButton = { BoardMenuButton(board, onAction) },
            )
        }
    }
}

@Composable
private fun boardDescription(board: BoardItemUi): String = stringResource(
    Res.string.board_item_description,
    board.name,
    board.key,
    board.cardCount.asString(),
    board.updated.asString(),
)

@Composable
private fun BoardMenuButton(board: BoardItemUi, onAction: (BoardListAction) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        CardiganIconButton(
            icon = CardiganIcons.More,
            contentDescription = stringResource(Res.string.board_actions_description, board.name),
            onClick = { expanded = true },
            selected = expanded,
        )
        Menu(expanded = expanded, onDismissRequest = { expanded = false }) {
            MenuItem(
                text = stringResource(Res.string.board_menu_rename),
                icon = CardiganIcons.Rename,
                onClick = {
                    expanded = false
                    onAction(BoardListAction.OnRenameClick(board.id))
                },
            )
            MenuDivider()
            MenuItem(
                text = stringResource(Res.string.board_menu_delete),
                icon = CardiganIcons.Trash,
                danger = true,
                onClick = {
                    expanded = false
                    onAction(BoardListAction.OnDeleteClick(board.id))
                },
            )
        }
    }
}

@Composable
private fun ListEndCount(count: Int) {
    Text(
        text = boardCountText(count).asString(),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = AppTheme.space.s14),
        style = AppTheme.textStyles.mono12Regular100,
        color = AppTheme.colors.text.muted,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun PhoneSkeletonList() {
    ListCard {
        repeat(PhoneSkeletonRows) { index ->
            val divider = AppTheme.colors.border.default
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (index < PhoneSkeletonRows - 1) {
                            Modifier.drawDividerBelow(divider)
                        } else {
                            Modifier
                        },
                    )
                    .padding(start = AppTheme.space.s14, end = AppTheme.space.s6, top = AppTheme.space.s12, bottom = AppTheme.space.s13),
                verticalArrangement = Arrangement.spacedBy(AppTheme.space.s6),
            ) {
                Skeleton(modifier = Modifier.fillMaxWidth(0.6f).height(14.dp))
                Skeleton(modifier = Modifier.fillMaxWidth(0.4f).height(10.dp))
            }
        }
    }
}

@Composable
private fun NoBoardsState(onAction: (BoardListAction) -> Unit, modifier: Modifier = Modifier) {
    EmptyState(
        icon = CardiganIcons.Board,
        title = stringResource(Res.string.boards_empty_title),
        description = stringResource(Res.string.boards_empty_description),
        modifier = modifier,
        actions = {
            CardiganButton(
                text = stringResource(Res.string.boards_new_board),
                onClick = { onAction(BoardListAction.OnNewBoardClick) },
                leadingIcon = CardiganIcons.Plus,
            )
            CardiganButton(
                text = stringResource(Res.string.boards_import),
                onClick = { onAction(BoardListAction.OnImportClick) },
                style = ButtonStyle.Secondary,
                leadingIcon = CardiganIcons.Upload,
            )
        },
    )
}

@Composable
private fun LoadErrorState(onAction: (BoardListAction) -> Unit, modifier: Modifier = Modifier) {
    EmptyState(
        icon = CardiganIcons.CloudOff,
        title = stringResource(Res.string.boards_load_error_title),
        description = stringResource(Res.string.boards_load_error_description),
        modifier = modifier,
        type = EmptyStateType.Error,
        actions = {
            CardiganButton(
                text = stringResource(Res.string.boards_try_again),
                onClick = { onAction(BoardListAction.OnRetry) },
                style = ButtonStyle.Secondary,
            )
        },
    )
}

@Composable
private fun GridBoardList(state: BoardListState, onAction: (BoardListAction) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        PageHeader(
            title = stringResource(Res.string.boards_title),
            meta = if (state.content is BoardListContent.Loaded) boardCountText(state.boardCount).asString() else null,
            actions = {
                CardiganButton(
                    text = stringResource(Res.string.boards_import),
                    onClick = { onAction(BoardListAction.OnImportClick) },
                    style = ButtonStyle.Secondary,
                    leadingIcon = CardiganIcons.Upload,
                )
                CardiganButton(
                    text = stringResource(Res.string.boards_new_board),
                    onClick = { onAction(BoardListAction.OnNewBoardClick) },
                    leadingIcon = CardiganIcons.Plus,
                )
            },
        )
        RefreshBox(
            isRefreshing = false,
            onRefresh = { onAction(BoardListAction.OnRefresh) },
            modifier = Modifier.fillMaxSize(),
        ) {
            when (val content = state.content) {
                BoardListContent.Error -> FullHeightState(state.isRefreshing) { LoadErrorState(onAction, it) }
                is BoardListContent.Loaded if content.boards.isEmpty() ->
                    FullHeightState(state.isRefreshing) { NoBoardsState(onAction, it) }
                else -> BoardGrid(state, content, onAction)
            }
        }
    }
}

@Composable
private fun BoardGrid(state: BoardListState, content: BoardListContent, onAction: (BoardListAction) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(BoardTileMinWidth),
        modifier = Modifier.fillMaxSize().testTag(BoardListTestTags.Grid),
        contentPadding = PaddingValues(AppTheme.space.s24) + contentInsets(),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.space.s14),
        verticalArrangement = Arrangement.spacedBy(AppTheme.space.s14),
    ) {
        if (state.isRefreshing) item(key = "refreshing", span = { GridItemSpan(maxLineSpan) }) { RefreshingRow() }
        when (content) {
            is BoardListContent.Loaded -> items(content.boards, key = { it.id }) { board ->
                BoardTile(
                    name = board.name,
                    boardKey = board.key,
                    cardCount = board.cardCount.asString(),
                    updated = stringResource(Res.string.board_updated, board.updated.asString()),
                    onClick = { onAction(BoardListAction.OnBoardClick(board.id)) },
                    contentDescription = boardDescription(board),
                    menuButton = { BoardMenuButton(board, onAction) },
                )
            }
            else -> items(GridSkeletonTiles) { SkeletonTile() }
        }
    }
}

@Composable
private fun FullHeightState(isRefreshing: Boolean, content: @Composable (Modifier) -> Unit) {
    val padding = PaddingValues(AppTheme.space.s24) + contentInsets()
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val height = maxHeight - padding.calculateTopPadding() - padding.calculateBottomPadding()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding),
        ) {
            if (isRefreshing) RefreshingRow()
            content(Modifier.fillMaxWidth().heightIn(min = height))
        }
    }
}

@Composable
private fun SkeletonTile() {
    val shape = RoundedCornerShape(AppTheme.radius.r8)
    val divider = AppTheme.colors.border.default
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppTheme.colors.background.surface, shape)
            .border(AppTheme.space.s1, AppTheme.colors.border.default, shape)
            .padding(start = AppTheme.space.s14, end = AppTheme.space.s14, top = AppTheme.space.s14, bottom = AppTheme.space.s12),
        verticalArrangement = Arrangement.spacedBy(AppTheme.space.s10),
    ) {
        Skeleton(modifier = Modifier.size(width = 42.dp, height = 18.dp))
        Skeleton(modifier = Modifier.fillMaxWidth(0.7f).height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .drawDividerAbove(divider)
                .padding(top = AppTheme.space.s11),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Skeleton(modifier = Modifier.size(width = 60.dp, height = 10.dp))
            Skeleton(modifier = Modifier.size(width = 90.dp, height = 10.dp))
        }
    }
}

private fun Modifier.drawDividerBelow(color: Color): Modifier = drawBehind {
    val y = size.height - 0.5.dp.toPx()
    drawLine(color, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
}

private fun Modifier.drawDividerAbove(color: Color): Modifier = drawBehind {
    val y = 0.5.dp.toPx()
    drawLine(color, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
}

/**
 * The safe-drawing insets the scrolling content keeps clear of: the sides (display cutout in
 * landscape) and the bottom. The top belongs to the app bar or page header above the content, and
 * insets a parent already consumed (the sidebar's start side) are left out.
 */
/** Test tags for the board list's scroll containers and collapsed bar. */
object BoardListTestTags {
    const val List = "board_list"
    const val Grid = "board_grid"
    const val CollapsedBar = "board_list_collapsed_bar"
}
