package com.plcoding.cardiganapp.core.domain.board

import kotlin.uuid.Uuid

/** A board with its columns in position order. Positions are dense, so a list index is the position. */
data class BoardContent(
    val board: Board,
    val columns: List<Column>,
) {
    val cardCount: Int get() = columns.sumOf { it.cards.size }
}

/** A column with its cards in position order. */
data class Column(
    val id: Uuid,
    val name: String,
    val cards: List<Card>,
)

/** A card as the board shows it. [number] is assigned by the backend and unique per board. */
data class Card(
    val id: Uuid,
    val number: Long,
    val title: String,
    val labelIds: List<Uuid>,
)

/** The display ID from cardigan-shared-specs `domain-card`: `{boardKey}-{number}`, without leading zeros. */
fun Card.displayId(boardKey: String): String = "$boardKey-$number"
