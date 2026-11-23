package com.plcoding.cardiganapp.feature.auth.data.fixture

import com.plcoding.cardiganapp.core.domain.session.SessionManager
import com.plcoding.cardiganapp.core.domain.session.SessionState
import com.plcoding.cardiganapp.core.domain.session.SignOutReason
import com.plcoding.cardiganapp.core.domain.text.normalizeText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * TEMPORARY (D3): an in-memory session for the manual check, debug builds only. Removed in task
 * 9.1 once mob-auth-connect binds the real session. Nothing is stored, so every start is signed out.
 */
class InMemorySessionManager : SessionManager {
    private val _state = MutableStateFlow<SessionState>(SessionState.Restoring)
    override val state: StateFlow<SessionState> = _state.asStateFlow()

    override suspend fun restore() {
        if (_state.value == SessionState.Restoring) _state.value = SessionState.SignedOut(SignOutReason.NoSession)
    }

    override suspend fun logout() {
        _state.value = SessionState.SignedOut(SignOutReason.LoggedOut)
    }

    /** Signs in with the email lowercased, as the backend returns it. */
    fun signIn(email: String) {
        _state.value = SessionState.SignedIn(normalizeText(email).lowercase())
    }

    /** Ends the session as a rejected refresh would. */
    fun expire() {
        _state.value = SessionState.SignedOut(SignOutReason.Expired)
    }
}
