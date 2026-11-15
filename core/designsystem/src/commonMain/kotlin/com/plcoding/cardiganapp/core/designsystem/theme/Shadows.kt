package com.plcoding.cardiganapp.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

/** One Figma effect style: one or more drop shadow layers, in Figma order. */
@Immutable
data class AppShadow(val layers: List<Shadow>)

/** Figma effect styles under `Shadow/` and `Focus/`, with colors from the active theme. */
@Immutable
data class AppShadows(
    val small: AppShadow,
    val popover: AppShadow,
    val drag: AppShadow,
    val panel: AppShadow,
    val raised: AppShadow,
    val focusAccent: AppShadow,
    val focusDanger: AppShadow,
)

internal fun appShadows(colors: AppColors): AppShadows = AppShadows(
    small = AppShadow(
        listOf(Shadow(radius = 2.dp, color = colors.shadow.small, offset = DpOffset(0.dp, 1.dp))),
    ),
    popover = AppShadow(
        listOf(
            Shadow(radius = 30.dp, color = colors.shadow.popover1, offset = DpOffset(0.dp, 10.dp)),
            Shadow(radius = 6.dp, color = colors.shadow.popover2, offset = DpOffset(0.dp, 2.dp)),
        ),
    ),
    drag = AppShadow(
        listOf(
            Shadow(radius = 30.dp, color = colors.shadow.drag1, offset = DpOffset(0.dp, 14.dp)),
            Shadow(radius = 6.dp, color = colors.shadow.drag2, offset = DpOffset(0.dp, 2.dp)),
        ),
    ),
    panel = AppShadow(
        listOf(Shadow(radius = 32.dp, color = colors.shadow.panel, offset = DpOffset((-12).dp, 0.dp))),
    ),
    raised = AppShadow(
        listOf(Shadow(radius = 8.dp, color = colors.shadow.raised, offset = DpOffset(0.dp, 1.dp))),
    ),
    focusAccent = AppShadow(
        listOf(Shadow(radius = 0.dp, color = colors.accent.soft, spread = 3.dp)),
    ),
    focusDanger = AppShadow(
        listOf(Shadow(radius = 0.dp, color = colors.danger.soft, spread = 3.dp)),
    ),
)

/** Draws every layer of [shadow] behind the content, in the geometry of [shape]. */
fun Modifier.dropShadow(shape: Shape, shadow: AppShadow): Modifier =
    shadow.layers.fold(this) { modifier, layer -> modifier.dropShadow(shape, layer) }
