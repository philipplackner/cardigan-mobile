package com.plcoding.cardiganapp.feature.boards.presentation.boardlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plcoding.cardiganapp.core.domain.util.Result
import com.plcoding.cardiganapp.feature.boards.domain.BoardRepository
import com.plcoding.cardiganapp.feature.boards.domain.BoardSummary
import com.plcoding.cardiganapp.feature.boards.presentation.navigation.BoardListReloader
import com.plcoding.cardiganapp.feature.boards.presentation.util.RelativeTimeFormatter
import kotlin.time.Clock
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone

/**
 * The board list reads the shared board store. The first load joins the app-start load; while the
 * store has no list, a failed load shows the error state. Pull to refresh goes through
 * [BoardListReloader], so it shares one indicator and request with the reload after an import.
 */
class BoardListViewModel(
    private val repository: BoardRepository,
    private val reloader: BoardListReloader,
    clock: Clock,
    timeZone: TimeZone,
) : ViewModel() {

    private val formatter = RelativeTimeFormatter(clock, timeZone)

    private val _state = MutableStateFlow(BoardListState())
    val state: StateFlow<BoardListState> = _state.asStateFlow()

    private val _events = Channel<BoardListEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            combine(repository.boards, reloader.isReloading, ::Pair).collect { (boards, isReloading) ->
                _state.update { it.withBoards(boards).copy(isRefreshing = isReloading) }
            }
        }
        load { repository.ensureLoaded() }
    }

    fun onAction(action: BoardListAction) {
        when (action) {
            is BoardListAction.OnBoardClick -> openBoard(action.boardId)
            BoardListAction.OnRefresh -> reloader.reload()
            BoardListAction.OnRetry -> {
                _state.update { it.copy(content = BoardListContent.Loading) }
                load { repository.refresh() }
            }
            BoardListAction.OnMenuClick -> _state.update { it.copy(isMenuOpen = true) }
            BoardListAction.OnMenuDismiss,
            BoardListAction.OnImportClick,
            BoardListAction.OnLabelsClick,
            BoardListAction.OnLogOutClick -> _state.update { it.copy(isMenuOpen = false) }
            BoardListAction.OnNewBoardClick,
            is BoardListAction.OnRenameClick,
            is BoardListAction.OnDeleteClick -> Unit
        }
    }

    private fun load(request: suspend () -> Result<Unit, *>) {
        viewModelScope.launch {
            val result = request()
            if (result is Result.Error && repository.boards.value == null) {
                _state.update { it.copy(content = BoardListContent.Error) }
            }
        }
    }

    private fun openBoard(boardId: String) {
        val board = repository.boards.value?.firstOrNull { it.id.toString() == boardId } ?: return
        _events.trySend(BoardListEvent.OpenBoard(boardId, board.name, board.key))
    }

    private fun BoardListState.withBoards(boards: List<BoardSummary>?): BoardListState = when {
        boards != null -> copy(
            content = BoardListContent.Loaded(boards.map { it.toBoardItemUi(formatter) }),
            boardCount = boards.size,
        )
        content is BoardListContent.Loaded -> copy(content = BoardListContent.Loading, boardCount = 0)
        else -> this
    }
}
