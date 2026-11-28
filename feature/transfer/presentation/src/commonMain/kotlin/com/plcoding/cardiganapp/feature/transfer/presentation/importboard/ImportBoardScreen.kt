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
        ImportPhase.Picking, ImportPhase.Done -> {
            Unit
        }

        is ImportPhase.ErrorShown -> {
            ImportErrorDialog(phase, onAction, onCancel)
        }

        is ImportPhase.Uploading -> {
            ProgressDialog(
                title = stringResource(Res.string.importing_title, phase.fileName),
                description = stringResource(Res.string.importing_description),
            )
        }

        is ImportPhase.ChoosingKey -> {
            ChooseKeyDialog(phase, onAction, onCancel)
        }
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
private fun errorMessage(phase: ImportPhase.ErrorShown): String =
    when (phase.error) {
        ImportDialogError.TooLarge -> {
            stringResource(
                Res.string.import_error_too_large,
                fileSizeText(phase.file?.sizeBytes ?: 0L).asString(),
            )
        }

        ImportDialogError.InvalidFile -> {
            stringResource(Res.string.import_error_invalid_file)
        }

        ImportDialogError.Failed -> {
            stringResource(Res.string.import_error_failed)
        }
    }

@Composable
private fun ChooseKeyDialog(
    phase: ImportPhase.ChoosingKey,
    onAction: (ImportBoardAction) -> Unit,
    onCancel: () -> Unit,
) {
    val touch = isTouchSize
    val description =
        if (phase.boardName != null) {
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
                    keyboardOptions =
                        KeyboardOptions(
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
