package com.plcoding.cardiganapp.feature.transfer.data

import com.plcoding.cardiganapp.core.domain.board.BoardKeyRule
import com.plcoding.cardiganapp.feature.transfer.domain.CheckedFile
import com.plcoding.cardiganapp.feature.transfer.domain.ExportFileChecker
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Checks `format` and `formatVersion` from cardigan-shared-specs `board-export-format`
 * "Export file identifies its format and version". The server stays the full validator.
 */
class JsonExportFileChecker : ExportFileChecker {

    override fun check(bytes: ByteArray): CheckedFile? {
        val text = decodeUtf8(bytes) ?: return null
        val root = runCatching { Json.parseToJsonElement(text) }.getOrNull() as? JsonObject ?: return null
        if (root[FORMAT].stringOrNull() != FORMAT_VALUE) return null
        if (!root[FORMAT_VERSION].isIntegerLiteral(FORMAT_VERSION_VALUE)) return null
        val key = ((root[BOARD] as? JsonObject)?.get(KEY)).stringOrNull()
        return CheckedFile(key = key?.let(BoardKeyRule::normalize))
    }

    private fun decodeUtf8(bytes: ByteArray): String? {
        val start = if (bytes.size >= 3 && bytes[0] == BOM_0 && bytes[1] == BOM_1 && bytes[2] == BOM_2) 3 else 0
        return runCatching { bytes.decodeToString(start, bytes.size, throwOnInvalidSequence = true) }.getOrNull()
    }

    private fun JsonElement?.stringOrNull(): String? =
        (this as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun JsonElement?.isIntegerLiteral(expected: String): Boolean =
        this is JsonPrimitive && !isString && content == expected

    private companion object {
        const val FORMAT = "format"
        const val FORMAT_VALUE = "cardigan.board"
        const val FORMAT_VERSION = "formatVersion"
        const val FORMAT_VERSION_VALUE = "1"
        const val BOARD = "board"
        const val KEY = "key"
        const val BOM_0 = 0xEF.toByte()
        const val BOM_1 = 0xBB.toByte()
        const val BOM_2 = 0xBF.toByte()
    }
}
