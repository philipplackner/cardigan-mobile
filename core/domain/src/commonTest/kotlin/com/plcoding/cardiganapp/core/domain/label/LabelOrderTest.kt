package com.plcoding.cardiganapp.core.domain.label

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isLessThan
import kotlin.test.Test
import kotlin.uuid.Uuid

/*
 * Break vectors: mixed case where plain string order would put uppercase first, names equal except
 * for case (stable order), a shared prefix in both comparison directions, non-ASCII letters,
 * an emoji (surrogate pair) against a letter near the end of the basic plane, U+FFFF (the last
 * single-unit code point) followed by more text, two supplementary characters whose high and low
 * surrogates order differently, and a lone high surrogate at the end of a name.
 */
class LabelOrderTest {

    private fun label(name: String) = Label(Uuid.random(), name, LabelColor.Blue)

    private fun List<Label>.names() = map { it.name }

    @Test
    fun ignoresCase() {
        val sorted = listOf(label("frontend"), label("Bug"), label("api")).sortedForCard()
        assertThat(sorted.names()).containsExactly("api", "Bug", "frontend")
    }

    @Test
    fun namesEqualExceptForCaseKeepTheirOrder() {
        val sorted = listOf(label("Bug"), label("api"), label("bug")).sortedForCard()
        assertThat(sorted.names()).containsExactly("api", "Bug", "bug")
        val reversed = listOf(label("bug"), label("Bug")).sortedForCard()
        assertThat(reversed.names()).containsExactly("bug", "Bug")
    }

    @Test
    fun sharedPrefixShorterFirst() {
        val sorted = listOf(label("Design-system"), label("design")).sortedForCard()
        assertThat(sorted.names()).containsExactly("design", "Design-system")
    }

    @Test
    fun nonAsciiUppercaseIsFoldedAndSortsByCodePoint() {
        val sorted = listOf(label("Ärger"), label("zebra"), label("äpfel")).sortedForCard()
        assertThat(sorted.names()).containsExactly("zebra", "äpfel", "Ärger")
    }

    @Test
    fun supplementaryCodePointsSortAfterTheEndOfTheBasicPlane() {
        val sorted = listOf(label("\uD83D\uDE00 smile"), label("\uFF41 wide")).sortedForCard()
        assertThat(sorted.names()).containsExactly("\uFF41 wide", "\uD83D\uDE00 smile")
    }

    @Test
    fun sharedPrefixIsShorterFirstInBothDirections() {
        assertThat(LabelOrder.compare(label("design"), label("design-system"))).isLessThan(0)
        assertThat(LabelOrder.compare(label("design-system"), label("design"))).isGreaterThan(0)
        assertThat(LabelOrder.compare(label("Design"), label("design"))).isEqualTo(0)
    }

    @Test
    fun textAfterTheLastSingleUnitCodePointIsCompared() {
        assertThat(LabelOrder.compare(label("\uFFFFa"), label("\uFFFFb"))).isLessThan(0)
        assertThat(LabelOrder.compare(label("\uFFFFb"), label("\uFFFFa"))).isGreaterThan(0)
    }

    @Test
    fun supplementaryCodePointsSortByTheirHighSurrogateFirst() {
        // U+103FF is D800 DFFF, U+10400 is D801 DC00: the low surrogates order the other way.
        assertThat(LabelOrder.compare(label("\uD800\uDFFF"), label("\uD801\uDC00"))).isLessThan(0)
        assertThat(LabelOrder.compare(label("\uD800\uDC00"), label("\uD800\uDC01"))).isLessThan(0)
    }

    @Test
    fun loneHighSurrogateAtTheEndSortsBeforeAFullPair() {
        assertThat(LabelOrder.compare(label("a\uD83D"), label("a\uD83D\uDE00"))).isLessThan(0)
        assertThat(LabelOrder.compare(label("a\uD83D\uDE00"), label("a\uD83D"))).isGreaterThan(0)
    }
}
