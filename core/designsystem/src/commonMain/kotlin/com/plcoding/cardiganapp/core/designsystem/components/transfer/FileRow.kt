package com.plcoding.cardiganapp.core.designsystem.components.transfer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.plcoding.cardiganapp.core.designsystem.icons.CardiganIcons
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme

private val FileIconSize = 16.dp

/** Chosen file with its size. [size] is formatted by the caller. */
@Composable
fun FileRow(
    fileName: String,
    size: String,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(AppTheme.radius.r8)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .border(AppTheme.space.s1, AppTheme.colors.border.default, shape)
            .padding(horizontal = AppTheme.space.s12, vertical = AppTheme.space.s10),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.space.s10),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(CardiganIcons.File, contentDescription = null, tint = AppTheme.colors.text.primary, modifier = Modifier.size(FileIconSize))
        Text(
            text = fileName,
            style = AppTheme.textStyles.sans13Regular145,
            color = AppTheme.colors.text.primary,
            modifier = Modifier.weight(1f),
        )
        Text(text = size, style = AppTheme.textStyles.mono12Regular145, color = AppTheme.colors.text.muted, maxLines = 1)
    }
}

@Composable
private fun FileRowSample() {
    Box(
        modifier = Modifier
            .background(AppTheme.colors.background.surface)
            .padding(AppTheme.space.s24)
            .width(380.dp),
    ) {
        FileRow(fileName = "board-backup.json", size = "14 KB")
    }
}

@Preview(name = "File row · Light")
@Composable
private fun FileRowLightPreview() {
    AppTheme(darkTheme = false) { FileRowSample() }
}

@Preview(name = "File row · Dark")
@Composable
private fun FileRowDarkPreview() {
    AppTheme(darkTheme = true) { FileRowSample() }
}
