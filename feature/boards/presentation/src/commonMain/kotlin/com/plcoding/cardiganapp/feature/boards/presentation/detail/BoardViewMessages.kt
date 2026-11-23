package com.plcoding.cardiganapp.feature.boards.presentation.detail

import cardiganapp.feature.boards.presentation.generated.resources.Res
import cardiganapp.feature.boards.presentation.generated.resources.board_view_add_card_failed
import cardiganapp.feature.boards.presentation.generated.resources.board_view_add_column_failed
import cardiganapp.feature.boards.presentation.generated.resources.board_view_board_gone
import cardiganapp.feature.boards.presentation.generated.resources.board_view_column_gone
import cardiganapp.feature.boards.presentation.generated.resources.board_view_delete_column_failed
import cardiganapp.feature.boards.presentation.generated.resources.board_view_move_column_failed
import cardiganapp.feature.boards.presentation.generated.resources.board_view_move_stale
import cardiganapp.feature.boards.presentation.generated.resources.board_view_refresh_failed
import cardiganapp.feature.boards.presentation.generated.resources.board_view_rename_column_failed
import com.plcoding.cardiganapp.core.presentation.toast.ToastMessage
import com.plcoding.cardiganapp.core.presentation.toast.ToastType
import com.plcoding.cardiganapp.core.presentation.util.UiText
import org.jetbrains.compose.resources.StringResource

/** Every board view toast (D15, D16, D23, D40), so the copy lives in one place. */
internal object BoardViewMessages {
    fun addColumnFailed() = error(Res.string.board_view_add_column_failed)
    fun renameColumnFailed(name: String) = error(Res.string.board_view_rename_column_failed, name)
    fun moveColumnFailed(name: String) = error(Res.string.board_view_move_column_failed, name)
    fun deleteColumnFailed(name: String) = error(Res.string.board_view_delete_column_failed, name)
    fun addCardFailed() = error(Res.string.board_view_add_card_failed)
    fun refreshFailed() = error(Res.string.board_view_refresh_failed)
    fun columnGone(name: String) = error(Res.string.board_view_column_gone, name)
    fun moveStale(name: String) = error(Res.string.board_view_move_stale, name)
    fun boardGone() = error(Res.string.board_view_board_gone)

    private fun error(id: StringResource, vararg args: Any) =
        ToastMessage(UiText.Resource(id, arrayOf(*args)), ToastType.Error)
}
