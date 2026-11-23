package com.plcoding.cardiganapp.feature.auth.presentation.components

/** Most UTF-16 characters an email field keeps. Above the 254 code point rule, so its error still shows. */
const val EMAIL_FIELD_MAX_CHARS = 500

/**
 * Cuts a typed or pasted email to [EMAIL_FIELD_MAX_CHARS] UTF-16 characters without splitting a
 * surrogate pair. The shared email pattern overflows the stack on very long input, so a huge paste
 * must never reach validation.
 */
fun capEmailInput(input: String): String {
    if (input.length <= EMAIL_FIELD_MAX_CHARS) return input
    val end = if (input[EMAIL_FIELD_MAX_CHARS - 1].isHighSurrogate()) EMAIL_FIELD_MAX_CHARS - 1 else EMAIL_FIELD_MAX_CHARS
    return input.substring(0, end)
}
