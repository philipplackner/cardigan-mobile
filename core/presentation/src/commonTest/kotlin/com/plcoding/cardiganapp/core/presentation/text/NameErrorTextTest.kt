package com.plcoding.cardiganapp.core.presentation.text

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import cardiganapp.core.presentation.generated.resources.Res
import cardiganapp.core.presentation.generated.resources.name_error_invalid_character
import cardiganapp.core.presentation.generated.resources.name_error_required
import cardiganapp.core.presentation.generated.resources.name_error_too_long
import com.plcoding.cardiganapp.core.domain.text.NameError
import com.plcoding.cardiganapp.core.presentation.util.UiText
import kotlin.test.Test

class NameErrorTextTest {
    @Test
    fun requiredMapsToItsString() {
        val text = NameError.Required.toUiText()
        assertThat(text).isInstanceOf<UiText.Resource>()
        assertThat((text as UiText.Resource).id).isEqualTo(Res.string.name_error_required)
    }

    @Test
    fun invalidCharacterMapsToItsString() {
        val text = NameError.InvalidCharacter.toUiText() as UiText.Resource
        assertThat(text.id).isEqualTo(Res.string.name_error_invalid_character)
    }

    @Test
    fun tooLongCarriesTheMaxAsQuantityAndArgument() {
        val text = NameError.TooLong(100).toUiText() as UiText.PluralResource
        assertThat(text.id).isEqualTo(Res.plurals.name_error_too_long)
        assertThat(text.quantity).isEqualTo(100)
        assertThat(text.args.toList()).containsExactly(100)
    }
}
