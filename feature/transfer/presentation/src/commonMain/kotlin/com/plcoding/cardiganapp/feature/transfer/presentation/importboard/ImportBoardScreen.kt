package com.plcoding.cardiganapp.feature.transfer.presentation.importboard

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import cardiganapp.feature.transfer.presentation.generated.resources.Res
import cardiganapp.feature.transfer.presentation.generated.resources.import_dialog_cancel
import cardiganapp.feature.transfer.presentation.generated.resources.import_dialog_description
import cardiganapp.feature.transfer.presentation.generated.resources.import_dialog_limit
import cardiganapp.feature.transfer.presentation.generated.resources.import_dialog_title
import cardiganapp.feature.transfer.presentation.generated.resources.import_drop_zone_description
import cardiganapp.feature.transfer.presentation.generated.resources.import_drop_zone_hint
import cardiganapp.feature.transfer.presentation.generated.resources.import_drop_zone_hint_accent
import cardiganapp.feature.transfer.presentation.generated.resources.import_drop_zone_title
import cardiganapp.feature.transfer.presentation.generated.resources.import_error_failed
import cardiganapp.feature.transfer.presentation.generated.resources.import_error_invalid_file
import cardiganapp.feature.transfer.presentation.generated.resources.import_error_too_large
import cardiganapp.feature.transfer.presentation.generated.resources.import_key_cancel
import cardiganapp.feature.transfer.presentation.generated.resources.import_key_confirm
import cardiganapp.feature.transfer.presentation.generated.resources.import_key_description_known
import cardiganapp.feature.transfer.presentation.generated.resources.import_key_description_unknown
import cardiganapp.feature.transfer.presentation.generated.resources.import_key_help
import cardiganapp.feature.transfer.presentation.generated.resources.import_key_label
import cardiganapp.feature.transfer.presentation.generated.resources.import_key_title
import cardiganapp.feature.transfer.presentation.generated.resources.importing_description
import cardiganapp.feature.transfer.presentation.generated.resources.importing_title
import com.plcoding.cardiganapp.core.designsystem.components.actions.ButtonStyle
import com.plcoding.cardiganapp.core.designsystem.components.actions.CardiganButton
import com.plcoding.cardiganapp.core.designsystem.components.feedback.Banner
import com.plcoding.cardiganapp.core.designsystem.components.feedback.BannerType
import com.plcoding.cardiganapp.core.designsystem.components.inputs.ErrorMessage
import com.plcoding.cardiganapp.core.designsystem.components.inputs.Field
import com.plcoding.cardiganapp.core.designsystem.components.inputs.Input
import com.plcoding.cardiganapp.core.designsystem.components.interaction.isTouchSize
import com.plcoding.cardiganapp.core.designsystem.components.overlays.CardiganDialog
import com.plcoding.cardiganapp.core.designsystem.components.overlays.ProgressDialog
import com.plcoding.cardiganapp.core.designsystem.components.transfer.DropZone
import com.plcoding.cardiganapp.core.designsystem.components.transfer.FileRow
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import com.plcoding.cardiganapp.core.designsystem.theme.WindowClass
import com.plcoding.cardiganapp.core.domain.board.BoardKeyError
import com.plcoding.cardiganapp.feature.transfer.presentation.util.fileSizeText
import com.plcoding.cardiganapp.feature.transfer.presentation.util.toUiText
import org.jetbrains.compose.resources.stringResource

/**
 * Draws the import dialog for the current phase. Nothing is drawn while the picker is open.
 * [onCancel] ends the import from the Import dialog or the key dialog.
 */
@Composable
fun ImportBoardScreen(
    state: ImportBoardState,
    onAction: (ImportBoardAction) -> Unit,
    onCancel: () -> Unit,
) {
    when (val phase = state.phase) {
        ImportPhase.Picking, ImportPhase.Done -> Unit
        is ImportPhase.ErrorShown -> ImportErrorDialog(phase, onAction, onCancel)
        is ImportPhase.Uploading -> ProgressDialog(
            title = stringResource(Res.string.importing_title, phase.fileName),
            description = stringResource(Res.string.importing_description),
        )
        is ImportPhase.ChoosingKey -> ChooseKeyDialog(phase, onAction, onCancel)
    }
}

