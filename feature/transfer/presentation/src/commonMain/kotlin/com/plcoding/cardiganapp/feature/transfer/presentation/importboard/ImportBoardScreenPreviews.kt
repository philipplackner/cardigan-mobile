package com.plcoding.cardiganapp.feature.transfer.presentation.importboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import com.plcoding.cardiganapp.core.designsystem.theme.WindowClass
import com.plcoding.cardiganapp.core.domain.board.BoardKeyError

private val previewFile = PickedFileInfo("board-backup.json", 7_549_747)

private val previewPhases = listOf(
    ImportPhase.ErrorShown(previewFile, ImportDialogError.TooLarge),
    ImportPhase.ErrorShown(PickedFileInfo("notes.json", 14_000), ImportDialogError.InvalidFile),
    ImportPhase.ErrorShown(null, ImportDialogError.Failed),
    ImportPhase.Uploading("board-backup.json"),
    ImportPhase.ChoosingKey("CRD", "CRD", "Cardigan v1", setOf("CRD"), KeyFieldError.AlreadyUsed("CRD")),
    ImportPhase.ChoosingKey("CRD", "CRD", null, setOf("CRD"), KeyFieldError.AlreadyUsed("CRD")),
    ImportPhase.ChoosingKey("CRDB", "CRD", "Cardigan v1", setOf("CRD"), null),
    ImportPhase.ChoosingKey("C", "CRD", "Cardigan v1", setOf("CRD"), KeyFieldError.Rule(BoardKeyError.TooShort)),
)

@Composable
private fun ImportPreview(index: Int, darkTheme: Boolean, windowClass: WindowClass) {
    AppTheme(darkTheme = darkTheme, windowClass = windowClass) {
        ImportBoardScreen(state = ImportBoardState(previewPhases[index]), onAction = {}, onCancel = {})
    }
}

@Preview(name = "Too large · Phone · Light", widthDp = 390, heightDp = 844)
@Composable
private fun TooLargePhoneLightPreview() = ImportPreview(0, darkTheme = false, windowClass = WindowClass.Phone)

@Preview(name = "Too large · Phone · Dark", widthDp = 390, heightDp = 844)
@Composable
private fun TooLargePhoneDarkPreview() = ImportPreview(0, darkTheme = true, windowClass = WindowClass.Phone)

@Preview(name = "Too large · Desktop · Light", widthDp = 1440, heightDp = 900)
@Composable
private fun TooLargeDesktopLightPreview() = ImportPreview(0, darkTheme = false, windowClass = WindowClass.Desktop)

@Preview(name = "Too large · Desktop · Dark", widthDp = 1440, heightDp = 900)
@Composable
private fun TooLargeDesktopDarkPreview() = ImportPreview(0, darkTheme = true, windowClass = WindowClass.Desktop)

@Preview(name = "Invalid file · Phone · Light", widthDp = 390, heightDp = 844)
@Composable
private fun InvalidPhoneLightPreview() = ImportPreview(1, darkTheme = false, windowClass = WindowClass.Phone)

@Preview(name = "Invalid file · Phone · Dark", widthDp = 390, heightDp = 844)
@Composable
private fun InvalidPhoneDarkPreview() = ImportPreview(1, darkTheme = true, windowClass = WindowClass.Phone)

@Preview(name = "Invalid file · Desktop · Light", widthDp = 1440, heightDp = 900)
@Composable
private fun InvalidDesktopLightPreview() = ImportPreview(1, darkTheme = false, windowClass = WindowClass.Desktop)

@Preview(name = "Invalid file · Desktop · Dark", widthDp = 1440, heightDp = 900)
@Composable
private fun InvalidDesktopDarkPreview() = ImportPreview(1, darkTheme = true, windowClass = WindowClass.Desktop)

@Preview(name = "Import failed · Phone · Light", widthDp = 390, heightDp = 844)
@Composable
private fun FailedPhoneLightPreview() = ImportPreview(2, darkTheme = false, windowClass = WindowClass.Phone)

@Preview(name = "Import failed · Phone · Dark", widthDp = 390, heightDp = 844)
@Composable
private fun FailedPhoneDarkPreview() = ImportPreview(2, darkTheme = true, windowClass = WindowClass.Phone)

@Preview(name = "Import failed · Desktop · Light", widthDp = 1440, heightDp = 900)
@Composable
private fun FailedDesktopLightPreview() = ImportPreview(2, darkTheme = false, windowClass = WindowClass.Desktop)

@Preview(name = "Import failed · Desktop · Dark", widthDp = 1440, heightDp = 900)
@Composable
private fun FailedDesktopDarkPreview() = ImportPreview(2, darkTheme = true, windowClass = WindowClass.Desktop)

@Preview(name = "Importing · Phone · Light", widthDp = 390, heightDp = 844)
@Composable
private fun ImportingPhoneLightPreview() = ImportPreview(3, darkTheme = false, windowClass = WindowClass.Phone)

