package com.plcoding.cardiganapp.feature.auth.data.fixture

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.plcoding.cardiganapp.core.domain.session.SessionState
import com.plcoding.cardiganapp.core.domain.session.SignOutReason
import com.plcoding.cardiganapp.core.domain.util.Result
import com.plcoding.cardiganapp.feature.auth.domain.AuthError
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class FixtureAuthRepositoryTest {

    private val session = InMemorySessionManager()
    private val repository = FixtureAuthRepository(session)

    @Test
    fun wrongGivesInvalidCredentials() = runTest {
        assertThat(repository.login("wrong@example.com", "x")).isEqualTo(Result.Error(AuthError.InvalidCredentials))
    }

    @Test
    fun takenGivesEmailAlreadyUsed() = runTest {
        assertThat(repository.register("taken@example.com", "x")).isEqualTo(Result.Error(AuthError.EmailAlreadyUsed))
    }

    @Test
    fun limitedGivesTwelveSeconds() = runTest {
        assertThat(repository.login(" Limited@Example.com ", "x")).isEqualTo(Result.Error(AuthError.RateLimited(12)))
    }

    @Test
    fun offlineGivesUnavailable() = runTest {
        assertThat(repository.register("offline@example.com", "x")).isEqualTo(Result.Error(AuthError.Unavailable))
    }

    @Test
    fun anythingElseSignsInWithTheLowercasedEmail() = runTest {
        assertThat(repository.login(" Ann@Example.com ", "x")).isEqualTo(Result.Success(Unit))
        assertThat(session.state.value).isEqualTo(SessionState.SignedIn("ann@example.com"))
    }

    @Test
    fun sessionRestoresToNoSessionThenLogsOutAndExpires() = runTest {
        assertThat(session.state.value).isEqualTo(SessionState.Restoring)
        session.restore()
        assertThat(session.state.value).isEqualTo(SessionState.SignedOut(SignOutReason.NoSession))
        session.signIn("ann@example.com")
        session.expire()
        assertThat(session.state.value).isEqualTo(SessionState.SignedOut(SignOutReason.Expired))
        session.signIn("ann@example.com")
        session.logout()
        assertThat(session.state.value).isEqualTo(SessionState.SignedOut(SignOutReason.LoggedOut))
    }
}
