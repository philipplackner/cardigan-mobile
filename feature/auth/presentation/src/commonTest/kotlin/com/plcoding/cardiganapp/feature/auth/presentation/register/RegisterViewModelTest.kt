package com.plcoding.cardiganapp.feature.auth.presentation.register

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
 * - live rules on every change, before any blur
 * - errors only after the first blur, then live; the first-focus callback doesn't count
 * - mismatch follows both passwords; exact text compared
 * - empty submit shows every error; unmet rules give the error state with no message
 * - limits: 129 code points, a pasted tab; no request in either case
 * - server: EmailAlreadyUsed cleared by an email edit only; each row of the field-error table;
 *   PASSWORD_TOO_WEAK, TOO_SHORT and Other go to the banner; mapped + unmapped show both;
 *   rate limit with and without a usable header; InvalidCredentials and Unavailable go to the banner
 * - double submit sends one request; values go out as typed; success stays loading
 * - saved state: email and blur flags restored, passwords never written
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {

    private val repository = FakeAuthRepository()
    private val goodEmail = "ann@example.com"
    private val goodPassword = "Correct horse 7"

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) = RegisterViewModel(repository, handle)

    private val RegisterViewModel.s get() = state.value

    private fun RegisterViewModel.fill(email: String = goodEmail, password: String = goodPassword, repeat: String = password) {
        onAction(RegisterAction.EmailChange(email))
        onAction(RegisterAction.PasswordChange(password))
        onAction(RegisterAction.RepeatPasswordChange(repeat))
    }

    private fun RegisterViewModel.blur(field: AuthInputField) {
        onAction(RegisterAction.FocusChange(field, true))
        onAction(RegisterAction.FocusChange(field, false))
    }

    private fun RegisterViewModel.submitWith(error: AuthError) {
        repository.failWith(error)
        fill()
        onAction(RegisterAction.Submit)
    }

    @Test
    fun rulesUpdateOnEveryChangeBeforeAnyBlur() {
        val viewModel = viewModel()
        viewModel.onAction(RegisterAction.PasswordChange("C"))
        assertThat(viewModel.s.rules.hasUppercase).isTrue()
        assertThat(viewModel.s.rules.hasLowercase).isFalse()
        viewModel.onAction(RegisterAction.PasswordChange("Co"))
        assertThat(viewModel.s.rules.hasLowercase).isTrue()
        viewModel.onAction(RegisterAction.PasswordChange(goodPassword))
        assertThat(viewModel.s.rules.meetsAllRules).isTrue()
        assertThat(viewModel.s.passwordIsError).isFalse()
    }

    @Test
    fun errorsOnlyAfterBlurThenLive() {
        val viewModel = viewModel()
        viewModel.onAction(RegisterAction.FocusChange(AuthInputField.Email, false))
        viewModel.onAction(RegisterAction.FocusChange(AuthInputField.Email, true))
        viewModel.onAction(RegisterAction.EmailChange("ann@"))
        assertThat(viewModel.s.emailError).isNull()
        viewModel.onAction(RegisterAction.FocusChange(AuthInputField.Email, false))
        assertThat(viewModel.s.emailError).isEqualTo(AuthFieldMessage.EmailInvalid)
        viewModel.onAction(RegisterAction.EmailChange("ann@example.com"))
        assertThat(viewModel.s.emailError).isNull()
    }

    @Test
    fun emptyPasswordAfterBlur() {
        val viewModel = viewModel()
        viewModel.blur(AuthInputField.Password)
        assertThat(viewModel.s.passwordError).isEqualTo(AuthFieldMessage.PasswordRequired)
        assertThat(viewModel.s.passwordIsError).isTrue()
        assertThat(viewModel.s.rules.hasMinLength || viewModel.s.rules.hasDigit).isFalse()
    }

    @Test
    fun mismatchFollowsEitherPassword() {
        val viewModel = viewModel()
        viewModel.fill(password = goodPassword, repeat = "Correct horse 8")
        viewModel.blur(AuthInputField.RepeatPassword)
        assertThat(viewModel.s.repeatPasswordError).isEqualTo(AuthFieldMessage.PasswordMismatch)
        viewModel.onAction(RegisterAction.PasswordChange("Correct horse 8"))
        assertThat(viewModel.s.repeatPasswordError).isNull()
        viewModel.onAction(RegisterAction.RepeatPasswordChange("Correct horse 9"))
        assertThat(viewModel.s.repeatPasswordError).isEqualTo(AuthFieldMessage.PasswordMismatch)
    }

    @Test
    fun emptySubmitShowsEveryError() {
        val viewModel = viewModel()
        viewModel.onAction(RegisterAction.Submit)
        assertThat(viewModel.s.emailError).isEqualTo(AuthFieldMessage.EmailRequired)
        assertThat(viewModel.s.passwordError).isEqualTo(AuthFieldMessage.PasswordRequired)
        assertThat(viewModel.s.passwordIsError).isTrue()
        assertThat(viewModel.s.repeatPasswordError).isEqualTo(AuthFieldMessage.RepeatRequired)
        assertThat(repository.registerCalls).isEmpty()
    }

    @Test
    fun unmetRuleOnSubmitHasErrorStateAndNoMessage() {
        val viewModel = viewModel()
        viewModel.fill(password = "Correct horse!")
        viewModel.onAction(RegisterAction.Submit)
        assertThat(viewModel.s.passwordError).isNull()
        assertThat(viewModel.s.passwordIsError).isTrue()
        assertThat(repository.registerCalls).isEmpty()
    }

    @Test
    fun passwordOverTheLimitIsBlocked() {
        val viewModel = viewModel()
        viewModel.fill(password = "Aa1" + "x".repeat(126))
        viewModel.onAction(RegisterAction.Submit)
        assertThat(viewModel.s.passwordError).isEqualTo(AuthFieldMessage.PasswordTooLong)
        assertThat(repository.registerCalls).isEmpty()
    }

    @Test
    fun pastedTabIsBlocked() {
        val viewModel = viewModel()
        viewModel.fill(password = "Correct\thorse 7")
        viewModel.onAction(RegisterAction.Submit)
        assertThat(viewModel.s.passwordError).isEqualTo(AuthFieldMessage.PasswordInvalidCharacter)
        assertThat(repository.registerCalls).isEmpty()
    }

    @Test
    fun validFormIsSentAsTypedAndStaysLoading() {
        val viewModel = viewModel()
        viewModel.fill(email = "  Ann@Example.com ")
        viewModel.onAction(RegisterAction.Submit)
        assertThat(repository.registerCalls).containsExactly("  Ann@Example.com " to goodPassword)
        assertThat(viewModel.s.isLoading).isTrue()
    }

    @Test
    fun doubleSubmitSendsOneRequest() {
        repository.gate = CompletableDeferred()
        val viewModel = viewModel()
        viewModel.fill()
        viewModel.onAction(RegisterAction.Submit)
        viewModel.onAction(RegisterAction.Submit)
        viewModel.onAction(RegisterAction.EmailChange("x"))
        assertThat(repository.registerCalls.size).isEqualTo(1)
        assertThat(viewModel.s.email).isEqualTo(goodEmail)
    }

    @Test
    fun emailAlreadyUsedClearsOnlyOnEmailEdit() {
        val viewModel = viewModel()
        viewModel.submitWith(AuthError.EmailAlreadyUsed)
        assertThat(viewModel.s.emailError).isEqualTo(AuthFieldMessage.EmailAlreadyUsed)
        assertThat(viewModel.s.password).isEqualTo(goodPassword)
        assertThat(viewModel.s.repeatPassword).isEqualTo(goodPassword)
        viewModel.onAction(RegisterAction.PasswordChange("$goodPassword!"))
        assertThat(viewModel.s.emailError).isEqualTo(AuthFieldMessage.EmailAlreadyUsed)
        viewModel.onAction(RegisterAction.EmailChange("ann@example.org"))
        assertThat(viewModel.s.emailError).isNull()
    }

    @Test
    fun serverFieldErrorTable() {
        val table = listOf(
            AuthFieldError(AuthField.Email, FieldErrorCode.Required) to AuthFieldMessage.EmailRequired,
            AuthFieldError(AuthField.Email, FieldErrorCode.TooLong) to AuthFieldMessage.EmailTooLong,
            AuthFieldError(AuthField.Email, FieldErrorCode.InvalidFormat) to AuthFieldMessage.EmailInvalid,
            AuthFieldError(AuthField.Email, FieldErrorCode.InvalidCharacter) to AuthFieldMessage.EmailInvalid,
            AuthFieldError(AuthField.Password, FieldErrorCode.Required) to AuthFieldMessage.PasswordRequired,
            AuthFieldError(AuthField.Password, FieldErrorCode.TooLong) to AuthFieldMessage.PasswordTooLong,
            AuthFieldError(AuthField.Password, FieldErrorCode.InvalidCharacter) to AuthFieldMessage.PasswordInvalidCharacter,
        )
        for ((fieldError, message) in table) {
            val viewModel = viewModel()
            viewModel.submitWith(AuthError.InvalidFields(listOf(fieldError)))
            val shown = if (fieldError.field == AuthField.Email) viewModel.s.emailError else viewModel.s.passwordError
            assertThat(shown).isEqualTo(message)
            assertThat(viewModel.s.banner).isNull()
        }
    }

    @Test
    fun serverFieldErrorsClearOnTheirFieldsEdit() {
        val viewModel = viewModel()
        viewModel.submitWith(
            AuthError.InvalidFields(
                listOf(
                    AuthFieldError(AuthField.Email, FieldErrorCode.InvalidFormat),
                    AuthFieldError(AuthField.Password, FieldErrorCode.InvalidCharacter),
                ),
            ),
        )
        viewModel.onAction(RegisterAction.EmailChange("ann@example.org"))
        assertThat(viewModel.s.emailError).isNull()
        assertThat(viewModel.s.passwordError).isEqualTo(AuthFieldMessage.PasswordInvalidCharacter)
        viewModel.onAction(RegisterAction.PasswordChange("Correct horse 8"))
        assertThat(viewModel.s.passwordError).isNull()
    }

    @Test
    fun unlistedCodesGoToTheBanner() {
        val unlisted = listOf(
            AuthFieldError(AuthField.Password, FieldErrorCode.PasswordTooWeak),
            AuthFieldError(AuthField.Password, FieldErrorCode.TooShort),
            AuthFieldError(AuthField.Password, FieldErrorCode.InvalidFormat),
            AuthFieldError(AuthField.Email, FieldErrorCode.Other),
            AuthFieldError(AuthField.Other, FieldErrorCode.Required),
        )
        for (fieldError in unlisted) {
            val viewModel = viewModel()
            viewModel.submitWith(AuthError.InvalidFields(listOf(fieldError)))
            assertThat(viewModel.s.banner).isEqualTo(AuthBanner.Unavailable)
            assertThat(viewModel.s.passwordError).isNull()
            assertThat(viewModel.s.password).isEqualTo(goodPassword)
        }
    }

    @Test
    fun mappedAndUnmappedShowBoth() {
        val viewModel = viewModel()
        viewModel.submitWith(
            AuthError.InvalidFields(
                listOf(
                    AuthFieldError(AuthField.Email, FieldErrorCode.TooLong),
                    AuthFieldError(AuthField.Password, FieldErrorCode.TooShort),
                ),
            ),
        )
        assertThat(viewModel.s.emailError).isEqualTo(AuthFieldMessage.EmailTooLong)
        assertThat(viewModel.s.banner).isEqualTo(AuthBanner.Unavailable)
    }

    @Test
    fun rateLimitAndGenericErrors() {
        val cases = listOf(
            AuthError.RateLimited(1) to AuthBanner.RateLimited(1),
            AuthError.RateLimited(null) to AuthBanner.Unavailable,
            AuthError.InvalidCredentials to AuthBanner.Unavailable,
            AuthError.Unavailable to AuthBanner.Unavailable,
            AuthError.InvalidFields(emptyList()) to AuthBanner.Unavailable,
        )
        for ((error, banner) in cases) {
            val viewModel = viewModel()
            viewModel.submitWith(error)
            assertThat(viewModel.s.banner).isEqualTo(banner)
            assertThat(viewModel.s.isLoading).isFalse()
        }
    }

    @Test
    fun bannerClearsOnNextSubmitNotOnEdit() {
        val viewModel = viewModel()
        viewModel.submitWith(AuthError.RateLimited(30))
        viewModel.onAction(RegisterAction.EmailChange("ann@example.org"))
        assertThat(viewModel.s.banner).isEqualTo(AuthBanner.RateLimited(30))
        repository.gate = CompletableDeferred()
        viewModel.onAction(RegisterAction.Submit)
        assertThat(viewModel.s.banner).isNull()
    }

    @Test
    fun savedStateKeepsEmailAndBlursButNoPasswords() {
        val handle = SavedStateHandle()
        val first = viewModel(handle)
        first.fill(email = "ann@", password = "Secret pass 1", repeat = "Secret pass 2")
        first.blur(AuthInputField.Email)
        first.blur(AuthInputField.RepeatPassword)
        assertThat(handle.keys().none { handle.get<Any?>(it).toString().startsWith("Secret") }).isTrue()

        val restored = viewModel(handle)
        assertThat(restored.s.email).isEqualTo("ann@")
        assertThat(restored.s.password).isEqualTo("")
        assertThat(restored.s.repeatPassword).isEqualTo("")
        assertThat(restored.s.emailBlurred).isTrue()
        assertThat(restored.s.repeatPasswordBlurred).isTrue()
        assertThat(restored.s.passwordBlurred).isFalse()
    }
}
