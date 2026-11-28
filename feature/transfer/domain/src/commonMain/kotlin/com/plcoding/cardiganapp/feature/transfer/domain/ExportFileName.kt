package com.plcoding.cardiganapp.feature.transfer.domain

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** Export file names from cardigan-shared-specs `board-export-format` "Export download". */
object ExportFileName {
    const val LAST_RESORT = "board-export.json"

    private const val EXTENSION = ".json"
    private val allowedName = Regex("[A-Za-z0-9._-]{1,100}")

    /** `KEY-yyyy-MM-dd.json` from the UTC date of [exportedAt], or [LAST_RESORT]. */
    fun fallback(
        key: String?,
        exportedAt: String?,
    ): String {
        if (key.isNullOrEmpty() || exportedAt == null) return LAST_RESORT
        val instant = runCatching { Instant.parse(exportedAt) }.getOrNull() ?: return LAST_RESORT
        val name = "$key-${instant.toLocalDateTime(TimeZone.UTC).date}$EXTENSION"
        return sanitize(name) ?: LAST_RESORT
    }

    /**
     * Returns [name] when it is safe to use as a file name, or null so the caller uses [fallback].
     * A name with a path separator is rejected, since the spec treats it as missing. Allowed names
     * match `[A-Za-z0-9._-]{1,100}` and end in `.json`.
     */
    fun sanitize(name: String?): String? =
        name?.takeIf {
            allowedName.matches(it) && it.endsWith(EXTENSION) && it.length > EXTENSION.length
        }
}
