package com.plcoding.cardiganapp.feature.boards.presentation.fakes

import com.plcoding.cardiganapp.core.domain.board.Board
import com.plcoding.cardiganapp.core.domain.board.BoardContent
import com.plcoding.cardiganapp.core.domain.board.Card
import com.plcoding.cardiganapp.core.domain.board.Column
import com.plcoding.cardiganapp.core.domain.label.Label
import com.plcoding.cardiganapp.core.domain.label.LabelColor
import com.plcoding.cardiganapp.core.domain.label.LabelSource
import com.plcoding.cardiganapp.core.domain.util.DataError
import com.plcoding.cardiganapp.core.domain.util.EmptyResult
import com.plcoding.cardiganapp.core.domain.util.Result
import com.plcoding.cardiganapp.feature.boards.domain.detail.BoardDetailRepository
import com.plcoding.cardiganapp.feature.boards.domain.detail.BoardLoadError
import com.plcoding.cardiganapp.feature.boards.domain.detail.CardCreateError
import com.plcoding.cardiganapp.feature.boards.domain.detail.CardCreateRepository
import com.plcoding.cardiganapp.feature.boards.domain.detail.ColumnWriteError
import kotlin.uuid.Uuid
import kotlinx.coroutines.CompletableDeferred

/**
 * A board server in memory. Writes change [server] like the backend would, unless a scripted
 * result is queued. A queued [gates] entry holds the next call until it completes.
 */
class FakeBoardDetailRepository(var server: BoardContent) : BoardDetailRepository {
    val calls = mutableListOf<String>()
    var loadResults = ArrayDeque<Result<BoardContent, BoardLoadError>>()
    var writeResults = ArrayDeque<Result<Any, ColumnWriteError>>()
    val gates = ArrayDeque<CompletableDeferred<Unit>>()
    val moves = mutableListOf<Pair<Uuid, Int>>()

    private suspend fun gate() {
        gates.removeFirstOrNull()?.await()
    }

    override suspend fun loadBoard(boardId: Uuid): Result<BoardContent, BoardLoadError> {
        calls += "load"
        gate()
        return loadResults.removeFirstOrNull() ?: Result.Success(server)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> scripted(): Result<T, ColumnWriteError>? =
        writeResults.removeFirstOrNull() as Result<T, ColumnWriteError>?

    override suspend fun addColumn(boardId: Uuid, name: String): Result<Column, ColumnWriteError> {
        calls += "addColumn:$name"
        gate()
        scripted<Column>()?.let { return it }
        val column = Column(Uuid.random(), name, emptyList())
        server = server.copy(columns = server.columns + column)
        return Result.Success(column)
    }

    override suspend fun renameColumn(boardId: Uuid, columnId: Uuid, name: String): Result<Column, ColumnWriteError> {
        calls += "renameColumn:$name"
        gate()
        scripted<Column>()?.let { return it }
        val column = server.columns.firstOrNull { it.id == columnId } ?: return Result.Error(ColumnWriteError.ColumnNotFound)
        val renamed = column.copy(name = name)
        server = server.copy(columns = server.columns.map { if (it.id == columnId) renamed else it })
        return Result.Success(renamed.copy(cards = emptyList()))
    }

    override suspend fun moveColumn(boardId: Uuid, columnId: Uuid, position: Int): Result<List<Uuid>, ColumnWriteError> {
        calls += "moveColumn:$position"
        moves += columnId to position
        gate()
        scripted<List<Uuid>>()?.let { return it }
        val column = server.columns.firstOrNull { it.id == columnId } ?: return Result.Error(ColumnWriteError.ColumnNotFound)
        val rest = server.columns.filterNot { it.id == columnId }
        if (position !in 0..rest.size) return Result.Error(ColumnWriteError.Stale)
        val moved = rest.toMutableList().apply { add(position, column) }
        server = server.copy(columns = moved)
        return Result.Success(moved.map { it.id })
    }

    override suspend fun deleteColumn(boardId: Uuid, columnId: Uuid): EmptyResult<ColumnWriteError> {
        calls += "deleteColumn"
        gate()
        scripted<Unit>()?.let { return it }
        server = server.copy(columns = server.columns.filterNot { it.id == columnId })
        return Result.Success(Unit)
    }
}

class FakeCardCreateRepository(private val boards: FakeBoardDetailRepository) : CardCreateRepository {
    val calls = mutableListOf<String>()
    val results = ArrayDeque<Result<Card, CardCreateError>>()
    val gates = ArrayDeque<CompletableDeferred<Unit>>()
    private var nextNumber = 500L

    override suspend fun createCard(boardId: Uuid, columnId: Uuid, title: String): Result<Card, CardCreateError> {
        calls += title
        gates.removeFirstOrNull()?.await()
        val card = Card(Uuid.random(), nextNumber++, title, emptyList())
        val server = boards.server
        if (server.columns.none { it.id == columnId }) return results.removeFirstOrNull() ?: Result.Error(CardCreateError.ColumnNotFound)
        // A scripted failure can still create the card on the server, like a lost response.
        boards.server = server.copy(columns = server.columns.map { if (it.id == columnId) it.copy(cards = it.cards + card) else it })
        return results.removeFirstOrNull() ?: Result.Success(card)
    }
}

class FakeLabelSource(var labels: List<Label> = emptyList()) : LabelSource {
    val results = ArrayDeque<Result<List<Label>, DataError>>()
    var calls = 0
        private set

    override suspend fun labels(): Result<List<Label>, DataError> {
        calls++
        return results.removeFirstOrNull() ?: Result.Success(labels)
    }
}

val sampleBoardId: Uuid = Uuid.parse("6c1f6a52-6a0e-4b9e-9d4f-1f2b3c4d5e6f")

fun label(name: String, color: LabelColor = LabelColor.Blue) = Label(Uuid.random(), name, color)

fun card(number: Long, title: String = "Card $number", labels: List<Label> = emptyList()) =
    Card(Uuid.random(), number, title, labels.map { it.id })

fun column(name: String, vararg cards: Card) = Column(Uuid.random(), name, cards.toList())

fun boardContent(vararg columns: Column, name: String = "Cardigan v1", key: String = "CRD") =
    BoardContent(Board(sampleBoardId, name, key), columns.toList())
