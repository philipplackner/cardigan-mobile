package com.plcoding.cardiganapp.feature.boards.presentation.components

import com.plcoding.cardiganapp.core.domain.board.BoardKeyError
import com.plcoding.cardiganapp.core.domain.text.NameError
import com.plcoding.cardiganapp.core.presentation.board.boardKeyAlreadyUsedText
import com.plcoding.cardiganapp.core.presentation.board.toUiText
import com.plcoding.cardiganapp.core.presentation.util.UiText
import com.plcoding.cardiganapp.feature.boards.domain.KeyFieldError
import com.plcoding.cardiganapp.feature.boards.domain.NameFieldError

/** Board name limit from cardigan-shared-specs `domain-board`. */
const val BOARD_NAME_MAX_LENGTH = 100

/** The error shown under a board key field. */
sealed interface BoardKeyFieldError {
    data class Rule(
        val error: BoardKeyError,
    ) : BoardKeyFieldError

    data class AlreadyUsed(
        val key: String,
    ) : BoardKeyFieldError
}

fun BoardKeyFieldError.toUiText(): UiText =
    when (this) {
        is BoardKeyFieldError.Rule -> error.toUiText()
        is BoardKeyFieldError.AlreadyUsed -> boardKeyAlreadyUsedText(key)
    }

/** A server name error as the shared name rule. Unknown codes show the invalid-character text. */
fun NameFieldError.toNameError(): NameError =
    when (this) {
        NameFieldError.Required -> NameError.Required
        is NameFieldError.TooLong -> NameError.TooLong(max)
        NameFieldError.Invalid -> NameError.InvalidCharacter
    }

/** A server key error for [key]. Codes without their own text show the format text. */
fun KeyFieldError.toBoardKeyFieldError(key: String): BoardKeyFieldError =
    when (this) {
        is KeyFieldError.Rule -> BoardKeyFieldError.Rule(error)
        KeyFieldError.AlreadyUsed -> BoardKeyFieldError.AlreadyUsed(key)
        KeyFieldError.Invalid -> BoardKeyFieldError.Rule(BoardKeyError.InvalidFormat)
    }
