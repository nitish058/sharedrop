package org.nitish.project.sharedrop

expect class FileReceiver() {
    fun startReceiving(
        port: Int,
        onProgress: (fileName : String, progress: Float) -> Unit,
        onFileReceived: (absoluteFilePath: String) -> Unit
    )

    fun stopReceiving()
}