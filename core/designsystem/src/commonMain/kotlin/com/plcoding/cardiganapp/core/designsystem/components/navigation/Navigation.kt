package com.plcoding.cardiganapp.core.designsystem.components.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.plcoding.cardiganapp.core.designsystem.components.actions.CardiganButton
import com.plcoding.cardiganapp.core.designsystem.components.actions.CardiganIconButton
import com.plcoding.cardiganapp.core.designsystem.components.actions.FilterButton
import com.plcoding.cardiganapp.core.designsystem.components.brand.Brand
import com.plcoding.cardiganapp.core.designsystem.components.brand.BrandSize
import com.plcoding.cardiganapp.core.designsystem.components.datadisplay.Avatar
import com.plcoding.cardiganapp.core.designsystem.components.datadisplay.Key
import com.plcoding.cardiganapp.core.designsystem.icons.CardiganIcons
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import com.plcoding.cardiganapp.core.designsystem.theme.WindowClass
import com.plcoding.cardiganapp.core.designsystem.theme.dropShadow

private val NavIconSize = 16.dp
private val PageHeaderMinHeight = 60.dp

enum class NavItemState {
    Default,
    Selected,

    /** Secondary entry, like "New board". */
    Dim,
}

/** Sidebar entry with an optional board [boardKey]. */
@Composable
fun NavItem(
    label: String,
    icon: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    state: NavItemState = NavItemState.Default,
    boardKey: String? = null,
) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(AppTheme.radius.r6)
    val isSelected = state == NavItemState.Selected
    val container = if (isSelected) {
        Modifier
            .dropShadow(shape, AppTheme.shadows.small)
            .background(colors.background.surface, shape)
            .border(AppTheme.space.s1, colors.border.default, shape)
    } else {
        Modifier
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(container)
            .semantics { selected = isSelected }
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = AppTheme.space.s8, vertical = AppTheme.space.s7),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.space.s9),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = if (isSelected) colors.accent.default else colors.text.muted,
            modifier = Modifier.size(NavIconSize),
        )
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = if (isSelected) AppTheme.textStyles.sans13_5Medium145 else AppTheme.textStyles.sans13_5Regular145,
            color = if (state == NavItemState.Dim) colors.text.muted else colors.text.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (boardKey != null) {
            Text(text = boardKey, style = AppTheme.textStyles.mono11Regular100, color = colors.text.faint, maxLines = 1)
        }
    }
}

/**
 * Persistent navigation on tablet ([AppTheme.width] `sidebarTablet`) and desktop
 * (`sidebarDesktop`); the caller sets the width, plus the start safe-drawing inset, which the
 * sidebar pads its content by (with the top and bottom ones) while its background runs edge to edge. [mainNav] holds the top Nav items, [boards]
 * the board items and the New board item. The board list scrolls when it is long. A null
 * [accountEmail] leaves the email and avatar out and keeps the Log out button.
 */
@Composable
fun Sidebar(
    boardsHeading: String,
    accountEmail: String?,
    avatarInitial: String?,
    onLogOutClick: () -> Unit,
    logOutContentDescription: String,
    modifier: Modifier = Modifier,
    mainNav: @Composable ColumnScope.() -> Unit,
    boards: @Composable ColumnScope.() -> Unit,
) {
    val colors = AppTheme.colors
    val lineColor = colors.border.default
    val lineWidth = AppTheme.space.s1
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(colors.background.sidebar)
            .drawBehind {
                val x = if (layoutDirection == LayoutDirection.Ltr) size.width - lineWidth.toPx() / 2 else lineWidth.toPx() / 2
                drawLine(lineColor, Offset(x, 0f), Offset(x, size.height), lineWidth.toPx())
            }
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start))
            .padding(PaddingValues(start = AppTheme.space.s10, end = AppTheme.space.s10, top = AppTheme.space.s14, bottom = AppTheme.space.s10)),
        verticalArrangement = Arrangement.spacedBy(AppTheme.space.s18),
    ) {
        Brand(size = BrandSize.Sidebar)
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.space.s1), content = mainNav)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AppTheme.space.s1),
        ) {
            Text(
                text = boardsHeading.uppercase(),
                modifier = Modifier
                    .semantics { heading() }
                    .padding(start = AppTheme.space.s8, end = AppTheme.space.s8, top = AppTheme.space.s2, bottom = AppTheme.space.s8),
                style = AppTheme.typography.sectionLabel,
                color = colors.text.muted,
            )
            boards()
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind { drawLine(lineColor, Offset(0f, lineWidth.toPx() / 2), Offset(size.width, lineWidth.toPx() / 2), lineWidth.toPx()) }
                .padding(top = AppTheme.space.s8)
                .padding(PaddingValues(start = AppTheme.space.s8, end = AppTheme.space.s4, top = AppTheme.space.s4, bottom = AppTheme.space.s4)),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.space.s9),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (accountEmail != null) {
                if (avatarInitial != null) Avatar(initial = avatarInitial)
                Text(
                    text = accountEmail,
                    modifier = Modifier.weight(1f),
                    style = AppTheme.textStyles.sans13Regular145,
                    color = colors.text.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
            CardiganIconButton(icon = CardiganIcons.LogOut, contentDescription = logOutContentDescription, onClick = onLogOutClick)
        }
    }
}

