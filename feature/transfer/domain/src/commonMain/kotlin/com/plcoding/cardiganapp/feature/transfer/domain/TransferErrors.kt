package com.plcoding.cardiganapp.feature.transfer.domain

import com.plcoding.cardiganapp.core.domain.board.BoardKeyError
import com.plcoding.cardiganapp.core.domain.util.Error

sealed interface ExportError : Error {
    data object BoardNotFound : ExportError

    /** Network failure, unknown outcome, 5xx or any unmapped error. */
    data object Failed : ExportError
}

sealed interface ImportError : Error {
    data object FileTooLarge : ImportError

    data object InvalidFile : ImportError

    /** 409 with the field error `key`/`ALREADY_USED`. */
    data object KeyAlreadyUsed : ImportError

    /** 400 with any `key` field error. Unknown codes map to [BoardKeyError.InvalidFormat]. */
    data class KeyInvalid(
        val rule: BoardKeyError,
    ) : ImportError

    /** Network failure, unknown outcome, 5xx or any unmapped error. */
    data object Failed : ImportError
}
