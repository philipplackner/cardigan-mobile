package com.plcoding.cardiganapp.feature.auth.presentation.navigation

import kotlinx.serialization.Serializable

/** Log in. [sessionExpired] shows the session expired banner (D9). */
@Serializable
data class LoginRoute(
    val sessionExpired: Boolean = false,
)

@Serializable
data object RegisterRoute
