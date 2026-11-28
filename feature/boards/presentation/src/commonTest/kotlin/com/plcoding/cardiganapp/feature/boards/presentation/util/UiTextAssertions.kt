package com.plcoding.cardiganapp.feature.boards.presentation.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.plcoding.cardiganapp.core.presentation.util.UiText
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource

fun assertResource(
    text: UiText,
    id: StringResource,
    vararg args: Any,
) {
    assertThat(text).isInstanceOf<UiText.Resource>()
    text as UiText.Resource
    assertThat(text.id).isEqualTo(id)
    assertThat(text.args.toList()).isEqualTo(args.toList())
}

fun assertPlural(
    text: UiText,
    id: PluralStringResource,
    quantity: Int,
) {
    assertThat(text).isInstanceOf<UiText.PluralResource>()
    text as UiText.PluralResource
    assertThat(text.id).isEqualTo(id)
    assertThat(text.quantity).isEqualTo(quantity)
    assertThat(text.args.toList()).isEqualTo(listOf(quantity))
}
