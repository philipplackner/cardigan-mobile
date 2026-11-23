package com.plcoding.cardiganapp.feature.boards.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cardiganapp.feature.boards.presentation.generated.resources.Res
import cardiganapp.feature.boards.presentation.generated.resources.board_view_add_card
import cardiganapp.feature.boards.presentation.generated.resources.board_view_add_card_to
import cardiganapp.feature.boards.presentation.generated.resources.board_view_add_column
import cardiganapp.feature.boards.presentation.generated.resources.board_view_cancel
import cardiganapp.feature.boards.presentation.generated.resources.board_view_card_tile
import cardiganapp.feature.boards.presentation.generated.resources.board_view_card_tile_no_labels
import cardiganapp.feature.boards.presentation.generated.resources.board_view_column_actions
import cardiganapp.feature.boards.presentation.generated.resources.board_view_delete_column
import cardiganapp.feature.boards.presentation.generated.resources.board_view_move_left
import cardiganapp.feature.boards.presentation.generated.resources.board_view_move_right
import cardiganapp.feature.boards.presentation.generated.resources.board_view_no_cards
import cardiganapp.feature.boards.presentation.generated.resources.board_view_rename
import com.plcoding.cardiganapp.core.designsystem.components.actions.ButtonStyle
import com.plcoding.cardiganapp.core.designsystem.components.actions.CardiganButton
import com.plcoding.cardiganapp.core.designsystem.components.actions.CardiganIconButton
import com.plcoding.cardiganapp.core.designsystem.components.actions.IconButtonSize
import com.plcoding.cardiganapp.core.designsystem.components.board.AddCardButton
import com.plcoding.cardiganapp.core.designsystem.components.board.BoardCard
import com.plcoding.cardiganapp.core.designsystem.components.board.BoardColumn
import com.plcoding.cardiganapp.core.designsystem.components.feedback.RefreshBox
import com.plcoding.cardiganapp.core.designsystem.components.feedback.Skeleton
import com.plcoding.cardiganapp.core.designsystem.components.inputs.ErrorMessage
import com.plcoding.cardiganapp.core.designsystem.components.inputs.Input
import com.plcoding.cardiganapp.core.designsystem.components.inputs.TextArea
import com.plcoding.cardiganapp.core.designsystem.components.inputs.TextAreaType
import com.plcoding.cardiganapp.core.designsystem.components.interaction.ControlSize
import com.plcoding.cardiganapp.core.designsystem.components.interaction.dashedBorder
import com.plcoding.cardiganapp.core.designsystem.components.labels.Label as LabelChip
import com.plcoding.cardiganapp.core.designsystem.components.overlays.Menu
import com.plcoding.cardiganapp.core.designsystem.components.overlays.MenuDivider
import com.plcoding.cardiganapp.core.designsystem.components.overlays.MenuItem
import com.plcoding.cardiganapp.core.designsystem.icons.CardiganIcons
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import com.plcoding.cardiganapp.core.domain.board.Card
import com.plcoding.cardiganapp.core.domain.board.Column
import com.plcoding.cardiganapp.core.domain.board.displayId
import com.plcoding.cardiganapp.core.domain.label.Label
import com.plcoding.cardiganapp.core.domain.label.sortedForCard
import com.plcoding.cardiganapp.core.presentation.text.toUiText
import kotlin.uuid.Uuid
import org.jetbrains.compose.resources.stringResource
import com.plcoding.cardiganapp.core.designsystem.components.labels.LabelColor as PaletteColor
import com.plcoding.cardiganapp.core.domain.label.LabelColor as DomainLabelColor

/** One column: header with its menu, the cards in a pull-to-refresh scroll, and quick add. */
@Composable
internal fun BoardViewColumn(
    column: Column,
    boardKey: String,
    labelsById: Map<Uuid, Label>,
    canMoveLeft: Boolean,
    canMoveRight: Boolean,
    quickAdd: InlineField?,
    focusRequest: FocusRequest?,
    isRefreshing: Boolean,
    onAction: (BoardAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    LaunchedEffect(focusRequest) {
        if (focusRequest != null) scrollState.animateScrollTo(scrollState.maxValue)
    }
    val addCardLabel = stringResource(Res.string.board_view_add_card_to, column.name)
    BoardColumn(
        name = column.name,
        count = column.cards.size.toString(),
        modifier = modifier.testTag(BoardTestTags.column(column.name)),
        menuButton = { ColumnMenuButton(column, canMoveLeft, canMoveRight, onAction) },
        addCard = if (quickAdd == null) {
            {
                AddCardButton(
                    text = stringResource(Res.string.board_view_add_card),
                    onClick = { onAction(BoardAction.OpenQuickAdd(column.id)) },
                    modifier = Modifier.clearAndSetSemantics {
                        contentDescription = addCardLabel
                        role = Role.Button
                        onClick { onAction(BoardAction.OpenQuickAdd(column.id)); true }
                    },
                )
            }
        } else {
            null
        },
    ) {
        RefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { onAction(BoardAction.Refresh(column.id)) },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .testTag(BoardTestTags.columnCards(column.name)),
                verticalArrangement = Arrangement.spacedBy(AppTheme.space.s8),
            ) {
                column.cards.forEach { card ->
                    CardTile(card, boardKey, labelsById, onAction)
                }
                if (column.cards.isEmpty() && quickAdd == null) NoCardsYet()
                if (quickAdd != null) QuickAddForm(quickAdd, focusRequest, onAction)
            }
        }
    }
}

