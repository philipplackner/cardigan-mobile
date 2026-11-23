package com.plcoding.cardiganapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.plcoding.cardiganapp.core.domain.session.SessionState
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import com.plcoding.cardiganapp.di.AppBuildInfo
import com.plcoding.cardiganapp.feature.auth.data.fixture.InMemorySessionManager
import com.plcoding.cardiganapp.feature.boards.domain.BoardRepository
import com.plcoding.cardiganapp.navigation.AppNavHost
import com.plcoding.cardiganapp.session.AppViewModel
import com.plcoding.cardiganapp.session.DebugSessionOverlay
import com.plcoding.cardiganapp.session.SessionGate
import com.plcoding.cardiganapp.shell.AppShell
import com.plcoding.cardiganapp.toast.AppToastController
import com.plcoding.cardiganapp.toast.AppToastHost
import org.koin.compose.getKoin
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * The app root. While the session restores, it draws only the app background (Android keeps its
 * splash). After that, [SessionGate] decides the start screen and follows every session change.
 */
@Composable
fun App(
    appViewModel: AppViewModel = koinViewModel(),
    navController: NavHostController = rememberNavController(),
) {
    AppTheme {
        val toastController = koinInject<AppToastController>()
        val boardRepository = koinInject<BoardRepository>()
        val session by appViewModel.state.collectAsStateWithLifecycle()
        val signedInEmail = (session as? SessionState.SignedIn)?.email
        LaunchedEffect(boardRepository, signedInEmail) {
            if (signedInEmail != null) boardRepository.ensureLoaded()
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppTheme.colors.background.app),
        ) {
            if (session != SessionState.Restoring) {
                val accountEmail by rememberUpdatedState(signedInEmail)
                val onLogOut = appViewModel::onLogout
                // TODO(board-view): navigate to BoardRoute(boardId, name, key) once add-board-view-ui lands.
                val onOpenBoard: (String, String, String) -> Unit = { _, _, _ -> }
                // TODO(labels): navigate to the labels page once mob-labels-ui lands.
                val onLabelsClick = {}
                SessionGate(session = session, navController = navController) { startDestination ->
                    // Kept in one movable slot, so the NavHost survives the shell appearing at sign-in.
                    val host = remember(navController) {
                        movableContentOf { contentModifier: Modifier ->
                            AppNavHost(
                                navController = navController,
                                startDestination = startDestination,
                                accountEmail = accountEmail,
                                onLogOut = onLogOut,
                                onOpenBoard = onOpenBoard,
                                onLabelsClick = onLabelsClick,
                                modifier = contentModifier,
                            )
                        }
                    }
                    if (signedInEmail != null) {
                        AppShell(
                            navController = navController,
                            accountEmail = accountEmail,
                            onLogOut = onLogOut,
                            onOpenBoard = onOpenBoard,
                            onLabelsClick = onLabelsClick,
                        ) { contentModifier -> host(contentModifier) }
                    } else {
                        host(Modifier.fillMaxSize())
                    }
                }
            }
            // TEMPORARY (D3): removed with the auth fixture in add-auth-ui task 9.1.
            val fixtureSession = koinInject<AppBuildInfo>().takeIf { it.isDebug }?.let { getKoin().getOrNull<InMemorySessionManager>() }
            if (fixtureSession != null && signedInEmail != null) {
                DebugSessionOverlay(
                    onExpireSession = fixtureSession::expire,
                    onLogOut = appViewModel::onLogout,
                    modifier = Modifier.align(Alignment.TopEnd),
                )
            }
            AppToastHost(controller = toastController, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}
