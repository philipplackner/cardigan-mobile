package com.plcoding.cardiganapp.core.designsystem.components.labels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.plcoding.cardiganapp.core.designsystem.components.actions.CardiganIconButton
import com.plcoding.cardiganapp.core.designsystem.components.actions.IconButtonSize
import com.plcoding.cardiganapp.core.designsystem.components.interaction.isTouchSize
import com.plcoding.cardiganapp.core.designsystem.icons.CardiganIcons
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import com.plcoding.cardiganapp.core.designsystem.theme.WindowClass
import org.jetbrains.compose.resources.stringResource

private val SmallDotSize = 7.dp
private val LargeDotSize = 10.dp
private val SwatchSize = 30.dp
private val SwatchRingGap = 2.dp
private val SwatchRingWidth = 2.dp
private val ChipRemoveIconSize = 12.dp

/** One of the 20 label colors as a dot. Size it freely (7dp in labels, 10dp in label rows). */
@Composable
fun LabelDot(
    color: LabelColor,
    modifier: Modifier = Modifier,
    size: Dp = LargeDotSize,
) {
    Box(modifier = modifier.size(size).background(color.color, CircleShape))
}

enum class LabelSize {
    /** On cards and in pickers. */
    Small,
    Medium,
    Large,
}

/** Color dot and name. Medium and Large are for the Labels list. */
@Composable
fun Label(
    name: String,
    color: LabelColor,
    modifier: Modifier = Modifier,
    size: LabelSize = LabelSize.Small,
) {
    val styles = AppTheme.textStyles
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.space.s5),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LabelDot(color = color, size = if (size == LabelSize.Small) SmallDotSize else LargeDotSize)
        Text(
            text = name,
            style =
                when (size) {
                    LabelSize.Small -> styles.sans12Medium100
                    LabelSize.Medium -> styles.sans13_5Medium100
                    LabelSize.Large -> styles.sans15Medium100
                },
            color = if (size == LabelSize.Small) AppTheme.colors.text.muted else AppTheme.colors.text.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Label on the card detail. Pass [onRemove] and [removeContentDescription] to make it removable. */
@Composable
fun LabelChip(
    name: String,
    color: LabelColor,
    modifier: Modifier = Modifier,
    onRemove: (() -> Unit)? = null,
    removeContentDescription: String? = null,
) {
    val shape = RoundedCornerShape(AppTheme.radius.full)
    Row(
        modifier =
            modifier
                .background(AppTheme.colors.background.surface, shape)
                .border(AppTheme.space.s1, AppTheme.colors.border.default, shape)
                .padding(
                    PaddingValues(
                        start = AppTheme.space.s8,
                        end = AppTheme.space.s9,
                        top = AppTheme.space.s4,
                        bottom = AppTheme.space.s4,
                    ),
                ),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.space.s5),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LabelDot(color = color, size = SmallDotSize)
        Text(
            text = name,
            style = AppTheme.textStyles.sans12Medium100,
            color = AppTheme.colors.text.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (onRemove != null) {
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(ChipRemoveIconSize),
                colors = IconButtonDefaults.iconButtonColors(contentColor = AppTheme.colors.text.muted),
            ) {
                Icon(
                    painter = CardiganIcons.CloseSmall,
                    contentDescription = removeContentDescription,
                    modifier = Modifier.size(ChipRemoveIconSize),
                )
            }
        }
    }
}

/** Color choice in the label dialog. Screen readers read the color name. */
@Composable
fun Swatch(
    color: LabelColor,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val name = stringResource(color.displayName)
    val ringColor = AppTheme.colors.text.primary
    Box(
        modifier =
            modifier
                .size(SwatchSize)
                .semantics { contentDescription = name }
                .selectable(
                    selected = selected,
                    onClick = onClick,
                    role = Role.RadioButton,
                    interactionSource = null,
                    indication = ripple(bounded = false, radius = SwatchSize / 2 + SwatchRingGap + SwatchRingWidth),
                ).drawBehind {
                    if (selected) {
                        val strokePx = SwatchRingWidth.toPx()
                        drawCircle(
                            color = ringColor,
                            radius = size.minDimension / 2 + SwatchRingGap.toPx() + strokePx / 2,
                            style = Stroke(strokePx),
                        )
                    }
                }.background(color.color, CircleShape),
    )
}

/**
 * Row in the Labels list with edit and delete actions. The callers pass the content
 * descriptions of the two actions.
 */
