package com.plcoding.cardiganapp.core.designsystem.components.overlays

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.plcoding.cardiganapp.core.designsystem.components.inputs.Option
import com.plcoding.cardiganapp.core.designsystem.components.labels.Label
import com.plcoding.cardiganapp.core.designsystem.components.labels.LabelColor
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import com.plcoding.cardiganapp.core.designsystem.theme.dropShadow

/**
 * Anchored panel for the label filter and label picker. Place it in a `Box` with its anchor.
 * The caller sets the width through [modifier] and the [alignment] from the screen design.
 */
@Composable
fun Popover(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    alignment: PopupAlignment,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!expanded) return
    Popup(
        popupPositionProvider = rememberAnchoredPositionProvider(alignment),
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
    ) {
        PopoverSurface(modifier = modifier, content = content)
    }
}

@Composable
private fun PopoverSurface(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(AppTheme.radius.r10)
    Column(
        modifier =
            modifier
                .dropShadow(shape, AppTheme.shadows.popover)
                .background(AppTheme.colors.background.surface, shape)
                .border(AppTheme.space.s1, AppTheme.colors.border.default, shape)
                .padding(AppTheme.space.s1),
        content = content,
    )
}

@Composable
private fun PopoverSample() {
    Box(
        modifier =
            Modifier
                .background(AppTheme.colors.background.app)
                .padding(AppTheme.space.s24),
    ) {
        PopoverSurface(modifier = Modifier.width(280.dp)) {
            listOf("bug", "feature", "docs").forEachIndexed { index, name ->
                Option(checked = index == 0, onCheckedChange = {}) {
                    Label(name = name, color = LabelColor.entries[index])
                }
            }
        }
    }
}

@Preview(name = "Popover · Light")
@Composable
private fun PopoverLightPreview() {
    AppTheme(darkTheme = false) { PopoverSample() }
}

@Preview(name = "Popover · Dark")
@Composable
private fun PopoverDarkPreview() {
    AppTheme(darkTheme = true) { PopoverSample() }
}
