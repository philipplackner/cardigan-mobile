package com.plcoding.cardiganapp.feature.transfer.presentation.platform

import android.content.Context
import android.content.Intent
import android.content.pm.LabeledIntent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import cardiganapp.feature.transfer.presentation.generated.resources.Res
import cardiganapp.feature.transfer.presentation.generated.resources.export_save_to_device
import com.plcoding.cardiganapp.feature.transfer.domain.ExportFileName
import com.plcoding.cardiganapp.feature.transfer.domain.ExportedFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import java.io.File

@Composable
actual fun rememberFileSharer(): FileSharer {
    val context = LocalContext.current
    return remember(context) { AndroidFileSharer(context) }
}

private class AndroidFileSharer(
    private val context: Context,
) : FileSharer {
    override suspend fun share(file: ExportedFile) {
        val name = ExportFileName.sanitize(file.fileName) ?: ExportFileName.LAST_RESORT
        val target =
            withContext(Dispatchers.IO) {
                val dir = exportsDir(context)
                dir.deleteRecursively()
                dir.mkdirs()
                File(dir, name).apply { writeBytes(file.bytes) }
            }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", target)
        val send =
            Intent(Intent.ACTION_SEND).apply {
                type = JSON_MIME_TYPE
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        // No app takes a shared file to save it locally (Files only offers the "Save to…" screen),
        // so the sheet gets our own entry that opens that screen.
        val saveToDevice =
            LabeledIntent(
                SaveExportActivity.intent(context, name),
                context.packageName,
                getString(Res.string.export_save_to_device),
                0,
            )
        val chooser =
            Intent
                .createChooser(send, name)
                .putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf<Intent>(saveToDevice))
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(chooser)
    }
}
