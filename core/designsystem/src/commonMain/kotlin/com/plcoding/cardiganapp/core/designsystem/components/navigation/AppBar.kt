package com.plcoding.cardiganapp.core.designsystem.components.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.plcoding.cardiganapp.core.designsystem.components.actions.CardiganIconButton
import com.plcoding.cardiganapp.core.designsystem.components.actions.IconButtonSize
import com.plcoding.cardiganapp.core.designsystem.components.brand.Brand
import com.plcoding.cardiganapp.core.designsystem.components.brand.BrandSize
import com.plcoding.cardiganapp.core.designsystem.icons.CardiganIcons
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import com.plcoding.cardiganapp.core.designsystem.theme.WindowClass
import com.plcoding.cardiganapp.core.designsystem.theme.dropShadow

private val AppBarMinHeight = 56.dp

/**
 * Phone top bar container. [raised] adds the divider and `Shadow/Raised` once content scrolls
 * under the bar. Doesn't clip, so its icon buttons keep their 48dp touch area.
 */
@Composable
private fun AppBarContainer(
    raised: Boolean,
    modifier: Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = AppTheme.colors
    val lineColor = colors.border.default
    val lineWidth = AppTheme.space.s1
    val raisedModifier = if (raised) {
        Modifier
            .dropShadow(RectangleShape, AppTheme.shadows.raised)
            .background(colors.background.app)
            .drawBehind {
                val y = size.height - lineWidth.toPx() / 2
                drawLine(lineColor, Offset(0f, y), Offset(size.width, y), lineWidth.toPx())
            }
    } else {
        Modifier.background(colors.background.app)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = AppBarMinHeight)
            .then(raisedModifier)
            .padding(PaddingValues(start = AppTheme.space.s10, end = AppTheme.space.s8, top = AppTheme.space.s6, bottom = AppTheme.space.s6)),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.space.s2),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun AppBarActions(actions: (@Composable RowScope.() -> Unit)?) {
    if (actions == null) return
    Row(
        horizontalArrangement = Arrangement.spacedBy(AppTheme.space.s2),
        verticalAlignment = Alignment.CenterVertically,
        content = actions,
    )
}

/** App bar with the brand, used on the phone board list. Use Large icon buttons in [actions]. */
@Composable
fun BrandAppBar(
    modifier: Modifier = Modifier,
    raised: Boolean = false,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    AppBarContainer(raised = raised, modifier = modifier) {
        Brand(size = BrandSize.AppBar)
        Box(modifier = Modifier.weight(1f))
        AppBarActions(actions)
    }
}

/** App bar with a title and back button, used inside a board on phones. */
@Composable
fun TitleAppBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    raised: Boolean = false,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    AppBarContainer(raised = raised, modifier = modifier) {
        navigationIcon?.invoke()
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = AppTheme.space.s2),
            verticalArrangement = Arrangement.spacedBy(AppTheme.space.s3),
        ) {
            Text(
                text = title,
                modifier = Modifier.semantics { heading() },
                style = AppTheme.textStyles.sans16_5SemiBold115LsMinus1,
                color = AppTheme.colors.text.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(text = subtitle, style = AppTheme.textStyles.mono11_5Regular100, color = AppTheme.colors.text.muted, maxLines = 1)
            }
        }
        AppBarActions(actions)
    }
}

/** App bar with a centered title, used on the phone card detail. */
@Composable
fun CenteredAppBar(
    title: String,
    modifier: Modifier = Modifier,
    raised: Boolean = false,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    AppBarContainer(raised = raised, modifier = modifier) {
        navigationIcon?.invoke()
        Text(
            text = title,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
            style = AppTheme.textStyles.mono13Medium100,
            color = AppTheme.colors.text.muted,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        AppBarActions(actions)
    }
}

@Composable
private fun SampleBack() {
    CardiganIconButton(icon = CardiganIcons.ChevronLeft, contentDescription = "Back", onClick = {}, size = IconButtonSize.Large)
}

@Composable
private fun SampleActions() {
    CardiganIconButton(icon = CardiganIcons.Search, contentDescription = "Search", onClick = {}, size = IconButtonSize.Large)
    CardiganIconButton(icon = CardiganIcons.More, contentDescription = "More", onClick = {}, size = IconButtonSize.Large)
}

@Composable
private fun AppBarSamples() {
    Column(
        modifier = Modifier.background(AppTheme.colors.background.app),
        verticalArrangement = Arrangement.spacedBy(AppTheme.space.s16),
    ) {
        listOf(false, true).forEach { raised ->
            BrandAppBar(raised = raised, actions = { SampleActions() })
            TitleAppBar(title = "Cardigan v1", subtitle = "CRD · 24 cards", raised = raised, navigationIcon = { SampleBack() }, actions = { SampleActions() })
            CenteredAppBar(title = "CRD-109", raised = raised, navigationIcon = { SampleBack() }, actions = { SampleActions() })
        }
    }
}

@Preview(name = "App bar · Light", widthDp = 390)
@Composable
private fun AppBarLightPreview() {
    AppTheme(darkTheme = false, windowClass = WindowClass.Phone) { AppBarSamples() }
}

@Preview(name = "App bar · Dark", widthDp = 390)
@Composable
private fun AppBarDarkPreview() {
    AppTheme(darkTheme = true, windowClass = WindowClass.Phone) { AppBarSamples() }
}
