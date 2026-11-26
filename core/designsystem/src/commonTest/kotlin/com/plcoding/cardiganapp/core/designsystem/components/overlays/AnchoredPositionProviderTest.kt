package com.plcoding.cardiganapp.core.designsystem.components.overlays

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

/*
 * Break vectors:
 * - alignment x layout direction: Start/End in LTR and RTL pick the left or the right anchor edge
 * - horizontal clamping: popup past the start margin, past the end margin, exactly on each margin,
 *   and wider than the window (the end limit falls below the start margin)
 * - vertical placement: fits below with room to spare, fits below exactly at the bottom margin,
 *   one pixel too tall (flips above), flipped popup clamped to the top margin, and exactly on it
 * - gap and margin differ, so a swapped or dropped term changes the result
 */
class AnchoredPositionProviderTest {

    private val window = IntSize(400, 800)
    private val popup = IntSize(100, 200)

    private fun position(
        anchor: IntRect,
        alignment: PopupAlignment = PopupAlignment.Start,
        direction: LayoutDirection = LayoutDirection.Ltr,
        popupSize: IntSize = popup,
    ): IntOffset = AnchoredPositionProvider(alignment, gapPx = GAP, marginPx = MARGIN)
        .calculatePosition(anchor, window, direction, popupSize)

    private val anchor = IntRect(left = 150, top = 100, right = 250, bottom = 140)

    @Test
    fun startInLtrLinesUpWithTheLeftEdgeBelowTheAnchor() {
        assertThat(position(anchor)).isEqualTo(IntOffset(150, 144))
    }

    @Test
    fun endInLtrLinesUpWithTheRightEdge() {
        assertThat(position(anchor, PopupAlignment.End)).isEqualTo(IntOffset(150, 144))
        val wide = IntRect(left = 100, top = 100, right = 300, bottom = 140)
        assertThat(position(wide, PopupAlignment.End)).isEqualTo(IntOffset(200, 144))
    }

    @Test
    fun rtlSwapsTheEdges() {
        val wide = IntRect(left = 100, top = 100, right = 300, bottom = 140)
        assertThat(position(wide, PopupAlignment.Start, LayoutDirection.Rtl)).isEqualTo(IntOffset(200, 144))
        assertThat(position(wide, PopupAlignment.End, LayoutDirection.Rtl)).isEqualTo(IntOffset(100, 144))
    }

    @Test
    fun popupPastTheStartIsClampedToTheMargin() {
        val nearStart = IntRect(left = 3, top = 100, right = 50, bottom = 140)
        assertThat(position(nearStart).x).isEqualTo(MARGIN)
    }

    @Test
    fun popupExactlyOnTheStartMarginStays() {
        val onMargin = IntRect(left = MARGIN, top = 100, right = 50, bottom = 140)
        assertThat(position(onMargin).x).isEqualTo(MARGIN)
        val justInside = IntRect(left = MARGIN + 1, top = 100, right = 50, bottom = 140)
        assertThat(position(justInside).x).isEqualTo(MARGIN + 1)
    }

    @Test
    fun popupPastTheEndIsClampedToTheEndMargin() {
        val nearEnd = IntRect(left = 350, top = 100, right = 390, bottom = 140)
        assertThat(position(nearEnd).x).isEqualTo(400 - 100 - MARGIN)
    }

    @Test
    fun popupExactlyOnTheEndMarginStays() {
        val onMargin = IntRect(left = 292, top = 100, right = 330, bottom = 140)
        assertThat(position(onMargin).x).isEqualTo(292)
        val justInside = IntRect(left = 291, top = 100, right = 330, bottom = 140)
        assertThat(position(justInside).x).isEqualTo(291)
    }

    @Test
    fun popupWiderThanTheWindowStartsAtTheMargin() {
        assertThat(position(anchor, popupSize = IntSize(395, 200)).x).isEqualTo(MARGIN)
        assertThat(position(anchor, PopupAlignment.End, popupSize = IntSize(395, 200)).x).isEqualTo(MARGIN)
    }

    @Test
    fun popupThatExactlyFitsBelowStaysBelow() {
        val bottom = 800 - MARGIN - 200 - GAP
        val low = IntRect(left = 150, top = bottom - 40, right = 250, bottom = bottom)
        assertThat(position(low).y).isEqualTo(bottom + GAP)
    }

    @Test
    fun popupOnePixelTooTallFlipsAboveTheAnchor() {
        val bottom = 800 - MARGIN - 200 - GAP + 1
        val low = IntRect(left = 150, top = bottom - 40, right = 250, bottom = bottom)
        assertThat(position(low).y).isEqualTo(low.top - GAP - 200)
    }

    @Test
    fun flippedPopupIsClampedToTheTopMargin() {
        val tall = IntSize(100, 700)
        assertThat(position(anchor, popupSize = tall).y).isEqualTo(MARGIN)
    }

    @Test
    fun flippedPopupExactlyOnTheTopMarginStays() {
        val tall = IntSize(100, 600)
        val top = MARGIN + GAP + 600
        val low = IntRect(left = 150, top = top, right = 250, bottom = top + 40)
        assertThat(position(low, popupSize = tall).y).isEqualTo(MARGIN)
        val lower = IntRect(left = 150, top = top + 1, right = 250, bottom = top + 41)
        assertThat(position(lower, popupSize = tall).y).isEqualTo(MARGIN + 1)
    }

    private companion object {
        const val GAP = 4
        const val MARGIN = 8
    }
}
