package com.plcoding.cardiganapp.session

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import com.plcoding.cardiganapp.core.domain.session.SessionState
import com.plcoding.cardiganapp.core.domain.session.SignOutReason
import com.plcoding.cardiganapp.feature.auth.presentation.navigation.LoginRoute
import com.plcoding.cardiganapp.feature.auth.presentation.navigation.RegisterRoute
import com.plcoding.cardiganapp.feature.boards.presentation.navigation.BoardListRoute

/**
 * Picks the start destination from the first known [session] state and keeps the back stack in
 * line with every later one: signing in replaces the auth screens with the board list (D8), and
 * signing out replaces everything, dialogs included, with Log in (D7, D9). The same check also
 * clears a back stack restored after process death that no longer fits the session.
 * Never call it while the state is [SessionState.Restoring].
 */
@Composable
fun SessionGate(
    session: SessionState,
    navController: NavHostController,
    content: @Composable (startDestination: Any) -> Unit,
) {
    val startDestination = remember { destinationFor(session) }
    content(startDestination)
    LaunchedEffect(session, navController) {
        val current = navController.currentBackStackEntry?.destination ?: return@LaunchedEffect
        when (session) {
            is SessionState.SignedIn -> {
                if (current.isAuthScreen()) navController.replaceBackStackWith(BoardListRoute)
            }

            is SessionState.SignedOut -> {
                if (!current.isAuthScreen()) {
                    navController.replaceBackStackWith(
                        destinationFor(session),
                    )
                }
            }

            SessionState.Restoring -> {
                Unit
            }
        }
    }
}

internal fun destinationFor(session: SessionState): Any =
    when (session) {
        is SessionState.SignedOut -> LoginRoute(sessionExpired = session.reason == SignOutReason.Expired)
        is SessionState.SignedIn, SessionState.Restoring -> BoardListRoute
    }

private fun NavDestination.isAuthScreen(): Boolean = hasRoute<LoginRoute>() || hasRoute<RegisterRoute>()

private fun NavHostController.replaceBackStackWith(route: Any) {
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}
