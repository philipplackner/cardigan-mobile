package com.plcoding.cardiganapp.core.domain.session

import kotlinx.coroutines.flow.StateFlow

/** The app's session: whether a user is signed in, and the actions that start and end it. */
interface SessionManager {
    val state: StateFlow<SessionState>

    /** Restores the stored session once per process. Later calls do nothing. */
    suspend fun restore()

    /**
     * Ends the session. Reads the refresh token, then clears the tokens and email from storage
     * and memory and sets `SignedOut(LoggedOut)` before sending the server logout. The server
     * call is best-effort, runs after the state change, and its result is ignored. Never fails.
     */
    suspend fun logout()
}
