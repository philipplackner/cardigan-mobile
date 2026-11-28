package com.plcoding.cardiganapp.feature.boards.presentation.renameboard

import com.plcoding.cardiganapp.core.domain.text.NameError

/** [boardName] is null until the board is found; the dialog draws nothing until then. */
data class RenameBoardState(
    val boardName: String? = null,
    val key: String = "",
    val name: String = "",
    val nameError: NameError? = null,
    val submitted: Boolean = false,
    val isSaving: Boolean = false,
) {
    val canSave: Boolean get() = !isSaving && nameError == null
}

sealed interface RenameBoardAction {
    data class NameChange(
        val name: String,
    ) : RenameBoardAction

    data object Save : RenameBoardAction

    data object Cancel : RenameBoardAction
}

sealed interface RenameBoardEvent {
    data class Renamed(
        val newName: String,
    ) : RenameBoardEvent

    data object Close : RenameBoardEvent
}
