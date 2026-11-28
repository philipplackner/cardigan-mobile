package com.plcoding.cardiganapp.core.designsystem.components.inputs

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.plcoding.cardiganapp.core.designsystem.components.interaction.isTouchSize
import com.plcoding.cardiganapp.core.designsystem.components.interaction.rememberPreviewInteractionSource
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import com.plcoding.cardiganapp.core.designsystem.theme.WindowClass

enum class TextAreaType {
    /** Quick add. */
    MultiLine,

    /** Card title in the card detail. */
    Title,

    /** Plain-text card description. */
    Description,
}

/**
 * Multi-line field that grows with its content and never scrolls itself. A Description inside a
 * scrolling container (the card detail Panel body) makes that container scroll instead.
 */
@Composable
fun TextArea(
    value: String,
    onValueChange: (String) -> Unit,
    type: TextAreaType,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    interactionSource: MutableInteractionSource? = null,
) {
    val colors = AppTheme.colors
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isFocused by source.collectIsFocusedAsState()
    val minHeight = if (isTouchSize) AppTheme.height.controlTouch else AppTheme.height.input
    val padding =
        when (type) {
            TextAreaType.MultiLine, TextAreaType.Title -> {
                PaddingValues(
                    horizontal = AppTheme.space.s10,
                    vertical = AppTheme.space.s8,
                )
            }

            TextAreaType.Description -> {
                PaddingValues(horizontal = AppTheme.space.s12, vertical = AppTheme.space.s10)
            }
        }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        textStyle = textAreaStyle(type).copy(color = colors.text.primary),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = false,
        interactionSource = source,
        cursorBrush = SolidColor(colors.accent.default),
        decorationBox = { innerTextField ->
            Box(
                modifier =
                    Modifier
                        .fieldContainer(isFocused = isFocused, isError = isError, readOnly = false, enabled = true)
                        .heightIn(min = minHeight)
                        .padding(padding),
            ) {
                innerTextField()
            }
        },
    )
}

@Composable
private fun textAreaStyle(type: TextAreaType): TextStyle {
    val styles = AppTheme.textStyles
    val touch = isTouchSize
    return when (type) {
        TextAreaType.MultiLine -> if (touch) styles.sans15Regular150 else styles.sans13_5Regular150
        TextAreaType.Title -> if (touch) styles.sans15SemiBold150LsMinus1 else styles.sans18SemiBold150LsMinus1
        TextAreaType.Description -> if (touch) styles.sans15Regular155 else styles.sans13_5Regular155
    }
}

private enum class TextAreaPreviewState { Default, Focus, Error }

@Composable
private fun TextAreaMatrix() {
    Column(
        modifier =
            Modifier
                .background(AppTheme.colors.background.app)
                .padding(AppTheme.space.s24),
        verticalArrangement = Arrangement.spacedBy(AppTheme.space.s16),
    ) {
        TextAreaType.entries.forEach { type ->
            Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.space.s16)) {
                TextAreaPreviewState.entries.forEach { state ->
                    TextArea(
                        value = "Text",
                        onValueChange = {},
                        type = type,
                        modifier = Modifier.width(240.dp),
                        isError = state == TextAreaPreviewState.Error,
                        interactionSource =
                            rememberPreviewInteractionSource(
                                focused =
                                    state == TextAreaPreviewState.Focus,
                            ),
                    )
                }
            }
        }
    }
}

@Preview(name = "Text area · Tablet · Light", widthDp = 834)
@Composable
private fun TextAreaTabletLightPreview() {
    AppTheme(darkTheme = false, windowClass = WindowClass.Tablet) { TextAreaMatrix() }
}

@Preview(name = "Text area · Tablet · Dark", widthDp = 834)
@Composable
private fun TextAreaTabletDarkPreview() {
    AppTheme(darkTheme = true, windowClass = WindowClass.Tablet) { TextAreaMatrix() }
}

@Preview(name = "Text area · Phone (Touch) · Light", widthDp = 834)
@Composable
private fun TextAreaPhoneLightPreview() {
    AppTheme(darkTheme = false, windowClass = WindowClass.Phone) { TextAreaMatrix() }
}

@Preview(name = "Text area · Phone (Touch) · Dark", widthDp = 834)
@Composable
private fun TextAreaPhoneDarkPreview() {
    AppTheme(darkTheme = true, windowClass = WindowClass.Phone) { TextAreaMatrix() }
}

@Preview(name = "Text area · Five lines", widthDp = 390)
@Composable
private fun TextAreaFiveLinesPreview() {
    AppTheme(darkTheme = false, windowClass = WindowClass.Phone) {
        Box(
            modifier =
                Modifier
                    .background(AppTheme.colors.background.app)
                    .padding(AppTheme.space.s24),
        ) {
            TextArea(
                value = "Line one\nLine two\nLine three\nLine four\nLine five",
                onValueChange = {},
                type = TextAreaType.MultiLine,
                modifier = Modifier.width(320.dp),
            )
        }
    }
}
