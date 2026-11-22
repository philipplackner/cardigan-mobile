package com.plcoding.cardiganapp.feature.boards.presentation.deleteboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cardiganapp.feature.boards.presentation.generated.resources.Res
import cardiganapp.feature.boards.presentation.generated.resources.board_dialog_cancel
import cardiganapp.feature.boards.presentation.generated.resources.delete_board_body
import cardiganapp.feature.boards.presentation.generated.resources.delete_board_body_empty
import cardiganapp.feature.boards.presentation.generated.resources.delete_board_confirm
import cardiganapp.feature.boards.presentation.generated.resources.delete_board_title
import com.plcoding.cardiganapp.core.designsystem.components.actions.ButtonStyle
import com.plcoding.cardiganapp.core.designsystem.components.actions.CardiganButton
import com.plcoding.cardiganapp.core.designsystem.components.interaction.isTouchSize
import com.plcoding.cardiganapp.core.designsystem.components.overlays.CardiganDialog
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import com.plcoding.cardiganapp.core.designsystem.theme.WindowClass
import com.plcoding.cardiganapp.core.presentation.util.ObserveAsEvents
import com.plcoding.cardiganapp.core.presentation.util.UiText
import org.jetbrains.compose.resources.stringResource

/** Delete board confirmation. [onDeleted] runs after the dialog asked to close. */
@Composable
fun DeleteBoardRoot(
    viewModel: DeleteBoardViewModel,
    onClose: () -> Unit,
    onDeleted: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            DeleteBoardEvent.Deleted -> {
                onClose()
                onDeleted()
            }
            DeleteBoardEvent.Close -> onClose()
        }
    }
    DeleteBoardScreen(state = state, onAction = viewModel::onAction)
}

/** "Its {n} cards will be deleted too. …", or only "This can’t be undone." without cards (D5). */
fun deleteBoardBody(cardCount: Int): UiText = if (cardCount == 0) {
    UiText.Resource(Res.string.delete_board_body_empty)
} else {
    UiText.PluralResource(Res.plurals.delete_board_body, cardCount, arrayOf(cardCount))
}

@Composable
fun DeleteBoardScreen(
    state: DeleteBoardState,
    onAction: (DeleteBoardAction) -> Unit,
) {
    val name = state.boardName ?: return
    val touch = isTouchSize
    CardiganDialog(
        title = stringResource(Res.string.delete_board_title, name),
        description = deleteBoardBody(state.cardCount).asString(),
        onDismissRequest = { onAction(DeleteBoardAction.Cancel) },
        actions = {
            val buttonModifier = if (touch) Modifier.weight(1f) else Modifier
            CardiganButton(
                text = stringResource(Res.string.board_dialog_cancel),
                onClick = { onAction(DeleteBoardAction.Cancel) },
                style = ButtonStyle.Secondary,
                enabled = !state.isDeleting,
                modifier = buttonModifier,
            )
            CardiganButton(
                text = stringResource(Res.string.delete_board_confirm),
                onClick = { onAction(DeleteBoardAction.Delete) },
                style = ButtonStyle.Danger,
                isLoading = state.isDeleting,
                modifier = buttonModifier,
            )
        },
    )
}

@Composable
private fun DeletePreview(state: DeleteBoardState, windowClass: WindowClass, darkTheme: Boolean = false) {
    AppTheme(darkTheme = darkTheme, windowClass = windowClass) {
        DeleteBoardScreen(state = state, onAction = {})
    }
}

@Preview(name = "Delete board · Phone", widthDp = 390, heightDp = 844)
@Composable
private fun DeletePhonePreview() = DeletePreview(DeleteBoardState("Cardigan v1", 12), WindowClass.Phone)

@Preview(name = "Delete board · Desktop", widthDp = 1440, heightDp = 900)
@Composable
private fun DeleteDesktopPreview() = DeletePreview(DeleteBoardState("Cardigan v1", 12), WindowClass.Desktop)

@Preview(name = "Delete board · Desktop · Dark", widthDp = 1440, heightDp = 900)
@Composable
private fun DeleteDesktopDarkPreview() = DeletePreview(DeleteBoardState("Cardigan v1", 12), WindowClass.Desktop, darkTheme = true)

@Preview(name = "Delete board, 1 card · Phone", widthDp = 390, heightDp = 844)
@Composable
private fun DeleteOneCardPhonePreview() = DeletePreview(DeleteBoardState("Raytracer", 1), WindowClass.Phone)

@Preview(name = "Delete board, no cards · Desktop", widthDp = 1440, heightDp = 900)
@Composable
private fun DeleteNoCardsDesktopPreview() = DeletePreview(DeleteBoardState("Raytracer", 0), WindowClass.Desktop)

@Preview(name = "Deleting · Desktop", widthDp = 1440, heightDp = 900)
@Composable
private fun DeletingDesktopPreview() = DeletePreview(DeleteBoardState("Cardigan v1", 12, isDeleting = true), WindowClass.Desktop)
