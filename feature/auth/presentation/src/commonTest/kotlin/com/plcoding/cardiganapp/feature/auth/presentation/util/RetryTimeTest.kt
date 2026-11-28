package com.plcoding.cardiganapp.feature.auth.presentation.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import cardiganapp.feature.auth.presentation.generated.resources.Res
import cardiganapp.feature.auth.presentation.generated.resources.auth_time_minutes
import cardiganapp.feature.auth.presentation.generated.resources.auth_time_seconds
import com.plcoding.cardiganapp.core.presentation.util.UiText
import kotlin.test.Test

class RetryTimeTest {
    @Test
    fun oneSecond() = assertTime(1, RetryTime.Seconds(1))

    @Test
    fun fiftyNineSeconds() = assertTime(59, RetryTime.Seconds(59))

    @Test
    fun sixtySecondsIsOneMinute() = assertTime(60, RetryTime.Minutes(1))

    @Test
    fun sixtyOneSecondsRoundsUp() = assertTime(61, RetryTime.Minutes(2))

    @Test
    fun oneHour() = assertTime(3600, RetryTime.Minutes(60))

    @Test
    fun pluralResources() {
        assertPlural(RetryTime.Seconds(12).toUiText(), "seconds", 12)
        assertPlural(RetryTime.Minutes(2).toUiText(), "minutes", 2)
    }

    private fun assertTime(
        seconds: Int,
        expected: RetryTime,
    ) {
        assertThat(RetryTime.of(seconds)).isEqualTo(expected)
    }

    private fun assertPlural(
        text: UiText,
        unit: String,
        quantity: Int,
    ) {
        assertThat(text).isInstanceOf<UiText.PluralResource>()
        text as UiText.PluralResource
        val expected = if (unit == "seconds") Res.plurals.auth_time_seconds else Res.plurals.auth_time_minutes
        assertThat(text.id).isEqualTo(expected)
        assertThat(text.quantity).isEqualTo(quantity)
        assertThat(text.args.toList()).isEqualTo(listOf<Any>(quantity))
    }
}
