package com.plcoding.cardiganapp.core.designsystem.components.board

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.plcoding.cardiganapp.core.designsystem.components.actions.CardiganIconButton
import com.plcoding.cardiganapp.core.designsystem.components.actions.IconButtonSize
import com.plcoding.cardiganapp.core.designsystem.components.interaction.dashedBorder
import com.plcoding.cardiganapp.core.designsystem.components.interaction.isTouchSize
import com.plcoding.cardiganapp.core.designsystem.components.labels.Label
import com.plcoding.cardiganapp.core.designsystem.components.labels.LabelColor
import com.plcoding.cardiganapp.core.designsystem.icons.CardiganIcons
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import com.plcoding.cardiganapp.core.designsystem.theme.WindowClass

private val DropIndicatorHeight = 3.dp
private val DropKnobSize = 9.dp
private val BoardIconSize = 16.dp
private val ColumnHeaderMinHeight = 30.dp

/** Shows where a dragged card will land: a bar with a knob overhanging its start. */
@Composable
fun DropIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(DropIndicatorHeight)
            .background(AppTheme.colors.accent.default, RoundedCornerShape(AppTheme.radius.r2)),
    ) {
        Box(
            modifier = Modifier
                // The 3dp bar would otherwise clamp the knob's height.
                .wrapContentSize(Alignment.TopStart, unbounded = true)
                .offset(x = (-4).dp, y = (-3).dp)
                .size(DropKnobSize)
                .background(AppTheme.colors.accent.default, CircleShape),
        )
    }
}

/** Opens quick add at the bottom of a column. */
@Composable
fun AddCardButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val touch = isTouchSize
    val shape = RoundedCornerShape(AppTheme.radius.r6)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = AppTheme.space.s6, vertical = if (touch) AppTheme.space.s10 else AppTheme.space.s7),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.space.s7),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(CardiganIcons.Plus, contentDescription = null, tint = AppTheme.colors.text.muted, modifier = Modifier.size(BoardIconSize))
        Text(
            text = text,
            style = if (touch) AppTheme.textStyles.sans14Regular145 else AppTheme.textStyles.sans13Regular145,
            color = AppTheme.colors.text.muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Dashed button after the last column. */
@Composable
fun AddColumnButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(AppTheme.radius.r10)
    Row(
        modifier = modifier
            .dashedBorder(AppTheme.space.s1, AppTheme.colors.border.strong, shape, dash = AppTheme.space.s4, gap = AppTheme.space.s3)
            .clip(shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = AppTheme.space.s12, vertical = AppTheme.space.s11),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.space.s7),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(CardiganIcons.Plus, contentDescription = null, tint = AppTheme.colors.text.muted, modifier = Modifier.size(BoardIconSize))
        Text(
            text = text,
            style = AppTheme.textStyles.sans13Regular145,
            color = AppTheme.colors.text.muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Board column: header with name, card count and [menuButton], cards in [cards], and an Add card
 * button when [addCard] is set. The board view sets the column width.
 *
 * Draws its shape without clipping, so the small header button keeps its 48dp touch area.
 */
@Composable
fun BoardColumn(
    name: String,
    count: String,
    modifier: Modifier = Modifier,
    menuButton: @Composable () -> Unit,
    addCard: (@Composable () -> Unit)? = null,
    cards: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(AppTheme.radius.r10)
    Column(
        modifier = modifier
            .background(AppTheme.colors.background.column, shape)
            .border(AppTheme.space.s1, AppTheme.colors.border.default, shape)
            .padding(AppTheme.space.s8),
        verticalArrangement = Arrangement.spacedBy(AppTheme.space.s8),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = ColumnHeaderMinHeight)
                .padding(PaddingValues(start = AppTheme.space.s6, end = AppTheme.space.s2, top = AppTheme.space.s2, bottom = AppTheme.space.s2)),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.space.s8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = name,
                style = AppTheme.textStyles.sans13_5SemiBold100,
                color = AppTheme.colors.text.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Text(text = count, style = AppTheme.textStyles.mono11_5Regular100, color = AppTheme.colors.text.muted, maxLines = 1)
            Box(modifier = Modifier.weight(1f))
            menuButton()
        }
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.space.s8), content = cards)
        addCard?.invoke()
    }
}

@Composable
private fun ColumnSample() {
    Column(
        modifier = Modifier
            .background(AppTheme.colors.background.app)
            .padding(AppTheme.space.s24),
        verticalArrangement = Arrangement.spacedBy(AppTheme.space.s24),
    ) {
        BoardColumn(
            name = "To do",
            count = "4",
            modifier = Modifier.width(if (isTouchSize) 300.dp else 276.dp),
            menuButton = {
                CardiganIconButton(CardiganIcons.More, contentDescription = "Column menu", onClick = {}, size = IconButtonSize.Small)
            },
            addCard = { AddCardButton(text = "Add card", onClick = {}) },
        ) {
            BoardCard(
                displayId = "CRD-101",
                title = "Single-flight token refresh in the Ktor client",
                onClick = {},
                labels = { Label(name = "api", color = LabelColor.Blue) },
            )
            DropIndicator()
            BoardCard(displayId = "CRD-102", title = "Card title", onClick = {})
        }
        AddColumnButton(text = "Add column", onClick = {}, modifier = Modifier.width(220.dp))
    }
}

@Preview(name = "Column · Tablet · Light")
@Composable
private fun ColumnTabletLightPreview() {
    AppTheme(darkTheme = false, windowClass = WindowClass.Tablet) { ColumnSample() }
}

@Preview(name = "Column · Tablet · Dark")
@Composable
private fun ColumnTabletDarkPreview() {
    AppTheme(darkTheme = true, windowClass = WindowClass.Tablet) { ColumnSample() }
}

@Preview(name = "Column · Phone (Touch) · Light")
@Composable
private fun ColumnPhoneLightPreview() {
    AppTheme(darkTheme = false, windowClass = WindowClass.Phone) { ColumnSample() }
}

@Preview(name = "Column · Phone (Touch) · Dark")
@Composable
private fun ColumnPhoneDarkPreview() {
    AppTheme(darkTheme = true, windowClass = WindowClass.Phone) { ColumnSample() }
}
