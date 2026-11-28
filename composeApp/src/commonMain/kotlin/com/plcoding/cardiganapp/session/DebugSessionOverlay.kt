package com.plcoding.cardiganapp.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.plcoding.cardiganapp.core.designsystem.components.actions.ButtonStyle
import com.plcoding.cardiganapp.core.designsystem.components.actions.CardiganButton
import com.plcoding.cardiganapp.core.designsystem.components.interaction.ControlSize
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme

/**
 * TEMPORARY overlay for the manual check of add-auth-ui (D3), debug builds only and only while
 * signed in. It has no design and hard-coded labels on purpose, and is removed in task 9.1.
 */
@Composable
fun DebugSessionOverlay(
    onExpireSession: () -> Unit,
    onLogOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.safeDrawingPadding().padding(AppTheme.space.s8),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.space.s8),
    ) {
        CardiganButton(
            text = "Expire session",
            onClick = onExpireSession,
            style = ButtonStyle.Secondary,
            size = ControlSize.Small,
        )
        CardiganButton(text = "Log out", onClick = onLogOut, style = ButtonStyle.Secondary, size = ControlSize.Small)
    }
}
