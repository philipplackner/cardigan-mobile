package com.plcoding.cardiganapp.core.designsystem.components.interaction

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp

/** Dashed stroke inside [shape], like a Figma inside stroke with a dash pattern. */
fun Modifier.dashedBorder(
    width: Dp,
    color: Color,
    shape: Shape,
    dash: Dp,
    gap: Dp,
): Modifier =
    drawBehind {
        val strokePx = width.toPx()
        val inset = strokePx / 2
        val outline = shape.createOutline(Size(size.width - strokePx, size.height - strokePx), layoutDirection, this)
        translate(inset, inset) {
            drawOutline(
                outline = outline,
                color = color,
                style =
                    Stroke(
                        width = strokePx,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash.toPx(), gap.toPx())),
                    ),
            )
        }
    }
