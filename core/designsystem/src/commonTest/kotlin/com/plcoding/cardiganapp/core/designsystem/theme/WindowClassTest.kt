package com.plcoding.cardiganapp.core.designsystem.theme

import assertk.assertThat
import assertk.assertions.isEqualTo
import androidx.compose.ui.unit.dp
import kotlin.test.Test

/*
 * Break vectors:
 * - width just below / at each breakpoint (600, 840), including fractional dp
 * - height just below / at 480, for widths that would otherwise be tablet or desktop
 * - height rule overriding a very wide window (landscape phone)
 * - zero size (window not measured yet)
 * - tall but narrow window (height must not promote a phone)
 */
class WindowClassTest {

    private val tallHeight = 1000.dp

    @Test
    fun `zero size is phone`() {
        assertThat(windowClassOf(0.dp, 0.dp)).isEqualTo(WindowClass.Phone)
    }

    @Test
    fun `width just below 600 is phone`() {
        assertThat(windowClassOf(599.9.dp, tallHeight)).isEqualTo(WindowClass.Phone)
    }

    @Test
    fun `width exactly 600 is tablet`() {
        assertThat(windowClassOf(600.dp, tallHeight)).isEqualTo(WindowClass.Tablet)
    }

    @Test
    fun `width just below 840 is tablet`() {
        assertThat(windowClassOf(839.9.dp, tallHeight)).isEqualTo(WindowClass.Tablet)
    }

    @Test
    fun `width exactly 840 is desktop`() {
        assertThat(windowClassOf(840.dp, tallHeight)).isEqualTo(WindowClass.Desktop)
    }

    @Test
    fun `tablet portrait 834 wide is tablet`() {
        assertThat(windowClassOf(834.dp, 1194.dp)).isEqualTo(WindowClass.Tablet)
    }

    @Test
    fun `tablet landscape 1194 wide is desktop`() {
        assertThat(windowClassOf(1194.dp, 834.dp)).isEqualTo(WindowClass.Desktop)
    }

    @Test
    fun `split screen 500 wide on a tablet is phone`() {
        assertThat(windowClassOf(500.dp, 834.dp)).isEqualTo(WindowClass.Phone)
    }

    @Test
    fun `tall narrow window stays phone`() {
        assertThat(windowClassOf(390.dp, 2000.dp)).isEqualTo(WindowClass.Phone)
    }

    @Test
    fun `landscape phone 844 by 390 is phone`() {
        assertThat(windowClassOf(844.dp, 390.dp)).isEqualTo(WindowClass.Phone)
    }

    @Test
    fun `height just below 480 overrides a tablet width`() {
        assertThat(windowClassOf(700.dp, 479.9.dp)).isEqualTo(WindowClass.Phone)
    }

    @Test
    fun `height just below 480 overrides a desktop width`() {
        assertThat(windowClassOf(2000.dp, 479.9.dp)).isEqualTo(WindowClass.Phone)
    }

    @Test
    fun `height exactly 480 with desktop width is desktop`() {
        assertThat(windowClassOf(900.dp, 480.dp)).isEqualTo(WindowClass.Desktop)
    }

    @Test
    fun `height exactly 480 with tablet width is tablet`() {
        assertThat(windowClassOf(700.dp, 480.dp)).isEqualTo(WindowClass.Tablet)
    }
}
