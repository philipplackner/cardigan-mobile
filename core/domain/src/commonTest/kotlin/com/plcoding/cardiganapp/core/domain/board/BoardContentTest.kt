package com.plcoding.cardiganapp.core.domain.board

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlin.uuid.Uuid

class BoardContentTest {

    private fun column(cards: Int) = Column(
        id = Uuid.random(),
        name = "c",
        cards = List(cards) { Card(Uuid.random(), it + 1L, "t", emptyList()) },
    )

    @Test
    fun cardCountSumsAllColumns() {
        val content = BoardContent(Board(Uuid.random(), "B", "CRD"), listOf(column(3), column(0), column(2)))
        assertThat(content.cardCount).isEqualTo(5)
    }

    @Test
    fun noColumnsMeansNoCards() {
        assertThat(BoardContent(Board(Uuid.random(), "B", "CRD"), emptyList()).cardCount).isEqualTo(0)
    }
}
