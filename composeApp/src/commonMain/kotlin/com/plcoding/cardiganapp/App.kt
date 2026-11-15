package com.plcoding.cardiganapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import com.plcoding.cardiganapp.di.AppBuildInfo
import com.plcoding.cardiganapp.navigation.AppNavHost
import com.plcoding.cardiganapp.toast.AppToastController
import com.plcoding.cardiganapp.toast.AppToastHost
import org.koin.compose.koinInject

@Composable
fun App() {
    AppTheme {
        val toastController = koinInject<AppToastController>()
        val buildInfo = koinInject<AppBuildInfo>()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppTheme.colors.background.app),
        ) {
            AppNavHost(
                navController = rememberNavController(),
                isDebug = buildInfo.isDebug,
                modifier = Modifier.fillMaxSize(),
            )
            AppToastHost(controller = toastController, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}
