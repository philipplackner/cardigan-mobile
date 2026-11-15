package com.plcoding.cardiganapp.core.designsystem.components.overlays

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.plcoding.cardiganapp.core.designsystem.components.actions.ButtonStyle
import com.plcoding.cardiganapp.core.designsystem.components.actions.CardiganButton
import com.plcoding.cardiganapp.core.designsystem.components.feedback.Spinner
import com.plcoding.cardiganapp.core.designsystem.components.feedback.SpinnerSize
import com.plcoding.cardiganapp.core.designsystem.components.inputs.Field
import com.plcoding.cardiganapp.core.designsystem.components.inputs.Input
import com.plcoding.cardiganapp.core.designsystem.components.interaction.isTouchSize
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import com.plcoding.cardiganapp.core.designsystem.theme.WindowClass
import com.plcoding.cardiganapp.core.designsystem.theme.dropShadow

private val DialogWidth = 420.dp

/**
 * Modal dialog. Fields go in [content], buttons in [actions]; on phones set the buttons to
 * `Modifier.weight(1f)` so they fill. Back, Escape and a scrim tap call [onDismissRequest].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardiganDialog(
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit,
) {
    val scrim = AppTheme.colors.background.scrim
    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        properties = cardiganDialogProperties(dismissible = true, scrimColor = scrim),
    ) {
        DialogScrimEffect(scrim)
        DialogContent(title = title, description = description, modifier = modifier, content = content, actions = actions)
    }
}

/**
 * Progress dialog with a large spinner. It can't be dismissed: back, Escape and scrim taps do
 * nothing. Remove it from composition when the work is done.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressDialog(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    val scrim = AppTheme.colors.background.scrim
    BasicAlertDialog(
        onDismissRequest = {},
        properties = cardiganDialogProperties(dismissible = false, scrimColor = scrim),
    ) {
        DialogScrimEffect(scrim)
        ProgressDialogContent(title = title, description = description, modifier = modifier)
    }
}

@Composable
private fun Modifier.dialogContainer(): Modifier {
    val shape = RoundedCornerShape(AppTheme.radius.r12)
    val sized = if (isTouchSize) {
        fillMaxWidth().padding(horizontal = AppTheme.space.s16)
    } else {
        width(DialogWidth)
    }
    return sized
        .dropShadow(shape, AppTheme.shadows.popover)
        .background(AppTheme.colors.background.surface, shape)
        .border(AppTheme.space.s1, AppTheme.colors.border.default, shape)
}

@Composable
private fun DialogContent(
    title: String,
    description: String?,
    modifier: Modifier = Modifier,
    content: (@Composable ColumnScope.() -> Unit)?,
    actions: @Composable RowScope.() -> Unit,
) {
    val touch = isTouchSize
    Column(
        modifier = modifier
            .dialogContainer()
            .padding(if (touch) AppTheme.space.s22 else AppTheme.space.s20),
        verticalArrangement = Arrangement.spacedBy(AppTheme.space.s16),
    ) {
        DialogTitle(title)
        if (description != null) DialogDescription(description)
        if (content != null) {
            Column(verticalArrangement = Arrangement.spacedBy(AppTheme.space.s16), content = content)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.space.s8, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
            content = actions,
        )
    }
}

@Composable
private fun ProgressDialogContent(
    title: String,
    description: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .dialogContainer()
            .padding(PaddingValues(horizontal = AppTheme.space.s24, vertical = AppTheme.space.s28)),
        verticalArrangement = Arrangement.spacedBy(AppTheme.space.s16),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DialogTitle(title, textAlign = TextAlign.Center)
        if (description != null) DialogDescription(description, textAlign = TextAlign.Center)
        Spinner(size = SpinnerSize.Large)
    }
}

@Composable
private fun DialogTitle(text: String, textAlign: TextAlign? = null) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth(),
        style = if (isTouchSize) AppTheme.textStyles.sans17SemiBold130LsMinus1 else AppTheme.textStyles.sans16SemiBold130LsMinus1,
        color = AppTheme.colors.text.primary,
        textAlign = textAlign,
    )
}

@Composable
private fun DialogDescription(text: String, textAlign: TextAlign? = null) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth(),
        style = if (isTouchSize) AppTheme.textStyles.sans14_5Regular145 else AppTheme.textStyles.sans13_5Regular145,
        color = AppTheme.colors.text.muted,
        textAlign = textAlign,
    )
}

@Composable
private fun DialogSample() {
    val touch = isTouchSize
    Box(
        modifier = Modifier
            .background(AppTheme.colors.background.app)
            .padding(vertical = AppTheme.space.s24),
    ) {
        DialogContent(
            title = "Title",
            description = "Description",
            content = {
                Field(label = "Label") {
                    Input(value = "", onValueChange = {}, placeholder = "Placeholder", modifier = Modifier.fillMaxWidth())
                }
            },
            actions = {
                val buttonModifier = if (touch) Modifier.weight(1f) else Modifier
                CardiganButton(text = "Cancel", onClick = {}, style = ButtonStyle.Secondary, modifier = buttonModifier)
                CardiganButton(text = "Create", onClick = {}, modifier = buttonModifier)
            },
        )
    }
}

@Composable
private fun ProgressDialogSample() {
    Box(
        modifier = Modifier
            .background(AppTheme.colors.background.app)
            .padding(vertical = AppTheme.space.s24),
    ) {
        ProgressDialogContent(title = "Title", description = "Description")
    }
}

@Preview(name = "Dialog · Tablet · Light", widthDp = 480)
@Composable
private fun DialogTabletLightPreview() {
    AppTheme(darkTheme = false, windowClass = WindowClass.Tablet) { DialogSample() }
}

@Preview(name = "Dialog · Tablet · Dark", widthDp = 480)
@Composable
private fun DialogTabletDarkPreview() {
    AppTheme(darkTheme = true, windowClass = WindowClass.Tablet) { DialogSample() }
}

@Preview(name = "Dialog · Phone (Touch) · Light", widthDp = 390)
@Composable
private fun DialogPhoneLightPreview() {
    AppTheme(darkTheme = false, windowClass = WindowClass.Phone) { DialogSample() }
}

@Preview(name = "Dialog · Phone (Touch) · Dark", widthDp = 390)
@Composable
private fun DialogPhoneDarkPreview() {
    AppTheme(darkTheme = true, windowClass = WindowClass.Phone) { DialogSample() }
}

@Preview(name = "Progress dialog · Tablet · Light", widthDp = 480)
@Composable
private fun ProgressDialogTabletLightPreview() {
    AppTheme(darkTheme = false, windowClass = WindowClass.Tablet) { ProgressDialogSample() }
}

@Preview(name = "Progress dialog · Phone (Touch) · Dark", widthDp = 390)
@Composable
private fun ProgressDialogPhoneDarkPreview() {
    AppTheme(darkTheme = true, windowClass = WindowClass.Phone) { ProgressDialogSample() }
}
