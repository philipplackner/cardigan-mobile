package com.plcoding.cardiganapp.core.designsystem.components.interaction

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import com.plcoding.cardiganapp.core.designsystem.theme.AppShadows
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import com.plcoding.cardiganapp.core.designsystem.theme.WindowClass
import com.plcoding.cardiganapp.core.designsystem.theme.dropShadow

/**
 * True while a pointer hovers over the component or it has keyboard focus. Components show their
 * Figma Highlighted / Hover look from this, never for touch alone.
 */
@Composable
fun InteractionSource.collectIsHighlightedAsState(): State<Boolean> {
    val isHovered by collectIsHoveredAsState()
    val isFocused by collectIsFocusedAsState()
    return remember { derivedStateOf { isHovered || isFocused } }
}

enum class FocusRingStyle {
    Accent,
    Danger,
}

/** Draws the Figma `Focus/Accent` or `Focus/Danger` ring around [shape] while [visible] is true. */
fun Modifier.focusRing(
    visible: Boolean,
    shape: Shape,
    shadows: AppShadows,
    style: FocusRingStyle = FocusRingStyle.Accent,
): Modifier {
    if (!visible) return this
    val ring = when (style) {
        FocusRingStyle.Accent -> shadows.focusAccent
        FocusRingStyle.Danger -> shadows.focusDanger
    }
    return dropShadow(shape, ring)
}

/** Semantic control size. In the phone class it maps to the Figma Touch counterpart. */
enum class ControlSize {
    Small,
    Medium,
}

/** True in the phone class, where components with Default/Touch variants use Touch. */
val isTouchSize: Boolean
    @Composable get() = AppTheme.windowClass == WindowClass.Phone

/** Minimum height for [size]: Small/Medium, or Touch small/Touch in the phone class. */
val ControlSize.minHeight: Dp
    @Composable get() {
        val height = AppTheme.height
        return when (this) {
            ControlSize.Small -> if (isTouchSize) height.controlTouchSm else height.controlSm
            ControlSize.Medium -> if (isTouchSize) height.controlTouch else height.controlMd
        }
    }