@Composable
private fun ColumnMenuButton(
    column: Column,
    canMoveLeft: Boolean,
    canMoveRight: Boolean,
    onAction: (BoardAction) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    fun pick(action: BoardAction) {
        expanded = false
        onAction(action)
    }
    Box {
        CardiganIconButton(
            icon = CardiganIcons.More,
            contentDescription = stringResource(Res.string.board_view_column_actions, column.name),
            onClick = { expanded = true },
            size = IconButtonSize.Small,
            selected = expanded,
        )
        Menu(expanded = expanded, onDismissRequest = { expanded = false }) {
            MenuItem(
                text = stringResource(Res.string.board_view_move_left),
                icon = CardiganIcons.ArrowLeft,
                enabled = canMoveLeft,
                onClick = { pick(BoardAction.MoveColumnLeft(column.id)) },
            )
            MenuItem(
                text = stringResource(Res.string.board_view_move_right),
                icon = CardiganIcons.ArrowRight,
                enabled = canMoveRight,
                onClick = { pick(BoardAction.MoveColumnRight(column.id)) },
            )
            MenuItem(
                text = stringResource(Res.string.board_view_rename),
                icon = CardiganIcons.Rename,
                onClick = { pick(BoardAction.RenameColumnClick(column.id)) },
            )
            MenuDivider()
            MenuItem(
                text = stringResource(Res.string.board_view_delete_column),
                icon = CardiganIcons.Trash,
                danger = true,
                onClick = { pick(BoardAction.DeleteColumnClick(column.id)) },
            )
        }
    }
}

@Composable
private fun CardTile(
    card: Card,
    boardKey: String,
    labelsById: Map<Uuid, Label>,
    onAction: (BoardAction) -> Unit,
) {
    val displayId = card.displayId(boardKey)
    val labels = card.labelIds.mapNotNull(labelsById::get).sortedForCard()
    val description = if (labels.isEmpty()) {
        stringResource(Res.string.board_view_card_tile_no_labels, displayId, card.title)
    } else {
        stringResource(Res.string.board_view_card_tile, displayId, card.title, labels.joinToString(", ") { it.name })
    }
    val onClick = { onAction(BoardAction.CardClick(card.id)) }
    BoardCard(
        displayId = displayId,
        title = card.title,
        onClick = onClick,
        // One node for screen readers with the D38 text.
        modifier = Modifier.clearAndSetSemantics {
            contentDescription = description
            role = Role.Button
            onClick { onClick(); true }
        },
        labels = if (labels.isEmpty()) {
            null
        } else {
            { labels.forEach { LabelChip(name = it.name, color = it.color.toPalette()) } }
        },
    )
}

internal fun DomainLabelColor.toPalette(): PaletteColor = PaletteColor.valueOf(name)

@Composable
private fun NoCardsYet() {
    val shape = RoundedCornerShape(AppTheme.radius.r8)
    Text(
        text = stringResource(Res.string.board_view_no_cards),
        modifier = Modifier
            .fillMaxWidth()
            .dashedBorder(AppTheme.space.s1, AppTheme.colors.border.strong, shape, dash = AppTheme.space.s4, gap = AppTheme.space.s3)
            .padding(horizontal = AppTheme.space.s7, vertical = AppTheme.space.s15),
        style = AppTheme.textStyles.sans12_5Regular145,
        color = AppTheme.colors.text.faint,
        textAlign = TextAlign.Center,
    )
}

/** Enter submits, Escape cancels. Pasted line breaks stay in the text and show the error (D33). */
private fun Modifier.inlineFieldKeys(onAction: (BoardAction) -> Unit): Modifier = onPreviewKeyEvent { event ->
    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
    when (event.key) {
        Key.Enter, Key.NumPadEnter -> {
            onAction(BoardAction.InlineSubmit)
            true
        }
        Key.Escape -> {
            onAction(BoardAction.InlineCancel)
            true
        }
        else -> false
    }
}

@Composable
private fun FocusEffect(focusRequest: FocusRequest?, requester: FocusRequester) {
    LaunchedEffect(focusRequest) {
        if (focusRequest != null) runCatching { requester.requestFocus() }
    }
}