/**
 * Title bar of a page on tablet and desktop. Buttons, search and filters go in [actions]. Its
 * background runs under the status bar and display cutout; the content is padded by them.
 */
@Composable
fun PageHeader(
    title: String,
    modifier: Modifier = Modifier,
    boardKey: String? = null,
    meta: String? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    val colors = AppTheme.colors
    val lineColor = colors.border.default
    val lineWidth = AppTheme.space.s1
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background.app)
            .drawBehind {
                val y = size.height - lineWidth.toPx() / 2
                drawLine(lineColor, Offset(0f, y), Offset(size.width, y), lineWidth.toPx())
            }
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .heightIn(min = PageHeaderMinHeight)
            .padding(horizontal = AppTheme.space.s20, vertical = AppTheme.space.s12),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.space.s10),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.semantics { heading() },
            style = AppTheme.textStyles.sans18SemiBold120LsMinus1_5,
            color = colors.text.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (boardKey != null) Key(key = boardKey)
        if (meta != null) Text(text = meta, style = AppTheme.textStyles.mono12Regular100, color = colors.text.muted, maxLines = 1)
        Box(modifier = Modifier.weight(1f))
        if (actions != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(AppTheme.space.s10),
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }
    }
}

@Composable
private fun NavSamples() {
    Column(
        modifier = Modifier
            .background(AppTheme.colors.background.sidebar)
            .padding(AppTheme.space.s10)
            .size(width = 228.dp, height = 140.dp),
        verticalArrangement = Arrangement.spacedBy(AppTheme.space.s8),
    ) {
        NavItem(label = "Cardigan v1", icon = CardiganIcons.Board, onClick = {}, boardKey = "CRD")
        NavItem(label = "Cardigan v1", icon = CardiganIcons.Board, onClick = {}, state = NavItemState.Selected, boardKey = "CRD")
        NavItem(label = "New board", icon = CardiganIcons.Plus, onClick = {}, state = NavItemState.Dim)
    }
}

@Preview(name = "Nav item · Light")
@Composable
private fun NavItemLightPreview() {
    AppTheme(darkTheme = false, windowClass = WindowClass.Desktop) { NavSamples() }
}

@Preview(name = "Nav item · Dark")
@Composable
private fun NavItemDarkPreview() {
    AppTheme(darkTheme = true, windowClass = WindowClass.Desktop) { NavSamples() }
}

@Composable
private fun SidebarSample() {
    Sidebar(
        boardsHeading = "Boards",
        accountEmail = "philipp@example.com",
        avatarInitial = "P",
        onLogOutClick = {},
        logOutContentDescription = "Log out",
        modifier = Modifier.size(width = AppTheme.width.sidebarDesktop, height = 600.dp),
        mainNav = {
            NavItem(label = "All boards", icon = CardiganIcons.Boards, onClick = {}, state = NavItemState.Selected)
            NavItem(label = "Labels", icon = CardiganIcons.Label, onClick = {})
        },
        boards = {
            NavItem(label = "Cardigan v1", icon = CardiganIcons.Board, onClick = {}, boardKey = "CRD")
            NavItem(label = "Website", icon = CardiganIcons.Board, onClick = {}, boardKey = "WEB")
            NavItem(label = "New board", icon = CardiganIcons.Plus, onClick = {}, state = NavItemState.Dim)
        },
    )
}

@Preview(name = "Sidebar · Light", heightDp = 600)
@Composable
private fun SidebarLightPreview() {
    AppTheme(darkTheme = false, windowClass = WindowClass.Desktop) { SidebarSample() }
}

@Preview(name = "Sidebar · Dark", heightDp = 600)
@Composable
private fun SidebarDarkPreview() {
    AppTheme(darkTheme = true, windowClass = WindowClass.Desktop) { SidebarSample() }
}

@Composable
private fun PageHeaderSamples() {
    Column(modifier = Modifier.background(AppTheme.colors.background.app)) {
        PageHeader(title = "Boards", meta = "6 boards") {
            CardiganButton(text = "New board", onClick = {}, leadingIcon = CardiganIcons.Plus)
        }
        PageHeader(title = "Cardigan v1", boardKey = "CRD") {
            FilterButton(label = "Labels", selectedCount = 0, onClick = {})
            CardiganIconButton(icon = CardiganIcons.More, contentDescription = "Board menu", onClick = {})
        }
    }
}

@Preview(name = "Page header · Light", widthDp = 834)
@Composable
private fun PageHeaderLightPreview() {
    AppTheme(darkTheme = false, windowClass = WindowClass.Tablet) { PageHeaderSamples() }
}

@Preview(name = "Page header · Dark", widthDp = 834)
@Composable
private fun PageHeaderDarkPreview() {
    AppTheme(darkTheme = true, windowClass = WindowClass.Tablet) { PageHeaderSamples() }
}

