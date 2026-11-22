package com.plcoding.cardiganapp.feature.boards.presentation.boardlist

import com.plcoding.cardiganapp.core.presentation.util.UiText
import com.plcoding.cardiganapp.feature.boards.domain.BoardSummary
import com.plcoding.cardiganapp.feature.boards.presentation.util.RelativeTimeFormatter
import com.plcoding.cardiganapp.feature.boards.presentation.util.cardCountText

/** One row or tile. [updated] is the relative time without the tiles' "Updated" prefix. */
data class BoardItemUi(
    val id: String,
    val name: String,
    val key: String,
    val cardCount: UiText,
    val updated: UiText,
)

fun BoardSummary.toBoardItemUi(formatter: RelativeTimeFormatter): BoardItemUi = BoardItemUi(
    id = id.toString(),
    name = name,
    key = key,
    cardCount = cardCountText(cardCount),
    updated = formatter.format(updatedAt),
)
