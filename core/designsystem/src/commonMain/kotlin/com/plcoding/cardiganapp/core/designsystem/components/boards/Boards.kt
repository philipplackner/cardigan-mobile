package com.plcoding.cardiganapp.core.designsystem.components.boards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.plcoding.cardiganapp.core.designsystem.components.actions.CardiganIconButton
import com.plcoding.cardiganapp.core.designsystem.components.datadisplay.Key
import com.plcoding.cardiganapp.core.designsystem.icons.CardiganIcons
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import com.plcoding.cardiganapp.core.designsystem.theme.dropShadow

private val TileTopRowHeight = 26.dp

/**
 * Board in the board list grid (tablet and desktop). [cardCount] and [updated] are formatted by
 * the caller. [menuButton] sits in the top end corner. The grid sets the width.
 *
 * Doesn't clip, so the menu button keeps its 48dp touch area.
 */
@Composable
fun BoardTile(
    name: String,
    boardKey: String,
    cardCount: String,
    updated: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    menuButton: @Composable () -> Unit,
) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(AppTheme.radius.r8)
    val dividerColor = colors.border.default
    val dividerWidth = AppTheme.space.s1
    Box(
        modifier = modifier
            .dropShadow(shape, AppTheme.shadows.small)
            .background(colors.background.surface, shape)
            .border(AppTheme.space.s1, colors.border.default, shape)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaddingValues(start = AppTheme.space.s14, end = AppTheme.space.s14, top = AppTheme.space.s14, bottom = AppTheme.space.s12)),
            verticalArrangement = Arrangement.spacedBy(AppTheme.space.s10),
        ) {
            Row(modifier = Modifier.height(TileTopRowHeight), verticalAlignment = Alignment.CenterVertically) {
                Key(key = boardKey)
            }
            Text(text = name, style = AppTheme.textStyles.sans15SemiBold130LsMinus1, color = colors.text.primary)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        drawLine(dividerColor, Offset(0f, dividerWidth.toPx() / 2), Offset(size.width, dividerWidth.toPx() / 2), dividerWidth.toPx())
                    }
                    .padding(top = AppTheme.space.s10),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.space.s10),
            ) {
                Text(text = cardCount, style = AppTheme.textStyles.mono12Regular100, color = colors.text.muted, maxLines = 1)
                Box(modifier = Modifier.weight(1f))
                Text(text = updated, style = AppTheme.textStyles.mono12Regular100, color = colors.text.muted, maxLines = 1)
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = AppTheme.space.s10, end = AppTheme.space.s8),
        ) {
            menuButton()
        }
    }
}

/** Board in the phone board list, with a divider below. [cardCount] and [updated] are formatted by the caller. */
@Composable
fun BoardRow(
    name: String,
    boardKey: String,
    cardCount: String,
    updated: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    menuButton: @Composable () -> Unit,
) {
    val colors = AppTheme.colors
    val dividerColor = colors.border.default
    val dividerWidth = AppTheme.space.s1
    val meta = AppTheme.textStyles.mono12Regular100
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .drawBehind {
                val y = size.height - dividerWidth.toPx() / 2
                drawLine(dividerColor, Offset(0f, y), Offset(size.width, y), dividerWidth.toPx())
            }
            .padding(PaddingValues(start = AppTheme.space.s14, end = AppTheme.space.s6, top = AppTheme.space.s12, bottom = AppTheme.space.s12)),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.space.s10),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppTheme.space.s6)) {
            Text(
                text = name,
                style = AppTheme.textStyles.sans15_5SemiBold130LsMinus1,
                color = colors.text.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.space.s8), verticalAlignment = Alignment.CenterVertically) {
                Key(key = boardKey)
                Text(text = cardCount, style = meta, color = colors.text.muted, maxLines = 1)
                Text(text = "·", style = meta, color = colors.text.muted)
                Text(text = updated, style = meta, color = colors.text.muted, maxLines = 1)
            }
        }
        menuButton()
    }
}

@Composable
private fun SampleMenuButton() {
    CardiganIconButton(icon = CardiganIcons.More, contentDescription = "Board menu", onClick = {})
}

@Composable
private fun BoardsSamples() {
    Column(
        modifier = Modifier
            .background(AppTheme.colors.background.app)
            .padding(AppTheme.space.s24),
        verticalArrangement = Arrangement.spacedBy(AppTheme.space.s24),
    ) {
        BoardTile(
            name = "Board name",
            boardKey = "CRD",
            cardCount = "12 cards",
            updated = "Updated 2 hours ago",
            onClick = {},
            modifier = Modifier.width(280.dp),
            menuButton = { SampleMenuButton() },
        )
        Column(modifier = Modifier.width(356.dp).background(AppTheme.colors.background.surface)) {
            BoardRow(
                name = "Board name",
                boardKey = "CRD",
                cardCount = "12 cards",
                updated = "2 hours ago",
                onClick = {},
                menuButton = { SampleMenuButton() },
            )
        }
    }
}

@Preview(name = "Board tile and row · Light")
@Composable
private fun BoardsLightPreview() {
    AppTheme(darkTheme = false) { BoardsSamples() }
}

@Preview(name = "Board tile and row · Dark")
@Composable
private fun BoardsDarkPreview() {
    AppTheme(darkTheme = true) { BoardsSamples() }
}
