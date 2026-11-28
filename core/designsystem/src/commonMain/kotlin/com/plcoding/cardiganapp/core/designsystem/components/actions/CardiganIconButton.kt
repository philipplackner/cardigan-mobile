package com.plcoding.cardiganapp.core.designsystem.components.actions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.plcoding.cardiganapp.core.designsystem.icons.CardiganIcons
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import com.plcoding.cardiganapp.core.designsystem.theme.dropShadow

enum class IconButtonStyle {
    Ghost,
    Boxed,
}

/** Small sits in column headers, Large in the phone app bar. Not tied to the window class. */
enum class IconButtonSize {
    Small,
    Medium,
    Large,
}

private val BadgeMinWidth = 14.dp

/**
 * Square icon-only button. [selected] marks an open menu or an active toggle. [badge] shows a
 * small count in the top end corner.
 */
@Composable
fun CardiganIconButton(
    icon: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: IconButtonStyle = IconButtonStyle.Ghost,
    size: IconButtonSize = IconButtonSize.Medium,
    selected: Boolean = false,
    badge: String? = null,
) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(AppTheme.radius.r6)
    val containerSize =
        when (size) {
            IconButtonSize.Small -> AppTheme.height.iconButtonSmall
            IconButtonSize.Medium -> AppTheme.height.iconButtonMedium
            IconButtonSize.Large -> AppTheme.height.iconButtonLarge
        }
    val iconSize: Dp = if (size == IconButtonSize.Large) 19.dp else 16.dp
    val containerColor =
        when {
            selected -> colors.background.hover
            style == IconButtonStyle.Boxed -> colors.background.surface
            else -> Color.Transparent
        }
    val contentColor = if (selected) colors.text.primary else colors.text.muted
    val buttonColors =
        IconButtonDefaults.iconButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        )
    val buttonModifier =
        Modifier
            .size(containerSize)
            .semantics {
                this.contentDescription = contentDescription
                this.selected = selected
            }
    val content: @Composable () -> Unit = {
        Icon(painter = icon, contentDescription = null, modifier = Modifier.size(iconSize))
    }

    Box(modifier = modifier) {
        when (style) {
            IconButtonStyle.Ghost -> {
                IconButton(
                    onClick = onClick,
                    modifier = buttonModifier,
                    colors = buttonColors,
                    shape = shape,
                    content = content,
                )
            }

            IconButtonStyle.Boxed -> {
                OutlinedIconButton(
                    onClick = onClick,
                    modifier =
                        Modifier
                            .dropShadow(shape, AppTheme.shadows.small)
                            .then(buttonModifier),
                    shape = shape,
                    colors = buttonColors,
                    border = BorderStroke(AppTheme.space.s1, colors.border.strong),
                    content = content,
                )
            }
        }
        if (badge != null) {
            val inset = AppTheme.space.s3
            val badgePosition =
                if (size == IconButtonSize.Small) {
                    Modifier.offset(x = inset, y = -inset)
                } else {
                    Modifier.padding(top = inset, end = inset)
                }
            Text(
                text = badge,
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .then(badgePosition)
                        .widthIn(min = BadgeMinWidth)
                        .background(colors.accent.default, RoundedCornerShape(AppTheme.radius.r7))
                        .padding(horizontal = AppTheme.space.s3),
                style = AppTheme.textStyles.mono9_5SemiBold147,
                color = colors.text.onAccent,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun IconButtonMatrix() {
    Column(
        modifier =
            Modifier
                .background(AppTheme.colors.background.app)
                .padding(AppTheme.space.s24),
        verticalArrangement = Arrangement.spacedBy(AppTheme.space.s24),
    ) {
        IconButtonStyle.entries.forEach { style ->
            listOf(false, true).forEach { selected ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.space.s32),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButtonSize.entries.forEach { size ->
                        CardiganIconButton(
                            icon = CardiganIcons.More,
                            contentDescription = "More",
                            onClick = {},
                            style = style,
                            size = size,
                            selected = selected,
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Icon button · Light")
@Composable
private fun IconButtonLightPreview() {
    AppTheme(darkTheme = false) { IconButtonMatrix() }
}

@Preview(name = "Icon button · Dark")
@Composable
private fun IconButtonDarkPreview() {
    AppTheme(darkTheme = true) { IconButtonMatrix() }
}

@Composable
private fun IconButtonBadges() {
    Row(
        modifier =
            Modifier
                .background(AppTheme.colors.background.app)
                .padding(AppTheme.space.s24),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.space.s32),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButtonSize.entries.forEach { size ->
            CardiganIconButton(CardiganIcons.Filter, "Filter", {}, size = size, badge = "1")
        }
        IconButtonSize.entries.forEach { size ->
            CardiganIconButton(
                CardiganIcons.Filter,
                "Filter",
                {},
                style = IconButtonStyle.Boxed,
                size = size,
                badge = "12",
            )
        }
    }
}

@Preview(name = "Icon button · Badge · Light")
@Composable
private fun IconButtonBadgeLightPreview() {
    AppTheme(darkTheme = false) { IconButtonBadges() }
}

@Preview(name = "Icon button · Badge · Dark")
@Composable
private fun IconButtonBadgeDarkPreview() {
    AppTheme(darkTheme = true) { IconButtonBadges() }
}
