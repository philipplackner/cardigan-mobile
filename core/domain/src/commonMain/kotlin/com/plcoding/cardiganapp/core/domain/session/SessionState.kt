package com.plcoding.cardiganapp.core.domain.session

sealed interface SessionState {
    /** The stored session is being restored. The app shows only its splash. */
    data object Restoring : SessionState

    /**
     * A user is signed in. [email] is the backend's lowercased email. A restore that can't reach
     * the backend still reports this state with the stored email.
     */
    data class SignedIn(val email: String) : SessionState

    data class SignedOut(val reason: SignOutReason) : SessionState
}

enum class SignOutReason {
    /** No session was stored at app start. */
    NoSession,

    /** The user chose Log out. */
    LoggedOut,

    /** The session ended without a logout, at app start or while the app was open. */
    Expired,
}
