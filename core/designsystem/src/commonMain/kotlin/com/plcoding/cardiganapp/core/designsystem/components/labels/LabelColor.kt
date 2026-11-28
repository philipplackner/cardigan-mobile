package com.plcoding.cardiganapp.core.designsystem.components.labels

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import cardiganapp.core.designsystem.generated.resources.Res
import cardiganapp.core.designsystem.generated.resources.label_color_amber
import cardiganapp.core.designsystem.generated.resources.label_color_blue
import cardiganapp.core.designsystem.generated.resources.label_color_brown
import cardiganapp.core.designsystem.generated.resources.label_color_cyan
import cardiganapp.core.designsystem.generated.resources.label_color_emerald
import cardiganapp.core.designsystem.generated.resources.label_color_fuchsia
import cardiganapp.core.designsystem.generated.resources.label_color_green
import cardiganapp.core.designsystem.generated.resources.label_color_indigo
import cardiganapp.core.designsystem.generated.resources.label_color_lime
import cardiganapp.core.designsystem.generated.resources.label_color_olive
import cardiganapp.core.designsystem.generated.resources.label_color_orange
import cardiganapp.core.designsystem.generated.resources.label_color_pink
import cardiganapp.core.designsystem.generated.resources.label_color_purple
import cardiganapp.core.designsystem.generated.resources.label_color_red
import cardiganapp.core.designsystem.generated.resources.label_color_rose
import cardiganapp.core.designsystem.generated.resources.label_color_sky
import cardiganapp.core.designsystem.generated.resources.label_color_slate
import cardiganapp.core.designsystem.generated.resources.label_color_teal
import cardiganapp.core.designsystem.generated.resources.label_color_violet
import cardiganapp.core.designsystem.generated.resources.label_color_yellow
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import org.jetbrains.compose.resources.StringResource

/**
 * The 20 label colors, named as in the shared `domain-label` palette and listed in Figma order.
 * Feature modules map their domain color to this.
 */
enum class LabelColor(
    val displayName: StringResource,
) {
    Red(Res.string.label_color_red),
    Orange(Res.string.label_color_orange),
    Amber(Res.string.label_color_amber),
    Yellow(Res.string.label_color_yellow),
    Lime(Res.string.label_color_lime),
    Green(Res.string.label_color_green),
    Emerald(Res.string.label_color_emerald),
    Teal(Res.string.label_color_teal),
    Cyan(Res.string.label_color_cyan),
    Sky(Res.string.label_color_sky),
    Blue(Res.string.label_color_blue),
    Indigo(Res.string.label_color_indigo),
    Violet(Res.string.label_color_violet),
    Purple(Res.string.label_color_purple),
    Fuchsia(Res.string.label_color_fuchsia),
    Pink(Res.string.label_color_pink),
    Rose(Res.string.label_color_rose),
    Brown(Res.string.label_color_brown),
    Olive(Res.string.label_color_olive),
    Slate(Res.string.label_color_slate),
}

/** The `label/<name>` color for this palette entry in the active theme. */
val LabelColor.color: Color
    @Composable @ReadOnlyComposable
    get() {
        val labels = AppTheme.colors.label
        return when (this) {
            LabelColor.Red -> labels.red
            LabelColor.Orange -> labels.orange
            LabelColor.Amber -> labels.amber
            LabelColor.Yellow -> labels.yellow
            LabelColor.Lime -> labels.lime
            LabelColor.Green -> labels.green
            LabelColor.Emerald -> labels.emerald
            LabelColor.Teal -> labels.teal
            LabelColor.Cyan -> labels.cyan
            LabelColor.Sky -> labels.sky
            LabelColor.Blue -> labels.blue
            LabelColor.Indigo -> labels.indigo
            LabelColor.Violet -> labels.violet
            LabelColor.Purple -> labels.purple
            LabelColor.Fuchsia -> labels.fuchsia
            LabelColor.Pink -> labels.pink
            LabelColor.Rose -> labels.rose
            LabelColor.Brown -> labels.brown
            LabelColor.Olive -> labels.olive
            LabelColor.Slate -> labels.slate
        }
    }
