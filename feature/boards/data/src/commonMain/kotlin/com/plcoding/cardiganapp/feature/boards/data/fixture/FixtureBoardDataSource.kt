package com.plcoding.cardiganapp.feature.boards.data.fixture

import com.plcoding.cardiganapp.core.domain.util.DataError
import com.plcoding.cardiganapp.core.domain.util.EmptyResult
import com.plcoding.cardiganapp.core.domain.util.Result
import com.plcoding.cardiganapp.feature.boards.domain.BoardOrder
import com.plcoding.cardiganapp.feature.boards.domain.BoardSaveError
import com.plcoding.cardiganapp.feature.boards.domain.BoardSummary
import com.plcoding.cardiganapp.feature.boards.domain.KeyFieldError
import com.plcoding.cardiganapp.feature.boards.domain.RemoteBoardDataSource
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * TEMPORARY stand-in for the board endpoints until `mob-boards-connect`, for checking the screens
 * by hand. Starts with the boards from the Figma frames. Trigger names:
 * - a create or rename whose name starts with "fail" fails with a network error
 * - the key `USED` fails with a key `ALREADY_USED` field error
 * - renaming or deleting a board named "Gone" fails with not found, and the board disappears
 * - a refresh within 2 seconds of a "fail" save fails with a network error
 */
class FixtureBoardDataSource(
    private val clock: Clock = Clock.System,
    private val latency: Duration = 600.milliseconds,
) : RemoteBoardDataSource {

    private val lock = Mutex()
    private val boards = initialBoards(clock.now()).toMutableList()
    private var lastFailedSave: Instant? = null

    override suspend fun getBoards(): Result<List<BoardSummary>, DataError.Network> {
        delay(latency)
        return lock.withLock {
            val failedAt = lastFailedSave
            if (failedAt != null && clock.now() - failedAt <= REFRESH_FAIL_WINDOW) {
                Result.Error(DataError.Network.NO_INTERNET)
            } else {
                Result.Success(boards.sortedWith(BoardOrder))
            }
        }
    }

    override suspend fun createBoard(name: String, key: String): Result<BoardSummary, BoardSaveError> {
        delay(latency)
        return lock.withLock {
            when {
                name.isFailTrigger() -> failSave()
                key == USED_KEY || boards.any { it.key == key } ->
                    Result.Error(BoardSaveError.InvalidFields(key = KeyFieldError.AlreadyUsed))
                else -> {
                    val now = clock.now()
                    val board = BoardSummary(Uuid.random(), name, key, cardCount = 0, createdAt = now, updatedAt = now)
                    boards += board
                    Result.Success(board)
                }
            }
        }
    }

    override suspend fun renameBoard(id: Uuid, name: String): Result<BoardSummary, BoardSaveError> {
        delay(latency)
        return lock.withLock {
            val board = boards.firstOrNull { it.id == id }
            when {
                board == null -> Result.Error(BoardSaveError.NotFound)
                board.name == GONE_NAME -> {
                    boards.remove(board)
                    Result.Error(BoardSaveError.NotFound)
                }
                name.isFailTrigger() -> failSave()
                else -> {
                    val renamed = board.copy(name = name, updatedAt = clock.now())
                    boards[boards.indexOf(board)] = renamed
                    Result.Success(renamed)
                }
            }
        }
    }

    override suspend fun deleteBoard(id: Uuid): EmptyResult<BoardSaveError> {
        delay(latency)
        return lock.withLock {
            val board = boards.firstOrNull { it.id == id } ?: return@withLock Result.Error(BoardSaveError.NotFound)
            boards.remove(board)
            if (board.name == GONE_NAME) Result.Error(BoardSaveError.NotFound) else Result.Success(Unit)
        }
    }

    private fun <T> failSave(): Result<T, BoardSaveError> {
        lastFailedSave = clock.now()
        return Result.Error(BoardSaveError.Network(DataError.Network.NO_INTERNET))
    }

    private fun String.isFailTrigger() = startsWith(FAIL_PREFIX, ignoreCase = true)

    companion object {
        const val FAIL_PREFIX = "fail"
        const val USED_KEY = "USED"
        const val GONE_NAME = "Gone"
        val REFRESH_FAIL_WINDOW = 2.seconds

        private fun initialBoards(now: Instant): List<BoardSummary> = listOf(
            fixture("Cardigan v1", "CRD", 12, now - 2.hours, now - 120.days),
            fixture("Raytracer", "RAY", 23, now - 1.days, now - 200.days),
            fixture("Conference talk", "TALK", 7, now - 44.days, now - 90.days),
            fixture("Home server", "HOME", 9, now - 53.days, now - 300.days),
            fixture("Dotfiles cleanup", "DOT", 4, now - 55.days, now - 400.days),
            fixture("Reading list", "READ", 15, now - 84.days, now - 500.days),
        )

        private fun fixture(name: String, key: String, cards: Int, updated: Instant, created: Instant) =
            BoardSummary(Uuid.random(), name, key, cards, createdAt = created, updatedAt = updated)
    }
}
