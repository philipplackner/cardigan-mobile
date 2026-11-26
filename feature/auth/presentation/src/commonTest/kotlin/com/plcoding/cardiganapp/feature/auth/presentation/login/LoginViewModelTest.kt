package com.plcoding.cardiganapp.feature.auth.presentation.login

import androidx.lifecycle.SavedStateHandle
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.plcoding.cardiganapp.feature.auth.domain.AuthError
import com.plcoding.cardiganapp.feature.auth.domain.AuthField
import com.plcoding.cardiganapp.feature.auth.domain.AuthFieldError
import com.plcoding.cardiganapp.feature.auth.domain.FieldErrorCode
import com.plcoding.cardiganapp.feature.auth.presentation.components.AuthBanner
import com.plcoding.cardiganapp.feature.auth.presentation.components.AuthFieldMessage
import com.plcoding.cardiganapp.feature.auth.presentation.components.AuthInputField
import com.plcoding.cardiganapp.feature.auth.presentation.fakes.FakeAuthRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/*
 * Break vectors:
 * - email field cap: a 600-character paste keeps 500, a surrogate pair at the cut isn't split,
 *   edits up to 500 are untouched, passwords are never cut
 * - blur timing: no error while typing, error after the first focus loss, the initial unfocused
 *   callback before any focus doesn't count, live after the blur; the same for the password field
 * - email: empty and whitespace-only are Required; a malformed email is sent; values go out as typed
 * - double submit: one request; every action ignored while loading
 * - errors: InvalidCredentials, EmailAlreadyUsed, RateLimited(n), RateLimited(1) at the boundary,
 *   RateLimited(null), RateLimited(0), InvalidFields with mapped Required, with unmapped codes,
 *   with Required on an unknown field, with no fields, Unavailable
 * - clearing: wrong credentials on any edit, server Required on its field's edit, rate limit and
 *   unavailable on the next submit, success keeps loading
 * - session expired: from the route, kept while typing, cleared by any submit (even an invalid
 *   one) and by "Create one"
 * - saved state: email and both blur flags restored, password never written, a cleared banner stays cleared
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val repository = FakeAuthRepository()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle(), sessionExpired: Boolean = false) =
        LoginViewModel(sessionExpired, repository, handle)

    private val LoginViewModel.s get() = state.value

    private fun LoginViewModel.fill(email: String, password: String) {
        onAction(LoginAction.EmailChange(email))
        onAction(LoginAction.PasswordChange(password))
    }

    private fun LoginViewModel.focus(field: AuthInputField, focused: Boolean) =
        onAction(LoginAction.FocusChange(field, focused))

    @Test
    fun startsEmptyWithoutErrors() {
        val viewModel = viewModel()
        assertThat(viewModel.s).isEqualTo(LoginState())
        assertThat(viewModel.s.emailError).isNull()
        assertThat(viewModel.s.passwordError).isNull()
    }

    @Test
    fun noErrorWhileTypingAndErrorAfterTheFirstBlur() {
        val viewModel = viewModel()
        viewModel.focus(AuthInputField.Email, true)
        viewModel.onAction(LoginAction.EmailChange(" "))
        assertThat(viewModel.s.emailError).isNull()
        viewModel.focus(AuthInputField.Email, false)
        assertThat(viewModel.s.emailError).isEqualTo(AuthFieldMessage.EmailRequired)
        viewModel.onAction(LoginAction.EmailChange("a"))
        assertThat(viewModel.s.emailError).isNull()
    }

    @Test
    fun unfocusedCallbackBeforeAnyFocusIsNotABlur() {
        val viewModel = viewModel()
        viewModel.focus(AuthInputField.Email, false)
        viewModel.focus(AuthInputField.Password, false)
        assertThat(viewModel.s.emailBlurred).isFalse()
        assertThat(viewModel.s.passwordBlurred).isFalse()
    }

    @Test
    fun passwordErrorShowsAfterItsFirstBlur() {
        val viewModel = viewModel()
        viewModel.focus(AuthInputField.Password, true)
        assertThat(viewModel.s.passwordError).isNull()
        viewModel.focus(AuthInputField.Password, false)
        assertThat(viewModel.s.passwordBlurred).isTrue()
        assertThat(viewModel.s.passwordError).isEqualTo(AuthFieldMessage.PasswordRequired)
        assertThat(viewModel.s.emailError).isNull()
    }

    @Test
    fun whitespaceOnlyEmailIsRequiredOnSubmit() {
        val viewModel = viewModel()
        viewModel.fill("   ", "secret")
        viewModel.onAction(LoginAction.Submit)
        assertThat(viewModel.s.emailError).isEqualTo(AuthFieldMessage.EmailRequired)
        assertThat(repository.loginCalls).isEmpty()
    }

    @Test
    fun emptySubmitShowsBothErrors() {
        val viewModel = viewModel()
        viewModel.onAction(LoginAction.Submit)
        assertThat(viewModel.s.emailError).isEqualTo(AuthFieldMessage.EmailRequired)
        assertThat(viewModel.s.passwordError).isEqualTo(AuthFieldMessage.PasswordRequired)
        assertThat(viewModel.s.passwordIsError).isTrue()
        assertThat(repository.loginCalls).isEmpty()
    }

    @Test
    fun malformedEmailIsSentAsTyped() {
        val viewModel = viewModel()
        viewModel.fill(" Ann@ ", " pass ")
        viewModel.onAction(LoginAction.Submit)
        assertThat(repository.loginCalls).containsExactly(" Ann@ " to " pass ")
    }

    @Test
    fun doubleSubmitSendsOneRequestAndLoadingIgnoresActions() {
        repository.gate = CompletableDeferred()
        val viewModel = viewModel()
        viewModel.fill("ann@example.com", "secret")
        viewModel.onAction(LoginAction.Submit)
        viewModel.onAction(LoginAction.Submit)
        viewModel.onAction(LoginAction.EmailChange("other"))
        assertThat(repository.loginCalls).containsExactly("ann@example.com" to "secret")
        assertThat(viewModel.s.isLoading).isTrue()
        assertThat(viewModel.s.email).isEqualTo("ann@example.com")
    }

    @Test
    fun successStaysLoading() {
        val viewModel = viewModel()
        viewModel.fill("ann@example.com", "secret")
        viewModel.onAction(LoginAction.Submit)
        assertThat(viewModel.s.isLoading).isTrue()
    }

    @Test
    fun wrongCredentialsKeepValuesAndClearOnEitherEdit() {
        repository.failWith(AuthError.InvalidCredentials)
        val viewModel = viewModel()
        viewModel.fill("ann@example.com", "secret")
        viewModel.onAction(LoginAction.Submit)
        assertThat(viewModel.s.banner).isEqualTo(AuthBanner.WrongCredentials)
        assertThat(viewModel.s.passwordIsError).isTrue()
        assertThat(viewModel.s.isLoading).isFalse()
        assertThat(viewModel.s.email).isEqualTo("ann@example.com")
        assertThat(viewModel.s.password).isEqualTo("secret")

        viewModel.onAction(LoginAction.EmailChange("ann@example.co"))
        assertThat(viewModel.s.banner).isNull()
        assertThat(viewModel.s.passwordIsError).isFalse()

        viewModel.onAction(LoginAction.Submit)
        viewModel.onAction(LoginAction.PasswordChange("secre"))
        assertThat(viewModel.s.banner).isNull()
    }

    @Test
    fun rateLimitedShowsSecondsAndClearsOnNextSubmitOnly() {
        repository.failWith(AuthError.RateLimited(12))
        val viewModel = viewModel()
        viewModel.fill("ann@example.com", "secret")
        viewModel.onAction(LoginAction.Submit)
        assertThat(viewModel.s.banner).isEqualTo(AuthBanner.RateLimited(12))
        viewModel.onAction(LoginAction.EmailChange("ann@example.org"))
        assertThat(viewModel.s.banner).isEqualTo(AuthBanner.RateLimited(12))

        repository.gate = CompletableDeferred()
        viewModel.onAction(LoginAction.Submit)
        assertThat(viewModel.s.banner).isNull()
    }

    @Test
    fun rateLimitedForOneSecondShowsTheSeconds() {
        repository.failWith(AuthError.RateLimited(1))
        val viewModel = viewModel()
        viewModel.fill("ann@example.com", "secret")
        viewModel.onAction(LoginAction.Submit)
        assertThat(viewModel.s.banner).isEqualTo(AuthBanner.RateLimited(1))
    }

    @Test
    fun rateLimitedWithoutUsableHeaderShowsGenericBanner() {
        for (seconds in listOf(null, 0, -5)) {
            repository.failWith(AuthError.RateLimited(seconds))
            val viewModel = viewModel()
            viewModel.fill("ann@example.com", "secret")
            viewModel.onAction(LoginAction.Submit)
            assertThat(viewModel.s.banner).isEqualTo(AuthBanner.Unavailable)
        }
    }

    @Test
    fun unavailableAndUnexpectedErrorsShowGenericBanner() {
        for (error in listOf(AuthError.Unavailable, AuthError.EmailAlreadyUsed)) {
            repository.failWith(error)
            val viewModel = viewModel()
            viewModel.fill("ann@example.com", "secret")
            viewModel.onAction(LoginAction.Submit)
            assertThat(viewModel.s.banner).isEqualTo(AuthBanner.Unavailable)
            assertThat(viewModel.s.password).isEqualTo("secret")
        }
    }

    @Test
    fun serverRequiredMapsToTheFieldAndClearsOnItsEdit() {
        repository.failWith(
            AuthError.InvalidFields(
                listOf(
                    AuthFieldError(AuthField.Email, FieldErrorCode.Required),
                    AuthFieldError(AuthField.Password, FieldErrorCode.Required),
                ),
            ),
        )
        val viewModel = viewModel()
        viewModel.fill("ann@example.com", "secret")
        viewModel.onAction(LoginAction.Submit)
        assertThat(viewModel.s.emailError).isEqualTo(AuthFieldMessage.EmailRequired)
        assertThat(viewModel.s.passwordError).isEqualTo(AuthFieldMessage.PasswordRequired)
        assertThat(viewModel.s.banner).isNull()

        viewModel.onAction(LoginAction.EmailChange("ann@example.com "))
        assertThat(viewModel.s.emailError).isNull()
        assertThat(viewModel.s.passwordError).isEqualTo(AuthFieldMessage.PasswordRequired)
        viewModel.onAction(LoginAction.PasswordChange("secret!"))
        assertThat(viewModel.s.passwordError).isNull()
    }

    @Test
    fun formatErrorOnLoginShowsGenericBannerAndNoFieldMessage() {
        repository.failWith(AuthError.InvalidFields(listOf(AuthFieldError(AuthField.Email, FieldErrorCode.InvalidFormat))))
        val viewModel = viewModel()
        viewModel.fill("ann@example.com", "secret")
        viewModel.onAction(LoginAction.Submit)
        assertThat(viewModel.s.banner).isEqualTo(AuthBanner.Unavailable)
        assertThat(viewModel.s.emailError).isNull()
    }

    @Test
    fun requiredOnAnUnknownFieldShowsGenericBanner() {
        repository.failWith(AuthError.InvalidFields(listOf(AuthFieldError(AuthField.Other, FieldErrorCode.Required))))
        val viewModel = viewModel()
        viewModel.fill("ann@example.com", "secret")
        viewModel.onAction(LoginAction.Submit)
        assertThat(viewModel.s.banner).isEqualTo(AuthBanner.Unavailable)
        assertThat(viewModel.s.emailError).isNull()
        assertThat(viewModel.s.passwordError).isNull()
    }

    @Test
    fun emptyFieldErrorListShowsGenericBanner() {
        repository.failWith(AuthError.InvalidFields(emptyList()))
        val viewModel = viewModel()
        viewModel.fill("ann@example.com", "secret")
        viewModel.onAction(LoginAction.Submit)
        assertThat(viewModel.s.banner).isEqualTo(AuthBanner.Unavailable)
    }

    @Test
    fun sessionExpiredComesFromTheRouteAndStaysWhileTyping() {
        val viewModel = viewModel(sessionExpired = true)
        assertThat(viewModel.s.banner).isEqualTo(AuthBanner.SessionExpired)
        assertThat(viewModel.s.email).isEqualTo("")
        viewModel.fill("ann@example.com", "secret")
        assertThat(viewModel.s.banner).isEqualTo(AuthBanner.SessionExpired)
    }

    @Test
    fun sessionExpiredClearsOnAnySubmit() {
        val viewModel = viewModel(sessionExpired = true)
        viewModel.onAction(LoginAction.Submit)
        assertThat(viewModel.s.banner).isNull()
    }

    @Test
    fun sessionExpiredClearsOnCreateOne() {
        val viewModel = viewModel(sessionExpired = true)
        viewModel.onAction(LoginAction.CreateAccountClick)
        assertThat(viewModel.s.banner).isNull()
    }

    @Test
    fun createOneKeepsOtherBanners() {
        repository.failWith(AuthError.Unavailable)
        val viewModel = viewModel()
        viewModel.fill("ann@example.com", "secret")
        viewModel.onAction(LoginAction.Submit)
        viewModel.onAction(LoginAction.CreateAccountClick)
        assertThat(viewModel.s.banner).isEqualTo(AuthBanner.Unavailable)
    }

    @Test
    fun savedStateRestoresEmailAndBlurButNeverThePassword() {
        val handle = SavedStateHandle()
        val first = viewModel(handle)
        first.focus(AuthInputField.Email, true)
        first.fill("ann@", "secret")
        first.focus(AuthInputField.Email, false)
        first.focus(AuthInputField.Password, true)
        first.focus(AuthInputField.Password, false)

        assertThat(handle.keys().none { handle.get<Any?>(it) == "secret" }).isTrue()
        val restored = viewModel(handle)
        assertThat(restored.s.email).isEqualTo("ann@")
        assertThat(restored.s.password).isEqualTo("")
        assertThat(restored.s.emailBlurred).isTrue()
        assertThat(restored.s.passwordBlurred).isTrue()
    }

    @Test
    fun clearedSessionBannerStaysClearedAfterRestore() {
        val handle = SavedStateHandle()
        viewModel(handle, sessionExpired = true).onAction(LoginAction.Submit)
        assertThat(viewModel(handle, sessionExpired = true).s.banner).isNull()
    }

    @Test
    fun pastedEmailIsCutToFiveHundredCharacters() {
        val viewModel = viewModel()
        viewModel.onAction(LoginAction.EmailChange("a".repeat(600)))
        assertThat(viewModel.s.email).isEqualTo("a".repeat(500))
    }

    @Test
    fun emailCutDoesNotSplitASurrogatePair() {
        val viewModel = viewModel()
        viewModel.onAction(LoginAction.EmailChange("a".repeat(499) + "😀" + "a".repeat(100)))
        assertThat(viewModel.s.email).isEqualTo("a".repeat(499))
        viewModel.onAction(LoginAction.EmailChange("a".repeat(498) + "😀" + "a".repeat(100)))
        assertThat(viewModel.s.email).isEqualTo("a".repeat(498) + "😀")
    }

    @Test
    fun emailEditsUpToFiveHundredCharactersAreKept() {
        val viewModel = viewModel()
        val email = " " + "😀".repeat(10) + "a".repeat(479)
        viewModel.onAction(LoginAction.EmailChange(email))
        assertThat(viewModel.s.email).isEqualTo(email)
        viewModel.onAction(LoginAction.EmailChange("Ann@Example.com "))
        assertThat(viewModel.s.email).isEqualTo("Ann@Example.com ")
    }

    @Test
    fun passwordIsNotCut() {
        val viewModel = viewModel()
        viewModel.onAction(LoginAction.PasswordChange("a".repeat(600)))
        assertThat(viewModel.s.password.length).isEqualTo(600)
    }
}
