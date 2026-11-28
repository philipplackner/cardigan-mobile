package com.plcoding.cardiganapp.feature.boards.presentation.util

import cardiganapp.feature.boards.presentation.generated.resources.Res
import cardiganapp.feature.boards.presentation.generated.resources.date_month_day_1
import cardiganapp.feature.boards.presentation.generated.resources.date_month_day_10
import cardiganapp.feature.boards.presentation.generated.resources.date_month_day_11
import cardiganapp.feature.boards.presentation.generated.resources.date_month_day_12
import cardiganapp.feature.boards.presentation.generated.resources.date_month_day_2
import cardiganapp.feature.boards.presentation.generated.resources.date_month_day_3
import cardiganapp.feature.boards.presentation.generated.resources.date_month_day_4
import cardiganapp.feature.boards.presentation.generated.resources.date_month_day_5
import cardiganapp.feature.boards.presentation.generated.resources.date_month_day_6
import cardiganapp.feature.boards.presentation.generated.resources.date_month_day_7
import cardiganapp.feature.boards.presentation.generated.resources.date_month_day_8
import cardiganapp.feature.boards.presentation.generated.resources.date_month_day_9
import cardiganapp.feature.boards.presentation.generated.resources.date_month_day_year_1
import cardiganapp.feature.boards.presentation.generated.resources.date_month_day_year_10
import cardiganapp.feature.boards.presentation.generated.resources.date_month_day_year_11
import cardiganapp.feature.boards.presentation.generated.resources.date_month_day_year_12
import cardiganapp.feature.boards.presentation.generated.resources.date_month_day_year_2
import cardiganapp.feature.boards.presentation.generated.resources.date_month_day_year_3
import cardiganapp.feature.boards.presentation.generated.resources.date_month_day_year_4
import cardiganapp.feature.boards.presentation.generated.resources.date_month_day_year_5
import cardiganapp.feature.boards.presentation.generated.resources.date_month_day_year_6
import cardiganapp.feature.boards.presentation.generated.resources.date_month_day_year_7
import cardiganapp.feature.boards.presentation.generated.resources.date_month_day_year_8
import cardiganapp.feature.boards.presentation.generated.resources.date_month_day_year_9
import cardiganapp.feature.boards.presentation.generated.resources.relative_time_hours
import cardiganapp.feature.boards.presentation.generated.resources.relative_time_just_now
import cardiganapp.feature.boards.presentation.generated.resources.relative_time_minutes
import cardiganapp.feature.boards.presentation.generated.resources.relative_time_yesterday
import com.plcoding.cardiganapp.core.presentation.util.UiText
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.StringResource
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * The relative updated time (D6) in [timeZone], relative to [clock] when called. Elapsed minutes and
 * hours are measured between the instants and rounded down, so a DST change doesn't shift them.
 */
class RelativeTimeFormatter(
    private val clock: Clock,
    private val timeZone: TimeZone,
) {
    fun format(time: Instant): UiText {
        val now = clock.now()
        val elapsed = now - time
        if (elapsed < 1.minutes) return UiText.Resource(Res.string.relative_time_just_now)
        if (elapsed < 1.hours) {
            val minutes = elapsed.inWholeMinutes.toInt()
            return UiText.PluralResource(Res.plurals.relative_time_minutes, minutes, arrayOf(minutes))
        }
        val today = now.toLocalDateTime(timeZone).date
        val date = time.toLocalDateTime(timeZone).date
        return when {
            date == today -> {
                val hours = elapsed.inWholeHours.toInt()
                UiText.PluralResource(Res.plurals.relative_time_hours, hours, arrayOf(hours))
            }

            date == today.minus(DatePeriod(days = 1)) -> {
                UiText.Resource(Res.string.relative_time_yesterday)
            }

            date.year == today.year -> {
                UiText.Resource(monthDay[date.month.number - 1], arrayOf(date.day))
            }

            else -> {
                UiText.Resource(monthDayYear[date.month.number - 1], arrayOf(date.day, date.year))
            }
        }
    }
}

private val monthDay: List<StringResource> =
    listOf(
        Res.string.date_month_day_1,
        Res.string.date_month_day_2,
        Res.string.date_month_day_3,
        Res.string.date_month_day_4,
        Res.string.date_month_day_5,
        Res.string.date_month_day_6,
        Res.string.date_month_day_7,
        Res.string.date_month_day_8,
        Res.string.date_month_day_9,
        Res.string.date_month_day_10,
        Res.string.date_month_day_11,
        Res.string.date_month_day_12,
    )
private val monthDayYear: List<StringResource> =
    listOf(
        Res.string.date_month_day_year_1,
        Res.string.date_month_day_year_2,
        Res.string.date_month_day_year_3,
        Res.string.date_month_day_year_4,
        Res.string.date_month_day_year_5,
        Res.string.date_month_day_year_6,
        Res.string.date_month_day_year_7,
        Res.string.date_month_day_year_8,
        Res.string.date_month_day_year_9,
        Res.string.date_month_day_year_10,
        Res.string.date_month_day_year_11,
        Res.string.date_month_day_year_12,
    )
