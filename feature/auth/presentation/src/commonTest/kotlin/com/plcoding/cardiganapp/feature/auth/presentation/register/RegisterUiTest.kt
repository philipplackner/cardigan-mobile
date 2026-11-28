package com.plcoding.cardiganapp.feature.auth.presentation.register

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.runComposeUiTest
import androidx.lifecycle.SavedStateHandle
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import com.plcoding.cardiganapp.core.designsystem.theme.WindowClass
import com.plcoding.cardiganapp.feature.auth.domain.AuthError
import com.plcoding.cardiganapp.feature.auth.presentation.fakes.FakeAuthRepository
import kotlinx.coroutines.CompletableDeferred
import kotlin.test.Test

/** Register with its real ViewModel and a fake repository. */
@OptIn(ExperimentalTestApi::class)
class RegisterUiTest {
    private val repository = FakeAuthRepository()
    private val isTextField = SemanticsMatcher.keyIsDefined(SemanticsProperties.EditableText)
    private var logInClicks = 0

    private fun ComposeUiTest.show(windowClass: WindowClass = WindowClass.Phone) {
        val viewModel = RegisterViewModel(repository, SavedStateHandle())
        setContent {
            AppTheme(darkTheme = false, windowClass = windowClass) {
                RegisterRoot(onLogIn = { logInClicks++ }, viewModel = viewModel)
            }
        }
    }

    private fun ComposeUiTest.fields() = onAllNodes(isTextField)

    private fun ComposeUiTest.rule(
        text: String,
        met: Boolean,
    ) = onNode(
        hasText(text) and
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, if (met) "Met" else "Not met"),
    )

    @Test
    fun rulesChangeWhileTyping() =
        runComposeUiTest {
            show()
            rule("Uppercase letter", met = false).assertExists()
            fields()[1].performTextInput("C")
            rule("Uppercase letter", met = true).assertExists()
            rule("Lowercase letter", met = false).assertExists()
            fields()[1].performTextInput("orrect horse")
            rule("Lowercase letter", met = true).assertExists()
            rule("12+ characters", met = true).assertExists()
            rule("Digit", met = false).assertExists()
            fields()[1].performTextInput(" 7")
            rule("Digit", met = true).assertExists()
        }

    @Test
    fun emailErrorOnlyAfterBlur() =
        runComposeUiTest {
            show()
            fields()[0].requestFocus()
            fields()[0].performTextInput("ann@")
            onAllNodes(hasText("Enter a valid email address.")).assertCountEquals(0)
            fields()[1].requestFocus()
            onNodeWithText("Enter a valid email address.").assertExists()
            fields()[0].performTextReplacement("ann@example.com")
            onAllNodes(hasText("Enter a valid email address.")).assertCountEquals(0)
        }

    @Test
    fun emptySubmitShowsEveryError() =
        runComposeUiTest {
            show(WindowClass.Desktop)
            onNodeWithText("Create account").performClick()
            onNodeWithText("Enter your email.").assertExists()
            onNodeWithText("Enter your password.").assertExists()
            onNodeWithText("Repeat your password.").assertExists()
            assertThat(repository.registerCalls).isEmpty()
        }

    @Test
    fun usedEmailErrorClearsOnEmailEdit() =
        runComposeUiTest {
            repository.failWith(AuthError.EmailAlreadyUsed)
            show()
            fields()[0].performTextReplacement("ann@example.com")
            fields()[1].performTextReplacement("Correct horse 7")
            fields()[2].performTextReplacement("Correct horse 7")
            onNodeWithText("Create account").performClick()
            onNodeWithText("An account with this email already exists.").assertExists()
            fields()[0].performTextInput("x")
            onAllNodes(hasText("An account with this email already exists.")).assertCountEquals(0)
        }

    @Test
    fun loadingShowsCreatingAccount() =
        runComposeUiTest {
            repository.gate = CompletableDeferred()
            show()
            fields()[0].performTextReplacement("ann@example.com")
            fields()[1].performTextReplacement("Correct horse 7")
            fields()[2].performTextReplacement("Correct horse 7")
            onNodeWithText("Create account").performClick()
            onNodeWithText("Creating account…").assertIsNotEnabled()
            onNodeWithText("Log in").assertIsNotEnabled()
            assertThat(repository.registerCalls.size).isEqualTo(1)
        }

    @Test
    fun logInLinkCallsBack() =
        runComposeUiTest {
            show()
            onNodeWithText("Log in").performClick()
            assertThat(logInClicks).isEqualTo(1)
        }
}
