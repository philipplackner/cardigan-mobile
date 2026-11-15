package com.plcoding.cardiganapp.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Figma Spacing variables. `space/12` → `s12`. */
@Immutable
object AppSpacing {
    val s1: Dp = 1.dp
    val s2: Dp = 2.dp
    val s3: Dp = 3.dp
    val s4: Dp = 4.dp
    val s5: Dp = 5.dp
    val s6: Dp = 6.dp
    val s7: Dp = 7.dp
    val s8: Dp = 8.dp
    val s9: Dp = 9.dp
    val s10: Dp = 10.dp
    val s11: Dp = 11.dp
    val s12: Dp = 12.dp
    val s13: Dp = 13.dp
    val s14: Dp = 14.dp
    val s15: Dp = 15.dp
    val s16: Dp = 16.dp
    val s18: Dp = 18.dp
    val s20: Dp = 20.dp
    val s22: Dp = 22.dp
    val s24: Dp = 24.dp
    val s25: Dp = 25.dp
    val s28: Dp = 28.dp
    val s32: Dp = 32.dp
}

/** Figma Radius variables. `radius/8` → `r8`, `radius/full` → `full`. */
@Immutable
object AppRadius {
    val r1: Dp = 1.dp
    val r2: Dp = 2.dp
    val r4: Dp = 4.dp
    val r5: Dp = 5.dp
    val r6: Dp = 6.dp
    val r7: Dp = 7.dp
    val r8: Dp = 8.dp
    val r10: Dp = 10.dp
    val r12: Dp = 12.dp
    val r16: Dp = 16.dp
    val full: Dp = 999.dp
}

/** Figma Size variables that start with `height/`. Used as minimum heights so controls can grow with large text. */
@Immutable
object AppHeight {
    val controlSm: Dp = 30.dp
    val controlMd: Dp = 34.dp
    val controlTouchSm: Dp = 38.dp
    val controlTouch: Dp = 44.dp
    val input: Dp = 36.dp
    val iconButtonSmall: Dp = 28.dp
    val iconButtonMedium: Dp = 32.dp
    val iconButtonLarge: Dp = 40.dp
}

/** Figma Size variables that start with `width/`. */
@Immutable
object AppWidth {
    val sidebarTablet: Dp = 224.dp
    val sidebarDesktop: Dp = 248.dp
    val panelTablet: Dp = 420.dp
    val panelDesktop: Dp = 452.dp
}
