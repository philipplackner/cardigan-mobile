package com.plcoding.cardiganapp.feature.boards.presentation.components

import com.plcoding.cardiganapp.feature.boards.domain.BoardRepository
import com.plcoding.cardiganapp.feature.boards.domain.BoardSummary

/**
 * The loaded board with [boardId]. Loads the list first when it isn't loaded (for example after
 * process death). Null when the board isn't in the list or the load failed.
 */
internal suspend fun BoardRepository.findBoard(boardId: String): BoardSummary? {
    if (boards.value == null) ensureLoaded()
    return boards.value?.firstOrNull { it.id.toString() == boardId }
}
