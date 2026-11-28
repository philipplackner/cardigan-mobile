package com.plcoding.cardiganapp.core.designsystem.components.board

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.plcoding.cardiganapp.core.designsystem.components.interaction.dashedBorder
import com.plcoding.cardiganapp.core.designsystem.components.interaction.isTouchSize
import com.plcoding.cardiganapp.core.designsystem.components.labels.Label
import com.plcoding.cardiganapp.core.designsystem.components.labels.LabelColor
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import com.plcoding.cardiganapp.core.designsystem.theme.WindowClass
import com.plcoding.cardiganapp.core.designsystem.theme.dropShadow

enum class BoardCardState {
    Default,
    Selected,

    /** The card under the finger while it is dragged: tilted 2.2° with `Shadow/Drag`. */
    Dragging,

    /** The gap where the dragged card will land. Keeps the card's size, hides its content. */
    Placeholder,
}

private const val DRAGGING_ROTATION = 2.2f
private val PlaceholderStroke = 1.5.dp

/**
 * Card tile in a column: display ID, title and Small labels in [labels]. Only the visual states;
 * the board view wires dragging and the accessibility move actions.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BoardCard(
    displayId: String,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    state: BoardCardState = BoardCardState.Default,
    labels: (@Composable FlowRowScope.() -> Unit)? = null,
) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(AppTheme.radius.r8)
    val touch = isTouchSize
    val container =
        when (state) {
            BoardCardState.Placeholder -> {
                Modifier.dashedBorder(
                    width = PlaceholderStroke,
                    color = colors.border.strong,
                    shape = shape,
                    dash = AppTheme.space.s5,
                    gap = AppTheme.space.s4,
                )
            }

            else -> {
                Modifier
                    .graphicsLayer { rotationZ = if (state == BoardCardState.Dragging) DRAGGING_ROTATION else 0f }
                    .dropShadow(
                        shape,
                        when (state) {
                            BoardCardState.Dragging -> AppTheme.shadows.drag
                            BoardCardState.Selected -> AppTheme.shadows.focusAccent
                            else -> AppTheme.shadows.small
                        },
                    ).background(colors.background.surface, shape)
                    .border(
                        BorderStroke(
                            AppTheme.space.s1,
                            if (state == BoardCardState.Default) colors.border.default else colors.accent.default,
                        ),
                        shape,
                    ).clip(shape)
                    .clickable(onClick = onClick)
            }
        }
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .then(container)
                .padding(
                    if (touch) {
                        PaddingValues(horizontal = AppTheme.space.s14, vertical = AppTheme.space.s12)
                    } else {
                        PaddingValues(
                            start = AppTheme.space.s12,
                            end = AppTheme.space.s12,
                            top = AppTheme.space.s10,
                            bottom = AppTheme.space.s11,
                        )
                    },
                ).alpha(if (state == BoardCardState.Placeholder) 0f else 1f),
        verticalArrangement = Arrangement.spacedBy(AppTheme.space.s7),
    ) {
        Text(text = displayId, style = AppTheme.textStyles.mono11Medium100, color = colors.text.muted, maxLines = 1)
        Text(
            text = title,
            style = if (touch) AppTheme.textStyles.sans14_5Medium138 else AppTheme.textStyles.sans13_5Medium138,
            color = colors.text.primary,
        )
        if (labels != null) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(AppTheme.space.s10),
                verticalArrangement = Arrangement.spacedBy(AppTheme.space.s4),
                content = labels,
            )
        }
    }
}

@Composable
private fun CardStates() {
    Column(
        modifier =
            Modifier
                .background(AppTheme.colors.background.column)
                .padding(AppTheme.space.s24)
                .width(258.dp),
        verticalArrangement = Arrangement.spacedBy(AppTheme.space.s16),
    ) {
        BoardCardState.entries.forEach { state ->
            BoardCard(
                displayId = "CRD-101",
                title = "Card title",
                onClick = {},
                state = state,
                labels = {
                    Label(name = "api", color = LabelColor.Blue)
                    Label(name = "auth", color = LabelColor.Orange)
                },
            )
        }
    }
}

@Preview(name = "Card · Tablet · Light")
@Composable
private fun CardTabletLightPreview() {
    AppTheme(darkTheme = false, windowClass = WindowClass.Tablet) { CardStates() }
}

@Preview(name = "Card · Tablet · Dark")
@Composable
private fun CardTabletDarkPreview() {
    AppTheme(darkTheme = true, windowClass = WindowClass.Tablet) { CardStates() }
}

@Preview(name = "Card · Phone (Touch) · Light")
@Composable
private fun CardPhoneLightPreview() {
    AppTheme(darkTheme = false, windowClass = WindowClass.Phone) { CardStates() }
}

@Preview(name = "Card · Phone (Touch) · Dark")
@Composable
private fun CardPhoneDarkPreview() {
    AppTheme(darkTheme = true, windowClass = WindowClass.Phone) { CardStates() }
}
