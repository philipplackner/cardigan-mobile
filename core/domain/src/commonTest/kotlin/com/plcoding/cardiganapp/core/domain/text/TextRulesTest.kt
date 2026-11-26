package com.plcoding.cardiganapp.core.domain.text

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test

/*
 * Break vectors:
 * - line breaks: CRLF, lone CR, mixed
 * - NFC: decomposed é becomes one code point and counts as 1
 * - trimming: spaces, whitespace only, NBSP, U+3000 and other White_Space; BOM and ZWSP are not White_Space;
 *   each edge of the U+0009..U+000D and U+2000..U+200A ranges on its own, and the neighbours outside them
 * - inner whitespace is kept
 * - invalid characters: tab, LF, NUL, U+001F, unpaired high and low surrogates (incl. a high surrogate
 *   followed by a plain character); DEL (U+007F) is allowed
 * - multi-line normalization is the same as single-line
 * - code points: emoji = 1, ZWJ family = 5, CJK = 2
 * - limits: 100 vs 101 code points, emoji at the limit, a limit reached only after trimming
 * - error order: required before invalid before too long
 */
class TextRulesTest {

    @Test
    fun crlfAndLoneCrBecomeLf() {
        assertThat(normalizeText("a\r\nb\rc")).isEqualTo("a\nb\nc")
    }

    @Test
    fun decomposedTextIsComposed() {
        val normalized = normalizeText("Caf" + "é")
        assertThat(normalized).isEqualTo("Café")
        assertThat(codePointLength(normalized)).isEqualTo(4)
    }

    @Test
    fun surroundingSpacesAreTrimmedAndInnerSpacesKept() {
        assertThat(normalizeText("  Release  plan ")).isEqualTo("Release  plan")
    }

    @Test
    fun whitespaceOnlyBecomesEmpty() {
        assertThat(normalizeText(" 　\n")).isEqualTo("")
        assertThat(validateName(normalizeText("   "), 100)).isEqualTo(NameError.Required)
    }

    @Test
    fun unicodeWhiteSpaceIsTrimmed() {
        val spaces = "        　\u0085\u000B\u000C"
        assertThat(normalizeText(spaces + "Board" + spaces)).isEqualTo("Board")
    }

    @Test
    fun byteOrderMarkAndZeroWidthSpaceAreNotTrimmed() {
        assertThat(normalizeText("﻿Board")).isEqualTo("﻿Board")
        assertThat(normalizeText("Board​")).isEqualTo("Board​")
    }

    @Test
    fun innerControlCharactersAreInvalidInANameButOuterOnesAreTrimmed() {
        assertThat(validateName(normalizeText("a\tb"), 100)).isEqualTo(NameError.InvalidCharacter)
        assertThat(validateName(normalizeText("a\nb"), 100)).isEqualTo(NameError.InvalidCharacter)
        assertThat(validateName(normalizeText("a\u0000b"), 100)).isEqualTo(NameError.InvalidCharacter)
        assertThat(validateName(normalizeText("a\u001Fb"), 100)).isEqualTo(NameError.InvalidCharacter)
        assertThat(validateName(normalizeText("\tBoard\n"), 100)).isNull()
    }

    @Test
    fun deleteCharacterIsAllowed() {
        assertThat(validateName("a\u007Fb", 100)).isNull()
    }

    @Test
    fun unpairedSurrogatesAreInvalid() {
        assertThat(validateName("a\uD83D", 100)).isEqualTo(NameError.InvalidCharacter)
        assertThat(validateName("\uDC4Da", 100)).isEqualTo(NameError.InvalidCharacter)
        assertThat(validateName("\uD83D👍", 100)).isEqualTo(NameError.InvalidCharacter)
    }

    @Test
    fun multiLineNormalizesTheSameWay() {
        assertThat(normalizeText(" a\r\nb ", multiLine = true)).isEqualTo("a\nb")
    }

    @Test
    fun codePointsAreCounted() {
        assertThat(codePointLength("👍")).isEqualTo(1)
        assertThat(codePointLength("👨‍👩‍👧")).isEqualTo(5)
        assertThat(codePointLength("看板")).isEqualTo(2)
        assertThat(codePointLength("a\nb")).isEqualTo(3)
        assertThat(codePointLength("")).isEqualTo(0)
        assertThat(codePointLength("\uD83D")).isEqualTo(1)
    }

    @Test
    fun lengthLimitEdges() {
        assertThat(validateName("a".repeat(100), 100)).isNull()
        assertThat(validateName("a".repeat(101), 100)).isEqualTo(NameError.TooLong(100))
    }

    @Test
    fun emojiAtTheLimitCountsAsOne() {
        assertThat(validateName("a".repeat(99) + "👍", 100)).isNull()
        assertThat(validateName("a".repeat(100) + "👍", 100)).isEqualTo(NameError.TooLong(100))
    }

    @Test
    fun limitReachedOnlyAfterTrimming() {
        assertThat(validateName(normalizeText("  " + "a".repeat(100) + "  "), 100)).isNull()
    }

    @Test
    fun invalidCharacterWinsOverTooLong() {
        assertThat(validateName("a".repeat(100) + "\tb", 100)).isEqualTo(NameError.InvalidCharacter)
    }

    @Test
    fun maxIsCarriedInTheError() {
        assertThat(validateName("abcdef", 5)).isEqualTo(NameError.TooLong(5))
    }

    @Test
    fun eachEdgeOfTheWhiteSpaceRangesIsTrimmedOnItsOwn() {
        listOf("\u0009", "\u000B", "\u000C", "\u2000", "\u200A").forEach { space ->
            assertThat(normalizeText(space + "Board" + space), space.first().code.toString(16)).isEqualTo("Board")
        }
    }

    @Test
    fun neighboursOfTheWhiteSpaceRangesAreNotTrimmed() {
        listOf("\u0008", "\u000E", "\u1FFF", "\u200B").forEach { other ->
            assertThat(normalizeText(other + "Board" + other), other.first().code.toString(16)).isEqualTo(other + "Board" + other)
        }
    }

    @Test
    fun highSurrogateFollowedByAPlainCharacterIsInvalid() {
        assertThat(hasInvalidCharacter("\uD83Da", multiLine = false)).isEqualTo(true)
        assertThat(validateName("\uD83Dab", 100)).isEqualTo(NameError.InvalidCharacter)
    }
}
