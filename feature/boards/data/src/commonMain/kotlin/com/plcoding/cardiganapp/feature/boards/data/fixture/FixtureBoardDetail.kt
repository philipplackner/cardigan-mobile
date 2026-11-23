package com.plcoding.cardiganapp.feature.boards.data.fixture

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
import com.plcoding.cardiganapp.feature.boards.domain.BoardRepository
import com.plcoding.cardiganapp.feature.boards.domain.detail.BoardDetailRepository
import com.plcoding.cardiganapp.feature.boards.domain.detail.BoardLoadError
import com.plcoding.cardiganapp.feature.boards.domain.detail.CardCreateError
import com.plcoding.cardiganapp.feature.boards.domain.detail.CardCreateRepository
import com.plcoding.cardiganapp.feature.boards.domain.detail.ColumnWriteError
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.Uuid
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * TEMPORARY board content for checking the board view by hand, until `mob-boards-connect` and
 * `mob-cards-connect` (D6). The board with the key `CRD` holds the Figma sample board; every other
 * board starts as a new board with the default columns. Trigger names:
 * - a column name, card title or new column name starting with "fail" fails with a network error
 *   (a card or column is still created first, like a lost response)
 * - any write on a column named "Gone" reports the column missing, and the column disappears
 * - moving a column named "Stale" is rejected as stale
 */
class FixtureBoardStore(
    private val boards: BoardRepository,
    private val latency: Duration = 600.milliseconds,
) {
    internal val lock = Mutex()
    internal val contents = mutableMapOf<Uuid, BoardContent>()
    private var nextNumber = mutableMapOf<Uuid, Long>()

    internal suspend fun wait() = delay(latency)

    /** The board's content, created on first use. Null when the board list doesn't have the board. */
    internal suspend fun content(boardId: Uuid): BoardContent? {
        if (boards.boards.value == null) boards.ensureLoaded()
        val summary = boards.boards.value?.firstOrNull { it.id == boardId } ?: return null
        val board = Board(summary.id, summary.name, summary.key)
        val content = contents.getOrPut(boardId) {
            if (summary.key == SAMPLE_KEY) sampleBoard(board) else newBoard(board)
        }
        return content.copy(board = board).also { contents[boardId] = it }
    }

    internal fun nextCardNumber(boardId: Uuid): Long {
        val content = contents.getValue(boardId)
        val next = nextNumber[boardId] ?: (content.columns.flatMap { it.cards }.maxOfOrNull { it.number } ?: 0L) + 1
        nextNumber[boardId] = next + 1
        return next
    }

    companion object {
        const val SAMPLE_KEY = "CRD"
        const val FAIL_PREFIX = "fail"
        const val GONE_NAME = "Gone"
        const val STALE_NAME = "Stale"

        val labels: List<Label> = listOf(
            label("api", LabelColor.Blue),
            label("infra", LabelColor.Amber),
            label("web", LabelColor.Teal),
            label("mobile", LabelColor.Violet),
            label("design", LabelColor.Pink),
            label("bug", LabelColor.Red),
            label("docs", LabelColor.Slate),
        )

        private fun label(name: String, color: LabelColor) = Label(Uuid.random(), name, color)

        private fun labelIds(vararg names: String) = names.map { name -> labels.first { it.name == name }.id }

        private fun card(number: Long, title: String, vararg labels: String) =
            Card(Uuid.random(), number, title, labelIds(*labels))

        private fun sampleBoard(board: Board) = BoardContent(
            board = board,
            columns = listOf(
                Column(
                    Uuid.random(),
                    "To do",
                    listOf(
                        card(118, "Decide card ordering: fractional ranks vs. integer positions", "api"),
                        card(121, "Define export file format v1", "api", "infra"),
                        card(124, "Empty state for the board list", "web", "mobile", "design"),
                        card(127, "Refresh token transport on web", "web", "api"),
                    ),
                ),
                Column(
                    Uuid.random(),
                    "In progress",
                    listOf(
                        card(109, "Single-flight token refresh in the Ktor client", "mobile", "api"),
                        card(112, "Column menu with move left and right", "web"),
                        card(115, "Long-press drag with auto-scroll between columns", "mobile"),
                    ),
                ),
                Column(
                    Uuid.random(),
                    "Review",
                    listOf(
                        card(104, "Import fails on a board with an empty column", "api", "bug"),
                        card(106, "Reverse proxy and TLS on the VPS", "infra"),
                    ),
                ),
                Column(
                    Uuid.random(),
                    "Done",
                    listOf(
                        card(101, "First draft of the REST contract", "api", "docs"),
                        card(102, "CMP project scaffolding with CI", "mobile", "infra"),
                        card(103, "React and TypeScript scaffold", "web"),
                    ),
                ),
            ),
        )

        private fun newBoard(board: Board) = BoardContent(
            board = board,
            columns = listOf("To do", "In progress", "Done").map { Column(Uuid.random(), it, emptyList()) },
        )
    }
}

/** TEMPORARY, see [FixtureBoardStore]. */
class FixtureBoardDetailRepository(private val store: FixtureBoardStore) : BoardDetailRepository {