@Preview(name = "Importing · Phone · Dark", widthDp = 390, heightDp = 844)
@Composable
private fun ImportingPhoneDarkPreview() = ImportPreview(3, darkTheme = true, windowClass = WindowClass.Phone)

@Preview(name = "Importing · Desktop · Light", widthDp = 1440, heightDp = 900)
@Composable
private fun ImportingDesktopLightPreview() = ImportPreview(3, darkTheme = false, windowClass = WindowClass.Desktop)

@Preview(name = "Importing · Desktop · Dark", widthDp = 1440, heightDp = 900)
@Composable
private fun ImportingDesktopDarkPreview() = ImportPreview(3, darkTheme = true, windowClass = WindowClass.Desktop)

@Preview(name = "Key used · Phone · Light", widthDp = 390, heightDp = 844)
@Composable
private fun KeyUsedPhoneLightPreview() = ImportPreview(4, darkTheme = false, windowClass = WindowClass.Phone)

@Preview(name = "Key used · Phone · Dark", widthDp = 390, heightDp = 844)
@Composable
private fun KeyUsedPhoneDarkPreview() = ImportPreview(4, darkTheme = true, windowClass = WindowClass.Phone)

@Preview(name = "Key used · Desktop · Light", widthDp = 1440, heightDp = 900)
@Composable
private fun KeyUsedDesktopLightPreview() = ImportPreview(4, darkTheme = false, windowClass = WindowClass.Desktop)

@Preview(name = "Key used · Desktop · Dark", widthDp = 1440, heightDp = 900)
@Composable
private fun KeyUsedDesktopDarkPreview() = ImportPreview(4, darkTheme = true, windowClass = WindowClass.Desktop)

@Preview(name = "Key used, unknown board · Phone · Light", widthDp = 390, heightDp = 844)
@Composable
private fun KeyUsedUnknownPhoneLightPreview() = ImportPreview(5, darkTheme = false, windowClass = WindowClass.Phone)

@Preview(name = "Key used, unknown board · Phone · Dark", widthDp = 390, heightDp = 844)
@Composable
private fun KeyUsedUnknownPhoneDarkPreview() = ImportPreview(5, darkTheme = true, windowClass = WindowClass.Phone)

@Preview(name = "Key used, unknown board · Desktop · Light", widthDp = 1440, heightDp = 900)
@Composable
private fun KeyUsedUnknownDesktopLightPreview() = ImportPreview(5, darkTheme = false, windowClass = WindowClass.Desktop)

@Preview(name = "Key used, unknown board · Desktop · Dark", widthDp = 1440, heightDp = 900)
@Composable
private fun KeyUsedUnknownDesktopDarkPreview() = ImportPreview(5, darkTheme = true, windowClass = WindowClass.Desktop)

@Preview(name = "Key changed · Phone · Light", widthDp = 390, heightDp = 844)
@Composable
private fun KeyChangedPhoneLightPreview() = ImportPreview(6, darkTheme = false, windowClass = WindowClass.Phone)

@Preview(name = "Key changed · Phone · Dark", widthDp = 390, heightDp = 844)
@Composable
private fun KeyChangedPhoneDarkPreview() = ImportPreview(6, darkTheme = true, windowClass = WindowClass.Phone)

@Preview(name = "Key changed · Desktop · Light", widthDp = 1440, heightDp = 900)
@Composable
private fun KeyChangedDesktopLightPreview() = ImportPreview(6, darkTheme = false, windowClass = WindowClass.Desktop)

@Preview(name = "Key changed · Desktop · Dark", widthDp = 1440, heightDp = 900)
@Composable
private fun KeyChangedDesktopDarkPreview() = ImportPreview(6, darkTheme = true, windowClass = WindowClass.Desktop)

@Preview(name = "Key too short · Phone · Light", widthDp = 390, heightDp = 844)
@Composable
private fun KeyTooShortPhoneLightPreview() = ImportPreview(7, darkTheme = false, windowClass = WindowClass.Phone)

@Preview(name = "Key too short · Phone · Dark", widthDp = 390, heightDp = 844)
@Composable
private fun KeyTooShortPhoneDarkPreview() = ImportPreview(7, darkTheme = true, windowClass = WindowClass.Phone)

@Preview(name = "Key too short · Desktop · Light", widthDp = 1440, heightDp = 900)
@Composable
private fun KeyTooShortDesktopLightPreview() = ImportPreview(7, darkTheme = false, windowClass = WindowClass.Desktop)

@Preview(name = "Key too short · Desktop · Dark", widthDp = 1440, heightDp = 900)
@Composable
private fun KeyTooShortDesktopDarkPreview() = ImportPreview(7, darkTheme = true, windowClass = WindowClass.Desktop)
