package com.plcoding.cardiganapp.feature.auth.domain.validation

import com.plcoding.cardiganapp.core.domain.text.normalizeText

enum class RequiredProblem { Required }

/** Log in checks only for empty fields. The email format is left to the backend. */
data class LoginFormCheck(
    val email: RequiredProblem?,
    val password: RequiredProblem?,
) {
    val isValid: Boolean get() = email == null && password == null
}

object LoginForm {
    fun check(
        email: String,
        password: String,
    ): LoginFormCheck =
        LoginFormCheck(
            email = RequiredProblem.Required.takeIf { normalizeText(email).isEmpty() },
            password = RequiredProblem.Required.takeIf { password.isEmpty() },
        )
}

/** [RulesUnmet] has no message of its own; the live rules show what is missing. */
enum class PasswordProblem { Required, InvalidCharacter, TooLong, RulesUnmet }

enum class RepeatPasswordProblem { Required, Mismatch }

data class RegisterFormCheck(
    val email: EmailProblem?,
    val password: PasswordProblem?,
    val repeatPassword: RepeatPasswordProblem?,
    val rules: PasswordCheck,
) {
    val isValid: Boolean get() = email == null && password == null && repeatPassword == null
}

object RegisterForm {
    fun check(
        email: String,
        password: String,
        repeatPassword: String,
    ): RegisterFormCheck {
        val rules = PasswordRules.check(password)
        return RegisterFormCheck(
            email = EmailRule.check(email),
            password = passwordProblem(rules),
            repeatPassword =
                when {
                    repeatPassword.isEmpty() -> RepeatPasswordProblem.Required
                    repeatPassword != password -> RepeatPasswordProblem.Mismatch
                    else -> null
                },
            rules = rules,
        )
    }

    private fun passwordProblem(rules: PasswordCheck): PasswordProblem? =
        when {
            rules.isEmpty -> PasswordProblem.Required
            rules.hasInvalidCharacter -> PasswordProblem.InvalidCharacter
            rules.isTooLong -> PasswordProblem.TooLong
            !rules.meetsAllRules -> PasswordProblem.RulesUnmet
            else -> null
        }
}
