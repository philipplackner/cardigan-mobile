package com.plcoding.cardiganapp.core.domain.board

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlin.test.Test

/*
 * Break vectors: lowercase input, the edges of a–z and A–Z and the characters right next to them
 * (` { @ [), the length edges (1, 2, 5, 6), digits, surrounding spaces (no
 * trimming), non-ASCII letters that uppercase to letters outside A–Z (Ä, the titlecase ǅ), an
 * emoji made of surrogate pairs, the empty string and a lone space.
 */
class BoardKeyRuleTest {

    private fun check(input: String) = BoardKeyRule.validate(BoardKeyRule.normalize(input))

    @Test
    fun lowercaseIsNormalizedAndValid() {
        assertThat(BoardKeyRule.normalize("crdb")).isEqualTo("CRDB")
        assertThat(check("crd")).isNull()
    }

    @Test
    fun uppercaseIsValid() {
        assertThat(check("CRD")).isNull()
    }

    @Test
    fun lengthEdges() {
        assertThat(check("C")).isEqualTo(BoardKeyError.TooShort)
        assertThat(check("CR")).isNull()
        assertThat(check("CRDBX")).isNull()
        assertThat(check("CRDBXY")).isEqualTo(BoardKeyError.TooLong)
    }

    @Test
    fun emptyIsRequired() {
        assertThat(check("")).isEqualTo(BoardKeyError.Required)
    }

    @Test
    fun singleSpaceIsTooShortNotRequired() {
        assertThat(check(" ")).isEqualTo(BoardKeyError.TooShort)
    }

    @Test
    fun digitIsInvalidFormat() {
        assertThat(check("CR1")).isEqualTo(BoardKeyError.InvalidFormat)
    }

    @Test
    fun leadingSpaceIsNotTrimmed() {
        assertThat(BoardKeyRule.normalize(" CRD")).isEqualTo(" CRD")
        assertThat(check(" CRD")).isEqualTo(BoardKeyError.InvalidFormat)
    }

    @Test
    fun nonAsciiLettersStayInvalid() {
        assertThat(BoardKeyRule.normalize("äbc")).isEqualTo("äBC")
        assertThat(check("ÄBC")).isEqualTo(BoardKeyError.InvalidFormat)
        assertThat(check("ǅAB")).isEqualTo(BoardKeyError.InvalidFormat)
    }

    @Test
    fun emojiIsInvalidFormat() {
        assertThat(check("A😀")).isEqualTo(BoardKeyError.InvalidFormat)
    }

    @Test
    fun tooLongWinsOverFormat() {
        assertThat(check("CRD123")).isEqualTo(BoardKeyError.TooLong)
    }

    @Test
    fun onlyTheLettersAToZAreUppercased() {
        assertThat(BoardKeyRule.normalize("az")).isEqualTo("AZ")
        assertThat(BoardKeyRule.normalize("`{")).isEqualTo("`{")
    }

    @Test
    fun theEdgesOfAToZAreValidAndTheirNeighboursAreNot() {
        assertThat(BoardKeyRule.validate("AZ")).isNull()
        assertThat(BoardKeyRule.validate("A@")).isEqualTo(BoardKeyError.InvalidFormat)
        assertThat(BoardKeyRule.validate("Z[")).isEqualTo(BoardKeyError.InvalidFormat)
    }

    @Test
    fun isValidMatchesValidate() {
        assertThat(BoardKeyRule.isValid("CRD")).isTrue()
        assertThat(BoardKeyRule.isValid("C")).isFalse()
        assertThat(BoardKeyRule.isValid("crd")).isFalse()
    }
}
