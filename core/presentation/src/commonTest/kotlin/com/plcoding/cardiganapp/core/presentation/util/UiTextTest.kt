package com.plcoding.cardiganapp.core.presentation.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class UiTextTest {

    @Test
    fun `empty dynamic string stays empty`() = runTest {
        assertThat(UiText.DynamicString("").asStringAsync()).isEqualTo("")
    }

    @Test
    fun `emoji and combining characters pass through unchanged`() = runTest {
        val value = "Fix 🐛 in café 👩‍💻 board"

        assertThat(UiText.DynamicString(value).asStringAsync()).isEqualTo(value)
    }

    @Test
    fun `format placeholders are not interpreted`() = runTest {
        val value = "%s moved %1\$d cards to %%"

        assertThat(UiText.DynamicString(value).asStringAsync()).isEqualTo(value)
    }
}
