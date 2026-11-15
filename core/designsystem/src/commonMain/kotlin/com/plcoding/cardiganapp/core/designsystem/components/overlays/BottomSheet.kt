package com.plcoding.cardiganapp.core.designsystem.components.overlays

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.plcoding.cardiganapp.core.designsystem.components.inputs.Option
import com.plcoding.cardiganapp.core.designsystem.components.labels.Label
import com.plcoding.cardiganapp.core.designsystem.components.labels.LabelColor
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import com.plcoding.cardiganapp.core.designsystem.theme.WindowClass
import com.plcoding.cardiganapp.core.designsystem.theme.dropShadow

private val HandleWidth = 36.dp
private val HandleHeight = 4.dp

/**
 * Phone sheet anchored to the bottom edge. Content goes in [content]. Every visible Material
 * default (shape, colors, elevation, scrim, drag handle, padding) is replaced by the Figma values.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = sheetShape()
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier
            .dropShadow(shape, AppTheme.shadows.popover)
            .topBorder(shape),
        sheetState = sheetState,
        shape = shape,
        containerColor = AppTheme.colors.background.surface,
        contentColor = AppTheme.colors.text.primary,
        tonalElevation = 0.dp,
        scrimColor = AppTheme.colors.background.scrim,
        dragHandle = { SheetHandle() },
    ) {
        SheetContent(content = content)
    }
}

@Composable
private fun sheetShape(): Shape = RoundedCornerShape(topStart = AppTheme.radius.r16, topEnd = AppTheme.radius.r16)

/** Figma strokes only the top edge (and its rounded corners) of the sheet. */
@Composable
private fun Modifier.topBorder(shape: Shape): Modifier {
    val color = AppTheme.colors.border.default
    val strokeWidth = AppTheme.space.s1
    val bandHeight = AppTheme.radius.r16
    return drawWithContent {
        drawContent()
        val strokePx = strokeWidth.toPx()
        clipRect(bottom = bandHeight.toPx()) {
            val inset = strokePx / 2
            val outline = shape.createOutline(Size(size.width - strokePx, size.height - strokePx), layoutDirection, this)
            translate(inset, inset) { drawOutline(outline, color, style = Stroke(strokePx)) }
        }
    }
}

@Composable
private fun SheetHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = AppTheme.space.s8, bottom = AppTheme.space.s4),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = HandleWidth, height = HandleHeight)
                .background(AppTheme.colors.border.strong, RoundedCornerShape(AppTheme.radius.r2)),
        )
    }
}

@Composable
private fun SheetContent(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = AppTheme.space.s16, end = AppTheme.space.s16, top = AppTheme.space.s12, bottom = AppTheme.space.s16),
        verticalArrangement = Arrangement.spacedBy(AppTheme.space.s12),
        content = content,
    )
}

@Composable
private fun BottomSheetSample() {
    val shape = sheetShape()
    Column(
        modifier = Modifier
            .background(AppTheme.colors.background.app)
            .padding(top = AppTheme.space.s32)
            .width(390.dp),
    ) {
        Column(
            modifier = Modifier
                .topBorder(shape)
                .background(AppTheme.colors.background.surface, shape),
        ) {
            SheetHandle()
            SheetContent {
                listOf("bug", "feature", "docs").forEachIndexed { index, name ->
                    Option(checked = index == 0, onCheckedChange = {}) {
                        Label(name = name, color = LabelColor.entries[index])
                    }
                }
            }
        }
    }
}

@Preview(name = "Bottom sheet · Light", widthDp = 390)
@Composable
private fun BottomSheetLightPreview() {
    AppTheme(darkTheme = false, windowClass = WindowClass.Phone) { BottomSheetSample() }
}

@Preview(name = "Bottom sheet · Dark", widthDp = 390)
@Composable
private fun BottomSheetDarkPreview() {
    AppTheme(darkTheme = true, windowClass = WindowClass.Phone) { BottomSheetSample() }
}
