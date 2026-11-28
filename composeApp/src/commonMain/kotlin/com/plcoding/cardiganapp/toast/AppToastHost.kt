package com.plcoding.cardiganapp.toast

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.plcoding.cardiganapp.core.designsystem.components.feedback.ToastHost
import com.plcoding.cardiganapp.core.designsystem.components.feedback.ToastHostState
import com.plcoding.cardiganapp.core.designsystem.components.interaction.isTouchSize
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import com.plcoding.cardiganapp.core.presentation.toast.ToastType
import com.plcoding.cardiganapp.core.presentation.util.ObserveAsEvents
import kotlinx.coroutines.launch
import com.plcoding.cardiganapp.core.designsystem.components.feedback.ToastType as DesignToastType

/** The app-wide toast host. Place it at the bottom center of the app. */
@Composable
fun AppToastHost(
    controller: AppToastController,
    modifier: Modifier = Modifier,
) {
    val state = remember { ToastHostState() }
    val scope = rememberCoroutineScope()
    ObserveAsEvents(controller.messages) { message ->
        scope.launch { state.show(message.text.asStringAsync(), message.type.toDesignToastType()) }
    }
    ToastHost(
        state = state,
        modifier =
            modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = if (isTouchSize) AppTheme.space.s28 else AppTheme.space.s24),
    )
}

private fun ToastType.toDesignToastType(): DesignToastType =
    when (this) {
        ToastType.Success -> DesignToastType.Success
        ToastType.Error -> DesignToastType.Error
    }
