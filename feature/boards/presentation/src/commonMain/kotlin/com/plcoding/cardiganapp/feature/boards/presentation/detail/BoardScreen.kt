package com.plcoding.cardiganapp.feature.boards.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cardiganapp.feature.boards.presentation.generated.resources.Res
import cardiganapp.feature.boards.presentation.generated.resources.board_view_add_column
import cardiganapp.feature.boards.presentation.generated.resources.board_view_back
import cardiganapp.feature.boards.presentation.generated.resources.board_view_board_actions
import cardiganapp.feature.boards.presentation.generated.resources.board_view_delete_board
import cardiganapp.feature.boards.presentation.generated.resources.board_view_empty_description
import cardiganapp.feature.boards.presentation.generated.resources.board_view_empty_title
import cardiganapp.feature.boards.presentation.generated.resources.board_view_export
import cardiganapp.feature.boards.presentation.generated.resources.board_view_load_error_description
import cardiganapp.feature.boards.presentation.generated.resources.board_view_load_error_title
import cardiganapp.feature.boards.presentation.generated.resources.board_view_phone_subtitle
import cardiganapp.feature.boards.presentation.generated.resources.board_view_rename_board
import cardiganapp.feature.boards.presentation.generated.resources.board_view_try_again
import cardiganapp.feature.boards.presentation.generated.resources.card_count
import com.plcoding.cardiganapp.core.designsystem.components.actions.ButtonStyle
import com.plcoding.cardiganapp.core.designsystem.components.actions.CardiganButton
import com.plcoding.cardiganapp.core.designsystem.components.actions.CardiganIconButton
import com.plcoding.cardiganapp.core.designsystem.components.actions.IconButtonSize
import com.plcoding.cardiganapp.core.designsystem.components.actions.IconButtonStyle
import com.plcoding.cardiganapp.core.designsystem.components.board.AddColumnButton
import com.plcoding.cardiganapp.core.designsystem.components.feedback.EmptyState
import com.plcoding.cardiganapp.core.designsystem.components.feedback.EmptyStateType
import com.plcoding.cardiganapp.core.designsystem.components.navigation.PageHeader
import com.plcoding.cardiganapp.core.designsystem.components.navigation.TitleAppBar
import com.plcoding.cardiganapp.core.designsystem.components.overlays.Menu
import com.plcoding.cardiganapp.core.designsystem.components.overlays.MenuDivider
import com.plcoding.cardiganapp.core.designsystem.components.overlays.MenuItem
import com.plcoding.cardiganapp.core.designsystem.icons.CardiganIcons
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import com.plcoding.cardiganapp.core.designsystem.theme.LocalWindowClass
import com.plcoding.cardiganapp.core.designsystem.theme.WindowClass
import com.plcoding.cardiganapp.feature.boards.presentation.components.ProvideConsumedInsets
import com.plcoding.cardiganapp.feature.boards.presentation.components.contentInsets
import com.plcoding.cardiganapp.feature.boards.presentation.components.plus
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** Column width on phones (Figma annotation) and on tablet and desktop. */
internal val PhoneColumnWidth = 300.dp
internal val DesktopColumnWidth = 276.dp
private val AddColumnButtonWidth = 220.dp

/** A request from the ViewModel to focus an inline field. [id] makes repeated requests distinct. */
data class FocusRequest(val target: InlineTarget, val id: Long)

/**
 * The board view's content area. The sidebar and phone menu come from the app shell.
 * [stripState] is owned by the Root so it can scroll to a new column; [focusRequest] focuses
 * an inline field and scrolls it into view.
 */
@Composable
fun BoardScreen(
    state: BoardState,
    onAction: (BoardAction) -> Unit,
    modifier: Modifier = Modifier,
    stripState: LazyListState = rememberLazyListState(),
    focusRequest: FocusRequest? = null,
) {
    val phone = LocalWindowClass.current == WindowClass.Phone
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background.app),
    ) {
        ProvideConsumedInsets {
            Column(modifier = Modifier.fillMaxSize()) {
                if (phone) PhoneHeader(state, onAction) else DesktopHeader(state, onAction)
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    BoardContent(state, onAction, stripState, focusRequest, phone)
                }
            }
        }
        BoardDialogs(state, onAction)
    }
}

