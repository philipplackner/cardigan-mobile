package com.plcoding.cardiganapp.core.designsystem.components.actions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.plcoding.cardiganapp.core.designsystem.components.feedback.Spinner
import com.plcoding.cardiganapp.core.designsystem.components.interaction.ControlSize
import com.plcoding.cardiganapp.core.designsystem.components.interaction.isTouchSize
import com.plcoding.cardiganapp.core.designsystem.components.interaction.minHeight
import com.plcoding.cardiganapp.core.designsystem.icons.CardiganIcons
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import com.plcoding.cardiganapp.core.designsystem.theme.WindowClass
import com.plcoding.cardiganapp.core.designsystem.theme.dropShadow

enum class ButtonStyle {
    Primary,
    Secondary,
    Ghost,
    Danger,
    DangerGhost,
}

internal val ButtonIconSize: Dp = 15.dp
internal const val DISABLED_ALPHA = 0.45f

/**
 * Text button with an optional leading icon. [size] is the semantic size; in the phone class it
 * becomes Touch small / Touch. While [isLoading], a spinner replaces the icon and the button
 * can't be clicked. Use `Modifier.fillMaxWidth()` for full-width buttons.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardiganButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ButtonStyle = ButtonStyle.Primary,
    size: ControlSize = ControlSize.Medium,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: Painter? = null,
) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(AppTheme.radius.r6)
    val containerColor =
        when (style) {
            ButtonStyle.Primary -> colors.accent.default
            ButtonStyle.Secondary -> colors.background.surface
            ButtonStyle.Danger -> colors.danger.default
            ButtonStyle.Ghost, ButtonStyle.DangerGhost -> Color.Transparent
        }
    val contentColor =
        when (style) {
            ButtonStyle.Primary -> colors.text.onAccent
            ButtonStyle.Secondary -> colors.text.primary
            ButtonStyle.Ghost -> colors.text.muted
            ButtonStyle.Danger -> colors.text.onDanger
            ButtonStyle.DangerGhost -> colors.danger.default
        }
    val rippleColor =
        when (style) {
            ButtonStyle.Primary, ButtonStyle.Danger -> colors.text.onAccent
            else -> colors.text.primary
        }
    val border = if (style == ButtonStyle.Secondary) BorderStroke(AppTheme.space.s1, colors.border.strong) else null
    val shadowModifier =
        if (style ==
            ButtonStyle.Secondary
        ) {
            Modifier.dropShadow(shape, AppTheme.shadows.small)
        } else {
            Modifier
        }

    CompositionLocalProvider(LocalRippleConfiguration provides RippleConfiguration(color = rippleColor)) {
        Surface(
            onClick = onClick,
            modifier =
                modifier
                    .alpha(if (enabled) 1f else DISABLED_ALPHA)
                    .then(shadowModifier)
                    .semantics { role = Role.Button },
            enabled = enabled && !isLoading,
            shape = shape,
            color = containerColor,
            contentColor = contentColor,
            border = border,
        ) {
            Row(
                modifier =
                    Modifier
                        .heightIn(min = size.minHeight)
                        .padding(buttonPadding(size)),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.space.s7, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                when {
                    isLoading -> {
                        Spinner()
                    }

                    leadingIcon != null -> {
                        Icon(
                            painter = leadingIcon,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonIconSize),
                        )
                    }
                }
                Text(
                    text = text,
                    style = buttonTextStyle(size),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun buttonPadding(size: ControlSize): PaddingValues {
    val space = AppTheme.space
    val horizontal =
        when {
            isTouchSize -> space.s16
            size == ControlSize.Small -> space.s10
            else -> space.s13
        }
    return PaddingValues(horizontal = horizontal)
}

@Composable
private fun buttonTextStyle(size: ControlSize): TextStyle {
    val styles = AppTheme.textStyles
    return when (size) {
        ControlSize.Small -> if (isTouchSize) styles.sans14Medium100 else styles.sans13Medium100
        ControlSize.Medium -> if (isTouchSize) styles.sans15Medium100 else styles.sans13_5Medium100
    }
}

@Composable
private fun ButtonMatrix() {
    Column(
        modifier =
            Modifier
                .background(AppTheme.colors.background.app)
                .padding(AppTheme.space.s24),
        verticalArrangement = Arrangement.spacedBy(AppTheme.space.s16),
    ) {
        ButtonStyle.entries.forEach { style ->
            listOf(true, false).forEach { enabled ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.space.s24),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ControlSize.entries.forEach { size ->
                        CardiganButton(
                            text = "Button",
                            onClick = {},
                            style = style,
                            size = size,
                            enabled = enabled,
                            leadingIcon = CardiganIcons.Plus,
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Button · Tablet · Light")
@Composable
private fun ButtonTabletLightPreview() {
    AppTheme(darkTheme = false, windowClass = WindowClass.Tablet) { ButtonMatrix() }
}

@Preview(name = "Button · Tablet · Dark")
@Composable
private fun ButtonTabletDarkPreview() {
    AppTheme(darkTheme = true, windowClass = WindowClass.Tablet) { ButtonMatrix() }
}

@Preview(name = "Button · Phone (Touch) · Light")
@Composable
private fun ButtonPhoneLightPreview() {
    AppTheme(darkTheme = false, windowClass = WindowClass.Phone) { ButtonMatrix() }
}

@Preview(name = "Button · Phone (Touch) · Dark")
@Composable
private fun ButtonPhoneDarkPreview() {
    AppTheme(darkTheme = true, windowClass = WindowClass.Phone) { ButtonMatrix() }
}

@Composable
private fun LoadingButtons() {
    Row(
        modifier =
            Modifier
                .background(AppTheme.colors.background.app)
                .padding(AppTheme.space.s24),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.space.s16),
    ) {
        CardiganButton(text = "Saving", onClick = {}, isLoading = true)
        CardiganButton(text = "Saving", onClick = {}, style = ButtonStyle.Secondary, isLoading = true)
    }
}

@Preview(name = "Button · Loading · Light")
@Composable
private fun ButtonLoadingLightPreview() {
    AppTheme(darkTheme = false, windowClass = WindowClass.Tablet) { LoadingButtons() }
}

@Preview(name = "Button · Loading · Dark")
@Composable
private fun ButtonLoadingDarkPreview() {
    AppTheme(darkTheme = true, windowClass = WindowClass.Tablet) { LoadingButtons() }
}

@Preview(name = "Button · Font scale 2x")
@Composable
private fun ButtonFontScalePreview() {
    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
        AppTheme(darkTheme = false, windowClass = WindowClass.Phone) {
            Column(
                modifier =
                    Modifier
                        .background(AppTheme.colors.background.app)
                        .padding(AppTheme.space.s24),
                verticalArrangement = Arrangement.spacedBy(AppTheme.space.s16),
            ) {
                CardiganButton(text = "Create board", onClick = {}, leadingIcon = CardiganIcons.Plus)
                CardiganButton(text = "Cancel", onClick = {}, style = ButtonStyle.Secondary, size = ControlSize.Small)
            }
        }
    }
}
