package com.plcoding.cardiganapp.session

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.plcoding.cardiganapp.core.domain.session.SessionState
import com.plcoding.cardiganapp.core.domain.session.SignOutReason
import com.plcoding.cardiganapp.feature.auth.presentation.navigation.LoginRoute
import com.plcoding.cardiganapp.feature.boards.presentation.navigation.BoardListRoute
import kotlin.test.Test

/*
 * Break vectors: every SessionState, and every SignOutReason, since only Expired may show the
 * "Session expired" state on the login screen.
 */
class DestinationForTest {

    @Test
    fun expiredSessionOpensLoginWithTheExpiredState() {
        assertThat(destinationFor(SessionState.SignedOut(SignOutReason.Expired)))
            .isEqualTo(LoginRoute(sessionExpired = true))
    }

    @Test
    fun otherSignOutsOpenPlainLogin() {
        listOf(SignOutReason.NoSession, SignOutReason.LoggedOut).forEach { reason ->
            assertThat(destinationFor(SessionState.SignedOut(reason)), reason.name)
                .isEqualTo(LoginRoute(sessionExpired = false))
        }
    }

    @Test
    fun signedInOpensTheBoardList() {
        assertThat(destinationFor(SessionState.SignedIn("ann@example.com"))).isEqualTo(BoardListRoute)
    }

    @Test
    fun restoringOpensTheBoardList() {
        assertThat(destinationFor(SessionState.Restoring)).isEqualTo(BoardListRoute)
    }
}
