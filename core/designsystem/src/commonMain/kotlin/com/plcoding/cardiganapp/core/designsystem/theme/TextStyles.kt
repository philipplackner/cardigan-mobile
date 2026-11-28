package com.plcoding.cardiganapp.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import cardiganapp.core.designsystem.generated.resources.Res
import cardiganapp.core.designsystem.generated.resources.geist_bold
import cardiganapp.core.designsystem.generated.resources.geist_medium
import cardiganapp.core.designsystem.generated.resources.geist_mono_medium
import cardiganapp.core.designsystem.generated.resources.geist_mono_regular
import cardiganapp.core.designsystem.generated.resources.geist_mono_semibold
import cardiganapp.core.designsystem.generated.resources.geist_regular
import cardiganapp.core.designsystem.generated.resources.geist_semibold
import org.jetbrains.compose.resources.Font

/**
 * All Figma text styles, named after the style: `Sans/13.5 Medium/138%` → `sans13_5Medium138`,
 * `· -1% LS` → `LsMinus1`, `· Caps` → `Caps`.
 *
 * Compose has no text case on [TextStyle]. Text set in a `Caps` style must be uppercased where it is shown.
 */
@Immutable
data class AppTextStyles(
    val mono11Medium100: TextStyle,
    val mono11Medium100LsPlus2: TextStyle,
    val mono11Medium100LsPlus6Caps: TextStyle,
    val mono11Regular100: TextStyle,
    val mono11SemiBold100: TextStyle,
    val mono11_5Regular100: TextStyle,
    val mono11_5Regular160: TextStyle,
    val mono12Regular100: TextStyle,
    val mono12Regular145: TextStyle,
    val mono12_5Regular145: TextStyle,
    val mono13Medium100: TextStyle,
    val mono9_5SemiBold147: TextStyle,
    val sans11_5SemiBold100: TextStyle,
    val sans12Medium100: TextStyle,
    val sans12Regular145: TextStyle,
    val sans12_5Medium100: TextStyle,
    val sans12_5Medium145: TextStyle,
    val sans12_5Regular145: TextStyle,
    val sans13Bold145: TextStyle,
    val sans13Medium100: TextStyle,
    val sans13Medium145: TextStyle,
    val sans13Regular145: TextStyle,
    val sans13_5Bold145: TextStyle,
    val sans13_5Medium100: TextStyle,
    val sans13_5Medium138: TextStyle,
    val sans13_5Medium145: TextStyle,
    val sans13_5Regular145: TextStyle,
    val sans13_5Regular150: TextStyle,
    val sans13_5Regular155: TextStyle,
    val sans13_5SemiBold100: TextStyle,
    val sans14Medium100: TextStyle,
    val sans14Medium145: TextStyle,
    val sans14Regular145: TextStyle,
    val sans14SemiBold145: TextStyle,
    val sans14_5Medium138: TextStyle,
    val sans14_5Regular145: TextStyle,
    val sans15Medium100: TextStyle,
    val sans15Regular145: TextStyle,
    val sans15Regular150: TextStyle,
    val sans15Regular155: TextStyle,
    val sans15SemiBold130: TextStyle,
    val sans15SemiBold130LsMinus1: TextStyle,
    val sans15SemiBold145: TextStyle,
    val sans15SemiBold150LsMinus1: TextStyle,
    val sans15_5SemiBold100LsMinus2: TextStyle,
    val sans15_5SemiBold130LsMinus1: TextStyle,
    val sans16Bold145: TextStyle,
    val sans16SemiBold130LsMinus1: TextStyle,
    val sans16_5SemiBold115LsMinus1: TextStyle,
    val sans17SemiBold100LsMinus2: TextStyle,
    val sans17SemiBold130LsMinus1: TextStyle,
    val sans18SemiBold100LsMinus2: TextStyle,
    val sans18SemiBold120LsMinus1_5: TextStyle,
    val sans18SemiBold135LsMinus1: TextStyle,
    val sans18SemiBold150LsMinus1: TextStyle,
    val sans20SemiBold135LsMinus1: TextStyle,
    val sans22SemiBold120LsMinus2: TextStyle,
    val sans26SemiBold110LsMinus2_5: TextStyle,
    val sans28SemiBold120LsMinus2: TextStyle,
)

