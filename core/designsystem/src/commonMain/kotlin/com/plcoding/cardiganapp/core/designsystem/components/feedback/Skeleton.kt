package com.plcoding.cardiganapp.core.designsystem.components.feedback

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme

private const val SHIMMER_MILLIS = 1200

/**
 * Loading placeholder block. Size it to the shape it stands in for and pass a [shape] where the
 * corner radius differs. Shimmers unless the OS asks for reduced motion.
 */
@Composable
fun Skeleton(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(AppTheme.radius.r4),
    reduceMotion: Boolean = rememberReduceMotion(),
) {
    val base = AppTheme.colors.background.skeleton
    if (reduceMotion) {
        Box(modifier = modifier.background(base, shape))
        return
    }
    val highlight = AppTheme.colors.background.skeletonHighlight
    val progress = rememberShimmerProgress()
    Box(
        modifier =
            modifier.drawBehind {
                val shift = size.width * (progress.value * 2f - 1f)
                val brush =
                    Brush.linearGradient(
                        colorStops = arrayOf(0f to base, 0.5f to highlight, 1f to base),
                        start = Offset(shift, 0f),
                        end = Offset(shift + size.width, 0f),
                    )
                drawOutline(shape.createOutline(size, layoutDirection, this), brush)
            },
    )
}

@Composable
private fun rememberShimmerProgress(): State<Float> =
    rememberInfiniteTransition(label = "skeleton").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(SHIMMER_MILLIS, easing = LinearEasing), RepeatMode.Restart),
        label = "skeletonShimmer",
    )

@Composable
private fun SkeletonSamples(reduceMotion: Boolean) {
    Column(
        modifier =
            Modifier
                .background(AppTheme.colors.background.surface)
                .padding(AppTheme.space.s24),
        verticalArrangement = Arrangement.spacedBy(AppTheme.space.s8),
    ) {
        Skeleton(modifier = Modifier.width(120.dp).height(12.dp), reduceMotion = reduceMotion)
        Skeleton(modifier = Modifier.width(200.dp).height(12.dp), reduceMotion = reduceMotion)
        Skeleton(modifier = Modifier.width(80.dp).height(12.dp), reduceMotion = reduceMotion)
    }
}

@Preview(name = "Skeleton · Shimmer · Light")
@Composable
private fun SkeletonShimmerLightPreview() {
    AppTheme(darkTheme = false) { SkeletonSamples(reduceMotion = false) }
}

@Preview(name = "Skeleton · Shimmer · Dark")
@Composable
private fun SkeletonShimmerDarkPreview() {
    AppTheme(darkTheme = true) { SkeletonSamples(reduceMotion = false) }
}

@Preview(name = "Skeleton · Reduced motion · Light")
@Composable
private fun SkeletonStaticLightPreview() {
    AppTheme(darkTheme = false) { SkeletonSamples(reduceMotion = true) }
}

@Preview(name = "Skeleton · Reduced motion · Dark")
@Composable
private fun SkeletonStaticDarkPreview() {
    AppTheme(darkTheme = true) { SkeletonSamples(reduceMotion = true) }
}
