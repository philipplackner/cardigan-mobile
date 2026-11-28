package com.plcoding.cardiganapp.feature.boards.presentation.util

import cardiganapp.feature.boards.presentation.generated.resources.Res
import cardiganapp.feature.boards.presentation.generated.resources.date_month_day_10
import cardiganapp.feature.boards.presentation.generated.resources.date_month_day_year_10
import cardiganapp.feature.boards.presentation.generated.resources.date_month_day_year_12
import cardiganapp.feature.boards.presentation.generated.resources.relative_time_hours
import cardiganapp.feature.boards.presentation.generated.resources.relative_time_just_now
import cardiganapp.feature.boards.presentation.generated.resources.relative_time_minutes
import cardiganapp.feature.boards.presentation.generated.resources.relative_time_yesterday
import com.plcoding.cardiganapp.feature.boards.presentation.fakes.FakeClock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.time.Instant

/*
 * Break vectors (pinned to Europe/Berlin, which is not UTC and has DST):
 * - under a minute, exactly one minute, 59 minutes, an updated time in the future
 * - hours rounded down, same-day check in the local zone, not UTC
 * - just after midnight (minutes win over the date), previous day, more than 1 h after midnight
 * - same year, earlier year, New Year's Eve and New Year's Day
 * - a DST change day: hours are measured between instants, not wall clock
 * - a fixed UTC+2 zone where UTC dates differ from local dates
 */
class RelativeTimeFormatterTest {
    private val berlin = TimeZone.of("Europe/Berlin")
    private val clock = FakeClock()

    private fun local(
        text: String,
        zone: TimeZone = berlin,
    ): Instant = LocalDateTime.parse(text).toInstant(zone)

    private fun format(
        now: String,
        updated: String,
        zone: TimeZone = berlin,
    ) = RelativeTimeFormatter(clock.apply { this.now = local(now, zone) }, zone).format(local(updated, zone))

    @Test
    fun secondsAgoIsJustNow() {
        assertResource(format("2026-11-21T10:00:40", "2026-11-21T10:00:00"), Res.string.relative_time_just_now)
    }

    @Test
    fun futureTimeIsJustNow() {
        assertResource(format("2026-11-21T10:00", "2026-11-21T10:05"), Res.string.relative_time_just_now)
    }

    @Test
    fun oneMinuteIsSingular() {
        assertPlural(format("2026-11-21T10:01", "2026-11-21T10:00"), Res.plurals.relative_time_minutes, 1)
    }

    @Test
    fun fiftyNineMinutes() {
        assertPlural(format("2026-11-21T10:59:59", "2026-11-21T10:00"), Res.plurals.relative_time_minutes, 59)
    }

    @Test
    fun exactlyOneHourIsHours() {
        assertPlural(format("2026-11-21T11:00", "2026-11-21T10:00"), Res.plurals.relative_time_hours, 1)
    }

    @Test
    fun earlierTodayRoundsDown() {
        assertPlural(format("2026-11-21T18:00", "2026-11-21T09:30"), Res.plurals.relative_time_hours, 8)
        assertPlural(format("2026-11-21T18:00", "2026-11-21T09:31"), Res.plurals.relative_time_hours, 8)
    }

    @Test
    fun justAfterMidnightShowsMinutes() {
        assertPlural(format("2026-11-21T00:20", "2026-11-20T23:50"), Res.plurals.relative_time_minutes, 30)
    }

    @Test
    fun previousDayIsYesterday() {
        assertResource(format("2026-11-21T08:00", "2026-11-20T22:00"), Res.string.relative_time_yesterday)
        assertResource(format("2026-11-21T01:00", "2026-11-20T23:30"), Res.string.relative_time_yesterday)
        assertResource(format("2026-11-21T23:59", "2026-11-20T00:00"), Res.string.relative_time_yesterday)
    }

    @Test
    fun earlierThisYearIsMonthAndDay() {
        assertResource(format("2026-11-21T10:00", "2026-10-09T12:00"), Res.string.date_month_day_10, 9)
    }

    @Test
    fun twoDaysAgoIsMonthAndDay() {
        assertResource(format("2026-10-11T00:10", "2026-10-09T23:00"), Res.string.date_month_day_10, 9)
    }

    @Test
    fun earlierYearIncludesTheYear() {
        assertResource(format("2026-11-21T10:00", "2025-10-09T12:00"), Res.string.date_month_day_year_10, 9, 2025)
    }

    @Test
    fun newYear() {
        assertPlural(format("2027-01-01T00:30", "2026-12-31T23:50"), Res.plurals.relative_time_minutes, 40)
        assertResource(format("2027-01-01T10:00", "2026-12-31T20:00"), Res.string.relative_time_yesterday)
        assertResource(format("2027-01-01T10:00", "2026-12-30T20:00"), Res.string.date_month_day_year_12, 30, 2026)
    }

    @Test
    fun dstChangeDayCountsRealHours() {
        // Clocks jump from 02:00 to 03:00; 00:30 to 12:00 on the wall is 10.5 real hours.
        assertPlural(format("2026-03-29T12:00", "2026-03-29T00:30"), Res.plurals.relative_time_hours, 10)
    }

    @Test
    fun sameLocalDayInAFixedOffsetZone() {
        val plusTwo = UtcOffset(hours = 2).asTimeZone()
        clock.now = local("2026-11-21T10:00", plusTwo)
        val text = RelativeTimeFormatter(clock, plusTwo).format(Instant.parse("2026-11-20T23:30:00.000Z"))
        assertPlural(text, Res.plurals.relative_time_hours, 8)
    }
}
