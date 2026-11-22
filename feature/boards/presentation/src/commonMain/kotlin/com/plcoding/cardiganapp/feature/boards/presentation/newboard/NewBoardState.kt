package com.plcoding.cardiganapp.feature.boards.presentation.newboard

import com.plcoding.cardiganapp.core.domain.text.NameError
import com.plcoding.cardiganapp.feature.boards.presentation.components.BoardKeyFieldError

data class NewBoardState(
    val name: String = "",
    val key: String = "",
    val nameError: NameError? = null,
    val keyError: BoardKeyFieldError? = null,
    val submitted: Boolean = false,
    val isSaving: Boolean = false,
) {
    /** Disabled only while an error shows or a save runs (D8). */
    val canCreate: Boolean get() = !isSaving && nameError == null && keyError == null
}
