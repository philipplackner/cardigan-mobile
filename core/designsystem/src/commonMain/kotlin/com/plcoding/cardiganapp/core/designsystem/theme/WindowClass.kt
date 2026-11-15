package com.plcoding.cardiganapp.core.designsystem.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Window classes from the Figma Foundations breakpoints. Layouts are picked from these, never from the device. */
enum class WindowClass {
    Phone,
    Tablet,
    Desktop,
}

/** Breakpoints from Figma Foundations (marked "proposal" there). Change them only here. */
object WindowBreakpoints {
    val TabletMinWidth: Dp = 600.dp
    val DesktopMinWidth: Dp = 840.dp
    val NonPhoneMinHeight: Dp = 480.dp
}

/**
 * Classifies a window. A window narrower than [WindowBreakpoints.TabletMinWidth] or lower than
 * [WindowBreakpoints.NonPhoneMinHeight] is always [WindowClass.Phone].
 */
fun windowClassOf(width: Dp, height: Dp): WindowClass = when {
    width < WindowBreakpoints.TabletMinWidth -> WindowClass.Phone
    height < WindowBreakpoints.NonPhoneMinHeight -> WindowClass.Phone
    width < WindowBreakpoints.DesktopMinWidth -> WindowClass.Tablet
    else -> WindowClass.Desktop
}

val LocalWindowClass = staticCompositionLocalOf { WindowClass.Phone }
