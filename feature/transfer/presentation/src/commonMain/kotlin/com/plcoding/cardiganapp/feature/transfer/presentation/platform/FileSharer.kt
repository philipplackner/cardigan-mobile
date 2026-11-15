package com.plcoding.cardiganapp.feature.transfer.presentation.platform

import androidx.compose.runtime.Composable
import com.plcoding.cardiganapp.feature.transfer.domain.ExportedFile

/** Opens the system share sheet with a file. */
interface FileSharer {
    /** Writes [file] to a temporary file under its name and presents the share sheet. */
    suspend fun share(file: ExportedFile)
}

@Composable
expect fun rememberFileSharer(): FileSharer
