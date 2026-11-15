package com.plcoding.cardiganapp.core.designsystem.components.overlays

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.PopupPositionProvider
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme

/** Which edge of the anchor a menu or popover lines up with. */
enum class PopupAlignment {
    Start,
    End,
}

/**
 * Places a popup [gapPx] below its anchor, lined up with the anchor's start or end edge. Flips
 * above the anchor when there is no room below, and keeps [marginPx] from the window edges.
 */
internal class AnchoredPositionProvider(
    private val alignment: PopupAlignment,
    private val gapPx: Int,
    private val marginPx: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val alignsLeft = (alignment == PopupAlignment.Start) == (layoutDirection == LayoutDirection.Ltr)
        val preferredX = if (alignsLeft) anchorBounds.left else anchorBounds.right - popupContentSize.width
        val maxX = (windowSize.width - popupContentSize.width - marginPx).coerceAtLeast(marginPx)
        val x = preferredX.coerceIn(marginPx, maxX)

        val below = anchorBounds.bottom + gapPx
        val fitsBelow = below + popupContentSize.height <= windowSize.height - marginPx
        val y = if (fitsBelow) below else (anchorBounds.top - gapPx - popupContentSize.height).coerceAtLeast(marginPx)
        return IntOffset(x, y)
    }
}

@Composable
internal fun rememberAnchoredPositionProvider(
    alignment: PopupAlignment,
    gap: Dp = AppTheme.space.s4,
    margin: Dp = AppTheme.space.s8,
): AnchoredPositionProvider {
    val density = LocalDensity.current
    return remember(alignment, gap, margin, density) {
        with(density) { AnchoredPositionProvider(alignment, gap.roundToPx(), margin.roundToPx()) }
    }
}
