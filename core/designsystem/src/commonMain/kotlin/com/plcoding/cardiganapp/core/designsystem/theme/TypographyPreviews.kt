package com.plcoding.cardiganapp.core.designsystem.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview

@Composable
private fun TypeSpecimen() {
    val type = AppTheme.typography
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(AppTheme.colors.background.surface)
                .padding(AppTheme.space.s20),
        verticalArrangement = Arrangement.spacedBy(AppTheme.space.s18),
    ) {
        SpecimenRow("Page title", "Log in to Cardigan", type.pageTitle)
        SpecimenRow("Screen title", "Cardigan v1", type.screenTitle)
        SpecimenRow("Card title", "Single-flight token refresh in the Ktor client", type.cardTitle)
        SpecimenRow(
            "Body",
            "When several requests get a 401 at the same time, only one refresh call runs.",
            type.body,
        )
        SpecimenRow("Label", "Description", type.label)
        SpecimenRow("ID & metadata", "CRD-109 · Updated 2 hours ago", type.idMetadata)
        SpecimenRow("Section label", "Boards".uppercase(), type.sectionLabel)
    }
}

@Composable
private fun SpecimenRow(
    role: String,
    sample: String,
    style: TextStyle,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.space.s6)) {
        Text(
            text = role.uppercase(),
            style = AppTheme.typography.sectionLabel,
            color = AppTheme.colors.text.muted,
        )
        Text(text = sample, style = style, color = AppTheme.colors.text.primary)
    }
}

@Preview(name = "Type · Phone · Light", widthDp = 390)
@Composable
private fun TypePhoneLightPreview() {
    AppTheme(darkTheme = false, windowClass = WindowClass.Phone) { TypeSpecimen() }
}

@Preview(name = "Type · Phone · Dark", widthDp = 390)
@Composable
private fun TypePhoneDarkPreview() {
    AppTheme(darkTheme = true, windowClass = WindowClass.Phone) { TypeSpecimen() }
}

@Preview(name = "Type · Tablet · Light", widthDp = 834)
@Composable
private fun TypeTabletLightPreview() {
    AppTheme(darkTheme = false, windowClass = WindowClass.Tablet) { TypeSpecimen() }
}

@Preview(name = "Type · Tablet · Dark", widthDp = 834)
@Composable
private fun TypeTabletDarkPreview() {
    AppTheme(darkTheme = true, windowClass = WindowClass.Tablet) { TypeSpecimen() }
}
