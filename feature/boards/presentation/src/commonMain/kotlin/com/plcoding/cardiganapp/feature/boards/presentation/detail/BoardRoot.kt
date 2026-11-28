package com.plcoding.cardiganapp.feature.boards.presentation.detail

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.plcoding.cardiganapp.core.presentation.util.ObserveAsEvents
import kotlinx.coroutines.launch

/**
 * The board view. Navigation goes through the callbacks: [onRenameBoardClick] and
 * [onDeleteBoardClick] open the board list's dialogs, whose results the destination passes back
 * as [BoardAction.BoardRenamed] and [BoardAction.BoardDeleted].
 */
@Composable
fun BoardRoot(
    viewModel: BoardViewModel,
    onBack: () -> Unit,
    onCardClick: (cardId: String) -> Unit,
    onExportClick: (boardId: String, boardName: String) -> Unit,
    onRenameBoardClick: (boardId: String) -> Unit,
    onDeleteBoardClick: (boardId: String) -> Unit,
    onBoardMissing: () -> Unit,
    onBoardDeleted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val stripState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var focusRequest by remember { mutableStateOf<FocusRequest?>(null) }

    fun scrollToItem(index: Int) {
        if (index < 0) return
        scope.launch {
            // The new column or field is laid out on the next frame.
            withFrameNanos { }
            val layout = stripState.layoutInfo
            val item = layout.visibleItemsInfo.firstOrNull { it.index == index }
            val fullyVisible =
                item != null &&
                    item.offset >= layout.viewportStartOffset &&
                    item.offset + item.size <= layout.viewportEndOffset
            if (!fullyVisible) stripState.animateScrollToItem(index)
        }
    }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            BoardEvent.BoardMissing -> {
                onBoardMissing()
            }

            BoardEvent.BoardDeleted -> {
                onBoardDeleted()
            }

            is BoardEvent.ScrollToColumn -> {
                val columns =
                    viewModel.state.value.loaded
                        ?.content
                        ?.columns
                        .orEmpty()
                scrollToItem(columns.indexOfFirst { it.id == event.columnId })
            }

            is BoardEvent.FocusInlineField -> {
                focusRequest = FocusRequest(event.target, (focusRequest?.id ?: 0) + 1)
                val columns =
                    viewModel.state.value.loaded
                        ?.content
                        ?.columns
                        .orEmpty()
                when (val target = event.target) {
                    InlineTarget.AddColumn -> scrollToItem(columns.size)
                    is InlineTarget.QuickAdd -> scrollToItem(columns.indexOfFirst { it.id == target.columnId })
                }
            }
        }
    }

    BoardScreen(
        state = state,
        onAction = { action ->
            when (action) {
                BoardAction.BackClick -> {
                    onBack()
                }

                is BoardAction.CardClick -> {
                    onCardClick(action.cardId.toString())
                }

                BoardAction.ExportClick -> {
                    state.loaded?.content?.board?.let {
                        onExportClick(
                            it.id.toString(),
                            it.name,
                        )
                    }
                }

                BoardAction.RenameBoardClick -> {
                    state.loaded?.content?.board?.let {
                        onRenameBoardClick(
                            it.id.toString(),
                        )
                    }
                }

                BoardAction.DeleteBoardClick -> {
                    state.loaded?.content?.board?.let {
                        onDeleteBoardClick(
                            it.id.toString(),
                        )
                    }
                }

                else -> {
                    viewModel.onAction(action)
                }
            }
        },
        modifier = modifier,
        stripState = stripState,
        focusRequest = focusRequest,
    )
}
