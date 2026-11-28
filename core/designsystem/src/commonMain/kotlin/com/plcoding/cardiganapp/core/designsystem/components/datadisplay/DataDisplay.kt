package com.plcoding.cardiganapp.core.designsystem.components.datadisplay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme

private val AvatarSize = 26.dp

/** Board key badge. [key] is 2 to 5 uppercase letters, as the shared `domain-board` spec allows. */
@Composable
fun Key(
    key: String,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(AppTheme.radius.r4)
    Text(
        text = key,
        modifier =
            modifier
                .background(AppTheme.colors.background.surface, shape)
                .border(AppTheme.space.s1, AppTheme.colors.border.strong, shape)
                .padding(horizontal = AppTheme.space.s5, vertical = AppTheme.space.s3),
        style = AppTheme.textStyles.mono11Medium100LsPlus2,
        color = AppTheme.colors.text.muted,
        maxLines = 1,
    )
}

/** Account initial. */
@Composable
fun Avatar(
    initial: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .size(AvatarSize)
                .background(AppTheme.colors.accent.soft, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            style = AppTheme.textStyles.sans11_5SemiBold100,
            color = AppTheme.colors.accent.default,
            maxLines = 1,
        )
    }
}

@Composable
private fun DataDisplaySamples() {
    Row(
        modifier =
            Modifier
                .background(AppTheme.colors.background.app)
                .padding(AppTheme.space.s24),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.space.s16),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Key(key = "CR")
        Key(key = "CARDS")
        Avatar(initial = "P")
    }
}

@Preview(name = "Key and avatar · Light")
@Composable
private fun DataDisplayLightPreview() {
    AppTheme(darkTheme = false) { DataDisplaySamples() }
}

@Preview(name = "Key and avatar · Dark")
@Composable
private fun DataDisplayDarkPreview() {
    AppTheme(darkTheme = true) { DataDisplaySamples() }
}
