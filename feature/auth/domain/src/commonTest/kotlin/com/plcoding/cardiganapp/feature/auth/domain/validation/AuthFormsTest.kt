package com.plcoding.cardiganapp.feature.auth.domain.validation

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlin.test.Test

/*
 * Break vectors:
 * - login: empty and whitespace-only email, empty password, malformed email passes, spaces-only password passes
 * - register: every combination of broken email, password and repeat
 * - password problem order: required, invalid character, too long, rules unmet
 * - repeat: empty, mismatch on exact text (no normalization), match
 */
class AuthFormsTest {
    private val goodEmail = "ann@example.com"
    private val goodPassword = "Correct horse 7"

    @Test
    fun loginChecksOnlyEmptiness() {
        assertThat(LoginForm.check("ann@", "x").isValid).isTrue()
        assertThat(LoginForm.check("ann@", " ").isValid).isTrue()
    }

    @Test
    fun loginEmptyFields() {
        val check = LoginForm.check("   ", "")
        assertThat(check.email).isEqualTo(RequiredProblem.Required)
        assertThat(check.password).isEqualTo(RequiredProblem.Required)
        assertThat(check.isValid).isFalse()
        assertThat(LoginForm.check("", "x").password).isNull()
        assertThat(LoginForm.check("a", "").email).isNull()
    }

    @Test
    fun registerEveryFieldCombination() {
        val emails = listOf(goodEmail to null, "" to EmailProblem.Required, "ann@" to EmailProblem.InvalidFormat)
        val passwords =
            listOf(
                goodPassword to null,
                "" to PasswordProblem.Required,
                "Correct horse!" to PasswordProblem.RulesUnmet,
            )
        for ((email, emailProblem) in emails) {
            for ((password, passwordProblem) in passwords) {
                for (repeat in listOf(password, "", password + "x")) {
                    val check = RegisterForm.check(email, password, repeat)
                    val repeatProblem =
                        when {
                            repeat.isEmpty() -> RepeatPasswordProblem.Required
                            repeat != password -> RepeatPasswordProblem.Mismatch
                            else -> null
                        }
                    assertThat(check.email).isEqualTo(emailProblem)
                    assertThat(check.password).isEqualTo(passwordProblem)
                    assertThat(check.repeatPassword).isEqualTo(repeatProblem)
                    assertThat(
                        check.isValid,
                    ).isEqualTo(emailProblem == null && passwordProblem == null && repeatProblem == null)
                }
            }
        }
    }

    @Test
    fun emptyRegisterForm() {
        val check = RegisterForm.check("", "", "")
        assertThat(check.email).isEqualTo(EmailProblem.Required)
        assertThat(check.password).isEqualTo(PasswordProblem.Required)
        assertThat(check.repeatPassword).isEqualTo(RepeatPasswordProblem.Required)
        assertThat(check.rules.meetsAllRules).isFalse()
    }

    @Test
    fun passwordProblemOrder() {
        val tooLongWithTab = "Aa1\t" + "x".repeat(200)
        assertThat(RegisterForm.check(goodEmail, tooLongWithTab, tooLongWithTab).password)
            .isEqualTo(PasswordProblem.InvalidCharacter)
        val tooLong = "Aa1" + "x".repeat(126)
        assertThat(RegisterForm.check(goodEmail, tooLong, tooLong).password).isEqualTo(PasswordProblem.TooLong)
        val withTab = "Correct\thorse 7"
        assertThat(RegisterForm.check(goodEmail, withTab, withTab).password).isEqualTo(PasswordProblem.InvalidCharacter)
    }

    @Test
    fun repeatComparesExactText() {
        val decomposed = "Correct horse 7e\u0301"
        val composed = "Correct horse 7é"
        assertThat(RegisterForm.check(goodEmail, decomposed, composed).repeatPassword)
            .isEqualTo(RepeatPasswordProblem.Mismatch)
        assertThat(RegisterForm.check(goodEmail, goodPassword, "$goodPassword ").repeatPassword)
            .isEqualTo(RepeatPasswordProblem.Mismatch)
    }

    @Test
    fun validRegisterForm() {
        assertThat(RegisterForm.check("  Ann@Example.com ", goodPassword, goodPassword).isValid).isTrue()
    }
}
