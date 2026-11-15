package com.plcoding.cardiganapp.feature.transfer.presentation.platform

import android.content.Context
import com.plcoding.cardiganapp.feature.transfer.domain.ExportFileName
import java.io.File

internal const val JSON_MIME_TYPE = "application/json"
private const val EXPORTS_DIR = "exports"

internal fun exportsDir(context: Context): File = File(context.cacheDir, EXPORTS_DIR)

/** The written export with this name, or null when the name isn't safe or the file is gone. */
internal fun exportedFile(context: Context, name: String?): File? {
    val safeName = ExportFileName.sanitize(name) ?: return null
    return File(exportsDir(context), safeName).takeIf { it.isFile }
}