@Composable
private fun PhoneHeader(state: BoardState, onAction: (BoardAction) -> Unit) {
    val key = state.headerKey
    val loaded = state.loaded
    val subtitle = when {
        key == null -> null
        loaded != null && loaded.content.cardCount > 0 -> stringResource(
            Res.string.board_view_phone_subtitle,
            key,
            pluralStringResource(Res.plurals.card_count, loaded.content.cardCount, loaded.content.cardCount),
        )
        else -> key
    }
    TitleAppBar(
        title = state.headerName.orEmpty(),
        subtitle = subtitle,
        navigationIcon = {
            CardiganIconButton(
                icon = CardiganIcons.ChevronLeft,
                contentDescription = stringResource(Res.string.board_view_back),
                onClick = { onAction(BoardAction.BackClick) },
                size = IconButtonSize.Large,
            )
        },
        actions = if (loaded != null) {
            { BoardMenuButton(onAction, IconButtonSize.Large, IconButtonStyle.Ghost) }
        } else {
            null
        },
    )
}

@Composable
private fun DesktopHeader(state: BoardState, onAction: (BoardAction) -> Unit) {
    val loaded = state.loaded
    val count = loaded?.content?.cardCount ?: 0
    PageHeader(
        title = state.headerName.orEmpty(),
        boardKey = state.headerKey,
        meta = if (count > 0) pluralStringResource(Res.plurals.card_count, count, count) else null,
        actions = if (loaded != null) {
            { BoardMenuButton(onAction, IconButtonSize.Medium, IconButtonStyle.Boxed) }
        } else {
            null
        },
    )
}

@Composable
private fun BoardMenuButton(onAction: (BoardAction) -> Unit, size: IconButtonSize, style: IconButtonStyle) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        CardiganIconButton(
            icon = CardiganIcons.More,
            contentDescription = stringResource(Res.string.board_view_board_actions),
            onClick = { expanded = true },
            size = size,
            style = style,
            selected = expanded,
            modifier = Modifier.testTag(BoardTestTags.BoardMenu),
        )
        Menu(expanded = expanded, onDismissRequest = { expanded = false }) {
            MenuItem(
                text = stringResource(Res.string.board_view_rename_board),
                icon = CardiganIcons.Rename,
                onClick = {
                    expanded = false
                    onAction(BoardAction.RenameBoardClick)
                },
            )
            MenuItem(
                text = stringResource(Res.string.board_view_export),
                icon = CardiganIcons.Download,
                onClick = {
                    expanded = false
                    onAction(BoardAction.ExportClick)
                },
            )
            MenuDivider()
            MenuItem(
                text = stringResource(Res.string.board_view_delete_board),
                icon = CardiganIcons.Trash,
                danger = true,
                onClick = {
                    expanded = false
                    onAction(BoardAction.DeleteBoardClick)
                },
            )
        }
    }
}

@Composable
private fun BoardContent(
    state: BoardState,
    onAction: (BoardAction) -> Unit,
    stripState: LazyListState,
    focusRequest: FocusRequest?,
    phone: Boolean,
) {
    when (val content = state.content) {
        BoardLoadState.Loading -> SkeletonStrip(phone)
        BoardLoadState.Error -> CenteredState {
            EmptyState(
                icon = CardiganIcons.CloudOff,
                title = stringResource(Res.string.board_view_load_error_title),
                description = stringResource(Res.string.board_view_load_error_description),
                type = EmptyStateType.Error,
                actions = {
                    CardiganButton(
                        text = stringResource(Res.string.board_view_try_again),
                        onClick = { onAction(BoardAction.Retry) },
                        style = ButtonStyle.Secondary,
                        leadingIcon = CardiganIcons.Refresh,
                    )
                },
            )
        }
        is BoardLoadState.Loaded -> {
            val addColumnOpen = state.inlineField?.target == InlineTarget.AddColumn
            if (content.content.columns.isEmpty() && !addColumnOpen) {
                CenteredState {
                    EmptyState(
                        icon = CardiganIcons.Columns,
                        title = stringResource(Res.string.board_view_empty_title),
                        description = stringResource(Res.string.board_view_empty_description),
                        actions = {
                            CardiganButton(
                                text = stringResource(Res.string.board_view_add_column),
                                onClick = { onAction(BoardAction.OpenAddColumn) },
                                leadingIcon = CardiganIcons.Plus,
                            )
                        },
                    )
                }
            } else {
                ColumnStrip(state, content, onAction, stripState, focusRequest, phone)
            }
        }
    }
}

