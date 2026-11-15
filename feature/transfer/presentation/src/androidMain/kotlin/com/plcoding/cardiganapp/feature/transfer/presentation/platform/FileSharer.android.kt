package com.plcoding.cardiganapp.feature.transfer.presentation.platform

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.plcoding.cardiganapp.feature.transfer.domain.ExportFileName
import com.plcoding.cardiganapp.feature.transfer.domain.ExportedFile
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val EXPORTS_DIR = "exports"
private const val JSON_MIME_TYPE = "application/json"

@Composable
actual fun rememberFileSharer(): FileSharer {
    val context = LocalContext.current
    return remember(context) { AndroidFileSharer(context) }
}

private class AndroidFileSharer(private val context: Context) : FileSharer {

    override suspend fun share(file: ExportedFile) {
        val name = ExportFileName.sanitize(file.fileName) ?: ExportFileName.LAST_RESORT
        val target = withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, EXPORTS_DIR)
            dir.deleteRecursively()
            dir.mkdirs()
            File(dir, name).apply { writeBytes(file.bytes) }
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", target)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = JSON_MIME_TYPE
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, name).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
    }
}
