package com.plcoding.cardiganapp.feature.auth.presentation.util

import cardiganapp.feature.auth.presentation.generated.resources.Res
import cardiganapp.feature.auth.presentation.generated.resources.auth_time_minutes
import cardiganapp.feature.auth.presentation.generated.resources.auth_time_seconds
import com.plcoding.cardiganapp.core.presentation.util.UiText

/** The wait in the rate-limit banner (D19): seconds under a minute, then whole minutes rounded up. */
sealed interface RetryTime {
    data class Seconds(val value: Int) : RetryTime
    data class Minutes(val value: Int) : RetryTime

    companion object {
        fun of(seconds: Int): RetryTime =
            if (seconds < 60) Seconds(seconds) else Minutes((seconds + 59) / 60)
    }
}

fun RetryTime.toUiText(): UiText = when (this) {
    is RetryTime.Seconds -> UiText.PluralResource(Res.plurals.auth_time_seconds, value, arrayOf(value))
    is RetryTime.Minutes -> UiText.PluralResource(Res.plurals.auth_time_minutes, value, arrayOf(value))
}
