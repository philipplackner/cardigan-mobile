package com.plcoding.cardiganapp.core.presentation.util

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

sealed interface UiText {
    data class DynamicString(
        val value: String,
    ) : UiText

    class Resource(
        val id: StringResource,
        val args: Array<Any> = emptyArray(),
    ) : UiText

    /** A plural string picked by [quantity]. [args] fill its placeholders, usually starting with the quantity. */
    class PluralResource(
        val id: PluralStringResource,
        val quantity: Int,
        val args: Array<Any> = emptyArray(),
    ) : UiText

    @Composable
    fun asString(): String =
        when (this) {
            is DynamicString -> value
            is Resource -> stringResource(id, *args)
            is PluralResource -> pluralStringResource(id, quantity, *args)
        }

    suspend fun asStringAsync(): String =
        when (this) {
            is DynamicString -> value
            is Resource -> getString(id, *args)
            is PluralResource -> getPluralString(id, quantity, *args)
        }
}
