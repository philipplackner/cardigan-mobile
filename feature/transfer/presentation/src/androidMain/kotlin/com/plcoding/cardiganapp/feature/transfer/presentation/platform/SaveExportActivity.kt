package com.plcoding.cardiganapp.feature.transfer.presentation.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import cardiganapp.feature.transfer.presentation.generated.resources.Res
import cardiganapp.feature.transfer.presentation.generated.resources.export_save_failed
import cardiganapp.feature.transfer.presentation.generated.resources.export_saved
import com.plcoding.cardiganapp.core.presentation.toast.ToastController
import com.plcoding.cardiganapp.core.presentation.toast.ToastMessage
import com.plcoding.cardiganapp.core.presentation.toast.ToastType
import com.plcoding.cardiganapp.core.presentation.util.UiText
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject

/**
 * The "Save to device" entry of the export share sheet. Opens the system "Save to…" screen for
 * the export written by the file sharer, copies it there and reports the result as a toast.
 */
class SaveExportActivity : ComponentActivity() {

    private val toastController: ToastController by inject()

    private val createDocument = registerForActivityResult(ActivityResultContracts.CreateDocument(JSON_MIME_TYPE)) { uri ->
        if (uri == null) finish() else save(uri)
    }

    private val fileName: String? get() = intent.getStringExtra(EXTRA_FILE_NAME)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) return
        val file = exportedFile(this, fileName)
        if (file == null) {
            showResult(saved = false)
            finish()
            return
        }
        createDocument.launch(file.name)
    }

    private fun save(target: Uri) {
        lifecycleScope.launch {
            val saved = withContext(Dispatchers.IO) { copy(exportedFile(this@SaveExportActivity, fileName), target) }
            showResult(saved)
            finish()
        }
    }

    private fun copy(source: File?, target: Uri): Boolean = try {
        if (source == null) {
            false
        } else {
            contentResolver.openOutputStream(target, "wt")?.use { output ->
                source.inputStream().use { it.copyTo(output) }
                true
            } ?: false
        }
    } catch (e: IOException) {
        false
    } catch (e: SecurityException) {
        false
    }

    private fun showResult(saved: Boolean) {
        val name = fileName.orEmpty()
        val message = if (saved) {
            ToastMessage(UiText.Resource(Res.string.export_saved, arrayOf(name)), ToastType.Success)
        } else {
            ToastMessage(UiText.Resource(Res.string.export_save_failed, arrayOf(name)), ToastType.Error)
        }
        toastController.show(message)
    }

    companion object {
        private const val EXTRA_FILE_NAME = "file_name"

        fun intent(context: Context, fileName: String): Intent =
            Intent(context, SaveExportActivity::class.java).putExtra(EXTRA_FILE_NAME, fileName)
    }
}
