package com.plcoding.cardiganapp.core.domain.board

/**
 * Board key format from cardigan-shared-specs `domain-board`: 2–5 letters A–Z. Shared by every key
 * field (New board, import key clash).
 */
object BoardKeyRule {
    const val MIN_LENGTH = 2
    const val MAX_LENGTH = 5

    /** Converts `a`–`z` to uppercase and leaves every other character as is. */
    fun normalize(input: String): String = buildString(input.length) {
        input.forEach { append(if (it in 'a'..'z') it - ('a' - 'A') else it) }
    }

    /** Validates an already normalized key in the order empty, too short, too long, format. */
    fun validate(key: String): BoardKeyError? = when {
        key.isEmpty() -> BoardKeyError.Required
        key.length < MIN_LENGTH -> BoardKeyError.TooShort
        key.length > MAX_LENGTH -> BoardKeyError.TooLong
        key.any { it !in 'A'..'Z' } -> BoardKeyError.InvalidFormat
        else -> null
    }

    fun isValid(key: String): Boolean = validate(key) == null
}
