package com.plcoding.cardiganapp.feature.transfer.domain

enum class FileSizeUnit {
    KB,
    MB,
}

data class FileSize(
    val value: String,
    val unit: FileSizeUnit,
)

/**
 * Formats byte sizes in base 1024: whole KB below 1024 KB, MB with one decimal from there. Rounds
 * half up, except sizes over [ImportFileRules.MAX_FILE_BYTES], which round up so they never show
 * as "5.0 MB".
 */
object FileSizeFormatter {
    private const val KIB = 1024L
    private const val MIB = KIB * KIB

    fun format(bytes: Long): FileSize {
        val size = bytes.coerceAtLeast(0L)
        val roundUp = ImportFileRules.isTooLarge(size)
        val kb = rounded(size / KIB, size % KIB, KIB, roundUp)
        if (kb < KIB) return FileSize(kb.toString(), FileSizeUnit.KB)
        // Tenths of a MiB aren't whole bytes, so split off whole MiB first to stay exact without overflow.
        val restTimesTen = (size % MIB) * 10
        val tenths = rounded((size / MIB) * 10 + restTimesTen / MIB, restTimesTen % MIB, MIB, roundUp)
        return FileSize("${tenths / 10}.${tenths % 10}", FileSizeUnit.MB)
    }

    private fun rounded(
        whole: Long,
        remainder: Long,
        divisor: Long,
        roundUp: Boolean,
    ): Long =
        when {
            remainder == 0L -> whole
            roundUp || remainder * 2 >= divisor -> whole + 1
            else -> whole
        }
}