    override suspend fun loadBoard(boardId: Uuid): Result<BoardContent, BoardLoadError> {
        store.wait()
        return store.lock.withLock {
            store.content(boardId)?.let { Result.Success(it) } ?: Result.Error(BoardLoadError.BoardNotFound)
        }
    }

    override suspend fun addColumn(boardId: Uuid, name: String): Result<Column, ColumnWriteError> = write(boardId) { content ->
        val column = Column(Uuid.random(), name, emptyList())
        store.contents[boardId] = content.copy(columns = content.columns + column)
        if (name.isFail()) Result.Error(ColumnWriteError.Failed) else Result.Success(column)
    }

    override suspend fun renameColumn(boardId: Uuid, columnId: Uuid, name: String): Result<Column, ColumnWriteError> =
        columnWrite(boardId, columnId) { content, column ->
            if (name.isFail()) return@columnWrite Result.Error(ColumnWriteError.Failed)
            val renamed = column.copy(name = name)
            store.contents[boardId] = content.copy(columns = content.columns.map { if (it.id == columnId) renamed else it })
            Result.Success(renamed)
        }

    override suspend fun moveColumn(boardId: Uuid, columnId: Uuid, position: Int): Result<List<Uuid>, ColumnWriteError> =
        columnWrite(boardId, columnId) { content, column ->
            val rest = content.columns.filterNot { it.id == columnId }
            when {
                column.name.startsWith(FixtureBoardStore.STALE_NAME) -> Result.Error(ColumnWriteError.Stale)
                column.name.isFail() -> Result.Error(ColumnWriteError.Failed)
                position !in 0..rest.size -> Result.Error(ColumnWriteError.Stale)
                else -> {
                    val moved = rest.toMutableList().apply { add(position, column) }
                    store.contents[boardId] = content.copy(columns = moved)
                    Result.Success(moved.map { it.id })
                }
            }
        }

    override suspend fun deleteColumn(boardId: Uuid, columnId: Uuid): EmptyResult<ColumnWriteError> = write(boardId) { content ->
        val column = content.columns.firstOrNull { it.id == columnId }
        if (column != null && column.name.isFail()) return@write Result.Error(ColumnWriteError.Failed)
        store.contents[boardId] = content.copy(columns = content.columns.filterNot { it.id == columnId })
        Result.Success(Unit)
    }

    private suspend fun <T> write(
        boardId: Uuid,
        block: (BoardContent) -> Result<T, ColumnWriteError>,
    ): Result<T, ColumnWriteError> {
        store.wait()
        return store.lock.withLock {
            val content = store.content(boardId) ?: return@withLock Result.Error(ColumnWriteError.BoardNotFound)
            block(content)
        }
    }

    private suspend fun <T> columnWrite(
        boardId: Uuid,
        columnId: Uuid,
        block: (BoardContent, Column) -> Result<T, ColumnWriteError>,
    ): Result<T, ColumnWriteError> = write(boardId) { content ->
        val column = content.columns.firstOrNull { it.id == columnId }
        when {
            column == null -> Result.Error(ColumnWriteError.ColumnNotFound)
            column.name == FixtureBoardStore.GONE_NAME -> {
                store.contents[boardId] = content.copy(columns = content.columns - column)
                Result.Error(ColumnWriteError.ColumnNotFound)
            }
            else -> block(content, column)
        }
    }
}

/** TEMPORARY, see [FixtureBoardStore]. */
class FixtureCardCreateRepository(private val store: FixtureBoardStore) : CardCreateRepository {

    override suspend fun createCard(boardId: Uuid, columnId: Uuid, title: String): Result<Card, CardCreateError> {
        store.wait()
        return store.lock.withLock {
            val content = store.content(boardId) ?: return@withLock Result.Error(CardCreateError.BoardNotFound)
            val column = content.columns.firstOrNull { it.id == columnId }
            when {
                column == null -> Result.Error(CardCreateError.ColumnNotFound)
                column.name == FixtureBoardStore.GONE_NAME -> {
                    store.contents[boardId] = content.copy(columns = content.columns - column)
                    Result.Error(CardCreateError.ColumnNotFound)
                }
                else -> {
                    val card = Card(Uuid.random(), store.nextCardNumber(boardId), title, emptyList())
                    store.contents[boardId] = content.copy(
                        columns = content.columns.map { if (it.id == columnId) it.copy(cards = it.cards + card) else it },
                    )
                    if (title.isFail()) Result.Error(CardCreateError.Failed) else Result.Success(card)
                }
            }
        }
    }
}

/** TEMPORARY: the sample labels until the labels feature implements [LabelSource]. */
class FixtureLabelSource(private val latency: Duration = 300.milliseconds) : LabelSource {
    override suspend fun labels(): Result<List<Label>, DataError> {
        delay(latency)
        return Result.Success(FixtureBoardStore.labels)
    }
}

private fun String.isFail() = startsWith(FixtureBoardStore.FAIL_PREFIX, ignoreCase = true)
