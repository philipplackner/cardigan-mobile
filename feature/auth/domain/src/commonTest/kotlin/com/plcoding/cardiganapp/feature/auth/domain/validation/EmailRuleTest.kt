package com.plcoding.cardiganapp.feature.auth.domain.validation

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isLessThan
import assertk.assertions.isNull
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.measureTime

/*
 * Break vectors:
 * - emptiness: empty, whitespace only (incl. U+3000)
 * - length: 254 vs 255 code points, 255 with a format error still gives TooLong, emoji counted once
 * - normalization: surrounding spaces trimmed before the check
 * - local part: plus address, quoted string with @, escaped space and escaped quote, bare space (folding whitespace), unclosed quote, quote then
 *   no @, leading, trailing and consecutive dots
 * - domain: missing, domain literal, unclosed literal, trailing dot, second @
 * - characters: non-ASCII, comment, tab inside, NUL, lone surrogate
 * - performance: a 250 KB near-match finishes fast
 */
class EmailRuleTest {

    @Test
    fun validAddress() {
        assertThat(EmailRule.check("ann.lee+cards@example.com")).isNull()
    }

    @Test
    fun missingDomain() {
        assertThat(EmailRule.check("ann@")).isEqualTo(EmailProblem.InvalidFormat)
    }

    @Test
    fun missingLocalPartAndMissingAt() {
        assertThat(EmailRule.check("@example.com")).isEqualTo(EmailProblem.InvalidFormat)
        assertThat(EmailRule.check("example.com")).isEqualTo(EmailProblem.InvalidFormat)
    }

    @Test
    fun nonAsciiAddress() {
        assertThat(EmailRule.check("jürgen@münchen.de")).isEqualTo(EmailProblem.InvalidFormat)
    }

    @Test
    fun commentInTheAddress() {
        assertThat(EmailRule.check("ann(work)@example.com")).isEqualTo(EmailProblem.InvalidFormat)
    }

    @Test
    fun emptyAndWhitespaceOnly() {
        assertThat(EmailRule.check("")).isEqualTo(EmailProblem.Required)
        assertThat(EmailRule.check("  \u3000 ")).isEqualTo(EmailProblem.Required)
    }

    @Test
    fun surroundingSpacesAreTrimmed() {
        assertThat(EmailRule.check("  Ann@Example.com ")).isNull()
    }

    @Test
    fun lengthLimit() {
        val domain = "@example.com"
        assertThat(EmailRule.check("a".repeat(254 - domain.length) + domain)).isNull()
        assertThat(EmailRule.check("a".repeat(255 - domain.length) + domain)).isEqualTo(EmailProblem.TooLong)
    }

    @Test
    fun overTheLimitWinsOverTheFormat() {
        assertThat(EmailRule.check("ä".repeat(255))).isEqualTo(EmailProblem.TooLong)
    }

    @Test
    fun emojiCountsAsOneCodePoint() {
        assertThat(EmailRule.check("😀".repeat(254))).isEqualTo(EmailProblem.InvalidFormat)
        assertThat(EmailRule.check("😀".repeat(255))).isEqualTo(EmailProblem.TooLong)
    }

    @Test
    fun quotedLocalPart() {
        assertThat(EmailRule.check("\"ann@lee\"@example.com")).isNull()
        assertThat(EmailRule.check("\"ann\\ lee\"@example.com")).isNull()
        assertThat(EmailRule.check("\"ann lee\"@example.com")).isEqualTo(EmailProblem.InvalidFormat)
        assertThat(EmailRule.check("\"ann\\\"lee\"@example.com")).isNull()
        assertThat(EmailRule.check("\"ann@example.com")).isEqualTo(EmailProblem.InvalidFormat)
        assertThat(EmailRule.check("\"ann\"x@example.com")).isEqualTo(EmailProblem.InvalidFormat)
    }

    @Test
    fun dotsInTheLocalPart() {
        assertThat(EmailRule.check(".ann@example.com")).isEqualTo(EmailProblem.InvalidFormat)
        assertThat(EmailRule.check("ann.@example.com")).isEqualTo(EmailProblem.InvalidFormat)
        assertThat(EmailRule.check("ann..lee@example.com")).isEqualTo(EmailProblem.InvalidFormat)
    }

    @Test
    fun domainShapes() {
        assertThat(EmailRule.check("ann@[192.168.0.1]")).isNull()
        assertThat(EmailRule.check("ann@localhost")).isNull()
        assertThat(EmailRule.check("ann@[192.168.0.1")).isEqualTo(EmailProblem.InvalidFormat)
        assertThat(EmailRule.check("ann@example.com.")).isEqualTo(EmailProblem.InvalidFormat)
        assertThat(EmailRule.check("ann@exa@mple.com")).isEqualTo(EmailProblem.InvalidFormat)
    }

    @Test
    fun controlCharactersAndSurrogates() {
        assertThat(EmailRule.check("ann\t@example.com")).isEqualTo(EmailProblem.InvalidFormat)
        assertThat(EmailRule.check("a\u0000nn@example.com")).isEqualTo(EmailProblem.InvalidFormat)
        assertThat(EmailRule.check("ann\uD83D@example.com")).isEqualTo(EmailProblem.InvalidFormat)
    }

    @Test
    fun longNearMatchFinishesFast() {
        val input = "a.".repeat(125_000) + "@"
        val duration = measureTime { EmailRule.isAddrSpec(input) }
        assertThat(duration).isLessThan(100.milliseconds)
    }
}
