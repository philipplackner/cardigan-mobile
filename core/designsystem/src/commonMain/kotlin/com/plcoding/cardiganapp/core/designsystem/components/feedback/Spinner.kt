package com.plcoding.cardiganapp.core.designsystem.components.feedback

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme

enum class SpinnerSize(
    internal val diameter: Dp,
    internal val strokeWidth: Dp,
) {
    Small(diameter = 15.dp, strokeWidth = 2.dp),
    Large(diameter = 24.dp, strokeWidth = 2.5.dp),
}

private const val ARC_FRACTION = 0.75f

// Figma draws the ring from 45° to 315°, so the opening faces right.
private const val ARC_START_ROTATION = 135f
private const val ROTATION_MILLIS = 1000

/**
 * Indeterminate progress ring. Small takes the content color of its context (`text/on-accent` in a
 * primary button), Large defaults to `accent/default`.
 */
@Composable
fun Spinner(
    modifier: Modifier = Modifier,
    size: SpinnerSize = SpinnerSize.Small,
    color: Color =
        when (size) {
            SpinnerSize.Small -> LocalContentColor.current
            SpinnerSize.Large -> AppTheme.colors.accent.default
        },
) {
    val rotation by rememberInfiniteTransition(label = "spinner").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(ROTATION_MILLIS, easing = LinearEasing), RepeatMode.Restart),
        label = "spinnerRotation",
    )
    CircularProgressIndicator(
        progress = { ARC_FRACTION },
        modifier =
            modifier
                .size(size.diameter)
                .graphicsLayer { rotationZ = ARC_START_ROTATION + rotation },
        color = color,
        strokeWidth = size.strokeWidth,
        trackColor = Color.Transparent,
        strokeCap = StrokeCap.Butt,
        gapSize = 0.dp,
    )
}

@Composable
private fun SpinnerSamples() {
    Row(
        modifier =
            Modifier
                .background(AppTheme.colors.background.app)
                .padding(AppTheme.space.s24),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.space.s32),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spinner(size = SpinnerSize.Small, color = AppTheme.colors.text.primary)
        Spinner(size = SpinnerSize.Large)
    }
}

@Preview(name = "Spinner · Light")
@Composable
private fun SpinnerLightPreview() {
    AppTheme(darkTheme = false) { SpinnerSamples() }
}

@Preview(name = "Spinner · Dark")
@Composable
private fun SpinnerDarkPreview() {
    AppTheme(darkTheme = true) { SpinnerSamples() }
}
