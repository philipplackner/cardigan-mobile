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
 * NFC, then leading and trailing Unicode White_Space is removed. Not for passwords. The steps are
 * the same for single- and multi-line fields; [multiLine] states the field type at the call site,
 * and which control characters are allowed is decided by validation.
 */
@Suppress("UNUSED_PARAMETER")
fun normalizeText(input: String, multiLine: Boolean = false): String {
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
 * Normalizes a password per cardigan-shared-specs `text-length` "Passwords are normalized without
 * trimming": NFC only. Line breaks and surrounding whitespace are kept.
 */
fun normalizePassword(input: String): String = input.toNfc()

/**
 * Calls [action] with every Unicode code point of [text]. A valid surrogate pair gives one code
 * point; an unpaired surrogate is passed on as its own code unit value.
 */
inline fun forEachCodePoint(text: String, action: (codePoint: Int) -> Unit) {
    var index = 0
    while (index < text.length) {
        val high = text[index]
        if (high.isHighSurrogate() && index + 1 < text.length && text[index + 1].isLowSurrogate()) {
            val low = text[index + 1]
            action(0x10000 + ((high.code - 0xD800) shl 10) + (low.code - 0xDC00))
            index += 2
        } else {
            action(high.code)
            index++
        }
    }
}

/**
 * True when [text] holds a character that cardigan-shared-specs `text-length` "Invalid characters
 * are rejected" forbids: an unpaired surrogate or U+0000–U+001F. Tab, LF and CR are allowed only
 * when [multiLine] is true.
 */
fun hasInvalidCharacter(text: String, multiLine: Boolean): Boolean {
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
            char.code <= 0x1F && !(multiLine && char.isAllowedMultiLineControl()) -> return true
        }
        index++
    }
    return false
}

private fun Char.isAllowedMultiLineControl(): Boolean = this == '\t' || this == '\n' || this == '\r'

/**
 * Validates an already normalized single-line name and returns the first broken rule in the
 * cardigan-shared-specs `api-errors` order: required, invalid character, too long.
 */
fun validateName(normalized: String, max: Int): NameError? = when {
    normalized.isEmpty() -> NameError.Required
    hasInvalidCharacter(normalized, multiLine = false) -> NameError.InvalidCharacter
    codePointLength(normalized) > max -> NameError.TooLong(max)
    else -> null
}

// Kotlin's isWhitespace() differs from the Unicode White_Space property that the shared spec names.
private fun isUnicodeWhiteSpace(char: Char): Boolean = when (char.code) {
    in 0x0009..0x000D, 0x0020, 0x0085, 0x00A0, 0x1680, in 0x2000..0x200A,
    0x2028, 0x2029, 0x202F, 0x205F, 0x3000 -> true
    else -> false
}
