package com.plcoding.cardiganapp.core.designsystem.components.feedback

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cardiganapp.core.designsystem.generated.resources.Res
import cardiganapp.core.designsystem.generated.resources.toast_close
import com.plcoding.cardiganapp.core.designsystem.icons.CardiganIcons
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import com.plcoding.cardiganapp.core.designsystem.theme.dropShadow
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

enum class ToastType {
    Success,
    Error,
}

private val ToastDotSize = 8.dp
private val ToastCloseIconSize = 16.dp
private const val ToastCloseAlpha = 0.7f

/** Short confirmation or error. Shown through [ToastHost]. */
@Composable
fun Toast(
    message: String,
    type: ToastType,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(AppTheme.radius.r8)
    Row(
        modifier = modifier
            .semantics { liveRegion = LiveRegionMode.Polite }
            .dropShadow(shape, AppTheme.shadows.popover)
            .background(colors.background.toast, shape)
            .padding(PaddingValues(start = AppTheme.space.s14, top = AppTheme.space.s10, end = AppTheme.space.s10, bottom = AppTheme.space.s10)),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.space.s12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(ToastDotSize)
                .background(if (type == ToastType.Success) colors.success.default else colors.danger.default, CircleShape),
        )
        Text(
            text = message,
            modifier = Modifier.weight(1f, fill = false),
            style = AppTheme.textStyles.sans13_5Regular145,
            color = colors.text.toast,
        )
        CompositionLocalProvider(LocalRippleConfiguration provides RippleConfiguration(color = colors.text.toast)) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(ToastCloseIconSize),
                colors = IconButtonDefaults.iconButtonColors(contentColor = colors.text.toast),
            ) {
                Icon(
                    painter = CardiganIcons.Close,
                    contentDescription = stringResource(Res.string.toast_close),
                    modifier = Modifier.size(ToastCloseIconSize).alpha(ToastCloseAlpha),
                )
            }
        }
    }
}

private class ToastVisuals(
    override val message: String,
    val type: ToastType,
    override val actionLabel: String,
) : SnackbarVisuals {
    override val withDismissAction: Boolean = false

    // Success: Material's Short is 4s and on Android goes through the accessibility timeout.
    override val duration: SnackbarDuration = when (type) {
        ToastType.Success -> SnackbarDuration.Short
        ToastType.Error -> SnackbarDuration.Indefinite
    }
}

/**
 * Shows one toast at a time. A new toast replaces the one showing; there is no queue. Success
 * toasts close after 4s (longer when Android's accessibility timeout asks for it), error toasts
 * stay until the user closes them.
 */
@Stable
class ToastHostState {
    internal val snackbarHostState = SnackbarHostState()

    /** Suspends until the toast closes or is replaced. */
    suspend fun show(message: String, type: ToastType) {
        snackbarHostState.currentSnackbarData?.dismiss()
        // The close control counts as an action, so Android applies its "Time to take action" timeout.
        snackbarHostState.showSnackbar(ToastVisuals(message, type, actionLabel = getString(Res.string.toast_close)))
    }
}

/** Place at the bottom of the screen, horizontally centered. The screen sets the bottom inset. */
@Composable
fun ToastHost(
    state: ToastHostState,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(hostState = state.snackbarHostState, modifier = modifier) { data ->
        val visuals = data.visuals as ToastVisuals
        Toast(message = visuals.message, type = visuals.type, onClose = data::dismiss)
    }
}

@Composable
private fun ToastSamples() {
    Column(
        modifier = Modifier
            .background(AppTheme.colors.background.app)
            .padding(AppTheme.space.s24),
        verticalArrangement = Arrangement.spacedBy(AppTheme.space.s16),
    ) {
        Toast(message = "Message", type = ToastType.Success, onClose = {})
        Toast(message = "Message", type = ToastType.Error, onClose = {})
    }
}

@Preview(name = "Toast · Light")
@Composable
private fun ToastLightPreview() {
    AppTheme(darkTheme = false) { ToastSamples() }
}

@Preview(name = "Toast · Dark")
@Composable
private fun ToastDarkPreview() {
    AppTheme(darkTheme = true) { ToastSamples() }
}
