package com.plcoding.cardiganapp.core.designsystem.components.feedback

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.plcoding.cardiganapp.core.designsystem.components.actions.ButtonStyle
import com.plcoding.cardiganapp.core.designsystem.components.actions.CardiganButton
import com.plcoding.cardiganapp.core.designsystem.icons.CardiganIcons
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import com.plcoding.cardiganapp.core.designsystem.theme.WindowClass
import com.plcoding.cardiganapp.core.designsystem.theme.dropShadow

enum class EmptyStateType {
    Default,
    Error,
}

private val IconBoxSize = 44.dp
private val EmptyStateIconSize = 20.dp
private val DescriptionMaxWidth = 322.dp

/** Empty, error and zero-result states. Put buttons in [actions]. */
@Composable
fun EmptyState(
    icon: Painter,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    type: EmptyStateType = EmptyStateType.Default,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    val colors = AppTheme.colors
    val iconBoxShape = RoundedCornerShape(AppTheme.radius.r10)
    Column(
        modifier = modifier.padding(AppTheme.space.s32),
        verticalArrangement = Arrangement.spacedBy(AppTheme.space.s10, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(IconBoxSize)
                .dropShadow(iconBoxShape, AppTheme.shadows.small)
                .background(colors.background.surface, iconBoxShape)
                .border(AppTheme.space.s1, colors.border.default, iconBoxShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = if (type == EmptyStateType.Error) colors.danger.default else colors.text.muted,
                modifier = Modifier.size(EmptyStateIconSize),
            )
        }
        Text(
            text = title,
            modifier = Modifier.padding(top = AppTheme.space.s6),
            style = AppTheme.textStyles.sans15SemiBold130,
            color = colors.text.primary,
            textAlign = TextAlign.Center,
        )
        Text(
            text = description,
            modifier = Modifier.widthIn(max = DescriptionMaxWidth),
            style = AppTheme.textStyles.sans13_5Regular145,
            color = colors.text.muted,
            textAlign = TextAlign.Center,
        )
        if (actions != null) {
            Row(
                modifier = Modifier.padding(top = AppTheme.space.s6),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.space.s8),
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }
    }
}

@Composable
private fun EmptyStateSamples() {
    Column(modifier = Modifier.background(AppTheme.colors.background.app)) {
        EmptyState(
            icon = CardiganIcons.Board,
            title = "Title",
            description = "Description",
            modifier = Modifier.fillMaxWidth(),
            actions = {
                CardiganButton(text = "Button", onClick = {}, leadingIcon = CardiganIcons.Plus)
            },
        )
        EmptyState(
            icon = CardiganIcons.Board,
            title = "Title",
            description = "Description",
            modifier = Modifier.fillMaxWidth(),
            type = EmptyStateType.Error,
            actions = {
                CardiganButton(text = "Button", onClick = {}, style = ButtonStyle.Secondary, leadingIcon = CardiganIcons.Refresh)
            },
        )
    }
}

@Preview(name = "Empty state · Tablet · Light", widthDp = 360)
@Composable
private fun EmptyStateTabletLightPreview() {
    AppTheme(darkTheme = false, windowClass = WindowClass.Tablet) { EmptyStateSamples() }
}

@Preview(name = "Empty state · Tablet · Dark", widthDp = 360)
@Composable
private fun EmptyStateTabletDarkPreview() {
    AppTheme(darkTheme = true, windowClass = WindowClass.Tablet) { EmptyStateSamples() }
}

@Preview(name = "Empty state · Phone · Light", widthDp = 390)
@Composable
private fun EmptyStatePhoneLightPreview() {
    AppTheme(darkTheme = false, windowClass = WindowClass.Phone) { EmptyStateSamples() }
}
