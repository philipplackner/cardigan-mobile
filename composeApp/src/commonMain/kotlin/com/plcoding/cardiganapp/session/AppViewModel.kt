package com.plcoding.cardiganapp.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plcoding.cardiganapp.core.domain.session.SessionManager
import com.plcoding.cardiganapp.core.domain.session.SessionScopedStore
import com.plcoding.cardiganapp.core.domain.session.SessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The app-wide session for the UI. Restores the session once. [state] follows the session, but
 * every [SessionScopedStore] is cleared before a signed-out state (or a different signed-in user)
 * is published, so the session gate never shows a screen with the previous user's data.
 */
class AppViewModel(
    private val sessionManager: SessionManager,
    private val stores: List<SessionScopedStore>,
) : ViewModel() {

    private val _state = MutableStateFlow(sessionManager.state.value)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            sessionManager.state.collect { next ->
                if (endsUserData(_state.value, next)) stores.forEach { it.clear() }
                _state.value = next
            }
        }
        viewModelScope.launch { sessionManager.restore() }
    }

    /** Logs out in this ViewModel's scope, so leaving the screen that asked can't cancel it. */
    fun onLogout() {
        viewModelScope.launch { sessionManager.logout() }
    }

    private fun endsUserData(previous: SessionState, next: SessionState): Boolean = when (next) {
        is SessionState.SignedOut -> previous !is SessionState.SignedOut
        is SessionState.SignedIn -> previous is SessionState.SignedIn && previous.email != next.email
        SessionState.Restoring -> false
    }
}
