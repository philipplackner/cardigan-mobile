package com.plcoding.cardiganapp.core.domain.text

import com.plcoding.cardiganapp.core.domain.util.Error

/** Name field errors from cardigan-shared-specs `text-length`, shared by board, column and label names. */
sealed interface NameError : Error {
    data object Required : NameError
    data object InvalidCharacter : NameError
    data class TooLong(val max: Int) : NameError
}

/**
 * Normalizes user text per cardigan-shared-specs `text-length`: CRLF and lone CR become LF, then
 * NFC, then leading and trailing Unicode White_Space is removed. Not for passwords.
 */
fun normalizeText(input: String): String {
    val lineFeeds = input.replace("\r\n", "\n").replace('\r', '\n')
    return lineFeeds.toNfc().trim(::isUnicodeWhiteSpace)
}

/** Number of Unicode code points. A valid surrogate pair counts as one, an unpaired surrogate as one. */
fun codePointLength(text: String): Int {
    var count = 0
    var index = 0
    while (index < text.length) {
        val char = text[index]
        index += if (char.isHighSurrogate() && index + 1 < text.length && text[index + 1].isLowSurrogate()) 2 else 1
        count++
    }
    return count
}

/**
 * True when [text] has an unpaired surrogate or a control character U+0000–U+001F. Tab, LF and CR
 * are allowed only when [multiLine] is true.
 */
fun hasInvalidCharacter(text: String, multiLine: Boolean = false): Boolean {
    var index = 0
    while (index < text.length) {
        val char = text[index]
        when {
            char.isHighSurrogate() -> {
                if (index + 1 >= text.length || !text[index + 1].isLowSurrogate()) return true
                index += 2
                continue
            }
            char.isLowSurrogate() -> return true
            char.code <= 0x1F -> if (!(multiLine && char in MULTI_LINE_CONTROLS)) return true
        }
        index++
    }
    return false
}

/**
 * Validates an already normalized single-line name and returns the first broken rule in the
 * cardigan-shared-specs `api-errors` order: required, invalid character, too long.
 */
fun validateName(normalized: String, max: Int): NameError? = when {
    normalized.isEmpty() -> NameError.Required
    hasInvalidCharacter(normalized) -> NameError.InvalidCharacter
    codePointLength(normalized) > max -> NameError.TooLong(max)
    else -> null
}

private val MULTI_LINE_CONTROLS = setOf('\t', '\n', '\r')

// Kotlin's isWhitespace() differs from the Unicode White_Space property that the shared spec names.
private fun isUnicodeWhiteSpace(char: Char): Boolean = when (char.code) {
    in 0x0009..0x000D, 0x0020, 0x0085, 0x00A0, 0x1680, in 0x2000..0x200A,
    0x2028, 0x2029, 0x202F, 0x205F, 0x3000 -> true
    else -> false
}
