package com.plcoding.cardiganapp.core.designsystem.icons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme

private val allIcons: List<Pair<String, @Composable () -> Painter>> =
    listOf(
        "more" to { CardiganIcons.More },
        "board" to { CardiganIcons.Board },
        "plus" to { CardiganIcons.Plus },
        "label" to { CardiganIcons.Label },
        "trash" to { CardiganIcons.Trash },
        "edit" to { CardiganIcons.Edit },
        "refresh" to { CardiganIcons.Refresh },
        "log-out" to { CardiganIcons.LogOut },
        "boards" to { CardiganIcons.Boards },
        "search" to { CardiganIcons.Search },
        "close" to { CardiganIcons.Close },
        "chevron-down" to { CardiganIcons.ChevronDown },
        "upload" to { CardiganIcons.Upload },
        "chevron-left" to { CardiganIcons.ChevronLeft },
        "close-small" to { CardiganIcons.CloseSmall },
        "filter" to { CardiganIcons.Filter },
        "check" to { CardiganIcons.Check },
        "alert" to { CardiganIcons.Alert },
        "rename" to { CardiganIcons.Rename },
        "file" to { CardiganIcons.File },
        "download" to { CardiganIcons.Download },
        "cloud-off" to { CardiganIcons.CloudOff },
        "info" to { CardiganIcons.Info },
        "arrow-left" to { CardiganIcons.ArrowLeft },
        "arrow-right" to { CardiganIcons.ArrowRight },
        "columns" to { CardiganIcons.Columns },
        "chevron-right" to { CardiganIcons.ChevronRight },
    )

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IconGrid() {
    FlowRow(
        modifier =
            Modifier
                .background(AppTheme.colors.background.surface)
                .padding(AppTheme.space.s16),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.space.s8),
        verticalArrangement = Arrangement.spacedBy(AppTheme.space.s16),
    ) {
        allIcons.forEach { (name, painter) ->
            Column(
                modifier = Modifier.width(80.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppTheme.space.s6),
            ) {
                Icon(painter = painter(), contentDescription = null, tint = AppTheme.colors.text.primary)
                Text(text = name, style = AppTheme.textStyles.mono11Regular100, color = AppTheme.colors.text.muted)
            }
        }
    }
}

@Preview(name = "Icons · Light", widthDp = 400)
@Composable
private fun IconsLightPreview() {
    AppTheme(darkTheme = false) { IconGrid() }
}

@Preview(name = "Icons · Dark", widthDp = 400)
@Composable
private fun IconsDarkPreview() {
    AppTheme(darkTheme = true) { IconGrid() }
}
