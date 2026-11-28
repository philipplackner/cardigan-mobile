package com.plcoding.cardiganapp.core.designsystem.components.actions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.plcoding.cardiganapp.core.designsystem.icons.CardiganIcons
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import com.plcoding.cardiganapp.core.designsystem.theme.dropShadow

private val FilterIconSize = 16.dp

/**
 * Label filter trigger in the board header. It is Active and shows [selectedCount] when one or
 * more labels are selected.
 */
@Composable
fun FilterButton(
    label: String,
    selectedCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val isActive = selectedCount > 0
    val shape = RoundedCornerShape(AppTheme.radius.r6)
    Surface(
        onClick = onClick,
        modifier = modifier.dropShadow(shape, AppTheme.shadows.small),
        shape = shape,
        color = if (isActive) colors.accent.soft else colors.background.surface,
        contentColor = colors.text.muted,
        border = BorderStroke(AppTheme.space.s1, if (isActive) colors.accent.default else colors.border.strong),
    ) {
        Row(
            modifier =
                Modifier
                    .heightIn(min = AppTheme.height.controlMd)
                    .padding(horizontal = AppTheme.space.s11),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.space.s7),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painter = CardiganIcons.Label, contentDescription = null, modifier = Modifier.size(FilterIconSize))
            Text(
                text = label,
                style = AppTheme.textStyles.sans13_5Regular145,
                color = colors.text.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (isActive) {
                Text(
                    text = selectedCount.toString(),
                    modifier =
                        Modifier
                            .background(colors.accent.default, RoundedCornerShape(AppTheme.radius.r4))
                            .padding(horizontal = AppTheme.space.s5, vertical = AppTheme.space.s2),
                    style = AppTheme.textStyles.mono11SemiBold100,
                    color = colors.text.onAccent,
                    maxLines = 1,
                )
            }
            Icon(
                painter = CardiganIcons.ChevronDown,
                contentDescription = null,
                modifier = Modifier.size(FilterIconSize),
            )
        }
    }
}

@Composable
private fun FilterButtonSamples() {
    Row(
        modifier =
            Modifier
                .background(AppTheme.colors.background.app)
                .padding(AppTheme.space.s24),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.space.s24),
    ) {
        FilterButton(label = "Labels", selectedCount = 0, onClick = {})
        FilterButton(label = "Labels", selectedCount = 2, onClick = {})
    }
}

@Preview(name = "Filter button · Light")
@Composable
private fun FilterButtonLightPreview() {
    AppTheme(darkTheme = false) { FilterButtonSamples() }
}

@Preview(name = "Filter button · Dark")
@Composable
private fun FilterButtonDarkPreview() {
    AppTheme(darkTheme = true) { FilterButtonSamples() }
}
