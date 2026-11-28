package com.plcoding.cardiganapp.feature.auth.domain.validation

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Test

/*
 * Break vectors:
 * - length in code points after NFC: 11, 12, 128, 129; emoji and decomposed letters counted once
 * - each class missing; non-ASCII Ll/Lu/Nd; supplementary digit; titlecase is not uppercase
 * - spaces: only spaces, leading and trailing kept and counted
 * - control characters: tab, LF, NUL; lone surrogate
 * - empty
 */
class PasswordRulesTest {
    @Test
    fun lengthEdges() {
        assertThat(PasswordRules.check("Aa1" + "x".repeat(8)).hasMinLength).isFalse()
        assertThat(PasswordRules.check("Aa1" + "x".repeat(9)).hasMinLength).isTrue()
        assertThat(PasswordRules.check("Aa1" + "x".repeat(125)).isTooLong).isFalse()
        assertThat(PasswordRules.check("Aa1" + "x".repeat(126)).isTooLong).isTrue()
    }

    @Test
    fun emojiCountsOnce() {
        val check = PasswordRules.check("Abcdefghij1😀")
        assertThat(check.hasMinLength).isTrue()
        assertThat(PasswordRules.check("Abcdefghi1😀").hasMinLength).isFalse()
    }

    @Test
    fun decomposedLettersAreComposedBeforeCounting() {
        assertThat(PasswordRules.check("Abcdefghi1" + "e\u0301").hasMinLength).isFalse()
    }

    @Test
    fun nonAsciiLettersAndDigits() {
        assertThat(PasswordRules.check("äöüÄÖÜ١٢٣ßßßß").meetsAllRules).isTrue()
    }

    @Test
    fun supplementaryDigitCounts() {
        assertThat(PasswordRules.check("Abcdefghijk\uD835\uDFD9").hasDigit).isTrue()
    }

    @Test
    fun titlecaseIsNotUppercase() {
        assertThat(PasswordRules.check("ǅ").hasUppercase).isFalse()
    }

    @Test
    fun eachMissingClass() {
        assertThat(PasswordRules.check("correct horse 7").hasUppercase).isFalse()
        assertThat(PasswordRules.check("CORRECT HORSE 7").hasLowercase).isFalse()
        assertThat(PasswordRules.check("Correct horse!").hasDigit).isFalse()
        assertThat(PasswordRules.check("Correct horse 7").meetsAllRules).isTrue()
    }

    @Test
    fun spacesAreKept() {
        val check = PasswordRules.check(" Secret Pass1 ")
        assertThat(check.hasMinLength).isTrue()
        assertThat(PasswordRules.check(" ".repeat(12)).hasMinLength).isTrue()
        assertThat(PasswordRules.check(" ".repeat(12)).isEmpty).isFalse()
    }

    @Test
    fun controlCharacters() {
        assertThat(PasswordRules.check("Correct\thorse 7").hasInvalidCharacter).isTrue()
        assertThat(PasswordRules.check("Correct\nhorse 7").hasInvalidCharacter).isTrue()
        assertThat(PasswordRules.check("Correct\u0000horse 7").hasInvalidCharacter).isTrue()
        assertThat(PasswordRules.check("Correct horse 7\uD83D").hasInvalidCharacter).isTrue()
        assertThat(PasswordRules.check("Correct horse 7").hasInvalidCharacter).isFalse()
    }

    @Test
    fun empty() {
        val check = PasswordRules.check("")
        assertThat(check.isEmpty).isTrue()
        assertThat(check.hasMinLength || check.hasLowercase || check.hasUppercase || check.hasDigit).isFalse()
    }

    @Test
    fun typingCompletesTheRulesInOrder() {
        val target = "Correct horse 7"
        for (end in 1..target.length) {
            val check = PasswordRules.check(target.substring(0, end))
            assertThat(check.hasUppercase).isTrue()
            assertThat(check.hasLowercase).isEqualTo(end >= 2)
            assertThat(check.hasMinLength).isEqualTo(end >= 12)
            assertThat(check.hasDigit).isEqualTo(end == target.length)
        }
    }
}
