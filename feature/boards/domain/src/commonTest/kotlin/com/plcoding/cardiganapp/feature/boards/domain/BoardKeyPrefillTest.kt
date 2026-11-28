package com.plcoding.cardiganapp.feature.boards.domain

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

/*
 * Break vectors: plain words, diacritics in composed and decomposed form, ß (uppercases to SS),
 * letters without decomposition (Ø, Æ, Ł), digits only, one letter plus digits, emoji, empty,
 * exactly 2 letters, non-Latin script, lowercase, the edges of A–Z and the characters next to them.
 */
class BoardKeyPrefillTest {
    @Test
    fun firstFourLettersUppercased() {
        assertThat(BoardKeyPrefill.fromName("Garden irrigation")).isEqualTo("GARD")
    }

    @Test
    fun diacriticsAreRemoved() {
        assertThat(BoardKeyPrefill.fromName("Ärger")).isEqualTo("ARGE")
        assertThat(BoardKeyPrefill.fromName("Ärger")).isEqualTo("ARGE")
        assertThat(BoardKeyPrefill.fromName("Café")).isEqualTo("CAFE")
    }

    @Test
    fun sharpSBecomesTwoLetters() {
        assertThat(BoardKeyPrefill.fromName("ß")).isEqualTo("SS")
        assertThat(BoardKeyPrefill.fromName("Straße")).isEqualTo("STRA")
    }

    @Test
    fun lettersWithoutDecompositionAreDropped() {
        assertThat(BoardKeyPrefill.fromName("Ø")).isEqualTo("")
        assertThat(BoardKeyPrefill.fromName("Øresund")).isEqualTo("RESU")
        assertThat(BoardKeyPrefill.fromName("Łódź")).isEqualTo("ODZ")
    }

    @Test
    fun digitsOnlyGiveNoKey() {
        assertThat(BoardKeyPrefill.fromName("2026")).isEqualTo("")
    }

    @Test
    fun oneLetterGivesNoKey() {
        assertThat(BoardKeyPrefill.fromName("X 1")).isEqualTo("")
    }

    @Test
    fun twoLettersAreEnough() {
        assertThat(BoardKeyPrefill.fromName("a b")).isEqualTo("AB")
    }

    @Test
    fun emojiIsDropped() {
        assertThat(BoardKeyPrefill.fromName("🚀")).isEqualTo("")
        assertThat(BoardKeyPrefill.fromName("🚀 Launch")).isEqualTo("LAUN")
    }

    @Test
    fun emptyGivesNoKey() {
        assertThat(BoardKeyPrefill.fromName("")).isEqualTo("")
    }

    @Test
    fun nonLatinScriptGivesNoKey() {
        assertThat(BoardKeyPrefill.fromName("看板")).isEqualTo("")
    }

    @Test
    fun edgesOfAToZAreKeptAndTheirNeighboursDropped() {
        assertThat(BoardKeyPrefill.fromName("A@[Z")).isEqualTo("AZ")
        assertThat(BoardKeyPrefill.fromName("a`{z")).isEqualTo("AZ")
    }
}
