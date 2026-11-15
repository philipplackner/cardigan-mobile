package com.plcoding.cardiganapp.core.presentation.board

import cardiganapp.core.presentation.generated.resources.Res
import cardiganapp.core.presentation.generated.resources.board_key_error_already_used
import cardiganapp.core.presentation.generated.resources.board_key_error_invalid_format
import cardiganapp.core.presentation.generated.resources.board_key_error_required
import cardiganapp.core.presentation.generated.resources.board_key_error_too_long
import cardiganapp.core.presentation.generated.resources.board_key_error_too_short
import com.plcoding.cardiganapp.core.domain.board.BoardKeyError
import com.plcoding.cardiganapp.core.presentation.util.UiText

/**
 * Board key field errors shared by every key field (New board, import key clash), so the same
 * rule always shows the same text.
 */
fun BoardKeyError.toUiText(): UiText = UiText.Resource(
    when (this) {
        BoardKeyError.Required -> Res.string.board_key_error_required
        BoardKeyError.TooShort -> Res.string.board_key_error_too_short
        BoardKeyError.TooLong -> Res.string.board_key_error_too_long
        BoardKeyError.InvalidFormat -> Res.string.board_key_error_invalid_format
    },
)

/** "You already have a board with the key {key}." under a key field. */
fun boardKeyAlreadyUsedText(key: String): UiText =
    UiText.Resource(Res.string.board_key_error_already_used, arrayOf(key))
