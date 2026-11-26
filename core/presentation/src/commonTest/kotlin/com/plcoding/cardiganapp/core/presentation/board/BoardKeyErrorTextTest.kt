package com.plcoding.cardiganapp.core.presentation.board

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import cardiganapp.core.presentation.generated.resources.Res
import cardiganapp.core.presentation.generated.resources.board_key_error_already_used
import cardiganapp.core.presentation.generated.resources.board_key_error_invalid_format
import cardiganapp.core.presentation.generated.resources.board_key_error_required
import cardiganapp.core.presentation.generated.resources.board_key_error_too_long
import cardiganapp.core.presentation.generated.resources.board_key_error_too_short
import com.plcoding.cardiganapp.core.domain.board.BoardKeyError
import com.plcoding.cardiganapp.core.presentation.util.UiText
import kotlin.test.Test

class BoardKeyErrorTextTest {

    @Test
    fun everyKeyErrorMapsToItsOwnString() {
        val expected = mapOf(
            BoardKeyError.Required to Res.string.board_key_error_required,
            BoardKeyError.TooShort to Res.string.board_key_error_too_short,
            BoardKeyError.TooLong to Res.string.board_key_error_too_long,
            BoardKeyError.InvalidFormat to Res.string.board_key_error_invalid_format,
        )

        expected.forEach { (error, id) ->
            val text = error.toUiText() as UiText.Resource
            assertThat(text.id, error.toString()).isEqualTo(id)
            assertThat(text.args.toList(), error.toString()).containsExactly()
        }
    }

    @Test
    fun alreadyUsedCarriesTheKey() {
        val text = boardKeyAlreadyUsedText("CRD") as UiText.Resource

        assertThat(text.id).isEqualTo(Res.string.board_key_error_already_used)
        assertThat(text.args.toList()).containsExactly("CRD")
    }
}
