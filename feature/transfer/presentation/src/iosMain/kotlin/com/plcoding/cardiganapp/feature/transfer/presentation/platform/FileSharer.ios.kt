package com.plcoding.cardiganapp.feature.transfer.presentation.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.plcoding.cardiganapp.feature.transfer.domain.ExportFileName
import com.plcoding.cardiganapp.feature.transfer.domain.ExportedFile
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.Foundation.writeToURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.popoverPresentationController

private const val EXPORTS_DIR = "exports"

@Composable
actual fun rememberFileSharer(): FileSharer = remember { IosFileSharer() }

private class IosFileSharer : FileSharer {

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun share(file: ExportedFile) {
        val name = ExportFileName.sanitize(file.fileName) ?: ExportFileName.LAST_RESORT
        val url = withContext(Dispatchers.IO) { writeExport(name, file.bytes) } ?: return
        val presenter = topViewController() ?: return
        val sheet = UIActivityViewController(activityItems = listOf(url), applicationActivities = null)
        // iPad shows the sheet as a popover. The design has no anchor element, so it is centered without an arrow.
        sheet.popoverPresentationController?.let { popover ->
            val view = presenter.view
            popover.sourceView = view
            popover.setSourceRect(
                view.bounds.useContents { CGRectMake(origin.x + size.width / 2, origin.y + size.height / 2, 0.0, 0.0) },
            )
            popover.permittedArrowDirections = 0uL
        }
        presenter.presentViewController(sheet, animated = true, completion = null)
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun writeExport(name: String, bytes: ByteArray): NSURL? {
        val fileManager = NSFileManager.defaultManager
        val dir = NSURL.fileURLWithPath(NSTemporaryDirectory()).URLByAppendingPathComponent(EXPORTS_DIR) ?: return null
        fileManager.removeItemAtURL(dir, error = null)
        if (!fileManager.createDirectoryAtURL(dir, withIntermediateDirectories = true, attributes = null, error = null)) return null
        val target = dir.URLByAppendingPathComponent(name) ?: return null
        val data = bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
        return target.takeIf { data.writeToURL(it, atomically = true) }
    }
}
