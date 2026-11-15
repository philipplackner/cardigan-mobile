package com.plcoding.cardiganapp.feature.transfer.presentation.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.NSFileHandle
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSNumber
import platform.Foundation.NSURL
import platform.Foundation.fileHandleForReadingFromURL
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UniformTypeIdentifiers.UTTypeJSON
import platform.darwin.NSObject
import platform.posix.memcpy

@Composable
actual fun rememberFilePicker(
    onPicked: (PickedFile) -> Unit,
    onCancelled: () -> Unit,
): FilePicker {
    val scope = rememberCoroutineScope()
    val currentOnPicked by rememberUpdatedState(onPicked)
    val currentOnCancelled by rememberUpdatedState(onCancelled)
    return remember(scope) {
        IosFilePicker(
            scope = scope,
            onPicked = { currentOnPicked(it) },
            onCancelled = { currentOnCancelled() },
        )
    }
}

private class IosFilePicker(
    scope: CoroutineScope,
    onPicked: (PickedFile) -> Unit,
    onCancelled: () -> Unit,
) : FilePicker {

    // UIDocumentPickerViewController holds its delegate weakly, so the picker keeps it alive.
    private val delegate = PickerDelegate(scope, onPicked, onCancelled)

    override fun pickJson() {
        val picker = UIDocumentPickerViewController(forOpeningContentTypes = listOf(UTTypeJSON), asCopy = true)
        picker.delegate = delegate
        picker.allowsMultipleSelection = false
        topViewController()?.presentViewController(picker, animated = true, completion = null)
    }
}

private class PickerDelegate(
    private val scope: CoroutineScope,
    private val onPicked: (PickedFile) -> Unit,
    private val onCancelled: () -> Unit,
) : NSObject(), UIDocumentPickerDelegateProtocol {

    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
        val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
        if (url == null) {
            onCancelled()
            return
        }
        scope.launch {
            val file = withContext(Dispatchers.IO) { readPickedFile(url) }
            onPicked(file)
        }
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        onCancelled()
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun readPickedFile(url: NSURL): PickedFile {
    val name = url.lastPathComponent.orEmpty()
    val fileManager = NSFileManager.defaultManager
    try {
        val path = url.path ?: return unreadable(name)
        val size = (fileManager.attributesOfItemAtPath(path, error = null)?.get(NSFileSize) as? NSNumber)?.longLongValue
            ?: return unreadable(name)
        if (size >= PICKED_FILE_READ_CAP) return PickedFile(name, size, bytes = null)
        val handle = NSFileHandle.fileHandleForReadingFromURL(url, error = null) ?: return unreadable(name)
        val data = handle.readDataUpToLength(PICKED_FILE_READ_CAP.toULong(), error = null)
        handle.closeAndReturnError(null)
        val bytes = data?.toByteArray() ?: return unreadable(name)
        return if (bytes.size.toLong() >= PICKED_FILE_READ_CAP) {
            PickedFile(name, size.coerceAtLeast(bytes.size.toLong()), bytes = null)
        } else {
            PickedFile(name, bytes.size.toLong(), bytes)
        }
    } finally {
        fileManager.removeItemAtURL(url, error = null)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    val bytes = ByteArray(size)
    if (size > 0) {
        bytes.usePinned { memcpy(it.addressOf(0), this.bytes, length) }
    }
    return bytes
}

// An unreadable file has no content, so the device check reports it as an invalid file.
private fun unreadable(name: String) = PickedFile(name, sizeBytes = 0L, bytes = ByteArray(0))
