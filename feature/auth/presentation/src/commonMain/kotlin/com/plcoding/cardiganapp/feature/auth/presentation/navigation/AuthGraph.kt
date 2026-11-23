package com.plcoding.cardiganapp.feature.auth.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.plcoding.cardiganapp.feature.auth.presentation.login.LoginRoot
import com.plcoding.cardiganapp.feature.auth.presentation.login.LoginViewModel
import com.plcoding.cardiganapp.feature.auth.presentation.register.RegisterRoot
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Registers Log in and Register. Leaving the auth screens after a login or register is up to the
 * app's session gate; nothing here knows where a signed-in user goes.
 */
fun NavGraphBuilder.authGraph(navController: NavController) {
    composable<LoginRoute> { entry ->
        val sessionExpired = entry.toRoute<LoginRoute>().sessionExpired
        LoginRoot(
            onCreateAccount = { navController.navigate(RegisterRoute) { launchSingleTop = true } },
            viewModel = koinViewModel<LoginViewModel> { parametersOf(sessionExpired) },
        )
    }
    composable<RegisterRoute> {
        RegisterRoot(onLogIn = navController::navigateBackToLogin)
    }
}

/** Returns to Log in if it is on the back stack, and opens it in place of Register otherwise. */
private fun NavController.navigateBackToLogin() {
    if (popBackStack<LoginRoute>(inclusive = false)) return
    navigate(LoginRoute()) {
        popUpTo<RegisterRoute> { inclusive = true }
    }
}
