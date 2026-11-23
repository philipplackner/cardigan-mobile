package com.plcoding.cardiganapp.feature.auth.domain.validation

import com.plcoding.cardiganapp.core.domain.text.CodePointCategory
import com.plcoding.cardiganapp.core.domain.text.codePointCategory
import com.plcoding.cardiganapp.core.domain.text.forEachCodePoint
import com.plcoding.cardiganapp.core.domain.text.hasInvalidCharacter
import com.plcoding.cardiganapp.core.domain.text.normalizePassword

/**
 * The result of checking a password against cardigan-shared-specs `domain-user` "Passwords follow
 * one composition rule". The first four flags are the live rules shown on Register.
 */
data class PasswordCheck(
    val hasMinLength: Boolean,
    val hasLowercase: Boolean,
    val hasUppercase: Boolean,
    val hasDigit: Boolean,
    val isTooLong: Boolean,
    val hasInvalidCharacter: Boolean,
    val isEmpty: Boolean,
) {
    val meetsAllRules: Boolean get() = hasMinLength && hasLowercase && hasUppercase && hasDigit
}

object PasswordRules {
    const val MIN_LENGTH = 12
    const val MAX_LENGTH = 128

    /** Checks [raw] after NFC, without trimming. Length is counted in code points. */
    fun check(raw: String): PasswordCheck {
        val password = normalizePassword(raw)
        var length = 0
        var lower = false
        var upper = false
        var digit = false
        forEachCodePoint(password) { codePoint ->
            length++
            when (codePointCategory(codePoint)) {
                CodePointCategory.Lower -> lower = true
                CodePointCategory.Upper -> upper = true
                CodePointCategory.Digit -> digit = true
                CodePointCategory.Other -> Unit
            }
        }
        return PasswordCheck(
            hasMinLength = length >= MIN_LENGTH,
            hasLowercase = lower,
            hasUppercase = upper,
            hasDigit = digit,
            isTooLong = length > MAX_LENGTH,
            hasInvalidCharacter = hasInvalidCharacter(password, multiLine = false),
            isEmpty = password.isEmpty(),
        )
    }
}
