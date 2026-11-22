package com.plcoding.cardiganapp.feature.boards.domain

import com.plcoding.cardiganapp.core.domain.board.Board
import kotlin.time.Instant
import kotlin.uuid.Uuid

/** One board as the board list shows it. */
data class BoardSummary(
    val id: Uuid,
    val name: String,
    val key: String,
    val cardCount: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    fun toBoard(): Board = Board(id = id, name = name, key = key)
}
