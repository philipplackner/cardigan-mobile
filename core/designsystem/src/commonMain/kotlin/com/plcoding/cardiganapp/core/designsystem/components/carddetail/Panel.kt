package com.plcoding.cardiganapp.core.designsystem.components.carddetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.plcoding.cardiganapp.core.designsystem.components.actions.ButtonStyle
import com.plcoding.cardiganapp.core.designsystem.components.actions.CardiganButton
import com.plcoding.cardiganapp.core.designsystem.components.actions.CardiganIconButton
import com.plcoding.cardiganapp.core.designsystem.components.inputs.Field
import com.plcoding.cardiganapp.core.designsystem.components.inputs.TextArea
import com.plcoding.cardiganapp.core.designsystem.components.inputs.TextAreaType
import com.plcoding.cardiganapp.core.designsystem.icons.CardiganIcons
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import com.plcoding.cardiganapp.core.designsystem.theme.WindowClass
import com.plcoding.cardiganapp.core.designsystem.theme.dropShadow

private val PanelHeaderMinHeight = 56.dp

/**
 * Card detail panel: side panel on tablet ([AppTheme.width] `panelTablet`) and desktop
 * (`panelDesktop`); the caller sets the width. [body] scrolls, so a growing Description field
 * makes the body scroll. Buttons go in [footer]. [columnText] is formatted by the caller.
 */
@Composable
fun Panel(
    cardId: String,
    columnText: String,
    onCloseClick: () -> Unit,
    closeContentDescription: String,
    modifier: Modifier = Modifier,
    footer: @Composable RowScope.() -> Unit,
    body: @Composable ColumnScope.() -> Unit,
) {
    val colors = AppTheme.colors
    val lineColor = colors.border.default
    val lineWidth = AppTheme.space.s1
    Column(
        modifier =
            modifier
                .fillMaxHeight()
                .dropShadow(RectangleShape, AppTheme.shadows.panel)
                .background(colors.background.surface)
                .drawBehind {
                    val x =
                        if (layoutDirection ==
                            LayoutDirection.Ltr
                        ) {
                            lineWidth.toPx() / 2
                        } else {
                            size.width - lineWidth.toPx() / 2
                        }
                    drawLine(lineColor, Offset(x, 0f), Offset(x, size.height), lineWidth.toPx())
                },
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = PanelHeaderMinHeight)
                    .drawBehind {
                        val y = size.height - lineWidth.toPx() / 2
                        drawLine(lineColor, Offset(0f, y), Offset(size.width, y), lineWidth.toPx())
                    }.padding(
                        PaddingValues(
                            start = AppTheme.space.s20,
                            end = AppTheme.space.s12,
                            top = AppTheme.space.s12,
                            bottom = AppTheme.space.s12,
                        ),
                    ),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.space.s6),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = cardId, style = AppTheme.textStyles.mono12_5Regular145, color = colors.text.muted, maxLines = 1)
            Text(
                text = columnText,
                style = AppTheme.textStyles.sans12_5Regular145,
                color = colors.text.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            CardiganIconButton(
                icon = CardiganIcons.Close,
                contentDescription = closeContentDescription,
                onClick = onCloseClick,
            )
        }
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(AppTheme.space.s20),
            verticalArrangement = Arrangement.spacedBy(AppTheme.space.s20),
            content = body,
        )
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        PaddingValues(
                            start = AppTheme.space.s12,
                            end = AppTheme.space.s16,
                            top = AppTheme.space.s12,
                            bottom = AppTheme.space.s12,
                        ),
                    ),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.space.s8),
            verticalAlignment = Alignment.CenterVertically,
            content = footer,
        )
    }
}

private val LongDescription =
    List(12) {
        "When several requests get a 401 at the same time, only one refresh call runs. The others wait for it."
    }.joinToString("\n")

@Composable
private fun PanelSample() {
    Box(modifier = Modifier.background(AppTheme.colors.background.app)) {
        Panel(
            cardId = "CRD-109",
            columnText = "in In progress",
            onCloseClick = {},
            closeContentDescription = "Close",
            modifier = Modifier.width(AppTheme.width.panelTablet),
            footer = {
                CardiganButton(
                    text = "Delete",
                    onClick = {},
                    style = ButtonStyle.DangerGhost,
                    leadingIcon = CardiganIcons.Trash,
                )
                Box(modifier = Modifier.weight(1f))
                CardiganButton(text = "Save", onClick = {})
            },
        ) {
            Field(label = "Title") {
                TextArea(value = "Single-flight token refresh", onValueChange = {
                }, type = TextAreaType.Title, modifier = Modifier.fillMaxWidth())
            }
            Field(label = "Description") {
                TextArea(value = LongDescription, onValueChange = {
                }, type = TextAreaType.Description, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Preview(name = "Panel · Long description · Light", widthDp = 420, heightDp = 900)
@Composable
private fun PanelLightPreview() {
    AppTheme(darkTheme = false, windowClass = WindowClass.Tablet) { PanelSample() }
}

@Preview(name = "Panel · Long description · Dark", widthDp = 420, heightDp = 900)
@Composable
private fun PanelDarkPreview() {
    AppTheme(darkTheme = true, windowClass = WindowClass.Tablet) { PanelSample() }
}
