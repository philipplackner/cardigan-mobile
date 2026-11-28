package com.plcoding.cardiganapp.core.designsystem.components.inputs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.plcoding.cardiganapp.core.designsystem.icons.CardiganIcons
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme

private val CheckboxSize = 16.dp
private val CheckboxIconSize = 12.dp
private val CheckboxStroke = 1.5.dp

/**
 * 16dp checkbox. Pass `onCheckedChange = null` when a parent row (like Option) handles the toggle.
 *
 * Built on `toggleable` instead of Material `Checkbox`, which draws its own 18dp box and check
 * mark and can't match the Figma size, radius and icon.
 */
@Composable
fun CardiganCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(AppTheme.radius.r4)
    val toggleModifier =
        if (onCheckedChange != null) {
            Modifier.toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                role = Role.Checkbox,
                indication = ripple(bounded = false, radius = CheckboxSize),
                interactionSource = null,
            )
        } else {
            Modifier
        }
    Box(
        modifier =
            modifier
                .then(toggleModifier)
                .size(CheckboxSize)
                .background(if (checked) colors.accent.default else colors.background.surface, shape)
                .border(CheckboxStroke, if (checked) colors.accent.default else colors.border.strong, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                painter = CardiganIcons.Check,
                contentDescription = null,
                tint = colors.text.onAccent,
                modifier = Modifier.size(CheckboxIconSize),
            )
        }
    }
}

@Composable
private fun CheckboxSamples() {
    Row(
        modifier =
            Modifier
                .background(AppTheme.colors.background.app)
                .padding(AppTheme.space.s24),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.space.s24),
    ) {
        CardiganCheckbox(checked = false, onCheckedChange = {})
        CardiganCheckbox(checked = true, onCheckedChange = {})
    }
}

@Preview(name = "Checkbox · Light")
@Composable
private fun CheckboxLightPreview() {
    AppTheme(darkTheme = false) { CheckboxSamples() }
}

@Preview(name = "Checkbox · Dark")
@Composable
private fun CheckboxDarkPreview() {
    AppTheme(darkTheme = true) { CheckboxSamples() }
}
