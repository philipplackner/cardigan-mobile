package com.plcoding.cardiganapp.fakes

import com.plcoding.cardiganapp.core.domain.session.SessionManager
import com.plcoding.cardiganapp.core.domain.session.SessionScopedStore
import com.plcoding.cardiganapp.core.domain.session.SessionState
import com.plcoding.cardiganapp.core.domain.session.SignOutReason
import com.plcoding.cardiganapp.core.domain.util.EmptyResult
import com.plcoding.cardiganapp.core.domain.util.Result
import com.plcoding.cardiganapp.feature.auth.domain.AuthError
import com.plcoding.cardiganapp.feature.auth.domain.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow

/** A session that restores to [restoreTo] and records every call. */
class FakeSessionManager(
    private val restoreTo: SessionState? = null,
    initial: SessionState = SessionState.Restoring,
) : SessionManager {
    override val state = MutableStateFlow(initial)
    var restoreCalls = 0
        private set
    var logoutCalls = 0
        private set

    override suspend fun restore() {
        restoreCalls++
        restoreTo?.let { state.value = it }
    }

    override suspend fun logout() {
        logoutCalls++
        state.value = SessionState.SignedOut(SignOutReason.LoggedOut)
    }

    fun signIn(email: String) {
        state.value = SessionState.SignedIn(email)
    }

    fun expire() {
        state.value = SessionState.SignedOut(SignOutReason.Expired)
    }
}

/** Signs [session] in on success, as the real repository's contract requires. */
class FakeAuthRepository(private val session: FakeSessionManager) : AuthRepository {
    var result: EmptyResult<AuthError> = Result.Success(Unit)

    override suspend fun login(email: String, password: String): EmptyResult<AuthError> =
        result.also { if (it is Result.Success) session.signIn(email.trim().lowercase()) }

    override suspend fun register(email: String, password: String): EmptyResult<AuthError> = login(email, password)
}

/** Records [clear] calls, and the session state each one saw, into [log]. */
class RecordingStore(
    private val name: String,
    private val log: MutableList<String>,
    private val session: () -> SessionState,
) : SessionScopedStore {
    override fun clear() {
        log += "$name cleared while ${session()}"
    }
}
