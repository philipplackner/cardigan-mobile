package com.plcoding.cardiganapp.feature.transfer.presentation.importboard

import com.plcoding.cardiganapp.core.domain.board.BoardKeyError
import com.plcoding.cardiganapp.core.domain.board.BoardKeyRule
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class ImportBoardState(
    val phase: ImportPhase = ImportPhase.Picking,
)

@Serializable
sealed interface ImportPhase {
    /** The system picker is open and nothing is drawn. */
    @Serializable
    @SerialName("picking")
    data object Picking : ImportPhase

    /** The Import dialog in its error state. [file] is null when the picked file is no longer known. */
    @Serializable
    @SerialName("error")
    data class ErrorShown(val file: PickedFileInfo?, val error: ImportDialogError) : ImportPhase

    @Serializable
    @SerialName("uploading")
    data class Uploading(val fileName: String) : ImportPhase

    /**
     * The key clash dialog. [clashKey], [usedKeys] and the key looked up for [boardName] are
     * normalized. [key] is what the user typed.
     */
    @Serializable
    @SerialName("choosing_key")
    data class ChoosingKey(
        val key: String,
        val clashKey: String,
        val boardName: String?,
        val usedKeys: Set<String>,
        val fieldError: KeyFieldError?,
    ) : ImportPhase {
        val canImport: Boolean get() = BoardKeyRule.normalize(key) !in usedKeys
    }

    @Serializable
    @SerialName("done")
    data object Done : ImportPhase
}

@Serializable
data class PickedFileInfo(val name: String, val sizeBytes: Long)

@Serializable
enum class ImportDialogError {
    TooLarge,
    InvalidFile,
    Failed,
}

@Serializable
sealed interface KeyFieldError {
    @Serializable
    @SerialName("already_used")
    data class AlreadyUsed(val key: String) : KeyFieldError

    @Serializable
    @SerialName("rule")
    data class Rule(val rule: BoardKeyError) : KeyFieldError
}
