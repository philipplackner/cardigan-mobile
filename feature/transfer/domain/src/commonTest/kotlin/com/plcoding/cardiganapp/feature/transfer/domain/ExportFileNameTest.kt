package com.plcoding.cardiganapp.feature.transfer.domain

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class ExportFileNameTest {

    @Test
    fun fallbackUsesTheUtcDate() {
        assertThat(ExportFileName.fallback("CRD", "2026-11-04T23:30:00Z")).isEqualTo("CRD-2026-11-04.json")
    }

    @Test
    fun fallbackIgnoresTheLocalDateAcrossMidnight() {
        val exportedAt = "2026-11-04T23:30:00Z"
        val berlinDate = Instant.parse(exportedAt).toLocalDateTime(TimeZone.of("Europe/Berlin")).date.toString()
        assertThat(berlinDate).isEqualTo("2026-11-05")
        assertThat(ExportFileName.fallback("CRD", exportedAt)).isEqualTo("CRD-2026-11-04.json")
    }

    @Test
    fun fallbackHandlesAnOffset() {
        assertThat(ExportFileName.fallback("CRD", "2026-11-05T00:30:00+01:00")).isEqualTo("CRD-2026-11-04.json")
    }

    @Test
    fun missingKeyGivesLastResort() {
        assertThat(ExportFileName.fallback(null, "2026-11-04T23:30:00Z")).isEqualTo("board-export.json")
        assertThat(ExportFileName.fallback("", "2026-11-04T23:30:00Z")).isEqualTo("board-export.json")
    }

    @Test
    fun unparseableExportedAtGivesLastResort() {
        assertThat(ExportFileName.fallback("CRD", "yesterday")).isEqualTo("board-export.json")
        assertThat(ExportFileName.fallback("CRD", null)).isEqualTo("board-export.json")
    }

    @Test
    fun keyWithUnsafeCharactersGivesLastResort() {
        assertThat(ExportFileName.fallback("../CRD", "2026-11-04T23:30:00Z")).isEqualTo("board-export.json")
    }

    @Test
    fun sanitizeKeepsAValidName() {
        assertThat(ExportFileName.sanitize("CRD-2026-11-04.json")).isEqualTo("CRD-2026-11-04.json")
    }

    @Test
    fun sanitizeRejectsPaths() {
        assertThat(ExportFileName.sanitize("../x.json")).isNull()
        assertThat(ExportFileName.sanitize("a/b.json")).isNull()
        assertThat(ExportFileName.sanitize("a\\b.json")).isNull()
    }

    @Test
    fun sanitizeRejectsOtherExtensions() {
        assertThat(ExportFileName.sanitize("x.txt")).isNull()
    }

    @Test
    fun sanitizeRejectsEmptyAndBareExtension() {
        assertThat(ExportFileName.sanitize("")).isNull()
        assertThat(ExportFileName.sanitize(".json")).isNull()
        assertThat(ExportFileName.sanitize(null)).isNull()
    }

    @Test
    fun sanitizeRejectsNamesOver100Characters() {
        val atLimit = "a".repeat(95) + ".json"
        assertThat(ExportFileName.sanitize(atLimit)).isEqualTo(atLimit)
        assertThat(ExportFileName.sanitize("a$atLimit")).isNull()
    }

    @Test
    fun sanitizeRejectsSpacesAndUnicode() {
        assertThat(ExportFileName.sanitize("my board.json")).isNull()
        assertThat(ExportFileName.sanitize("bõard.json")).isNull()
    }

    @Test
    fun suggestedPathFallsBackToKeyAndDate() {
        val name = ExportFileName.sanitize("../x.json") ?: ExportFileName.fallback("CRD", "2026-11-04T23:30:00Z")
        assertThat(name).isEqualTo("CRD-2026-11-04.json")
    }
}
