package com.plcoding.cardiganapp.feature.auth.domain.validation

import com.plcoding.cardiganapp.core.domain.text.codePointLength
import com.plcoding.cardiganapp.core.domain.text.hasInvalidCharacter
import com.plcoding.cardiganapp.core.domain.text.normalizeText

enum class EmailProblem { Required, TooLong, InvalidFormat }

/**
 * The reference pattern from cardigan-shared-specs `domain-user` "Email format has one reference
 * pattern". Copied exactly; every platform must accept the same emails.
 */
const val EMAIL_PATTERN =
    """^(?=[^@]{1,64}@)[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\.)+[A-Za-z]{2,63}$"""

/**
 * The cardigan-shared-specs `domain-user` email rule for Register: 1–254 code points after
 * `text-length` normalization, no `text-length` invalid characters, and a full match of
 * [EMAIL_PATTERN]. Log in doesn't use it ("Login does not check the email format").
 */
object EmailRule {
    const val MAX_LENGTH = 254

    private val emailRegex = Regex(EMAIL_PATTERN)

    /**
     * Returns the first broken rule in the `api-errors` order: required, invalid character, too
     * long, format. An invalid character shows the same message as a wrong format, so both are
     * [EmailProblem.InvalidFormat].
     */
    fun check(raw: String): EmailProblem? {
        val email = normalizeText(raw)
        return when {
            email.isEmpty() -> EmailProblem.Required
            hasInvalidCharacter(email, multiLine = false) -> EmailProblem.InvalidFormat
            codePointLength(email) > MAX_LENGTH -> EmailProblem.TooLong
            !hasValidFormat(email) -> EmailProblem.InvalidFormat
            else -> null
        }
    }

    internal fun hasValidFormat(email: String): Boolean = emailRegex.matches(email)
}
