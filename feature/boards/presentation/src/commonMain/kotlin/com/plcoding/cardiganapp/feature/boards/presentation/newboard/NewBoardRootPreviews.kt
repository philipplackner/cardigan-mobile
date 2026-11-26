package com.plcoding.cardiganapp.feature.boards.presentation.newboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import com.plcoding.cardiganapp.core.designsystem.theme.WindowClass
import com.plcoding.cardiganapp.core.domain.text.NameError
import com.plcoding.cardiganapp.feature.boards.presentation.components.BoardKeyFieldError
import com.plcoding.cardiganapp.core.domain.board.BoardKeyError

private val previewStates = listOf(
    NewBoardState(name = "Garden irrigation", key = "GARD"),
    NewBoardState(name = "Garden irrigation", key = "CRD", submitted = true, keyError = BoardKeyFieldError.AlreadyUsed("CRD")),
    NewBoardState(name = "", key = "", submitted = true, nameError = NameError.Required, keyError = BoardKeyFieldError.Rule(BoardKeyError.Required)),
    NewBoardState(name = "Garden irrigation", key = "GARD", submitted = true, isSaving = true),
)

@Composable
private fun NewBoardPreview(index: Int, windowClass: WindowClass, darkTheme: Boolean = false) {
    AppTheme(darkTheme = darkTheme, windowClass = windowClass) {
        NewBoardScreen(state = previewStates[index], onAction = {})
    }
}

@Preview(name = "New board · Phone", widthDp = 390, heightDp = 844)
@Composable
private fun NewBoardPhonePreview() = NewBoardPreview(0, WindowClass.Phone)

@Preview(name = "New board · Desktop", widthDp = 1440, heightDp = 900)
@Composable
private fun NewBoardDesktopPreview() = NewBoardPreview(0, WindowClass.Desktop)

@Preview(name = "New board · Desktop · Dark", widthDp = 1440, heightDp = 900)
@Composable
private fun NewBoardDesktopDarkPreview() = NewBoardPreview(0, WindowClass.Desktop, darkTheme = true)

@Preview(name = "Key already used · Phone", widthDp = 390, heightDp = 844)
@Composable
private fun KeyUsedPhonePreview() = NewBoardPreview(1, WindowClass.Phone)

@Preview(name = "Key already used · Desktop", widthDp = 1440, heightDp = 900)
@Composable
private fun KeyUsedDesktopPreview() = NewBoardPreview(1, WindowClass.Desktop)

@Preview(name = "Empty submit · Phone", widthDp = 390, heightDp = 844)
@Composable
private fun EmptySubmitPhonePreview() = NewBoardPreview(2, WindowClass.Phone)

@Preview(name = "Saving · Desktop", widthDp = 1440, heightDp = 900)
@Composable
private fun SavingDesktopPreview() = NewBoardPreview(3, WindowClass.Desktop)
