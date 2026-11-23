package com.plcoding.cardiganapp.feature.boards.presentation.detail

import assertk.assertThat
import assertk.assertions.isEqualTo
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
import com.plcoding.cardiganapp.feature.boards.presentation.util.assertResource
import kotlin.test.Test
import org.jetbrains.compose.resources.StringResource

/** Every board view toast is an error toast with its own string and the column name where it has one. */
class BoardViewMessagesTest {

    private fun check(message: ToastMessage, id: StringResource, vararg args: Any) {
        assertThat(message.type).isEqualTo(ToastType.Error)
        assertResource(message.text, id, *args)
    }

    @Test
    fun messages() {
        check(BoardViewMessages.addColumnFailed(), Res.string.board_view_add_column_failed)
        check(BoardViewMessages.renameColumnFailed("Review"), Res.string.board_view_rename_column_failed, "Review")
        check(BoardViewMessages.moveColumnFailed("Review"), Res.string.board_view_move_column_failed, "Review")
        check(BoardViewMessages.deleteColumnFailed("Review"), Res.string.board_view_delete_column_failed, "Review")
        check(BoardViewMessages.addCardFailed(), Res.string.board_view_add_card_failed)
        check(BoardViewMessages.refreshFailed(), Res.string.board_view_refresh_failed)
        check(BoardViewMessages.columnGone("Review"), Res.string.board_view_column_gone, "Review")
        check(BoardViewMessages.moveStale("Review"), Res.string.board_view_move_stale, "Review")
        check(BoardViewMessages.boardGone(), Res.string.board_view_board_gone)
    }
}