// Figma centers text in its line box and doesn't trim the first and last line.
private val FigmaLineHeightStyle =
    LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None,
    )

private fun style(
    family: FontFamily,
    size: Float,
    weight: FontWeight,
    lineHeight: Float,
    letterSpacing: Float,
) = TextStyle(
    fontFamily = family,
    fontSize = size.sp,
    fontWeight = weight,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.em,
    lineHeightStyle = FigmaLineHeightStyle,
)

@Composable
internal fun geistFontFamily(): FontFamily =
    FontFamily(
        Font(Res.font.geist_regular, FontWeight.Normal),
        Font(Res.font.geist_medium, FontWeight.Medium),
        Font(Res.font.geist_semibold, FontWeight.SemiBold),
        Font(Res.font.geist_bold, FontWeight.Bold),
    )

@Composable
internal fun geistMonoFontFamily(): FontFamily =
    FontFamily(
        Font(Res.font.geist_mono_regular, FontWeight.Normal),
        Font(Res.font.geist_mono_medium, FontWeight.Medium),
        Font(Res.font.geist_mono_semibold, FontWeight.SemiBold),
    )

@Composable
internal fun rememberAppTextStyles(): AppTextStyles {
    val sans = geistFontFamily()
    val mono = geistMonoFontFamily()
    return remember(sans, mono) {
        AppTextStyles(
            mono11Medium100 = style(mono, 11.0f, FontWeight.Medium, 11.0f, 0.0f),
            mono11Medium100LsPlus2 = style(mono, 11.0f, FontWeight.Medium, 11.0f, 0.02f),
            mono11Medium100LsPlus6Caps = style(mono, 11.0f, FontWeight.Medium, 11.0f, 0.06f),
            mono11Regular100 = style(mono, 11.0f, FontWeight.Normal, 11.0f, 0.0f),
            mono11SemiBold100 = style(mono, 11.0f, FontWeight.SemiBold, 11.0f, 0.0f),
            mono11_5Regular100 = style(mono, 11.5f, FontWeight.Normal, 11.5f, 0.0f),
            mono11_5Regular160 = style(mono, 11.5f, FontWeight.Normal, 18.4f, 0.0f),
            mono12Regular100 = style(mono, 12.0f, FontWeight.Normal, 12.0f, 0.0f),
            mono12Regular145 = style(mono, 12.0f, FontWeight.Normal, 17.4f, 0.0f),
            mono12_5Regular145 = style(mono, 12.5f, FontWeight.Normal, 18.125f, 0.0f),
            mono13Medium100 = style(mono, 13.0f, FontWeight.Medium, 13.0f, 0.0f),
            mono9_5SemiBold147 = style(mono, 9.5f, FontWeight.SemiBold, 13.965f, 0.0f),
            sans11_5SemiBold100 = style(sans, 11.5f, FontWeight.SemiBold, 11.5f, 0.0f),
            sans12Medium100 = style(sans, 12.0f, FontWeight.Medium, 12.0f, 0.0f),
            sans12Regular145 = style(sans, 12.0f, FontWeight.Normal, 17.4f, 0.0f),
            sans12_5Medium100 = style(sans, 12.5f, FontWeight.Medium, 12.5f, 0.0f),
            sans12_5Medium145 = style(sans, 12.5f, FontWeight.Medium, 18.125f, 0.0f),
            sans12_5Regular145 = style(sans, 12.5f, FontWeight.Normal, 18.125f, 0.0f),
            sans13Bold145 = style(sans, 13.0f, FontWeight.Bold, 18.85f, 0.0f),
            sans13Medium100 = style(sans, 13.0f, FontWeight.Medium, 13.0f, 0.0f),
            sans13Medium145 = style(sans, 13.0f, FontWeight.Medium, 18.85f, 0.0f),
            sans13Regular145 = style(sans, 13.0f, FontWeight.Normal, 18.85f, 0.0f),
            sans13_5Bold145 = style(sans, 13.5f, FontWeight.Bold, 19.575f, 0.0f),
            sans13_5Medium100 = style(sans, 13.5f, FontWeight.Medium, 13.5f, 0.0f),
            sans13_5Medium138 = style(sans, 13.5f, FontWeight.Medium, 18.63f, 0.0f),
            sans13_5Medium145 = style(sans, 13.5f, FontWeight.Medium, 19.575f, 0.0f),
            sans13_5Regular145 = style(sans, 13.5f, FontWeight.Normal, 19.575f, 0.0f),
            sans13_5Regular150 = style(sans, 13.5f, FontWeight.Normal, 20.25f, 0.0f),
            sans13_5Regular155 = style(sans, 13.5f, FontWeight.Normal, 20.925f, 0.0f),
            sans13_5SemiBold100 = style(sans, 13.5f, FontWeight.SemiBold, 13.5f, 0.0f),
            sans14Medium100 = style(sans, 14.0f, FontWeight.Medium, 14.0f, 0.0f),
            sans14Medium145 = style(sans, 14.0f, FontWeight.Medium, 20.3f, 0.0f),
            sans14Regular145 = style(sans, 14.0f, FontWeight.Normal, 20.3f, 0.0f),
            sans14SemiBold145 = style(sans, 14.0f, FontWeight.SemiBold, 20.3f, 0.0f),
            sans14_5Medium138 = style(sans, 14.5f, FontWeight.Medium, 20.01f, 0.0f),
            sans14_5Regular145 = style(sans, 14.5f, FontWeight.Normal, 21.025f, 0.0f),
            sans15Medium100 = style(sans, 15.0f, FontWeight.Medium, 15.0f, 0.0f),
            sans15Regular145 = style(sans, 15.0f, FontWeight.Normal, 21.75f, 0.0f),
            sans15Regular150 = style(sans, 15.0f, FontWeight.Normal, 22.5f, 0.0f),
            sans15Regular155 = style(sans, 15.0f, FontWeight.Normal, 23.25f, 0.0f),
            sans15SemiBold130 = style(sans, 15.0f, FontWeight.SemiBold, 19.5f, 0.0f),
            sans15SemiBold130LsMinus1 = style(sans, 15.0f, FontWeight.SemiBold, 19.5f, -0.01f),
            sans15SemiBold145 = style(sans, 15.0f, FontWeight.SemiBold, 21.75f, 0.0f),
            sans15SemiBold150LsMinus1 = style(sans, 15.0f, FontWeight.SemiBold, 22.5f, -0.01f),
            sans15_5SemiBold100LsMinus2 = style(sans, 15.5f, FontWeight.SemiBold, 15.5f, -0.02f),
            sans15_5SemiBold130LsMinus1 = style(sans, 15.5f, FontWeight.SemiBold, 20.15f, -0.01f),
            sans16Bold145 = style(sans, 16.0f, FontWeight.Bold, 23.2f, 0.0f),
            sans16SemiBold130LsMinus1 = style(sans, 16.0f, FontWeight.SemiBold, 20.8f, -0.01f),
            sans16_5SemiBold115LsMinus1 = style(sans, 16.5f, FontWeight.SemiBold, 18.975f, -0.01f),
            sans17SemiBold100LsMinus2 = style(sans, 17.0f, FontWeight.SemiBold, 17.0f, -0.02f),
            sans17SemiBold130LsMinus1 = style(sans, 17.0f, FontWeight.SemiBold, 22.1f, -0.01f),
            sans18SemiBold100LsMinus2 = style(sans, 18.0f, FontWeight.SemiBold, 18.0f, -0.02f),
            sans18SemiBold120LsMinus1_5 = style(sans, 18.0f, FontWeight.SemiBold, 21.6f, -0.015f),
            sans18SemiBold135LsMinus1 = style(sans, 18.0f, FontWeight.SemiBold, 24.3f, -0.01f),
            sans18SemiBold150LsMinus1 = style(sans, 18.0f, FontWeight.SemiBold, 27.0f, -0.01f),
            sans20SemiBold135LsMinus1 = style(sans, 20.0f, FontWeight.SemiBold, 27.0f, -0.01f),
            sans22SemiBold120LsMinus2 = style(sans, 22.0f, FontWeight.SemiBold, 26.4f, -0.02f),
            sans26SemiBold110LsMinus2_5 = style(sans, 26.0f, FontWeight.SemiBold, 28.6f, -0.025f),
            sans28SemiBold120LsMinus2 = style(sans, 28.0f, FontWeight.SemiBold, 33.6f, -0.02f),
        )
    }
}
