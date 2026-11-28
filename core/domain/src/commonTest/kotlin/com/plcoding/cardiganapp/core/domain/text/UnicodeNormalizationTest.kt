package com.plcoding.cardiganapp.core.domain.text

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class UnicodeNormalizationTest {
    private val composed = "é"
    private val decomposed = "é"

    @Test
    fun decomposedBecomesOneCodePoint() {
        assertThat(decomposed.toNfc()).isEqualTo(composed)
    }

    @Test
    fun composedBecomesBaseLetterAndMark() {
        assertThat(composed.toNfd()).isEqualTo(decomposed)
    }

    @Test
    fun roundTripKeepsText() {
        val text = "Caf$decomposed 👩‍💻 Ärger"
        assertThat(text.toNfc().toNfd().toNfc()).isEqualTo(text.toNfc())
    }

    @Test
    fun emptyStaysEmpty() {
        assertThat("".toNfc()).isEqualTo("")
        assertThat("".toNfd()).isEqualTo("")
    }
}
