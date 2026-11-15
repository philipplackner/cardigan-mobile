package com.plcoding.cardiganapp.core.designsystem.components.overlays

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.plcoding.cardiganapp.core.designsystem.components.interaction.collectIsHighlightedAsState
import com.plcoding.cardiganapp.core.designsystem.components.interaction.isTouchSize
import com.plcoding.cardiganapp.core.designsystem.components.interaction.rememberPreviewInteractionSource
import com.plcoding.cardiganapp.core.designsystem.icons.CardiganIcons
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import com.plcoding.cardiganapp.core.designsystem.theme.WindowClass
import com.plcoding.cardiganapp.core.designsystem.theme.dropShadow

private val MenuMinWidth = 208.dp
private val MenuTouchMinWidth = 232.dp
private val MenuIconSize = 16.dp

/**
 * Dropdown menu anchored to the composable it is placed next to (put both in a `Box`). Fill it
 * with [MenuItem], [MenuHeader] and [MenuDivider].
 *
 * Built on `Popup` instead of Material `DropdownMenu`, which forces 8dp vertical padding and
 * Material elevation where Figma has 4dp and `Shadow/Popover`.
 */
@Composable
fun Menu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    alignment: PopupAlignment = PopupAlignment.End,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!expanded) return
    Popup(
        popupPositionProvider = rememberAnchoredPositionProvider(alignment),
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
    ) {
        MenuSurface(modifier = modifier, content = content)
    }
}

@Composable
private fun MenuSurface(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(AppTheme.radius.r8)
    Column(
        modifier = modifier
            .dropShadow(shape, AppTheme.shadows.popover)
            .background(AppTheme.colors.background.surface, shape)
            .border(AppTheme.space.s1, AppTheme.colors.border.default, shape)
            .widthIn(min = if (isTouchSize) MenuTouchMinWidth else MenuMinWidth)
            .width(IntrinsicSize.Max)
            .verticalScroll(rememberScrollState())
            .padding(AppTheme.space.s4),
        verticalArrangement = Arrangement.spacedBy(AppTheme.space.s1),
        content = content,
    )
}

/**
 * One menu entry. [danger] uses the danger color, a disabled item uses `text/faint` and can't be
 * clicked. Highlighted on pointer hover and keyboard focus.
 */
@Composable
fun MenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    danger: Boolean = false,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
) {
    val colors = AppTheme.colors
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isHighlighted by source.collectIsHighlightedAsState()
    val shape = RoundedCornerShape(AppTheme.radius.r5)
    val (iconColor, textColor) = when {
        !enabled -> colors.text.faint to colors.text.faint
        danger -> colors.danger.default to colors.danger.default
        else -> colors.text.muted to colors.text.primary
    }
    val touch = isTouchSize
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(if (isHighlighted && enabled) colors.background.hover else Color.Transparent, shape)
            .clickable(
                interactionSource = source,
                indication = ripple(),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(if (touch) PaddingValues(AppTheme.space.s12) else PaddingValues(horizontal = AppTheme.space.s10, vertical = AppTheme.space.s8)),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.space.s10),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(painter = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(MenuIconSize))
        }
        Text(
            text = text,
            style = if (touch) AppTheme.textStyles.sans15Regular145 else AppTheme.textStyles.sans13_5Regular145,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Non-interactive section title inside a menu. */
@Composable
fun MenuHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    val touch = isTouchSize
    Text(
        text = text,
        modifier = modifier
            .fillMaxWidth()
            .semantics { heading() }
            .padding(
                if (touch) {
                    PaddingValues(AppTheme.space.s12)
                } else {
                    PaddingValues(start = AppTheme.space.s10, end = AppTheme.space.s10, top = AppTheme.space.s8, bottom = AppTheme.space.s6)
                },
            ),
        style = if (touch) AppTheme.textStyles.sans15Regular145 else AppTheme.textStyles.sans12Regular145,
        color = AppTheme.colors.text.muted,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun MenuDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppTheme.space.s2, vertical = AppTheme.space.s4)
            .height(AppTheme.space.s1)
            .background(AppTheme.colors.border.default),
    )
}

@Composable
private fun MenuSample() {
    Box(
        modifier = Modifier
            .background(AppTheme.colors.background.app)
            .padding(AppTheme.space.s24),
    ) {
        MenuSurface {
            MenuHeader(text = "Board")
            MenuItem(text = "Rename", onClick = {}, icon = CardiganIcons.Rename)
            MenuItem(
                text = "Export",
                onClick = {},
                icon = CardiganIcons.Download,
                interactionSource = rememberPreviewInteractionSource(hovered = true),
            )
            MenuItem(text = "Import", onClick = {}, icon = CardiganIcons.Upload, enabled = false)
            MenuDivider()
            MenuItem(text = "Delete", onClick = {}, icon = CardiganIcons.Trash, danger = true)
        }
    }
}

@Preview(name = "Menu · Tablet · Light")
@Composable
private fun MenuTabletLightPreview() {
    AppTheme(darkTheme = false, windowClass = WindowClass.Tablet) { MenuSample() }
}

@Preview(name = "Menu · Tablet · Dark")
@Composable
private fun MenuTabletDarkPreview() {
    AppTheme(darkTheme = true, windowClass = WindowClass.Tablet) { MenuSample() }
}

@Preview(name = "Menu · Phone (Touch) · Light")
@Composable
private fun MenuPhoneLightPreview() {
    AppTheme(darkTheme = false, windowClass = WindowClass.Phone) { MenuSample() }
}

@Preview(name = "Menu · Phone (Touch) · Dark")
@Composable
private fun MenuPhoneDarkPreview() {
    AppTheme(darkTheme = true, windowClass = WindowClass.Phone) { MenuSample() }
}
