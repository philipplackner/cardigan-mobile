package com.plcoding.cardiganapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import com.plcoding.cardiganapp.feature.boards.domain.BoardRepository
import com.plcoding.cardiganapp.feature.boards.presentation.navigation.navigateToBoard
import com.plcoding.cardiganapp.navigation.AppNavHost
import com.plcoding.cardiganapp.shell.AppShell
import com.plcoding.cardiganapp.toast.AppToastController
import com.plcoding.cardiganapp.toast.AppToastHost
import org.koin.compose.koinInject

@Composable
fun App() {
    AppTheme {
        val toastController = koinInject<AppToastController>()
        val boardRepository = koinInject<BoardRepository>()
        // TODO(auth): start the first load once the session is SignedIn, when add-auth-ui lands.
        LaunchedEffect(boardRepository) { boardRepository.ensureLoaded() }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppTheme.colors.background.app),
        ) {
            val navController = rememberNavController()
            // TODO(auth): feed the email from SessionManager.state and Log out from AppViewModel.onLogout() (D20).
            val accountEmail: String? = null
            val onLogOut = {}
            val onOpenBoard: (String, String, String) -> Unit = { id, name, key -> navController.navigateToBoard(id, name, key) }
            // TODO(labels): navigate to the labels page once mob-labels-ui lands.
            val onLabelsClick = {}
            AppShell(
                navController = navController,
                accountEmail = accountEmail,
                onLogOut = onLogOut,
                onOpenBoard = onOpenBoard,
                onLabelsClick = onLabelsClick,
            ) { contentModifier ->
                AppNavHost(
                    navController = navController,
                    accountEmail = accountEmail,
                    onLogOut = onLogOut,
                    onOpenBoard = onOpenBoard,
                    onLabelsClick = onLabelsClick,
                    modifier = contentModifier,
                )
            }
            AppToastHost(controller = toastController, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}
