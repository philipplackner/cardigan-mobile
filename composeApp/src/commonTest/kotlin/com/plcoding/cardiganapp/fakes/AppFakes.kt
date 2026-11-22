package com.plcoding.cardiganapp.fakes

import com.plcoding.cardiganapp.core.domain.board.Board
import com.plcoding.cardiganapp.core.domain.util.DataError
import com.plcoding.cardiganapp.core.domain.util.EmptyResult
import com.plcoding.cardiganapp.core.domain.util.Result
import com.plcoding.cardiganapp.feature.boards.domain.BoardSaveError
import com.plcoding.cardiganapp.feature.boards.domain.BoardSummary
import com.plcoding.cardiganapp.feature.boards.domain.RemoteBoardDataSource
import com.plcoding.cardiganapp.feature.transfer.domain.BoardTransferRepository
import com.plcoding.cardiganapp.feature.transfer.domain.CheckedFile
import com.plcoding.cardiganapp.feature.transfer.domain.ExportError
import com.plcoding.cardiganapp.feature.transfer.domain.ExportFileChecker
import com.plcoding.cardiganapp.feature.transfer.domain.ExportedFile
import com.plcoding.cardiganapp.feature.transfer.domain.ImportError
import com.plcoding.cardiganapp.feature.transfer.presentation.platform.FilePicker
import com.plcoding.cardiganapp.feature.transfer.presentation.platform.PickedFile
import kotlin.time.Instant
import kotlin.uuid.Uuid

/** The board endpoints. [responses] are returned by the list calls in order; the last one repeats. */
class FakeRemoteBoardDataSource(vararg responses: List<BoardSummary>) : RemoteBoardDataSource {
    private val responses = responses.toMutableList()
    var getBoardsCalls = 0
        private set

    override suspend fun getBoards(): Result<List<BoardSummary>, DataError.Network> {
        getBoardsCalls++
        val next = if (responses.size > 1) responses.removeAt(0) else responses.first()
        return Result.Success(next)
    }

    override suspend fun createBoard(name: String, key: String): Result<BoardSummary, BoardSaveError> =
        Result.Error(BoardSaveError.Network(DataError.Network.UNKNOWN))

    override suspend fun renameBoard(id: Uuid, name: String): Result<BoardSummary, BoardSaveError> =
        Result.Error(BoardSaveError.Network(DataError.Network.UNKNOWN))

    override suspend fun deleteBoard(id: Uuid): EmptyResult<BoardSaveError> =
        Result.Error(BoardSaveError.Network(DataError.Network.UNKNOWN))
}

class FakeBoardTransferRepository(var importResult: Result<Board, ImportError>) : BoardTransferRepository {
    override suspend fun exportBoard(boardId: Uuid): Result<ExportedFile, ExportError> = Result.Error(ExportError.Failed)

    override suspend fun importBoard(file: ByteArray, key: String?): Result<Board, ImportError> = importResult
}

class FakeExportFileChecker(private val key: String?) : ExportFileChecker {
    override fun check(bytes: ByteArray): CheckedFile = CheckedFile(key)
}

class FakeFilePicker : FilePicker {
    var openCount = 0
        private set
    private var onPicked: (PickedFile) -> Unit = {}

    fun bind(onPicked: (PickedFile) -> Unit): FakeFilePicker {
        this.onPicked = onPicked
        return this
    }

    override fun pickJson() {
        openCount++
    }

    fun pick(name: String = "board-backup.json") {
        val bytes = "{}".encodeToByteArray()
        onPicked(PickedFile(name, bytes.size.toLong(), bytes))
    }
}

fun summary(id: String, name: String, key: String, updated: String = "2026-11-20T10:00:00Z") = BoardSummary(
    id = Uuid.parse(id),
    name = name,
    key = key,
    cardCount = 3,
    createdAt = Instant.parse("2026-11-01T10:00:00Z"),
    updatedAt = Instant.parse(updated),
)
