package com.plcoding.cardiganapp.feature.boards.presentation.sidebar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plcoding.cardiganapp.feature.boards.domain.BoardRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SidebarBoardUi(
    val id: String,
    val name: String,
    val key: String,
)

/** The sidebar's board section, in the list's order. Starts the load if nothing loaded it yet. */
class BoardsSidebarViewModel(
    private val repository: BoardRepository,
) : ViewModel() {
    val boards: StateFlow<List<SidebarBoardUi>> =
        repository.boards
            .map { boards -> boards.orEmpty().map { SidebarBoardUi(it.id.toString(), it.name, it.key) } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch { repository.ensureLoaded() }
    }
}
