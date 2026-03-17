package org.nitish.project.sharedrop

import android.content.Context

object AndroidContext {
    var context: Context? = null
    var appContext: Context? = null
    var onFilePicked: ((path: String) -> Unit)? = null
    const val FILE_PICK_REQUEST_CODE = 1001
}