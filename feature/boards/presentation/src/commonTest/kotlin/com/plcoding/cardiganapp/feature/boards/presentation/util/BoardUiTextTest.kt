package com.plcoding.cardiganapp.feature.boards.presentation.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import cardiganapp.feature.boards.presentation.generated.resources.Res
import cardiganapp.feature.boards.presentation.generated.resources.board_count
import cardiganapp.feature.boards.presentation.generated.resources.card_count
import cardiganapp.feature.boards.presentation.generated.resources.relative_time_hours
import com.plcoding.cardiganapp.feature.boards.presentation.boardlist.toBoardItemUi
import com.plcoding.cardiganapp.feature.boards.presentation.fakes.FakeClock
import com.plcoding.cardiganapp.feature.boards.presentation.fakes.board
import kotlinx.datetime.TimeZone
import kotlin.test.Test

class BoardUiTextTest {
    @Test
    fun boardCountForZeroOneAndMany() {
        assertPlural(boardCountText(0), Res.plurals.board_count, 0)
        assertPlural(boardCountText(1), Res.plurals.board_count, 1)
        assertPlural(boardCountText(14), Res.plurals.board_count, 14)
    }

    @Test
    fun cardCountForZeroOneAndMany() {
        assertPlural(cardCountText(0), Res.plurals.card_count, 0)
        assertPlural(cardCountText(1), Res.plurals.card_count, 1)
        assertPlural(cardCountText(12), Res.plurals.card_count, 12)
    }

    @Test
    fun itemMapping() {
        val summary =
            board(
                id = "0b5c8a6e-1f00-4a3c-9a43-6f5e0e3c1d2a",
                name = "Cardigan v1",
                key = "CRD",
                cardCount = 12,
                updated = "2026-11-21T08:00:00Z",
            )
        val formatter = RelativeTimeFormatter(FakeClock(), TimeZone.UTC)

        val item = summary.toBoardItemUi(formatter)

        assertThat(item.id).isEqualTo("0b5c8a6e-1f00-4a3c-9a43-6f5e0e3c1d2a")
        assertThat(item.name).isEqualTo("Cardigan v1")
        assertThat(item.key).isEqualTo("CRD")
        assertPlural(item.cardCount, Res.plurals.card_count, 12)
        assertPlural(item.updated, Res.plurals.relative_time_hours, 2)
    }
}
