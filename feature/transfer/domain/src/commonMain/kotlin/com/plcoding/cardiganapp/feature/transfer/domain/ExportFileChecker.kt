package com.plcoding.cardiganapp.feature.transfer.domain

/** Checks on the device that a picked file is a Cardigan board export before it is uploaded. */
interface ExportFileChecker {
    /** Returns null when [bytes] isn't a Cardigan export of a supported version. */
    fun check(bytes: ByteArray): CheckedFile?
}

/** [key] is the file's `board.key`, normalized with [com.plcoding.cardiganapp.core.domain.board.BoardKeyRule.normalize], or null when missing. */
data class CheckedFile(val key: String?)

/** Finds the name of a loaded board that uses [key], or null when no loaded board has it. */
fun interface BoardKeyOwnerLookup {
    fun boardNameForKey(key: String): String?
}
