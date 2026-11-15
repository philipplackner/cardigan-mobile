package com.plcoding.cardiganapp.core.designsystem.components.inputs

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.plcoding.cardiganapp.core.designsystem.components.interaction.collectIsHighlightedAsState
import com.plcoding.cardiganapp.core.designsystem.components.interaction.isTouchSize
import com.plcoding.cardiganapp.core.designsystem.components.interaction.rememberPreviewInteractionSource
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import com.plcoding.cardiganapp.core.designsystem.theme.WindowClass

/**
 * Checkbox row in the label filter and label picker. The whole row toggles. [label] holds the
 * label instance. Highlighted on pointer hover and keyboard focus.
 */
@Composable
fun Option(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource? = null,
    label: @Composable RowScope.() -> Unit,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isHighlighted by source.collectIsHighlightedAsState()
    val shape = RoundedCornerShape(AppTheme.radius.r5)
    val padding = if (isTouchSize) {
        PaddingValues(horizontal = AppTheme.space.s6, vertical = AppTheme.space.s12)
    } else {
        PaddingValues(AppTheme.space.s8)
    }
    Row(
        modifier = modifier
            .background(if (isHighlighted) AppTheme.colors.background.hover else Color.Transparent, shape)
            .toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                role = Role.Checkbox,
                interactionSource = source,
                indication = ripple(),
            )
            .padding(padding),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.space.s10),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CardiganCheckbox(checked = checked, onCheckedChange = null)
        label()
    }
}

@Composable
private fun OptionSamples() {
    Column(
        modifier = Modifier
            .background(AppTheme.colors.background.surface)
            .padding(AppTheme.space.s16)
            .width(260.dp),
    ) {
        listOf(false, true).forEach { highlighted ->
            Option(
                checked = highlighted,
                onCheckedChange = {},
                interactionSource = rememberPreviewInteractionSource(hovered = highlighted),
            ) {
                Text(text = "Bug", style = AppTheme.textStyles.sans12_5Medium100, color = AppTheme.colors.text.primary)
            }
        }
    }
}

@Preview(name = "Option · Tablet · Light")
@Composable
private fun OptionTabletLightPreview() {
    AppTheme(darkTheme = false, windowClass = WindowClass.Tablet) { OptionSamples() }
}

@Preview(name = "Option · Tablet · Dark")
@Composable
private fun OptionTabletDarkPreview() {
    AppTheme(darkTheme = true, windowClass = WindowClass.Tablet) { OptionSamples() }
}

@Preview(name = "Option · Phone (Touch) · Light")
@Composable
private fun OptionPhoneLightPreview() {
    AppTheme(darkTheme = false, windowClass = WindowClass.Phone) { OptionSamples() }
}

@Preview(name = "Option · Phone (Touch) · Dark")
@Composable
private fun OptionPhoneDarkPreview() {
    AppTheme(darkTheme = true, windowClass = WindowClass.Phone) { OptionSamples() }
}
