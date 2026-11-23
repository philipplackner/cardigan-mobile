package com.plcoding.cardiganapp.feature.auth.presentation.login

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import androidx.lifecycle.SavedStateHandle
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import androidx.compose.ui.semantics.SemanticsProperties
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import com.plcoding.cardiganapp.core.designsystem.theme.WindowClass
import com.plcoding.cardiganapp.feature.auth.domain.AuthError
import com.plcoding.cardiganapp.feature.auth.presentation.fakes.FakeAuthRepository
import kotlin.test.Test
import kotlinx.coroutines.CompletableDeferred

/** Log in with its real ViewModel and a fake repository. */
@OptIn(ExperimentalTestApi::class)
class LoginUiTest {

    private val repository = FakeAuthRepository()
    private val isTextField = SemanticsMatcher.keyIsDefined(SemanticsProperties.EditableText)
    private var createAccountClicks = 0

    private fun ComposeUiTest.show(
        sessionExpired: Boolean = false,
        windowClass: WindowClass = WindowClass.Phone,
    ) {
        val viewModel = LoginViewModel(sessionExpired, repository, SavedStateHandle())
        setContent {
            AppTheme(darkTheme = false, windowClass = windowClass) {
                LoginRoot(onCreateAccount = { createAccountClicks++ }, viewModel = viewModel)
            }
        }
    }

    private fun ComposeUiTest.fields() = onAllNodes(isTextField)

    private fun ComposeUiTest.fill(email: String, password: String) {
        fields()[0].performTextReplacement(email)
        fields()[1].performTextReplacement(password)
    }

    @Test
    fun doneOnThePasswordSubmits() = runComposeUiTest {
        show()
        fill("ann@example.com", "secret")
        fields()[1].performImeAction()
        waitForIdle()
        assertThat(repository.loginCalls).containsExactly("ann@example.com" to "secret")
    }

    @Test
    fun doubleTapSendsOneRequestAndDisablesEverything() = runComposeUiTest {
        repository.gate = CompletableDeferred()
        show()
        fill("ann@example.com", "secret")
        onNodeWithText("Log in").performClick()
        onNodeWithText("Logging in…").performClick()
        waitForIdle()
        assertThat(repository.loginCalls.size).isEqualTo(1)
        fields()[0].assertIsNotEnabled()
        fields()[1].assertIsNotEnabled()
        onNodeWithText("Logging in…").assertIsNotEnabled()
        onNodeWithText("Create one").assertIsNotEnabled()
        onNodeWithText("Create one").performClick()
        assertThat(createAccountClicks).isEqualTo(0)
    }

    @Test
    fun wrongCredentialsBannerClearsOnEdit() = runComposeUiTest {
        repository.failWith(AuthError.InvalidCredentials)
        show(windowClass = WindowClass.Desktop)
        fill("ann@example.com", "secret")
        onNodeWithText("Log in").performClick()
        onNodeWithText("Email or password is incorrect.").assertExists()
        fields()[0].assertIsEnabled()
        fields()[0].performTextInput("x")
        onAllNodes(hasText("Email or password is incorrect.")).assertCountEquals(0)
    }

    @Test
    fun sessionExpiredBannerStaysWhileTypingAndGoesOnSubmit() = runComposeUiTest {
        repository.gate = CompletableDeferred()
        show(sessionExpired = true)
        val message = "Your session has expired. Log in again to continue."
        onNodeWithText(message).assertExists()
        fill("ann@example.com", "secret")
        onNodeWithText(message).assertExists()
        onNodeWithText("Log in").performClick()
        onAllNodes(hasText(message)).assertCountEquals(0)
    }

    @Test
    fun rateLimitedTwelveSeconds() = runComposeUiTest {
        repository.failWith(AuthError.RateLimited(12))
        show()
        fill("ann@example.com", "secret")
        onNodeWithText("Log in").performClick()
        onNodeWithText("Too many attempts. Try again in 12 seconds.").assertExists()
        onNodeWithText("Log in").assertIsEnabled()
    }

    @Test
    fun rateLimitedOneSecond() = runComposeUiTest {
        repository.failWith(AuthError.RateLimited(1))
        show()
        fill("ann@example.com", "secret")
        onNodeWithText("Log in").performClick()
        onNodeWithText("Too many attempts. Try again in 1 second.").assertExists()
    }

    @Test
    fun rateLimitedInMinutes() = runComposeUiTest {
        repository.failWith(AuthError.RateLimited(61))
        show()
        fill("ann@example.com", "secret")
        onNodeWithText("Log in").performClick()
        onNodeWithText("Too many attempts. Try again in 2 minutes.").assertExists()
    }

    @Test
    fun emptySubmitShowsFieldErrorsAndSendsNothing() = runComposeUiTest {
        show()
        onNodeWithText("Log in").performClick()
        onNodeWithText("Enter your email.").assertExists()
        onNodeWithText("Enter your password.").assertExists()
        assertThat(repository.loginCalls).isEmpty()
    }

    @Test
    fun createOneCallsBack() = runComposeUiTest {
        show()
        onNodeWithText("Create one").performClick()
        assertThat(createAccountClicks).isEqualTo(1)
    }
}
