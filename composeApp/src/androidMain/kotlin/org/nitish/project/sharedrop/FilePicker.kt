package org.nitish.project.sharedrop

import android.app.Activity
import android.content.Intent
import android.net.Uri

actual class FilePicker {
    actual fun pickFile(onFilePicked: (path: String) -> Unit) {
        val activity = AndroidContext.context as? Activity ?: return
        AndroidContext.onFilePicked = onFilePicked
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
        }
        activity.startActivityForResult(intent, AndroidContext.FILE_PICK_REQUEST_CODE)
    }
}
