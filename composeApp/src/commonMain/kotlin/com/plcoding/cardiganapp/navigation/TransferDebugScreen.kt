package com.plcoding.cardiganapp.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.plcoding.cardiganapp.core.designsystem.components.actions.ButtonStyle
import com.plcoding.cardiganapp.core.designsystem.components.actions.CardiganButton
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import kotlinx.serialization.Serializable

@Serializable
data object TransferDebugRoute

/**
 * TEMPORARY entry point for the manual check of add-transfer-ui, debug builds only. It has no
 * design and hard-coded labels on purpose, and is removed before the branch is pushed (group 10).
 */
@Composable
fun TransferDebugScreen(
    onImportClick: () -> Unit,
    onExportClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.space.s16, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CardiganButton(text = "Import board", onClick = onImportClick)
        CardiganButton(text = "Export fixture board", onClick = onExportClick, style = ButtonStyle.Secondary)
    }
}
