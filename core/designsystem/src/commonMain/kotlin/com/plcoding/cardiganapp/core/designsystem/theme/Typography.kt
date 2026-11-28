package com.plcoding.cardiganapp.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle

/**
 * The 7 text roles from Figma Foundations. Page title, Card title and Body use their larger phone
 * style in [WindowClass.Phone].
 *
 * [sectionLabel] is a Caps style: uppercase the text where it is shown.
 */
@Immutable
data class AppTypography(
    val pageTitle: TextStyle,
    val screenTitle: TextStyle,
    val cardTitle: TextStyle,
    val body: TextStyle,
    val label: TextStyle,
    val idMetadata: TextStyle,
    val sectionLabel: TextStyle,
)

internal fun appTypography(
    styles: AppTextStyles,
    windowClass: WindowClass,
): AppTypography {
    val isPhone = windowClass == WindowClass.Phone
    return AppTypography(
        pageTitle = if (isPhone) styles.sans28SemiBold120LsMinus2 else styles.sans22SemiBold120LsMinus2,
        screenTitle = styles.sans18SemiBold120LsMinus1_5,
        cardTitle = if (isPhone) styles.sans14_5Medium138 else styles.sans13_5Medium138,
        body = if (isPhone) styles.sans15Regular150 else styles.sans13_5Regular150,
        label = styles.sans12_5Medium100,
        idMetadata = styles.mono12Regular100,
        sectionLabel = styles.mono11Medium100LsPlus6Caps,
    )
}

/** Material typography for the Material bases (menus, dialogs, sheets). Components read [AppTypography]. */
internal fun AppTypography.toMaterialTypography(): Typography =
    Typography(
        displayLarge = pageTitle,
        displayMedium = pageTitle,
        displaySmall = pageTitle,
        headlineLarge = pageTitle,
        headlineMedium = pageTitle,
        headlineSmall = screenTitle,
        titleLarge = screenTitle,
        titleMedium = cardTitle,
        titleSmall = label,
        bodyLarge = body,
        bodyMedium = body,
        bodySmall = label,
        labelLarge = label,
        labelMedium = label,
        labelSmall = idMetadata,
    )
