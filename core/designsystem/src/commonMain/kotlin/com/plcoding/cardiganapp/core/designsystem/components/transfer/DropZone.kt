package com.plcoding.cardiganapp.core.designsystem.components.transfer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.plcoding.cardiganapp.core.designsystem.components.interaction.collectIsHighlightedAsState
import com.plcoding.cardiganapp.core.designsystem.components.interaction.dashedBorder
import com.plcoding.cardiganapp.core.designsystem.icons.CardiganIcons
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import com.plcoding.cardiganapp.core.designsystem.theme.WindowClass

private val DropZoneIconSize = 24.dp
private val DropZoneStrokeWidth = 1.5.dp
private val DropZoneDash = 6.dp
private val DropZoneGap = 4.dp

/**
 * Tap-only drop zone (Figma Drop zone, Default variant). Nothing can be dropped on it; a tap
 * calls [onClick], which opens the file picker. [hintAccent] is the part of [hint] drawn in the
 * accent color.
 */
@Composable
fun DropZone(
    title: String,
    hint: String,
    hintAccent: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(AppTheme.radius.r10)
    val source = remember { MutableInteractionSource() }
    val isHighlighted by source.collectIsHighlightedAsState()
    val hintText = remember(hint, hintAccent, colors.accent.default) {
        val start = hint.indexOf(hintAccent)
        buildAnnotatedString {
            if (hintAccent.isEmpty() || start < 0) {
                append(hint)
            } else {
                append(hint.substring(0, start))
                withStyle(SpanStyle(color = colors.accent.default, fontWeight = FontWeight.Medium)) { append(hintAccent) }
                append(hint.substring(start + hintAccent.length))
            }
        }
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (isHighlighted) colors.background.hover else colors.background.app, shape)
            .dashedBorder(DropZoneStrokeWidth, colors.border.strong, shape, DropZoneDash, DropZoneGap)
            .clickable(interactionSource = source, indication = ripple(), role = Role.Button, onClick = onClick)
            .clearAndSetSemantics { this.contentDescription = contentDescription }
            .padding(horizontal = AppTheme.space.s16, vertical = AppTheme.space.s28),
        verticalArrangement = Arrangement.spacedBy(AppTheme.space.s8, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = CardiganIcons.File,
            contentDescription = null,
            tint = colors.text.muted,
            modifier = Modifier.size(DropZoneIconSize),
        )
        Text(
            text = title,
            style = AppTheme.textStyles.sans14SemiBold145,
            color = colors.text.primary,
            textAlign = TextAlign.Center,
        )
        Text(
            text = hintText,
            style = AppTheme.textStyles.sans13Regular145,
            color = colors.text.muted,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DropZoneSample() {
    Box(
        modifier = Modifier
            .background(AppTheme.colors.background.surface)
            .padding(AppTheme.space.s20),
    ) {
        DropZone(
            title = "Choose a board export",
            hint = "Tap to browse files",
            hintAccent = "browse files",
            contentDescription = "Choose a board export file",
            onClick = {},
            modifier = if (AppTheme.windowClass == WindowClass.Phone) Modifier else Modifier.width(380.dp),
        )
    }
}

@Preview(name = "Drop zone · Phone · Light", widthDp = 390)
@Composable
private fun DropZonePhoneLightPreview() {
    AppTheme(darkTheme = false, windowClass = WindowClass.Phone) { DropZoneSample() }
}

@Preview(name = "Drop zone · Phone · Dark", widthDp = 390)
@Composable
private fun DropZonePhoneDarkPreview() {
    AppTheme(darkTheme = true, windowClass = WindowClass.Phone) { DropZoneSample() }
}

@Preview(name = "Drop zone · Desktop · Light", widthDp = 420)
@Composable
private fun DropZoneDesktopLightPreview() {
    AppTheme(darkTheme = false, windowClass = WindowClass.Desktop) { DropZoneSample() }
}

@Preview(name = "Drop zone · Desktop · Dark", widthDp = 420)
@Composable
private fun DropZoneDesktopDarkPreview() {
    AppTheme(darkTheme = true, windowClass = WindowClass.Desktop) { DropZoneSample() }
}

@Preview(name = "Drop zone · Font scale 2x", widthDp = 390)
@Composable
private fun DropZoneFontScalePreview() {
    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
        AppTheme(darkTheme = false, windowClass = WindowClass.Phone) { DropZoneSample() }
    }
}