@Composable
private fun QuickAddForm(
    field: InlineField,
    focusRequest: FocusRequest?,
    onAction: (BoardAction) -> Unit,
) {
    val requester = remember { FocusRequester() }
    FocusEffect(focusRequest, requester)
    InlineFieldBackHandler(enabled = true) { onAction(BoardAction.InlineCancel) }
    val error = field.error?.toUiText()?.asString()
    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.space.s8)) {
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.space.s6)) {
            TextArea(
                value = field.text,
                onValueChange = { onAction(BoardAction.InlineTextChange(it)) },
                type = TextAreaType.MultiLine,
                isError = error != null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onAction(BoardAction.InlineSubmit) }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(requester)
                    .inlineFieldKeys(onAction)
                    .testTag(BoardTestTags.InlineField),
            )
            if (error != null) ErrorMessage(text = error)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.space.s6), verticalAlignment = Alignment.CenterVertically) {
            CardiganButton(
                text = stringResource(Res.string.board_view_add_card),
                onClick = { onAction(BoardAction.InlineSubmit) },
                enabled = field.canSubmit,
                isLoading = field.isSaving,
            )
            CardiganButton(
                text = stringResource(Res.string.board_view_cancel),
                onClick = { onAction(BoardAction.InlineCancel) },
                style = ButtonStyle.Ghost,
                enabled = !field.isSaving,
            )
        }
    }
}

/** The inline add column field at the end of the strip, drawn like a column (D20, D32). */
@Composable
internal fun AddColumnForm(
    field: InlineField,
    focusRequest: FocusRequest?,
    onAction: (BoardAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val requester = remember { FocusRequester() }
    FocusEffect(focusRequest, requester)
    InlineFieldBackHandler(enabled = true) { onAction(BoardAction.InlineCancel) }
    val shape = RoundedCornerShape(AppTheme.radius.r10)
    val error = field.error?.toUiText()?.asString()
    Column(
        modifier = modifier
            .background(AppTheme.colors.background.column, shape)
            .border(AppTheme.space.s1, AppTheme.colors.border.default, shape)
            .padding(AppTheme.space.s9),
        verticalArrangement = Arrangement.spacedBy(AppTheme.space.s8),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.space.s6)) {
            Input(
                value = field.text,
                onValueChange = { onAction(BoardAction.InlineTextChange(it)) },
                isError = error != null,
                enabled = !field.isSaving,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onAction(BoardAction.InlineSubmit) }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(requester)
                    .inlineFieldKeys(onAction)
                    .testTag(BoardTestTags.InlineField),
            )
            if (error != null) ErrorMessage(text = error)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.space.s6), verticalAlignment = Alignment.CenterVertically) {
            CardiganButton(
                text = stringResource(Res.string.board_view_add_column),
                onClick = { onAction(BoardAction.InlineSubmit) },
                size = ControlSize.Small,
                enabled = field.canSubmit,
                isLoading = field.isSaving,
            )
            CardiganButton(
                text = stringResource(Res.string.board_view_cancel),
                onClick = { onAction(BoardAction.InlineCancel) },
                style = ButtonStyle.Ghost,
                size = ControlSize.Small,
                enabled = !field.isSaving,
            )
        }
    }
}

private val SkeletonTileHeight = 73.dp
private val PhoneSkeletonColumns = listOf(4, 3)
private val DesktopSkeletonColumns = listOf(4, 3, 2, 3)

/** Loading skeleton: columns of placeholder tiles (Figma "Loading"). */
@Composable
internal fun SkeletonStrip(phone: Boolean) {
    val padding = stripPadding(phone)
    val width = if (phone) PhoneColumnWidth else DesktopColumnWidth
    val shape = RoundedCornerShape(AppTheme.radius.r10)
    val tileShape = RoundedCornerShape(AppTheme.radius.r8)
    Row(
        modifier = Modifier
            .padding(padding)
            .testTag(BoardTestTags.Skeleton),
        horizontalArrangement = Arrangement.spacedBy(columnGap(phone)),
    ) {
        (if (phone) PhoneSkeletonColumns else DesktopSkeletonColumns).forEach { tiles ->
            Column(
                modifier = Modifier
                    .width(width)
                    .background(AppTheme.colors.background.column, shape)
                    .border(AppTheme.space.s1, AppTheme.colors.border.default, shape)
                    .padding(AppTheme.space.s9),
                verticalArrangement = Arrangement.spacedBy(AppTheme.space.s8),
            ) {
                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .padding(start = AppTheme.space.s6),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Skeleton(modifier = Modifier.width(80.dp).height(12.dp))
                }
                repeat(tiles) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(SkeletonTileHeight)
                            .background(AppTheme.colors.background.surface, tileShape)
                            .border(AppTheme.space.s1, AppTheme.colors.border.default, tileShape)
                            .padding(horizontal = AppTheme.space.s15, vertical = AppTheme.space.s13),
                        verticalArrangement = Arrangement.spacedBy(AppTheme.space.s7),
                    ) {
                        Skeleton(modifier = Modifier.width(48.dp).height(9.dp))
                        Skeleton(modifier = Modifier.fillMaxWidth(0.9f).height(12.dp))
                        Skeleton(modifier = Modifier.fillMaxWidth(0.6f).height(12.dp))
                    }
                }
            }
        }
    }
}
