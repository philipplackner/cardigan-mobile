package com.plcoding.cardiganapp.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp

internal val LocalAppColors = staticCompositionLocalOf { LightColors }
internal val LocalAppShadows = staticCompositionLocalOf { appShadows(LightColors) }
internal val LocalAppTextStyles =
    staticCompositionLocalOf<AppTextStyles> { error("No AppTheme around this composable") }
internal val LocalAppTypography =
    staticCompositionLocalOf<AppTypography> { error("No AppTheme around this composable") }

/**
 * The app theme. Every screen and preview is wrapped in it.
 *
 * @param darkTheme follows the system setting. Only previews pass it explicitly.
 * @param windowClass replaces the measured window class. Only previews pass it, to show the
 * tablet and desktop variants.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    windowClass: WindowClass? = null,
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val shadows = remember(colors) { appShadows(colors) }
    val colorScheme = remember(colors, darkTheme) { colors.toColorScheme(darkTheme) }
    val resolvedWindowClass = windowClass ?: measuredWindowClass()
    val textStyles = rememberAppTextStyles()
    val typography = remember(textStyles, resolvedWindowClass) { appTypography(textStyles, resolvedWindowClass) }
    val materialTypography = remember(typography) { typography.toMaterialTypography() }

    MaterialTheme(colorScheme = colorScheme, typography = materialTypography) {
        CompositionLocalProvider(
            LocalAppColors provides colors,
            LocalAppShadows provides shadows,
            LocalAppTextStyles provides textStyles,
            LocalAppTypography provides typography,
            LocalWindowClass provides resolvedWindowClass,
            // Keeps Figma visual sizes; the 48dp touch area comes from hit testing instead.
            LocalMinimumInteractiveComponentSize provides Dp.Unspecified,
            LocalRippleConfiguration provides RippleConfiguration(color = colors.text.primary),
            content = content,
        )
    }
}

@Composable
private fun measuredWindowClass(): WindowClass {
    val size = LocalWindowInfo.current.containerDpSize
    return windowClassOf(width = size.width, height = size.height)
}

/** Entry point for theme values: `AppTheme.colors.text.primary`, `AppTheme.space.s12`, ... */
object AppTheme {
    val colors: AppColors
        @Composable @ReadOnlyComposable
        get() = LocalAppColors.current

    val shadows: AppShadows
        @Composable @ReadOnlyComposable
        get() = LocalAppShadows.current

    val textStyles: AppTextStyles
        @Composable @ReadOnlyComposable
        get() = LocalAppTextStyles.current

    val typography: AppTypography
        @Composable @ReadOnlyComposable
        get() = LocalAppTypography.current

    val windowClass: WindowClass
        @Composable @ReadOnlyComposable
        get() = LocalWindowClass.current

    val space: AppSpacing get() = AppSpacing
    val radius: AppRadius get() = AppRadius
    val height: AppHeight get() = AppHeight
    val width: AppWidth get() = AppWidth
}

private fun AppColors.toColorScheme(darkTheme: Boolean): ColorScheme {
    val base = if (darkTheme) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accent.default,
        onPrimary = text.onAccent,
        primaryContainer = accent.soft,
        onPrimaryContainer = accent.default,
        inversePrimary = accent.default,
        secondary = accent.default,
        onSecondary = text.onAccent,
        secondaryContainer = accent.soft,
        onSecondaryContainer = accent.default,
        tertiary = accent.default,
        onTertiary = text.onAccent,
        tertiaryContainer = accent.soft,
        onTertiaryContainer = accent.default,
        background = background.app,
        onBackground = text.primary,
        surface = background.surface,
        onSurface = text.primary,
        surfaceVariant = background.hover,
        onSurfaceVariant = text.muted,
        surfaceTint = background.surface,
        inverseSurface = background.toast,
        inverseOnSurface = text.toast,
        error = danger.default,
        onError = text.onDanger,
        errorContainer = danger.soft,
        onErrorContainer = danger.default,
        outline = border.strong,
        outlineVariant = border.default,
        scrim = background.scrim,
        surfaceBright = background.surface,
        surfaceDim = background.surface,
        surfaceContainer = background.surface,
        surfaceContainerHigh = background.surface,
        surfaceContainerHighest = background.surface,
        surfaceContainerLow = background.surface,
        surfaceContainerLowest = background.surface,
    )
}
