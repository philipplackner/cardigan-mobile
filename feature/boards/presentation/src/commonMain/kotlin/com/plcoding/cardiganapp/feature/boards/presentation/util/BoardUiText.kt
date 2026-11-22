package com.plcoding.cardiganapp.feature.boards.presentation.util

import cardiganapp.feature.boards.presentation.generated.resources.Res
import cardiganapp.feature.boards.presentation.generated.resources.board_count
import cardiganapp.feature.boards.presentation.generated.resources.card_count
import com.plcoding.cardiganapp.core.presentation.util.UiText

/** "1 board" / "{n} boards", also for 0. */
fun boardCountText(count: Int): UiText = UiText.PluralResource(Res.plurals.board_count, count, arrayOf(count))

/** "1 card" / "{n} cards", also for 0. */
fun cardCountText(count: Int): UiText = UiText.PluralResource(Res.plurals.card_count, count, arrayOf(count))
