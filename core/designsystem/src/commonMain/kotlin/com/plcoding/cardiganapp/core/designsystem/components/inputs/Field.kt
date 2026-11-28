package com.plcoding.cardiganapp.core.designsystem.components.inputs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.plcoding.cardiganapp.core.designsystem.components.interaction.isTouchSize
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import com.plcoding.cardiganapp.core.designsystem.theme.WindowClass

/**
 * Label above a control. Put the control, help text or [ErrorMessage] in [control]. Pass
 * `label = null` to hide the label.
 */
@Composable
fun Field(
    label: String?,
    modifier: Modifier = Modifier,
    control: @Composable ColumnScope.() -> Unit,
) {
    val gap = AppTheme.space.s6
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(gap)) {
        if (label != null) {
            Text(
                text = label,
                style =
                    if (isTouchSize) AppTheme.textStyles.sans13_5Medium100 else AppTheme.textStyles.sans12_5Medium100,
                color = AppTheme.colors.text.primary,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(gap), content = control)
    }
}

@Composable
private fun FieldSamples() {
    Column(
        modifier =
            Modifier
                .background(AppTheme.colors.background.app)
                .padding(AppTheme.space.s24)
                .width(320.dp),
        verticalArrangement = Arrangement.spacedBy(AppTheme.space.s24),
    ) {
        Field(label = "Label") {
            Input(value = "", onValueChange = {}, placeholder = "Placeholder", modifier = Modifier.fillMaxWidth())
        }
        Field(label = "Email") {
            Input(value = "philipp@", onValueChange = {}, isError = true, modifier = Modifier.fillMaxWidth())
            ErrorMessage(text = "Something is wrong.")
        }
    }
}

@Preview(name = "Field · Tablet · Light")
@Composable
private fun FieldTabletLightPreview() {
    AppTheme(darkTheme = false, windowClass = WindowClass.Tablet) { FieldSamples() }
}

@Preview(name = "Field · Tablet · Dark")
@Composable
private fun FieldTabletDarkPreview() {
    AppTheme(darkTheme = true, windowClass = WindowClass.Tablet) { FieldSamples() }
}

@Preview(name = "Field · Phone (Touch) · Light")
@Composable
private fun FieldPhoneLightPreview() {
    AppTheme(darkTheme = false, windowClass = WindowClass.Phone) { FieldSamples() }
}

@Preview(name = "Field · Phone (Touch) · Dark")
@Composable
private fun FieldPhoneDarkPreview() {
    AppTheme(darkTheme = true, windowClass = WindowClass.Phone) { FieldSamples() }
}
