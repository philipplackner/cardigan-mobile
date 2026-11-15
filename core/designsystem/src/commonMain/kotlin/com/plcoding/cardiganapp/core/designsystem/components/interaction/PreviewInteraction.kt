package com.plcoding.cardiganapp.core.designsystem.components.interaction

import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember

/**
 * For previews only: an interaction source that reports focus or hover, so the Focus and
 * Highlighted looks can be shown without real input.
 */
@Composable
internal fun rememberPreviewInteractionSource(
    focused: Boolean = false,
    hovered: Boolean = false,
): MutableInteractionSource {
    val source = remember { MutableInteractionSource() }
    LaunchedEffect(source, focused, hovered) {
        if (focused) source.emit(FocusInteraction.Focus())
        if (hovered) source.emit(HoverInteraction.Enter())
    }
    return source
}
