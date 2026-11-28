package com.plcoding.cardiganapp.core.designsystem.components.inputs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.plcoding.cardiganapp.core.designsystem.icons.CardiganIcons
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme

private val ErrorIconSize = 16.dp

/** Inline validation message below a field. Screen readers announce it when it appears. */
@Composable
fun ErrorMessage(
    text: String,
    modifier: Modifier = Modifier,
) {
    val color = AppTheme.colors.danger.default
    Row(
        modifier = modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(AppTheme.space.s5),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = CardiganIcons.Alert,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(ErrorIconSize),
        )
        Text(text = text, style = AppTheme.textStyles.sans12Regular145, color = color)
    }
}

@Preview(name = "Error message · Light")
@Composable
private fun ErrorMessageLightPreview() {
    AppTheme(darkTheme = false) {
        ErrorMessage(
            text = "Something is wrong.",
            modifier = Modifier.background(AppTheme.colors.background.app).padding(AppTheme.space.s24),
        )
    }
}

@Preview(name = "Error message · Dark")
@Composable
private fun ErrorMessageDarkPreview() {
    AppTheme(darkTheme = true) {
        ErrorMessage(
            text = "Something is wrong.",
            modifier = Modifier.background(AppTheme.colors.background.app).padding(AppTheme.space.s24),
        )
    }
}
