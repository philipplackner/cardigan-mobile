package com.plcoding.cardiganapp.core.domain.text

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Test

/*
 * Break vectors:
 * - password normalization: decomposed é composed, CRLF and surrounding spaces kept, whitespace only kept
 * - code point walk: emoji = one code point, ZWJ family = 5, lone high and lone low surrogate passed on
 * - invalid characters: U+0000, U+001F, tab/LF/CR in single-line vs multi-line, unpaired surrogates in
 *   both modes, DEL and U+3000 allowed
 * - categories: ASCII and non-ASCII Ll/Lu/Nd, Lt and Lm are Other, supplementary Nd/Lu/Ll (U+1D7D9,
 *   U+1D400, U+1D41A), emoji and a lone surrogate are Other
 */
class PasswordTextRulesTest {
    @Test
    fun passwordIsComposed() {
        assertThat(normalizePassword("Caf" + "é")).isEqualTo("Café")
    }

    @Test
    fun passwordKeepsSpacesAndLineBreaks() {
        assertThat(normalizePassword(" Secret Pass1 ")).isEqualTo(" Secret Pass1 ")
        assertThat(normalizePassword("a\r\nb")).isEqualTo("a\r\nb")
        assertThat(normalizePassword(" 　 ")).isEqualTo(" 　 ")
    }

    @Test
    fun codePointWalkJoinsSurrogatePairs() {
        assertThat(codePoints("a👍")).containsExactly(0x61, 0x1F44D)
    }

    @Test
    fun zwjFamilyIsFiveCodePoints() {
        assertThat(codePoints("👨‍👩‍👧").size).isEqualTo(5)
        assertThat(codePointLength("👨‍👩‍👧")).isEqualTo(5)
    }

    @Test
    fun loneSurrogatesArePassedOn() {
        assertThat(codePoints("\uD83Da")).containsExactly(0xD83D, 0x61)
        assertThat(codePoints("a\uDC4D")).containsExactly(0x61, 0xDC4D)
        assertThat(codePoints("\uD83D")).containsExactly(0xD83D)
    }

    @Test
    fun emptyTextHasNoCodePoints() {
        assertThat(codePoints("")).isEqualTo(emptyList())
    }

    @Test
    fun controlCharactersAreInvalid() {
        assertThat(hasInvalidCharacter("a\u0000b", multiLine = false)).isTrue()
        assertThat(hasInvalidCharacter("a\u001Fb", multiLine = false)).isTrue()
        assertThat(hasInvalidCharacter("a\u0000b", multiLine = true)).isTrue()
        assertThat(hasInvalidCharacter("a\u001Fb", multiLine = true)).isTrue()
    }

    @Test
    fun tabAndLineBreaksDependOnMultiLine() {
        for (text in listOf("a\tb", "a\nb", "a\rb")) {
            assertThat(hasInvalidCharacter(text, multiLine = false)).isTrue()
            assertThat(hasInvalidCharacter(text, multiLine = true)).isFalse()
        }
    }

    @Test
    fun unpairedSurrogatesAreInvalidInBothModes() {
        for (multiLine in listOf(false, true)) {
            assertThat(hasInvalidCharacter("a\uD83D", multiLine)).isTrue()
            assertThat(hasInvalidCharacter("\uDC4Da", multiLine)).isTrue()
            assertThat(hasInvalidCharacter("\uD83D👍", multiLine)).isTrue()
        }
    }

    @Test
    fun ordinaryTextIsValid() {
        assertThat(hasInvalidCharacter("", multiLine = false)).isFalse()
        assertThat(hasInvalidCharacter("Ärger 👨‍👩‍👧 \u007F 　", multiLine = false)).isFalse()
    }

    @Test
    fun asciiCategories() {
        assertThat(codePointCategory('a'.code)).isEqualTo(CodePointCategory.Lower)
        assertThat(codePointCategory('Z'.code)).isEqualTo(CodePointCategory.Upper)
        assertThat(codePointCategory('7'.code)).isEqualTo(CodePointCategory.Digit)
        assertThat(codePointCategory(' '.code)).isEqualTo(CodePointCategory.Other)
        assertThat(codePointCategory('!'.code)).isEqualTo(CodePointCategory.Other)
    }

    @Test
    fun nonAsciiBmpCategories() {
        assertThat(codePointCategory('ß'.code)).isEqualTo(CodePointCategory.Lower)
        assertThat(codePointCategory('Ä'.code)).isEqualTo(CodePointCategory.Upper)
        assertThat(codePointCategory('١'.code)).isEqualTo(CodePointCategory.Digit)
        assertThat(codePointCategory('ǅ'.code)).isEqualTo(CodePointCategory.Other)
        assertThat(codePointCategory('ʰ'.code)).isEqualTo(CodePointCategory.Other)
        assertThat(codePointCategory('Ⅻ'.code)).isEqualTo(CodePointCategory.Other)
    }

    @Test
    fun supplementaryCategories() {
        assertThat(codePointCategory(0x1D7D9)).isEqualTo(CodePointCategory.Digit)
        assertThat(codePointCategory(0x1D400)).isEqualTo(CodePointCategory.Upper)
        assertThat(codePointCategory(0x1D41A)).isEqualTo(CodePointCategory.Lower)
        assertThat(codePointCategory(0x1F44D)).isEqualTo(CodePointCategory.Other)
    }

    @Test
    fun loneSurrogateIsOther() {
        assertThat(codePointCategory(0xD83D)).isEqualTo(CodePointCategory.Other)
    }

    private fun codePoints(text: String): List<Int> = buildList { forEachCodePoint(text) { add(it) } }
}
