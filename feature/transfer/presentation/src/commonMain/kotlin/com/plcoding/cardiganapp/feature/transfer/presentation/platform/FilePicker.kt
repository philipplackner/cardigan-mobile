package com.plcoding.cardiganapp.feature.transfer.presentation.platform

import androidx.compose.runtime.Composable

/** Opens the system file picker for JSON files. The result goes to the callbacks of [rememberFilePicker]. */
interface FilePicker {
    fun pickJson()
}

/**
 * A picked file. [sizeBytes] is the number of bytes read, or the real size when the file is over
 * the import limit. [bytes] is null when the file is over the limit.
 */
class PickedFile(
    val name: String,
    val sizeBytes: Long,
    val bytes: ByteArray?,
)

/**
 * Returns a picker whose result is delivered to [onPicked] or [onCancelled], also when the result
 * arrives after the app was recreated behind the picker.
 */
@Composable
expect fun rememberFilePicker(
    onPicked: (PickedFile) -> Unit,
    onCancelled: () -> Unit,
): FilePicker
