package com.plcoding.cardiganapp.core.designsystem.components.inputs

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.plcoding.cardiganapp.core.designsystem.components.interaction.isTouchSize
import com.plcoding.cardiganapp.core.designsystem.components.interaction.rememberPreviewInteractionSource
import com.plcoding.cardiganapp.core.designsystem.icons.CardiganIcons
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import com.plcoding.cardiganapp.core.designsystem.theme.WindowClass

/** Size for icons in the Input's leading and trailing slots. */
val InputIconSize = 16.dp

/**
 * Single-line text field. Default size on tablet and desktop, Touch on phones. The leading and
 * trailing slots get `text/muted` as content color; use [InputIconSize] for icons in them.
 */
@Composable
fun Input(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    isError: Boolean = false,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    interactionSource: MutableInteractionSource? = null,
) {
    val colors = AppTheme.colors
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isFocused by source.collectIsFocusedAsState()
    val textStyle = inputTextStyle().copy(color = if (readOnly) colors.text.muted else colors.text.primary)
    val minHeight = if (isTouchSize) AppTheme.height.controlTouch else AppTheme.height.input

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = true,
        visualTransformation = visualTransformation,
        interactionSource = source,
        cursorBrush = SolidColor(colors.accent.default),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fieldContainer(isFocused = isFocused, isError = isError, readOnly = readOnly, enabled = enabled)
                    .heightIn(min = minHeight)
                    .padding(horizontal = AppTheme.space.s10),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.space.s8),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CompositionLocalProvider(LocalContentColor provides colors.text.muted) {
                    leadingIcon?.invoke()
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty() && placeholder != null) {
                            Text(
                                text = placeholder,
                                style = textStyle,
                                color = colors.text.faint,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        innerTextField()
                    }
                    trailingIcon?.invoke()
                }
            }
        },
    )
}

@Composable
private fun inputTextStyle(): TextStyle =
    if (isTouchSize) AppTheme.textStyles.sans15Regular145 else AppTheme.textStyles.sans13_5Regular145

private enum class InputPreviewState { Default, Focus, Error, ReadOnly, Disabled }

@Composable
private fun InputMatrix() {
    Column(
        modifier = Modifier
            .background(AppTheme.colors.background.app)
            .padding(AppTheme.space.s24),
        verticalArrangement = Arrangement.spacedBy(AppTheme.space.s16),
    ) {
        InputPreviewState.entries.forEach { state ->
            Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.space.s16)) {
                listOf("Value", "").forEach { value ->
                    PreviewInput(state = state, value = value, modifier = Modifier.width(280.dp))
                }
            }
        }
    }
}

@Composable
private fun PreviewInput(state: InputPreviewState, value: String, modifier: Modifier = Modifier) {
    val source = rememberPreviewInteractionSource(focused = state == InputPreviewState.Focus)
    Input(
        value = value,
        onValueChange = {},
        modifier = modifier,
        placeholder = "Placeholder",
        isError = state == InputPreviewState.Error,
        readOnly = state == InputPreviewState.ReadOnly,
        enabled = state != InputPreviewState.Disabled,
        interactionSource = source,
    )
}

@Preview(name = "Input · Tablet · Light", widthDp = 834)
@Composable
private fun InputTabletLightPreview() {
    AppTheme(darkTheme = false, windowClass = WindowClass.Tablet) { InputMatrix() }
}

@Preview(name = "Input · Tablet · Dark", widthDp = 834)
@Composable
private fun InputTabletDarkPreview() {
    AppTheme(darkTheme = true, windowClass = WindowClass.Tablet) { InputMatrix() }
}

@Preview(name = "Input · Phone (Touch) · Light", widthDp = 834)
@Composable
private fun InputPhoneLightPreview() {
    AppTheme(darkTheme = false, windowClass = WindowClass.Phone) { InputMatrix() }
}

@Preview(name = "Input · Phone (Touch) · Dark", widthDp = 834)
@Composable
private fun InputPhoneDarkPreview() {
    AppTheme(darkTheme = true, windowClass = WindowClass.Phone) { InputMatrix() }
}

@Composable
private fun InputIconSamples() {
    Column(
        modifier = Modifier
            .background(AppTheme.colors.background.app)
            .padding(AppTheme.space.s24),
        verticalArrangement = Arrangement.spacedBy(AppTheme.space.s16),
    ) {
        Input(
            value = "",
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            placeholder = "Search cards",
            leadingIcon = { Icon(CardiganIcons.Search, contentDescription = null, modifier = Modifier.size(InputIconSize)) },
            trailingIcon = { Icon(CardiganIcons.Close, contentDescription = null, modifier = Modifier.size(InputIconSize)) },
        )
        Input(
            value = "correct horse",
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
        )
    }
}

@Preview(name = "Input · Icons · Light", widthDp = 390)
@Composable
private fun InputIconsLightPreview() {
    AppTheme(darkTheme = false, windowClass = WindowClass.Phone) { InputIconSamples() }
}

@Preview(name = "Input · Icons · Dark", widthDp = 390)
@Composable
private fun InputIconsDarkPreview() {
    AppTheme(darkTheme = true, windowClass = WindowClass.Phone) { InputIconSamples() }
}
