package com.plcoding.cardiganapp.feature.boards.domain.detail

import com.plcoding.cardiganapp.core.domain.text.NameError
import com.plcoding.cardiganapp.core.domain.util.Error

/**
 * Why loading a board failed. [Failed] covers network failures, 5xx and unknown codes
 * (cardigan-shared-specs `api-errors` "Clients handle unknown error codes by status").
 */
sealed interface BoardLoadError : Error {
    data object BoardNotFound : BoardLoadError

    data object Failed : BoardLoadError
}

/** Why a column write failed. */
sealed interface ColumnWriteError : Error {
    data object BoardNotFound : ColumnWriteError

    data object ColumnNotFound : ColumnWriteError

    /** A move rejected with `OUT_OF_RANGE`: the board changed since it was loaded. */
    data object Stale : ColumnWriteError

    data class InvalidName(
        val rule: NameError,
    ) : ColumnWriteError

    data object Failed : ColumnWriteError
}

/** Why a card create failed. */
sealed interface CardCreateError : Error {
    data object BoardNotFound : CardCreateError

    data object ColumnNotFound : CardCreateError

    data class InvalidTitle(
        val rule: NameError,
    ) : CardCreateError

    data object Failed : CardCreateError
}
