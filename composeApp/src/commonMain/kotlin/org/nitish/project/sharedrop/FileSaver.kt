package org.nitish.project.sharedrop

expect class FileSaver() {
    fun moveFile(sourcePath: String, onResult: (success: Boolean, filePath: String) -> Unit)
    fun saveFile(fileName: String, bytes: ByteArray, onResult: (success: Boolean, filePath: String) -> Unit)
}
