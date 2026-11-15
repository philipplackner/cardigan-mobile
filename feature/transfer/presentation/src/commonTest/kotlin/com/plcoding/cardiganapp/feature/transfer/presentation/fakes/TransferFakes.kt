package com.plcoding.cardiganapp.feature.transfer.presentation.fakes

import com.plcoding.cardiganapp.core.domain.board.Board
import com.plcoding.cardiganapp.core.domain.util.Result
import com.plcoding.cardiganapp.feature.transfer.domain.BoardKeyOwnerLookup
import com.plcoding.cardiganapp.feature.transfer.domain.BoardTransferRepository
import com.plcoding.cardiganapp.feature.transfer.domain.CheckedFile
import com.plcoding.cardiganapp.feature.transfer.domain.ExportError
import com.plcoding.cardiganapp.feature.transfer.domain.ExportFileChecker
import com.plcoding.cardiganapp.feature.transfer.domain.ExportedFile
import com.plcoding.cardiganapp.feature.transfer.domain.ImportError
import com.plcoding.cardiganapp.feature.transfer.presentation.platform.PickedFile
import kotlin.uuid.Uuid
import kotlinx.coroutines.CompletableDeferred

class FakeBoardTransferRepository : BoardTransferRepository {
    val importCalls = mutableListOf<Pair<ByteArray, String?>>()
    val exportCalls = mutableListOf<Uuid>()

    /** Results for the next import calls, in order. When empty, imports succeed with [importedBoard]. */
    val importResults = ArrayDeque<Result<Board, ImportError>>()
    var importedBoard = Board(id = Uuid.parse("00000000-0000-0000-0000-000000000001"), name = "Cardigan v1", key = "CRD")
    var exportResult: Result<ExportedFile, ExportError> = Result.Success(ExportedFile("CRD-2026-11-04.json", byteArrayOf(1, 2, 3)))

    /** When set, calls suspend until it completes. */
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun exportBoard(boardId: Uuid): Result<ExportedFile, ExportError> {
        exportCalls += boardId
        gate?.await()
        return exportResult
    }

    override suspend fun importBoard(file: ByteArray, key: String?): Result<Board, ImportError> {
        importCalls += file to key
        gate?.await()
        return importResults.removeFirstOrNull() ?: Result.Success(importedBoard)
    }
}

class FakeExportFileChecker(var result: CheckedFile? = CheckedFile(key = "CRD")) : ExportFileChecker {
    val checked = mutableListOf<ByteArray>()

    override fun check(bytes: ByteArray): CheckedFile? {
        checked += bytes
        return result
    }
}

class FakeBoardKeyOwnerLookup(var owners: Map<String, String> = emptyMap()) : BoardKeyOwnerLookup {
    val lookups = mutableListOf<String>()

    override fun boardNameForKey(key: String): String? {
        lookups += key
        return owners[key]
    }
}

fun validPick(name: String = "board-backup.json", bytes: ByteArray = "{}".encodeToByteArray()) =
    PickedFile(name = name, sizeBytes = bytes.size.toLong(), bytes = bytes)

fun oversizedPick(name: String = "board-backup.json", size: Long = 7_549_747) =
    PickedFile(name = name, sizeBytes = size, bytes = null)
