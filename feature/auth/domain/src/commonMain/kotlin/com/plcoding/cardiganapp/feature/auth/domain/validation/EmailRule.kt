package com.plcoding.cardiganapp.feature.auth.domain.validation

import com.plcoding.cardiganapp.core.domain.text.codePointLength
import com.plcoding.cardiganapp.core.domain.text.hasInvalidCharacter
import com.plcoding.cardiganapp.core.domain.text.normalizeText

enum class EmailProblem { Required, TooLong, InvalidFormat }

/**
 * The cardigan-shared-specs `domain-user` email rule: 1–254 code points after `text-length`
 * normalization, and an RFC 5322 `addr-spec` in ASCII without comments or folding whitespace.
 */
object EmailRule {
    const val MAX_LENGTH = 254

    /** Returns the first broken rule in the backend's order (empty, too long, invalid character, format), or null. */
    fun check(raw: String): EmailProblem? {
        val email = normalizeText(raw)
        return when {
            email.isEmpty() -> EmailProblem.Required
            codePointLength(email) > MAX_LENGTH -> EmailProblem.TooLong
            hasInvalidCharacter(email, multiLine = false) -> EmailProblem.InvalidFormat
            !isAddrSpec(email) -> EmailProblem.InvalidFormat
            else -> null
        }
    }

    // A single forward scan instead of a regex, so no input can make the check backtrack.
    internal fun isAddrSpec(text: String): Boolean {
        val localEnd = if (text.startsWith('"')) quotedStringEnd(text) else text.indexOf('@')
        if (localEnd <= 0 || localEnd >= text.length || text[localEnd] != '@') return false
        val local = text.substring(0, localEnd)
        val domain = text.substring(localEnd + 1)
        val localValid = local.startsWith('"') || isDotAtom(local)
        return localValid && (isDomainLiteral(domain) || isDotAtom(domain))
    }

    /** Index right after the closing quote of a quoted string at the start of [text], or -1. */
    private fun quotedStringEnd(text: String): Int {
        var index = 1
        while (index < text.length) {
            val code = text[index].code
            when {
                code == '"'.code -> return index + 1
                code == '\\'.code -> {
                    if (index + 1 >= text.length || text[index + 1].code !in 0x20..0x7E) return -1
                    index += 2
                }
                code == 0x21 || code in 0x23..0x5B || code in 0x5D..0x7E -> index++
                else -> return -1
            }
        }
        return -1
    }

    private fun isDotAtom(text: String): Boolean {
        if (text.isEmpty()) return false
        var previousWasDot = true
        for (char in text) {
            if (char == '.') {
                if (previousWasDot) return false
                previousWasDot = true
            } else {
                if (!char.isAtext()) return false
                previousWasDot = false
            }
        }
        return !previousWasDot
    }

    private fun isDomainLiteral(text: String): Boolean {
        if (text.length < 2 || text.first() != '[' || text.last() != ']') return false
        for (index in 1 until text.length - 1) {
            val code = text[index].code
            if (code !in 0x21..0x5A && code !in 0x5E..0x7E) return false
        }
        return true
    }

    private fun Char.isAtext(): Boolean =
        this in 'A'..'Z' || this in 'a'..'z' || this in '0'..'9' || this in ATEXT_SYMBOLS

    private const val ATEXT_SYMBOLS = "!#$%&'*+/=?^_`{|}~-"
}
