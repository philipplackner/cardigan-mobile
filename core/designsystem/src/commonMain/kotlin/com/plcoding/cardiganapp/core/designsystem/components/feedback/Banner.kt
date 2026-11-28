package com.plcoding.cardiganapp.core.designsystem.components.feedback

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.plcoding.cardiganapp.core.designsystem.icons.CardiganIcons
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme

enum class BannerType {
    Error,
    Info,
}

private val BannerIconSize = 16.dp

/** Inline message inside a screen or dialog. Error banners are announced by screen readers. */
@Composable
fun Banner(
    message: String,
    type: BannerType,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(AppTheme.radius.r8)
    val (fill, stroke, iconColor) =
        when (type) {
            BannerType.Error -> Triple(colors.danger.soft, colors.border.dangerSubtle, colors.danger.default)
            BannerType.Info -> Triple(colors.accent.soft, colors.border.accentSubtle, colors.accent.default)
        }
    Row(
        modifier =
            modifier
                .semantics(mergeDescendants = true) {
                    if (type == BannerType.Error) liveRegion = LiveRegionMode.Polite
                }.background(fill, shape)
                .border(AppTheme.space.s1, stroke, shape)
                .padding(horizontal = AppTheme.space.s12, vertical = AppTheme.space.s10),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.space.s9),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            painter = if (type == BannerType.Error) CardiganIcons.Alert else CardiganIcons.Info,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(BannerIconSize),
        )
        Text(
            text = message,
            modifier = Modifier.weight(1f),
            style = AppTheme.textStyles.sans13Regular145,
            color = colors.text.primary,
        )
    }
}

@Composable
private fun BannerSamples() {
    Column(
        modifier =
            Modifier
                .background(AppTheme.colors.background.surface)
                .padding(AppTheme.space.s24)
                .width(332.dp),
        verticalArrangement = Arrangement.spacedBy(AppTheme.space.s12),
    ) {
        Banner(message = "Message", type = BannerType.Error)
        Banner(message = "Message", type = BannerType.Info)
    }
}

@Preview(name = "Banner · Light")
@Composable
private fun BannerLightPreview() {
    AppTheme(darkTheme = false) { BannerSamples() }
}

@Preview(name = "Banner · Dark")
@Composable
private fun BannerDarkPreview() {
    AppTheme(darkTheme = true) { BannerSamples() }
}
