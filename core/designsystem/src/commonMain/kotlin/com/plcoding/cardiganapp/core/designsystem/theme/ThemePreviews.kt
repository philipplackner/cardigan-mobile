package com.plcoding.cardiganapp.core.designsystem.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Composable
private fun ThemeSurfaceSample() {
    val shape = RoundedCornerShape(AppTheme.radius.r10)
    Column(
        modifier =
            Modifier
                .background(AppTheme.colors.background.app)
                .padding(AppTheme.space.s24),
    ) {
        Column(
            modifier =
                Modifier
                    .dropShadow(shape, AppTheme.shadows.small)
                    .background(AppTheme.colors.background.surface, shape)
                    .border(width = AppTheme.space.s1, color = AppTheme.colors.border.default, shape = shape)
                    .padding(AppTheme.space.s16),
            verticalArrangement = Arrangement.spacedBy(AppTheme.space.s4),
        ) {
            Text(text = "Primary text", color = AppTheme.colors.text.primary)
            Text(text = "Muted text", color = AppTheme.colors.text.muted)
            Text(text = "Faint text", color = AppTheme.colors.text.faint)
            Text(text = "Accent", color = AppTheme.colors.accent.default)
            Text(text = "Danger", color = AppTheme.colors.danger.default)
        }
    }
}

@Preview(name = "Theme surface · Light")
@Composable
private fun ThemeSurfaceLightPreview() {
    AppTheme(darkTheme = false) { ThemeSurfaceSample() }
}

@Preview(name = "Theme surface · Dark")
@Composable
private fun ThemeSurfaceDarkPreview() {
    AppTheme(darkTheme = true) { ThemeSurfaceSample() }
}
