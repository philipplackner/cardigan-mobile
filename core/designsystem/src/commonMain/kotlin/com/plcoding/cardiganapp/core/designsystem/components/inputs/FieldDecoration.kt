package com.plcoding.cardiganapp.core.designsystem.components.inputs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import com.plcoding.cardiganapp.core.designsystem.components.interaction.FocusRingStyle
import com.plcoding.cardiganapp.core.designsystem.components.interaction.focusRing
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme

internal const val INPUT_DISABLED_ALPHA = 0.55f

/** Container look shared by Input and Text area: fill, 1dp inside border and focus ring. */
@Composable
internal fun Modifier.fieldContainer(
    isFocused: Boolean,
    isError: Boolean,
    readOnly: Boolean,
    enabled: Boolean,
): Modifier {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(AppTheme.radius.r6)
    val showFocus = isFocused && !readOnly && enabled
    val borderColor =
        when {
            isError -> colors.danger.default
            showFocus -> colors.accent.default
            else -> colors.border.strong
        }
    return this
        .alpha(if (enabled) 1f else INPUT_DISABLED_ALPHA)
        .focusRing(
            visible = isError || showFocus,
            shape = shape,
            shadows = AppTheme.shadows,
            style = if (isError) FocusRingStyle.Danger else FocusRingStyle.Accent,
        ).background(if (readOnly) colors.background.column else colors.background.surface, shape)
        .border(AppTheme.space.s1, borderColor, shape)
}