@Composable
fun LabelRow(
    name: String,
    color: LabelColor,
    onEditClick: () -> Unit,
    editContentDescription: String,
    onDeleteClick: () -> Unit,
    deleteContentDescription: String,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
) {
    val touch = isTouchSize
    val dividerColor = AppTheme.colors.border.default
    val dividerWidth = AppTheme.space.s1
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .drawBehind {
                    if (showDivider) {
                        val y = size.height - dividerWidth.toPx() / 2
                        drawLine(dividerColor, Offset(0f, y), Offset(size.width, y), dividerWidth.toPx())
                    }
                }.padding(
                    if (touch) {
                        PaddingValues(
                            start = AppTheme.space.s16,
                            end = AppTheme.space.s6,
                            top = AppTheme.space.s14,
                            bottom = AppTheme.space.s14,
                        )
                    } else {
                        PaddingValues(
                            start = AppTheme.space.s16,
                            end = AppTheme.space.s8,
                            top = AppTheme.space.s10,
                            bottom = AppTheme.space.s10,
                        )
                    },
                ),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.space.s12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Label(
            name = name,
            color = color,
            size = if (touch) LabelSize.Large else LabelSize.Medium,
            modifier = Modifier.weight(1f),
        )
        CardiganIconButton(
            icon = CardiganIcons.Edit,
            contentDescription = editContentDescription,
            onClick = onEditClick,
            size = IconButtonSize.Medium,
        )
        CardiganIconButton(
            icon = CardiganIcons.Trash,
            contentDescription = deleteContentDescription,
            onClick = onDeleteClick,
            size = IconButtonSize.Medium,
        )
    }
}

@Composable
private fun LabelDotsGrid() {
    Column(
        modifier =
            Modifier
                .background(AppTheme.colors.background.surface)
                .padding(AppTheme.space.s24),
        verticalArrangement = Arrangement.spacedBy(AppTheme.space.s12),
    ) {
        LabelColor.entries.chunked(10).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.space.s12)) {
                row.forEach { LabelDot(color = it) }
            }
        }
    }
}

@Preview(name = "Label dots · Light")
@Composable
private fun LabelDotsLightPreview() {
    AppTheme(darkTheme = false) { LabelDotsGrid() }
}

@Preview(name = "Label dots · Dark")
@Composable
private fun LabelDotsDarkPreview() {
    AppTheme(darkTheme = true) { LabelDotsGrid() }
}

@Composable
private fun LabelSamples() {
    Column(
        modifier =
            Modifier
                .background(AppTheme.colors.background.surface)
                .padding(AppTheme.space.s24),
        verticalArrangement = Arrangement.spacedBy(AppTheme.space.s16),
    ) {
        LabelSize.entries.forEach { Label(name = "api", color = LabelColor.Blue, size = it) }
        Row(
            horizontalArrangement = Arrangement.spacedBy(AppTheme.space.s8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LabelChip(name = "api", color = LabelColor.Blue)
            LabelChip(name = "api", color = LabelColor.Blue, onRemove = {}, removeContentDescription = "Remove api")
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(AppTheme.space.s16),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Swatch(color = LabelColor.Red, selected = false, onClick = {})
            Swatch(color = LabelColor.Red, selected = true, onClick = {})
        }
    }
}

@Preview(name = "Labels · Light")
@Composable
private fun LabelsLightPreview() {
    AppTheme(darkTheme = false) { LabelSamples() }
}

@Preview(name = "Labels · Dark")
@Composable
private fun LabelsDarkPreview() {
    AppTheme(darkTheme = true) { LabelSamples() }
}

@Composable
private fun LabelRowSamples() {
    Column(modifier = Modifier.background(AppTheme.colors.background.surface).width(390.dp)) {
        LabelRow("api", LabelColor.Blue, {}, "Edit api", {}, "Delete api")
        LabelRow("bug", LabelColor.Red, {}, "Edit bug", {}, "Delete bug", showDivider = false)
    }
}

@Preview(name = "Label row · Tablet · Light")
@Composable
private fun LabelRowTabletLightPreview() {
    AppTheme(darkTheme = false, windowClass = WindowClass.Tablet) { LabelRowSamples() }
}

@Preview(name = "Label row · Tablet · Dark")
@Composable
private fun LabelRowTabletDarkPreview() {
    AppTheme(darkTheme = true, windowClass = WindowClass.Tablet) { LabelRowSamples() }
}

@Preview(name = "Label row · Phone (Touch) · Light")
@Composable
private fun LabelRowPhoneLightPreview() {
    AppTheme(darkTheme = false, windowClass = WindowClass.Phone) { LabelRowSamples() }
}

@Preview(name = "Label row · Phone (Touch) · Dark")
@Composable
private fun LabelRowPhoneDarkPreview() {
    AppTheme(darkTheme = true, windowClass = WindowClass.Phone) { LabelRowSamples() }
}
