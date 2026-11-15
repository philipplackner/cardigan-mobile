package com.plcoding.cardiganapp.core.presentation.board

import cardiganapp.core.presentation.generated.resources.Res
import cardiganapp.core.presentation.generated.resources.board_key_error_already_used
import cardiganapp.core.presentation.generated.resources.board_key_error_invalid_format
import cardiganapp.core.presentation.generated.resources.board_key_error_too_long
import cardiganapp.core.presentation.generated.resources.board_key_error_too_short
import com.plcoding.cardiganapp.core.presentation.util.UiText

/**
 * Board key field errors shared by every key field (New board, import key clash), so the same
 * rule always shows the same text.
 */
object BoardKeyErrorText {
    fun tooShort(): UiText = UiText.Resource(Res.string.board_key_error_too_short)
    fun tooLong(): UiText = UiText.Resource(Res.string.board_key_error_too_long)
    fun invalidFormat(): UiText = UiText.Resource(Res.string.board_key_error_invalid_format)
    fun alreadyUsed(key: String): UiText = UiText.Resource(Res.string.board_key_error_already_used, arrayOf(key))
}
