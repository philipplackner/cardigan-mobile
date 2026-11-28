package com.plcoding.cardiganapp.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class BackgroundColors(
    val app: Color,
    val surface: Color,
    val column: Color,
    val sidebar: Color,
    val hover: Color,
    val skeleton: Color,
    val skeletonHighlight: Color,
    val scrim: Color,
    val toast: Color,
)

@Immutable
data class TextColors(
    val primary: Color,
    val muted: Color,
    val faint: Color,
    val onAccent: Color,
    val onDanger: Color,
    val toast: Color,
)

@Immutable
data class BorderColors(
    val default: Color,
    val strong: Color,
    val accentSubtle: Color,
    val dangerSubtle: Color,
)

@Immutable
data class AccentColors(
    val default: Color,
    val hover: Color,
    val soft: Color,
)

@Immutable
data class DangerColors(
    val default: Color,
    val soft: Color,
)

@Immutable
data class SuccessColors(
    val default: Color,
    val soft: Color,
)

@Immutable
data class ShadowColors(
    val small: Color,
    val popover1: Color,
    val popover2: Color,
    val drag1: Color,
    val drag2: Color,
    val panel: Color,
    val raised: Color,
)

@Immutable
data class LabelColors(
    val red: Color,
    val orange: Color,
    val amber: Color,
    val yellow: Color,
    val lime: Color,
    val green: Color,
    val emerald: Color,
    val teal: Color,
    val cyan: Color,
    val sky: Color,
    val blue: Color,
    val indigo: Color,
    val violet: Color,
    val purple: Color,
    val fuchsia: Color,
    val pink: Color,
    val rose: Color,
    val brown: Color,
    val olive: Color,
    val slate: Color,
)

/** All Figma Color variables, grouped by their Figma path (`background/surface` → `background.surface`). */
@Immutable
data class AppColors(
    val background: BackgroundColors,
    val text: TextColors,
    val border: BorderColors,
    val accent: AccentColors,
    val danger: DangerColors,
    val success: SuccessColors,
    val shadow: ShadowColors,
    val label: LabelColors,
)

internal val LightColors =
    AppColors(
        background =
            BackgroundColors(
                app = Color(0xFFF7F8FA),
                surface = Color(0xFFFFFFFF),
                column = Color(0xFFEEF0F4),
                sidebar = Color(0xFFF2F4F7),
                hover = Color(0xFFEEF1F5),
                skeleton = Color(0xFFE7EAEF),
                skeletonHighlight = Color(0xFFF3F5F8),
                scrim = Color(0x520F121C),
                toast = Color(0xFF151821),
            ),
        text =
            TextColors(
                primary = Color(0xFF151821),
                muted = Color(0xFF5F6676),
                faint = Color(0xFF98A0AE),
                onAccent = Color(0xFFFFFFFF),
                onDanger = Color(0xFFFFFFFF),
                toast = Color(0xFFF3F4F7),
            ),
        border =
            BorderColors(
                default = Color(0xFFE1E4EA),
                strong = Color(0xFFCBD0D9),
                accentSubtle = Color(0x402B59F0),
                dangerSubtle = Color(0x4DD23B3B),
            ),
        accent =
            AccentColors(
                default = Color(0xFF2B59F0),
                hover = Color(0xFF2149D6),
                soft = Color(0xFFE7EDFE),
            ),
        danger =
            DangerColors(
                default = Color(0xFFD23B3B),
                soft = Color(0xFFFDECEC),
            ),
        success =
            SuccessColors(
                default = Color(0xFF1F9D57),
                soft = Color(0xFFE5F6EC),
            ),
        shadow =
            ShadowColors(
                small = Color(0x0F141828),
                popover1 = Color(0x24141828),
                popover2 = Color(0x0F141828),
                drag1 = Color(0x33141828),
                drag2 = Color(0x14141828),
                panel = Color(0x14141828),
                raised = Color(0x0F141828),
            ),
        label =
            LabelColors(
                red = Color(0xFFDE2121),
                orange = Color(0xFFEC6313),
                amber = Color(0xFFF29E0D),
                yellow = Color(0xFFE8C517),
                lime = Color(0xFF9ACF30),
                green = Color(0xFF40BF55),
                emerald = Color(0xFF30CF8D),
                teal = Color(0xFF30CFB9),
                cyan = Color(0xFF24C3DB),
                sky = Color(0xFF1CA1E3),
                blue = Color(0xFF1964E6),
                indigo = Color(0xFF2937D6),
                violet = Color(0xFF5326D9),
                purple = Color(0xFF8D30CF),
                fuchsia = Color(0xFFC130CF),
                pink = Color(0xFFD9268E),
                rose = Color(0xFFDE2150),
                brown = Color(0xFFB0774F),
                olive = Color(0xFFA6A857),
                slate = Color(0xFF6B7C94),
            ),
    )

internal val DarkColors =
    AppColors(
        background =
            BackgroundColors(
                app = Color(0xFF0D0F14),
                surface = Color(0xFF161A22),
                column = Color(0xFF12151B),
                sidebar = Color(0xFF101318),
                hover = Color(0xFF1C212B),
                skeleton = Color(0xFF1B1F28),
                skeletonHighlight = Color(0xFF252B36),
                scrim = Color(0x8C000000),
                toast = Color(0xFFE7E9EF),
            ),
        text =
            TextColors(
                primary = Color(0xFFE7E9EF),
                muted = Color(0xFF8C93A3),
                faint = Color(0xFF5D6474),
                onAccent = Color(0xFFFFFFFF),
                onDanger = Color(0xFFFFFFFF),
                toast = Color(0xFF0D0F14),
            ),
        border =
            BorderColors(
                default = Color(0xFF232833),
                strong = Color(0xFF343B48),
                accentSubtle = Color(0x405C80FF),
                dangerSubtle = Color(0x4DFF6B6B),
            ),
        accent =
            AccentColors(
                default = Color(0xFF5C80FF),
                hover = Color(0xFF7491FF),
                soft = Color(0xFF1A2440),
            ),
        danger =
            DangerColors(
                default = Color(0xFFFF6B6B),
                soft = Color(0xFF3A1818),
            ),
        success =
            SuccessColors(
                default = Color(0xFF3FCB7E),
                soft = Color(0xFF132A1D),
            ),
        shadow =
            ShadowColors(
                small = Color(0x08FFFFFF),
                popover1 = Color(0x8C000000),
                popover2 = Color(0x05FFFFFF),
                drag1 = Color(0x99000000),
                drag2 = Color(0x00000000),
                panel = Color(0x66000000),
                raised = Color(0x4D000000),
            ),
        label =
            LabelColors(
                red = Color(0xFFE75F5F),
                orange = Color(0xFFF18E55),
                amber = Color(0xFFF6B951),
                yellow = Color(0xFFEED558),
                lime = Color(0xFFB6DC6A),
                green = Color(0xFF75D185),
                emerald = Color(0xFF6ADCAD),
                teal = Color(0xFF6ADCCD),
                cyan = Color(0xFF61D4E5),
                sky = Color(0xFF5CBBEB),
                blue = Color(0xFF5A90ED),
                indigo = Color(0xFF656FE2),
                violet = Color(0xFF8363E3),
                purple = Color(0xFFAD6ADC),
                fuchsia = Color(0xFFD36ADC),
                pink = Color(0xFFE363AE),
                rose = Color(0xFFE75F81),
                brown = Color(0xFFC69D80),
                olive = Color(0xFFBFC186),
                slate = Color(0xFF95A1B2),
            ),
    )
