package com.plcoding.cardiganapp.feature.boards.presentation.renameboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import com.plcoding.cardiganapp.core.designsystem.theme.WindowClass
import com.plcoding.cardiganapp.core.domain.text.NameError

private val previewStates = listOf(
    RenameBoardState(boardName = "Cardigan v1", key = "CRD", name = "Cardigan v1"),
    RenameBoardState(boardName = "Cardigan v1", key = "CRD", name = "", submitted = true, nameError = NameError.Required),
    RenameBoardState(boardName = "Cardigan v1", key = "CRD", name = "Cardigan v2", submitted = true, isSaving = true),
)

@Composable
private fun RenamePreview(index: Int, windowClass: WindowClass, darkTheme: Boolean = false) {
    AppTheme(darkTheme = darkTheme, windowClass = windowClass) {
        RenameBoardScreen(state = previewStates[index], onAction = {})
    }
}

@Preview(name = "Rename board · Phone", widthDp = 390, heightDp = 844)
@Composable
private fun RenamePhonePreview() = RenamePreview(0, WindowClass.Phone)

@Preview(name = "Rename board · Desktop", widthDp = 1440, heightDp = 900)
@Composable
private fun RenameDesktopPreview() = RenamePreview(0, WindowClass.Desktop)

@Preview(name = "Rename board · Desktop · Dark", widthDp = 1440, heightDp = 900)
@Composable
private fun RenameDesktopDarkPreview() = RenamePreview(0, WindowClass.Desktop, darkTheme = true)

@Preview(name = "Empty name · Phone", widthDp = 390, heightDp = 844)
@Composable
private fun RenameEmptyPhonePreview() = RenamePreview(1, WindowClass.Phone)

@Preview(name = "Saving · Desktop", widthDp = 1440, heightDp = 900)
@Composable
private fun RenameSavingDesktopPreview() = RenamePreview(2, WindowClass.Desktop)
