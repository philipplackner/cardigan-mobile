package com.plcoding.cardiganapp.feature.auth.domain.validation

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isLessThan
import assertk.assertions.isNull
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.measureTime

/*
 * Break vectors (cardigan-shared-specs `domain-user`, change practical-email-format):
 * - emptiness: empty, whitespace only (incl. U+3000)
 * - normalization: surrounding spaces trimmed before the check
 * - local part: plus and dots, 64 vs 65 characters (the lookahead), leading/trailing/double dots,
 *   quoted forms, a comment, two @, a missing local part
 * - domain: subdomains, punycode below the top level, single label, missing, domain literal,
 *   trailing dot, hyphen at a label edge, 1-letter / numeric / punycode top-level label,
 *   a 63-character label vs 64
 * - characters: non-ASCII, tab, NUL, lone surrogate
 * - order: invalid character before too long, too long before format (255 without @)
 * - performance: the spec's 250 KB near-match goes through the rule as TooLong without reaching the
 *   pattern; the pattern itself only ever sees up to 500 characters (field cap), rejected fast
 */
class EmailRuleTest {

    private val domain = "@example.com"

    private fun assertValid(email: String) = assertThat(EmailRule.check(email), email).isNull()

    private fun assertInvalidFormat(email: String) =
        assertThat(EmailRule.check(email), email).isEqualTo(EmailProblem.InvalidFormat)

    @Test
    fun acceptedAddresses() {
        assertValid("a@b.co")
        assertValid("ann.lee+cards@example.com")
        assertValid("ann@mail.example.com")
        assertValid("ann@a.b.example.co.uk")
        assertValid("ann@xn--mnchen-3ya.de")
        assertValid("a".repeat(64) + domain)
        assertValid("!#$%&'*+/=?^_`{|}~-@example.com")
        assertValid("ann@" + "a".repeat(63) + ".com")
    }

    @Test
    fun surroundingSpacesAreTrimmed() {
        assertValid("  Ann@Example.com ")
    }

    @Test
    fun singleLabelAndMissingDomains() {
        assertInvalidFormat("mail@pl-coding")
        assertInvalidFormat("ann@localhost")
        assertInvalidFormat("ann@")
        assertInvalidFormat("@example.com")
        assertInvalidFormat("example.com")
    }

    @Test
    fun domainLiteralAndTrailingDot() {
        assertInvalidFormat("ann@[192.168.0.1]")
        assertInvalidFormat("ann@example.com.")
    }

    @Test
    fun hyphenAtALabelEdge() {
        assertInvalidFormat("ann@-example.com")
        assertInvalidFormat("ann@example-.com")
    }

    @Test
    fun invalidTopLevelLabels() {
        assertInvalidFormat("ann@example.c")
        assertInvalidFormat("ann@example.123")
        assertInvalidFormat("ann@example.xn--p1ai")
    }

    @Test
    fun labelOverSixtyThreeCharacters() {
        assertInvalidFormat("ann@" + "a".repeat(64) + ".com")
    }

    @Test
    fun quotedLocalParts() {
        assertInvalidFormat("\"ann lee\"@example.com")
        assertInvalidFormat("\"ann\"@example.com")
        assertInvalidFormat("\"ann@lee\"@example.com")
        assertInvalidFormat("\"ann\\\"lee\"@example.com")
    }

    @Test
    fun localPartOverSixtyFourCharacters() {
        assertInvalidFormat("a".repeat(65) + domain)
    }

    @Test
    fun misplacedDots() {
        assertInvalidFormat(".ann@example.com")
        assertInvalidFormat("ann.@example.com")
        assertInvalidFormat("an..n@example.com")
    }

    @Test
    fun commentAndTwoAtSigns() {
        assertInvalidFormat("ann(work)@example.com")
        assertInvalidFormat("ann@@example.com")
        assertInvalidFormat("ann@exa@mple.com")
    }

    @Test
    fun nonAsciiIsAFormatError() {
        assertInvalidFormat("jürgen@münchen.de")
        assertInvalidFormat("ann@exämple.com")
    }

    @Test
    fun emptyAndWhitespaceOnly() {
        assertThat(EmailRule.check("")).isEqualTo(EmailProblem.Required)
        assertThat(EmailRule.check("  　 ")).isEqualTo(EmailProblem.Required)
    }

    @Test
    fun controlCharactersAndSurrogates() {
        assertInvalidFormat("ann\t@example.com")
        assertInvalidFormat("a\u0000nn@example.com")
        assertInvalidFormat("ann\uD83D@example.com")
    }

    @Test
    fun lengthLimit() {
        assertValid("a".repeat(64) + "@" + "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(57) + ".com")
        assertThat(EmailRule.check("a".repeat(255))).isEqualTo(EmailProblem.TooLong)
        assertThat(EmailRule.check("ä".repeat(255))).isEqualTo(EmailProblem.TooLong)
        assertThat(EmailRule.check("😀".repeat(254))).isEqualTo(EmailProblem.InvalidFormat)
        assertThat(EmailRule.check("😀".repeat(255))).isEqualTo(EmailProblem.TooLong)
    }

    @Test
    fun invalidCharacterComesBeforeTooLong() {
        assertThat(EmailRule.check("a\t" + "a".repeat(300))).isEqualTo(EmailProblem.InvalidFormat)
    }

    @Test
    fun specNearMatchThroughTheRuleIsTooLongFast() {
        val input = "a@" + "a.".repeat(125_000) + "1"
        var problem: EmailProblem? = null
        val duration = measureTime { problem = EmailRule.check(input) }
        println("EmailRule.check 250 KB near-match: $duration")
        assertThat(problem).isEqualTo(EmailProblem.TooLong)
        assertThat(duration).isLessThan(100.milliseconds)
    }

    @Test
    fun longestNearMatchThatReachesThePatternIsRejectedFast() {
        val input = "a@" + "a.".repeat(125) + "1"
        assertThat(input.length).isEqualTo(253)
        assertPatternRejectsFast(input + "1", "254-character")
    }

    @Test
    fun nearMatchAtTheFieldCapIsRejectedFast() {
        val input = "a@" + "a.".repeat(248) + "1"
        assertThat(input.length).isEqualTo(499)
        assertPatternRejectsFast(input + "1", "500-character")
    }

    @Test
    fun localPartNearMatchAtTheFieldCapIsRejectedFast() {
        assertPatternRejectsFast("a.".repeat(249) + "a@", "500-character local-part")
    }

    private fun assertPatternRejectsFast(input: String, label: String) {
        var valid = true
        val duration = measureTime { valid = EmailRule.hasValidFormat(input) }
        println("EMAIL_PATTERN $label near-match (${input.length} chars): $duration")
        assertThat(valid).isFalse()
        assertThat(duration).isLessThan(100.milliseconds)
    }
}
