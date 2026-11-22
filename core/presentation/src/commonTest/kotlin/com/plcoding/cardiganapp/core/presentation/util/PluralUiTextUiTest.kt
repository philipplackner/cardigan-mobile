package com.plcoding.cardiganapp.core.presentation.util

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import assertk.assertThat
import assertk.assertions.isEqualTo
import cardiganapp.core.presentation.generated.resources.Res
import cardiganapp.core.presentation.generated.resources.name_error_too_long
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalTestApi::class)
class PluralUiTextUiTest {

    private fun plural(quantity: Int) = UiText.PluralResource(Res.plurals.name_error_too_long, quantity, arrayOf(quantity))

    @Test
    fun rendersZeroOneAndTwo() = runComposeUiTest {
        setContent {
            Column {
                Text(plural(0).asString())
                Text(plural(1).asString())
                Text(plural(2).asString())
            }
        }
        onNodeWithText("Use at most 0 characters.").assertIsDisplayed()
        onNodeWithText("Use at most 1 character.").assertIsDisplayed()
        onNodeWithText("Use at most 2 characters.").assertIsDisplayed()
    }

    @Test
    fun asyncMatchesComposable() = runTest {
        assertThat(plural(1).asStringAsync()).isEqualTo("Use at most 1 character.")
        assertThat(plural(100).asStringAsync()).isEqualTo("Use at most 100 characters.")
    }
}
