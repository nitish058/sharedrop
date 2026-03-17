package org.nitish.project.sharedrop

import java.io.File
import java.net.Socket
import java.nio.ByteBuffer

actual class FileSender {
    actual fun sendFile(
        host: String,
        port: Int,
        absolutePath: String,
        onProgress: (Float) -> Unit,
        onResult: (Boolean) -> Unit
    ) {
        Thread {
            try {
                val socket = Socket(host, port)
                val output = socket.getOutputStream()
                val inputFile = File(absolutePath)
                val fileNameBytes = inputFile.name.toByteArray()
                output.write(fileNameBytes.size)
                output.write(fileNameBytes)

                val totalBytes = inputFile.length()

                ByteBuffer.wrap(ByteArray(8)).putLong(totalBytes).array().also { output.write(it) }

                var bytesSent = 0L
                val inputStream = inputFile.inputStream()
                val buffer = ByteArray(8192)

                var bytesRead = inputStream.read(buffer)
                while (bytesRead != -1) {
                    output.write(buffer, 0, bytesRead)
                    bytesSent += bytesRead

                    onProgress(bytesSent.toFloat() / totalBytes.toFloat())

                    bytesRead = inputStream.read(buffer)
                }

                output.flush()
                socket.close()
                onResult(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false)
            }
        }.start()
    }
}