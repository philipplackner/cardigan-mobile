package com.plcoding.cardiganapp.feature.transfer.presentation.exportboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.tooling.preview.Preview
import cardiganapp.feature.transfer.presentation.generated.resources.Res
import cardiganapp.feature.transfer.presentation.generated.resources.exporting_description
import cardiganapp.feature.transfer.presentation.generated.resources.exporting_title
import com.plcoding.cardiganapp.core.designsystem.components.overlays.ProgressDialog
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import com.plcoding.cardiganapp.core.designsystem.theme.WindowClass
import com.plcoding.cardiganapp.core.presentation.util.ObserveAsEvents
import com.plcoding.cardiganapp.feature.transfer.presentation.platform.FileSharer
import com.plcoding.cardiganapp.feature.transfer.presentation.platform.rememberFileSharer
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Export flow: the Exporting dialog while the file is prepared, then the share sheet. [fileSharer]
 * is replaced in tests.
 */
@Composable
fun ExportBoardRoot(
    boardId: String,
    boardName: String,
    onDone: () -> Unit,
    viewModel: ExportBoardViewModel = koinViewModel { parametersOf(boardId, boardName) },
    fileSharer: FileSharer = rememberFileSharer(),
) {
    val scope = rememberCoroutineScope()
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is ExportBoardEvent.Share -> {
                scope.launch {
                    fileSharer.share(event.file)
                    onDone()
                }
            }

            ExportBoardEvent.Finished -> {
                onDone()
            }
        }
    }
    ExportBoardScreen(boardName = viewModel.boardName)
}

@Composable
fun ExportBoardScreen(boardName: String) {
    ProgressDialog(
        title = stringResource(Res.string.exporting_title, boardName),
        description = stringResource(Res.string.exporting_description),
    )
}

@Preview(name = "Exporting · Phone · Light", widthDp = 390, heightDp = 844)
@Composable
private fun ExportingPhoneLightPreview() {
    AppTheme(darkTheme = false, windowClass = WindowClass.Phone) { ExportBoardScreen("Cardigan v1") }
}

@Preview(name = "Exporting · Phone · Dark", widthDp = 390, heightDp = 844)
@Composable
private fun ExportingPhoneDarkPreview() {
    AppTheme(darkTheme = true, windowClass = WindowClass.Phone) { ExportBoardScreen("Cardigan v1") }
}

@Preview(name = "Exporting · Desktop · Light", widthDp = 1440, heightDp = 900)
@Composable
private fun ExportingDesktopLightPreview() {
    AppTheme(darkTheme = false, windowClass = WindowClass.Desktop) { ExportBoardScreen("Cardigan v1") }
}

@Preview(name = "Exporting · Desktop · Dark", widthDp = 1440, heightDp = 900)
@Composable
private fun ExportingDesktopDarkPreview() {
    AppTheme(darkTheme = true, windowClass = WindowClass.Desktop) { ExportBoardScreen("Cardigan v1") }
}
