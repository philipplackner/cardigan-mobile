package com.plcoding.cardiganapp.feature.transfer.presentation.platform

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

private const val BUFFER_SIZE = 64 * 1024

@Composable
actual fun rememberFilePicker(
    onPicked: (PickedFile) -> Unit,
    onCancelled: () -> Unit,
): FilePicker {
    val resolver = LocalContext.current.contentResolver
    val scope = rememberCoroutineScope()
    val currentOnPicked by rememberUpdatedState(onPicked)
    val currentOnCancelled by rememberUpdatedState(onCancelled)
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) {
                currentOnCancelled()
            } else {
                scope.launch {
                    val file = withContext(Dispatchers.IO) { readPickedFile(resolver, uri) }
                    currentOnPicked(file)
                }
            }
        }
    return remember(launcher) {
        object : FilePicker {
            override fun pickJson() = launcher.launch(arrayOf(JSON_MIME_TYPE))
        }
    }
}

private fun readPickedFile(
    resolver: ContentResolver,
    uri: Uri,
): PickedFile {
    var name = uri.lastPathSegment.orEmpty()
    var reportedSize: Long? = null
    runCatching {
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
        resolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex >= 0 && !cursor.isNull(nameIndex)) name = cursor.getString(nameIndex)
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) reportedSize = cursor.getLong(sizeIndex)
            }
        }
    }
    return try {
        resolver.openInputStream(uri)?.use { readCapped(it, name, reportedSize) }
            ?: unreadable(name)
    } catch (e: IOException) {
        unreadable(name)
    } catch (e: SecurityException) {
        unreadable(name)
    }
}

/**
 * Keeps at most [PICKED_FILE_READ_CAP] bytes. Past the cap, the provider's size is used only
 * when it is larger than the cap, since some providers report 0 or a wrong value. Otherwise the
 * rest of the stream is counted so the error shows the real size.
 */
private fun readCapped(
    input: InputStream,
    name: String,
    reportedSize: Long?,
): PickedFile {
    val kept = ByteArrayOutputStream()
    val buffer = ByteArray(BUFFER_SIZE)
    var total = 0L
    while (true) {
        val read = input.read(buffer)
        if (read < 0) break
        val keep = (PICKED_FILE_READ_CAP - total).coerceIn(0L, read.toLong()).toInt()
        if (keep > 0) kept.write(buffer, 0, keep)
        total += read
        if (total >= PICKED_FILE_READ_CAP && reportedSize != null && reportedSize > PICKED_FILE_READ_CAP) {
            return PickedFile(name, reportedSize, bytes = null)
        }
    }
    return if (total < PICKED_FILE_READ_CAP) {
        PickedFile(name, total, kept.toByteArray())
    } else {
        PickedFile(name, total, bytes = null)
    }
}

// An unreadable file has no content, so the device check reports it as an invalid file.
private fun unreadable(name: String) = PickedFile(name, sizeBytes = 0L, bytes = ByteArray(0))
