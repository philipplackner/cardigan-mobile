package com.plcoding.cardiganapp.feature.boards.domain

import com.plcoding.cardiganapp.core.domain.text.toNfd

/** The key the New board dialog suggests while the user hasn't edited the key (shared with web). */
object BoardKeyPrefill {
    private const val LENGTH = 4
    private const val MIN_LENGTH = 2

    /**
     * Removes diacritics, uppercases, keeps only `A`–`Z` and takes the first 4 letters. Returns ""
     * when fewer than 2 letters remain. Letters without a decomposition, such as Ø, are dropped.
     */
    fun fromName(name: String): String {
        // Decomposing splits diacritics off as combining marks, which the A–Z filter then drops.
        val letters =
            name
                .toNfd()
                .uppercase()
                .filter { it in 'A'..'Z' }
                .take(LENGTH)
        return if (letters.length < MIN_LENGTH) "" else letters
    }
}
