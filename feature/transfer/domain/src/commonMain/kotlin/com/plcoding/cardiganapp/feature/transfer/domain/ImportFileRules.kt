package com.plcoding.cardiganapp.feature.transfer.domain

/** Device-side limits from cardigan-shared-specs `board-export-format` "Import size limits". */
object ImportFileRules {
    const val MAX_FILE_BYTES: Long = 5_242_880L

    fun isTooLarge(sizeBytes: Long): Boolean = sizeBytes > MAX_FILE_BYTES
}
