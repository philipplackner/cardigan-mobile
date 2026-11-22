package com.plcoding.cardiganapp.feature.boards.domain

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isGreaterThan
import assertk.assertions.isLessThan
import kotlin.test.Test
import kotlin.time.Instant
import kotlin.uuid.Uuid

/*
 * Break vectors: newer updated time first; equal updated time down to the millisecond falls back to
 * created time; equal times fall back to the id string (lowercase canonical, not numeric); insert
 * at the start, middle and end; insert into an empty list; replace moves a renamed board.
 */
class BoardOrderTest {

    private fun board(
        id: String,
        updated: String = "2026-11-20T10:00:00.000Z",
        created: String = "2026-11-01T10:00:00.000Z",
    ) = BoardSummary(
        id = Uuid.parse(id),
        name = id,
        key = "KEY",
        cardCount = 0,
        createdAt = Instant.parse(created),
        updatedAt = Instant.parse(updated),
    )

    private val idA = "0a000000-0000-0000-0000-000000000000"
    private val idB = "b0000000-0000-0000-0000-000000000000"
    private val idC = "c0000000-0000-0000-0000-000000000000"

    @Test
    fun newerUpdatedTimeComesFirst() {
        val newer = board(idB, updated = "2026-11-20T10:00:00.001Z")
        val older = board(idA, updated = "2026-11-20T10:00:00.000Z")
        assertThat(BoardOrder.compare(newer, older)).isLessThan(0)
    }

    @Test
    fun equalUpdatedTimeFallsBackToNewerCreatedTime() {
        val newer = board(idB, created = "2026-11-02T10:00:00.000Z")
        val older = board(idA, created = "2026-11-01T10:00:00.000Z")
        assertThat(BoardOrder.compare(newer, older)).isLessThan(0)
    }

    @Test
    fun equalTimesFallBackToIdStringAscending() {
        assertThat(BoardOrder.compare(board(idA), board(idB))).isLessThan(0)
        assertThat(BoardOrder.compare(board(idC), board(idB))).isGreaterThan(0)
    }

    @Test
    fun idsCompareAsLowercaseStrings() {
        val digit = board("90000000-0000-0000-0000-000000000000")
        val letter = board("a0000000-0000-0000-0000-000000000000")
        assertThat(BoardOrder.compare(digit, letter)).isLessThan(0)
    }

    @Test
    fun insertPlacesTheBoardByTheRule() {
        val first = board(idA, updated = "2026-11-20T12:00:00.000Z")
        val last = board(idC, updated = "2026-11-20T08:00:00.000Z")
        val middle = board(idB, updated = "2026-11-20T10:00:00.000Z")
        val newest = board("d0000000-0000-0000-0000-000000000000", updated = "2026-11-21T00:00:00.000Z")
        val oldest = board("e0000000-0000-0000-0000-000000000000", updated = "2026-01-01T00:00:00.000Z")

        assertThat(BoardOrder.insert(listOf(first, last), middle)).containsExactly(first, middle, last)
        assertThat(BoardOrder.insert(listOf(first, last), newest)).containsExactly(newest, first, last)
        assertThat(BoardOrder.insert(listOf(first, last), oldest)).containsExactly(first, last, oldest)
        assertThat(BoardOrder.insert(emptyList(), middle)).containsExactly(middle)
    }

    @Test
    fun replaceMovesTheBoard() {
        val a = board(idA, updated = "2026-11-20T12:00:00.000Z")
        val b = board(idB, updated = "2026-11-20T10:00:00.000Z")
        val renamedB = b.copy(name = "Renamed", updatedAt = Instant.parse("2026-11-21T00:00:00.000Z"))

        assertThat(BoardOrder.replace(listOf(a, b), renamedB)).containsExactly(renamedB, a)
    }
}
