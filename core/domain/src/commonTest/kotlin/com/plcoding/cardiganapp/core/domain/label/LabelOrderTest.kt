package com.plcoding.cardiganapp.core.domain.label

import assertk.assertThat
import assertk.assertions.containsExactly
import kotlin.test.Test
import kotlin.uuid.Uuid

/*
 * Break vectors: mixed case where plain string order would put uppercase first, names equal except
 * for case (stable order), a shared prefix, non-ASCII letters, and
 * an emoji (surrogate pair) against a letter near the end of the basic plane.
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
}
