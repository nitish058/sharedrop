package org.nitish.project.sharedrop

import android.os.Environment
import androidx.compose.ui.input.key.Key.Companion.F
import java.io.File

actual class FileSaver {

    actual fun moveFile(
        sourcePath: String,
        onResult: (success: Boolean, filePath: String) -> Unit
    ) {
        runCatching {
            val sourceFile = File(sourcePath)

            val downloadsDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }

            val outputFile = File(downloadsDir, sourceFile.name)
            sourceFile.copyTo(
                target = outputFile,
                overwrite = true
            ).also {
                sourceFile.delete()
            }
        }.onFailure {
            onResult(false, "")
        }.onSuccess {
            onResult(true, it.absolutePath)
        }

    }

    actual fun saveFile(
        fileName: String,
        bytes: ByteArray,
        onResult: (success: Boolean, filePath: String) -> Unit
    ) {
        val context = AndroidContext.appContext
        if (context == null) {
            onResult(false, "")
            return
        }

        runCatching {
            val downloadsDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }

            val outputFile = File(downloadsDir, fileName)
            outputFile.writeBytes(bytes)
            outputFile.absolutePath
        }.onSuccess { filePath ->
            onResult(true, filePath)
        }.onFailure {
            onResult(false, "")
        }
    }
}
