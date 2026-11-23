package com.plcoding.cardiganapp.core.domain.board

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlin.uuid.Uuid

/*
 * Break vectors: the first number, a three-digit number, numbers past Int.MAX_VALUE, padding with
 * leading zeros, and keys of the shortest and longest length.
 */
class CardDisplayIdTest {

    private fun card(number: Long) = Card(id = Uuid.random(), number = number, title = "t", labelIds = emptyList())

    @Test
    fun firstNumber() {
        assertThat(card(1).displayId("CRD")).isEqualTo("CRD-1")
    }

    @Test
    fun threeDigits() {
        assertThat(card(109).displayId("CRD")).isEqualTo("CRD-109")
    }

    @Test
    fun largeNumberBeyondInt() {
        assertThat(card(3_000_000_000).displayId("CRD")).isEqualTo("CRD-3000000000")
        assertThat(card(Long.MAX_VALUE).displayId("AB")).isEqualTo("AB-9223372036854775807")
    }

    @Test
    fun noLeadingZeros() {
        assertThat(card(7).displayId("CRDBX")).isEqualTo("CRDBX-7")
    }
}
