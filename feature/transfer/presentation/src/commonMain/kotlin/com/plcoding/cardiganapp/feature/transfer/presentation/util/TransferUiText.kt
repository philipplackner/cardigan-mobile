package com.plcoding.cardiganapp.feature.transfer.presentation.util

import cardiganapp.feature.transfer.presentation.generated.resources.Res
import cardiganapp.feature.transfer.presentation.generated.resources.file_size_kb
import cardiganapp.feature.transfer.presentation.generated.resources.file_size_mb
import com.plcoding.cardiganapp.core.presentation.board.boardKeyAlreadyUsedText
import com.plcoding.cardiganapp.core.presentation.board.toUiText
import com.plcoding.cardiganapp.core.presentation.util.UiText
import com.plcoding.cardiganapp.feature.transfer.domain.FileSizeFormatter
import com.plcoding.cardiganapp.feature.transfer.domain.FileSizeUnit
import com.plcoding.cardiganapp.feature.transfer.presentation.importboard.KeyFieldError

fun fileSizeText(bytes: Long): UiText {
    val size = FileSizeFormatter.format(bytes)
    val id =
        when (size.unit) {
            FileSizeUnit.KB -> Res.string.file_size_kb
            FileSizeUnit.MB -> Res.string.file_size_mb
        }
    return UiText.Resource(id, arrayOf(size.value))
}

fun KeyFieldError.toUiText(): UiText =
    when (this) {
        is KeyFieldError.AlreadyUsed -> boardKeyAlreadyUsedText(key)
        is KeyFieldError.Rule -> rule.toUiText()
    }
