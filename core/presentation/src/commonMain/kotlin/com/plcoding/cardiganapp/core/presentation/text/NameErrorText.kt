package com.plcoding.cardiganapp.core.presentation.text

import cardiganapp.core.presentation.generated.resources.Res
import cardiganapp.core.presentation.generated.resources.name_error_invalid_character
import cardiganapp.core.presentation.generated.resources.name_error_required
import cardiganapp.core.presentation.generated.resources.name_error_too_long
import com.plcoding.cardiganapp.core.domain.text.NameError
import com.plcoding.cardiganapp.core.presentation.util.UiText

/** Name field errors shared by board, column and label names. */
fun NameError.toUiText(): UiText =
    when (this) {
        NameError.Required -> UiText.Resource(Res.string.name_error_required)
        NameError.InvalidCharacter -> UiText.Resource(Res.string.name_error_invalid_character)
        is NameError.TooLong -> UiText.PluralResource(Res.plurals.name_error_too_long, max, arrayOf(max))
    }
