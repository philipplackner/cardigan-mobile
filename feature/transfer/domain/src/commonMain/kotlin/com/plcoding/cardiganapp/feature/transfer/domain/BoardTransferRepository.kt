package com.plcoding.cardiganapp.feature.transfer.domain

import com.plcoding.cardiganapp.core.domain.board.Board
import com.plcoding.cardiganapp.core.domain.util.Result
import kotlin.uuid.Uuid

interface BoardTransferRepository {
    suspend fun exportBoard(boardId: Uuid): Result<ExportedFile, ExportError>

    /** Uploads [file]. [key] replaces the file's board key after a clash; null keeps the file's key. */
    suspend fun importBoard(file: ByteArray, key: String?): Result<Board, ImportError>
}

class ExportedFile(
    val fileName: String,
    val bytes: ByteArray,
)