@Composable
private fun CenteredState(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentInsets()),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** Strip padding and gaps from Figma (see design.md "Figma values"). */
@Composable
internal fun stripPadding(phone: Boolean): PaddingValues = if (phone) {
    PaddingValues(start = AppTheme.space.s12, end = AppTheme.space.s12, top = AppTheme.space.s8, bottom = AppTheme.space.s12)
} else {
    PaddingValues(horizontal = AppTheme.space.s20, vertical = AppTheme.space.s16)
}

@Composable
internal fun columnGap(phone: Boolean): Dp = if (phone) AppTheme.space.s10 else AppTheme.space.s12

@Composable
private fun ColumnStrip(
    state: BoardState,
    loaded: BoardLoadState.Loaded,
    onAction: (BoardAction) -> Unit,
    stripState: LazyListState,
    focusRequest: FocusRequest?,
    phone: Boolean,
) {
    val board = loaded.content
    val columns = board.columns
    val columnWidth = if (phone) PhoneColumnWidth else DesktopColumnWidth
    val padding = stripPadding(phone) + contentInsets()
    val layoutDirection = LocalLayoutDirection.current
    val currentOnAction by rememberUpdatedState(onAction)
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val maxColumnHeight = maxHeight - padding.calculateTopPadding() - padding.calculateBottomPadding()
        LazyRow(
            state = stripState,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) { detectTapGestures { currentOnAction(BoardAction.InlineOutsideTap) } }
                .testTag(BoardTestTags.Strip),
            contentPadding = PaddingValues(
                start = padding.calculateStartPadding(layoutDirection),
                end = padding.calculateEndPadding(layoutDirection),
                top = padding.calculateTopPadding(),
            ),
            horizontalArrangement = Arrangement.spacedBy(columnGap(phone)),
            verticalAlignment = Alignment.Top,
        ) {
            items(columns, key = { it.id.toString() }) { column ->
                val index = columns.indexOf(column)
                val field = state.inlineField?.takeIf { (it.target as? InlineTarget.QuickAdd)?.columnId == column.id }
                BoardViewColumn(
                    column = column,
                    boardKey = board.board.key,
                    labelsById = loaded.labelsById,
                    canMoveLeft = index > 0,
                    canMoveRight = index < columns.lastIndex,
                    quickAdd = field,
                    focusRequest = focusRequest?.takeIf { it.target == field?.target },
                    isRefreshing = state.isRefreshing && state.refreshingColumnId == column.id,
                    onAction = onAction,
                    modifier = Modifier
                        .width(columnWidth)
                        .heightIn(max = maxColumnHeight.coerceAtLeast(0.dp)),
                )
            }
            item(key = ADD_COLUMN_KEY) {
                val field = state.inlineField?.takeIf { it.target == InlineTarget.AddColumn }
                if (field != null) {
                    AddColumnForm(
                        field = field,
                        focusRequest = focusRequest?.takeIf { it.target == InlineTarget.AddColumn },
                        onAction = onAction,
                        modifier = Modifier.width(DesktopColumnWidth),
                    )
                } else {
                    AddColumnButton(
                        text = stringResource(Res.string.board_view_add_column),
                        onClick = { onAction(BoardAction.OpenAddColumn) },
                        modifier = Modifier
                            .width(AddColumnButtonWidth)
                            .testTag(BoardTestTags.AddColumn),
                    )
                }
            }
        }
    }
}

internal const val ADD_COLUMN_KEY = "add_column"

/** Test tags for the board view's containers. */
object BoardTestTags {
    const val Strip = "board_strip"
    const val Skeleton = "board_skeleton"
    const val BoardMenu = "board_menu_button"
    const val AddColumn = "board_add_column"
    const val InlineField = "board_inline_field"

    fun column(name: String) = "board_column_$name"
    fun columnCards(name: String) = "board_column_cards_$name"
}
