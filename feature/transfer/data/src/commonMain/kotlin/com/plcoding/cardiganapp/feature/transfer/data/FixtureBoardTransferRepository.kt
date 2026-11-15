package com.plcoding.cardiganapp.feature.transfer.data

import com.plcoding.cardiganapp.core.domain.board.Board
import com.plcoding.cardiganapp.core.domain.util.Result
import com.plcoding.cardiganapp.feature.transfer.domain.BoardKeyRule
import com.plcoding.cardiganapp.feature.transfer.domain.BoardTransferRepository
import com.plcoding.cardiganapp.feature.transfer.domain.ExportError
import com.plcoding.cardiganapp.feature.transfer.domain.ExportedFile
import com.plcoding.cardiganapp.feature.transfer.domain.ImportError
import kotlin.uuid.Uuid
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * TEMPORARY stand-in for the server, bound only in debug builds for the manual check of
 * add-transfer-ui. Removed before the branch is pushed (task group 10).
 */
class FixtureBoardTransferRepository : BoardTransferRepository {

    override suspend fun exportBoard(boardId: Uuid): Result<ExportedFile, ExportError> {
        delay(RESPONSE_DELAY_MS)
        if (boardId == MISSING_BOARD_ID) return Result.Error(ExportError.BoardNotFound)
        return Result.Success(ExportedFile(EXPORT_FILE_NAME, fixtureExport().encodeToByteArray()))
    }

    override suspend fun importBoard(file: ByteArray, key: String?): Result<Board, ImportError> {
        delay(RESPONSE_DELAY_MS)
        val board = runCatching { Json.parseToJsonElement(file.decodeToString()) }.getOrNull()
            ?.let { (it as? JsonObject)?.get("board") as? JsonObject }
            ?: return Result.Error(ImportError.InvalidFile)
        val name = (board["name"] as? JsonPrimitive)?.takeIf { it.isString }?.content
            ?: return Result.Error(ImportError.InvalidFile)
        val fileKey = (board["key"] as? JsonPrimitive)?.takeIf { it.isString }?.content
            ?: return Result.Error(ImportError.InvalidFile)
        val finalKey = BoardKeyRule.normalize(key ?: fileKey)
        BoardKeyRule.validate(finalKey)?.let { return Result.Error(ImportError.KeyInvalid(it)) }
        if (finalKey in USED_KEYS) return Result.Error(ImportError.KeyAlreadyUsed)
        return Result.Success(Board(id = Uuid.random(), name = name, key = finalKey))
    }

    companion object {
        val MISSING_BOARD_ID: Uuid = Uuid.parse("00000000-0000-0000-0000-000000000404")
        val FIXTURE_BOARD_ID: Uuid = Uuid.parse("00000000-0000-0000-0000-000000000001")
        const val FIXTURE_BOARD_NAME = "Fixture board"
        const val EXPORT_FILE_NAME = "FIX-2026-11-04.json"
        val USED_KEYS = setOf("CRD", "TAKEN")
        private const val RESPONSE_DELAY_MS = 1_500L

        /** An export shaped like cardigan-shared-specs `board-export-format` "Exact fields". */
        fun fixtureExport(): String = buildJsonObject {
            put("format", "cardigan.board")
            put("formatVersion", 1)
            put("exportedAt", "2026-11-04T23:30:00Z")
            putJsonObject("board") {
                put("name", FIXTURE_BOARD_NAME)
                put("key", "FIX")
                putJsonArray("labels") {
                    addJsonObject {
                        put("name", "Bug")
                        put("color", "Red")
                    }
                }
                putJsonArray("columns") {
                    addJsonObject {
                        put("name", "To do")
                        putJsonArray("cards") {
                            addJsonObject {
                                put("number", 1)
                                put("title", "Try the import")
                                put("description", "")
                                putJsonArray("labels") { add("Bug") }
                                put("createdAt", "2026-11-01T10:00:00Z")
                                put("updatedAt", "2026-11-02T10:00:00Z")
                            }
                        }
                    }
                }
            }
        }.toString()
    }
}
