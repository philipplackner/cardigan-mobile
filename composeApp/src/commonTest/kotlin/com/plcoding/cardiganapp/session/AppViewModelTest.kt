package com.plcoding.cardiganapp.session

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.plcoding.cardiganapp.core.domain.session.SessionState
import com.plcoding.cardiganapp.core.domain.session.SignOutReason
import com.plcoding.cardiganapp.fakes.FakeSessionManager
import com.plcoding.cardiganapp.fakes.RecordingStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

/*
 * Break vectors:
 * - restore() runs exactly once, also with several state changes
 * - each start state passes through unchanged (signed in, no session, expired)
 * - clear() runs for LoggedOut and Expired, for every store, and before the new state is published
 * - no clear while signed in, or for NoSession after NoSession
 * - a different user signed in without a visible signed-out state still clears
 * - onLogout() calls logout() once per tap
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {
    private val log = mutableListOf<String>()
    private lateinit var viewModel: AppViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun create(session: FakeSessionManager): AppViewModel {
        val published = { viewModel.state.value }
        viewModel =
            AppViewModel(
                session,
                listOf(RecordingStore("boards", log, published), RecordingStore("labels", log, published)),
            )
        return viewModel
    }

    @Test
    fun restoresOnce() {
        val session = FakeSessionManager(restoreTo = SessionState.SignedIn("ann@example.com"))
        val viewModel = create(session)
        session.expire()
        session.signIn("ann@example.com")
        assertThat(session.restoreCalls).isEqualTo(1)
        assertThat(viewModel.state.value).isEqualTo(SessionState.SignedIn("ann@example.com"))
    }

    @Test
    fun startStatesPassThrough() {
        val states =
            listOf(
                SessionState.SignedIn("ann@example.com"),
                SessionState.SignedOut(SignOutReason.NoSession),
                SessionState.SignedOut(SignOutReason.Expired),
            )
        for (state in states) {
            assertThat(create(FakeSessionManager(restoreTo = state)).state.value).isEqualTo(state)
        }
    }

    @Test
    fun staysRestoringUntilTheSessionKnows() {
        val viewModel = create(FakeSessionManager(restoreTo = null))
        assertThat(viewModel.state.value).isEqualTo(SessionState.Restoring)
    }

    @Test
    fun logoutClearsEveryStoreBeforeTheStateIsPublished() {
        val session = FakeSessionManager(restoreTo = SessionState.SignedIn("ann@example.com"))
        val viewModel = create(session)
        log.clear()
        viewModel.onLogout()
        assertThat(session.logoutCalls).isEqualTo(1)
        assertThat(log).containsExactly(
            "boards cleared while SignedIn(email=ann@example.com)",
            "labels cleared while SignedIn(email=ann@example.com)",
        )
        assertThat(viewModel.state.value).isEqualTo(SessionState.SignedOut(SignOutReason.LoggedOut))
    }

    @Test
    fun expiryClearsEveryStoreBeforeTheStateIsPublished() {
        val session = FakeSessionManager(restoreTo = SessionState.SignedIn("ann@example.com"))
        create(session)
        log.clear()
        session.expire()
        assertThat(log).containsExactly(
            "boards cleared while SignedIn(email=ann@example.com)",
            "labels cleared while SignedIn(email=ann@example.com)",
        )
    }

    @Test
    fun noClearWhileSignedInOrBetweenSignedOutStates() {
        val session =
            FakeSessionManager(
                restoreTo = SessionState.SignedOut(SignOutReason.NoSession),
                initial = SessionState.SignedOut(SignOutReason.NoSession),
            )
        create(session)
        session.signIn("ann@example.com")
        assertThat(log).isEmpty()
    }

    @Test
    fun anotherUserSignedInClears() {
        val session = FakeSessionManager(restoreTo = SessionState.SignedIn("ann@example.com"))
        create(session)
        log.clear()
        session.signIn("bob@example.com")
        assertThat(log.size).isEqualTo(2)
    }
}
