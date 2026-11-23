package com.plcoding.cardiganapp.feature.auth.domain

import com.plcoding.cardiganapp.core.domain.util.EmptyResult

/**
 * Logs in and registers users.
 *
 * Contract: when [login] or [register] succeeds, `SessionManager.state` is already `SignedIn`
 * before the call returns. The app's session gate navigates from there; callers don't navigate
 * on success.
 *
 * The email and password are passed exactly as the user typed them.
 */
interface AuthRepository {
    suspend fun login(email: String, password: String): EmptyResult<AuthError>
    suspend fun register(email: String, password: String): EmptyResult<AuthError>
}
