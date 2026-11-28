package com.plcoding.cardiganapp.feature.boards.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.onConsumedWindowInsetsChanged
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection

/** Side and bottom safe-drawing insets that nothing above this point has consumed yet. */
@Composable
internal fun contentInsets(): PaddingValues =
    WindowInsets.safeDrawing
        .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
        .exclude(LocalConsumedInsets.current)
        .asPaddingValues()

private val LocalConsumedInsets = compositionLocalOf { WindowInsets(0, 0, 0, 0) }

/** Makes the insets consumed above this point visible to [contentInsets] below it. */
@Composable
internal fun ProvideConsumedInsets(content: @Composable () -> Unit) {
    var consumed by remember { mutableStateOf(WindowInsets(0, 0, 0, 0)) }
    Box(modifier = Modifier.fillMaxSize().onConsumedWindowInsetsChanged { consumed = it }) {
        CompositionLocalProvider(LocalConsumedInsets provides consumed, content = content)
    }
}

@Composable
internal operator fun PaddingValues.plus(other: PaddingValues): PaddingValues {
    val direction = LocalLayoutDirection.current
    return PaddingValues(
        start = calculateStartPadding(direction) + other.calculateStartPadding(direction),
        top = calculateTopPadding() + other.calculateTopPadding(),
        end = calculateEndPadding(direction) + other.calculateEndPadding(direction),
        bottom = calculateBottomPadding() + other.calculateBottomPadding(),
    )
}