@Composable
private fun ImportErrorDialog(
    phase: ImportPhase.ErrorShown,
    onAction: (ImportBoardAction) -> Unit,
    onCancel: () -> Unit,
) {
    val touch = isTouchSize
    CardiganDialog(
        title = stringResource(Res.string.import_dialog_title),
        description = stringResource(Res.string.import_dialog_description),
        onDismissRequest = onCancel,
        content = {
            DropZone(
                title = stringResource(Res.string.import_drop_zone_title),
                hint = stringResource(Res.string.import_drop_zone_hint),
                hintAccent = stringResource(Res.string.import_drop_zone_hint_accent),
                contentDescription = stringResource(Res.string.import_drop_zone_description),
                onClick = { onAction(ImportBoardAction.DropZoneClick) },
            )
            phase.file?.let { file ->
                FileRow(fileName = file.name, size = fileSizeText(file.sizeBytes).asString())
            }
            Banner(message = errorMessage(phase), type = BannerType.Error, modifier = Modifier.fillMaxWidth())
            Text(
                text = stringResource(Res.string.import_dialog_limit),
                modifier = Modifier.fillMaxWidth(),
                style = AppTheme.textStyles.sans12Regular145,
                color = AppTheme.colors.text.muted,
            )
        },
        actions = {
            CardiganButton(
                text = stringResource(Res.string.import_dialog_cancel),
                onClick = onCancel,
                style = ButtonStyle.Secondary,
                modifier = if (touch) Modifier.weight(1f) else Modifier,
            )
        },
    )
}

@Composable
private fun errorMessage(phase: ImportPhase.ErrorShown): String = when (phase.error) {
    ImportDialogError.TooLarge -> stringResource(
        Res.string.import_error_too_large,
        fileSizeText(phase.file?.sizeBytes ?: 0L).asString(),
    )
    ImportDialogError.InvalidFile -> stringResource(Res.string.import_error_invalid_file)
    ImportDialogError.Failed -> stringResource(Res.string.import_error_failed)
}

@Composable
private fun ChooseKeyDialog(
    phase: ImportPhase.ChoosingKey,
    onAction: (ImportBoardAction) -> Unit,
    onCancel: () -> Unit,
) {
    val touch = isTouchSize
    val description = if (phase.boardName != null) {
        stringResource(Res.string.import_key_description_known, phase.boardName, phase.clashKey)
    } else {
        stringResource(Res.string.import_key_description_unknown, phase.clashKey)
    }
    CardiganDialog(
        title = stringResource(Res.string.import_key_title),
        description = description,
        onDismissRequest = onCancel,
        content = {
            Field(label = stringResource(Res.string.import_key_label), modifier = Modifier.fillMaxWidth()) {
                Input(
                    value = phase.key,
                    onValueChange = { onAction(ImportBoardAction.KeyChange(it)) },
                    isError = phase.fieldError != null,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { onAction(ImportBoardAction.ImportWithKey) }),
                    modifier = Modifier.fillMaxWidth(),
                )
                val error = phase.fieldError
                if (error != null) {
                    ErrorMessage(text = error.toUiText().asString())
                } else {
                    Text(
                        text = stringResource(Res.string.import_key_help),
                        modifier = Modifier.fillMaxWidth(),
                        style = AppTheme.textStyles.sans12Regular145,
                        color = AppTheme.colors.text.muted,
                    )
                }
            }
        },
        actions = {
            val buttonModifier = if (touch) Modifier.weight(1f) else Modifier
            CardiganButton(
                text = stringResource(Res.string.import_key_cancel),
                onClick = onCancel,
                style = ButtonStyle.Secondary,
                modifier = buttonModifier,
            )
            CardiganButton(
                text = stringResource(Res.string.import_key_confirm),
                onClick = { onAction(ImportBoardAction.ImportWithKey) },
                enabled = phase.canImport,
                modifier = buttonModifier,
            )
        },
    )
}

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
