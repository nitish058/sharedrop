package org.nitish.project.sharedrop

import java.io.File
import java.net.ServerSocket
import java.net.Socket
import java.nio.ByteBuffer

actual class FileReceiver {
    private var serverSocket: ServerSocket? = null
    private var isRunning = false

    actual fun startReceiving(
        port: Int,
        onProgress: (fileName: String, progress: Float) -> Unit,
        onFileReceived: (absoluteFilePath: String) -> Unit
    ) {
        Thread {
            try {
                serverSocket = ServerSocket(port)
                isRunning = true
                while (isRunning) {
                    val client: Socket = serverSocket?.accept() ?: break
                    Thread {
                        try {
                            val input = client.getInputStream()
                            val fileNameLength = input.read()
                            val fileNameBytes = ByteArray(fileNameLength)
                            input.read(fileNameBytes)
                            val fileName = String(fileNameBytes)

                            val fileLengthBytes = ByteArray(8)
                            input.read(fileLengthBytes)
                            val fileLength = ByteBuffer.wrap(fileLengthBytes).getLong()

                            val systemTempDir = System.getProperty("java.io.tmpdir")
                            val targetFile = File(systemTempDir, "temp_$fileName")

                            targetFile.outputStream().use { output ->
                                val buffer = ByteArray(8192)
                                var totalRead: Long = 0

                                onProgress(fileName, 0f)

                                while (totalRead < fileLength) {
                                    val toRead = buffer.size.toLong()
                                        .coerceAtMost(fileLength - totalRead).toInt()
                                    val bytesRead = input.read(buffer, 0, toRead)

                                    if (bytesRead == -1) break

                                    output.write(buffer, 0, bytesRead)
                                    totalRead += bytesRead

                                    onProgress(fileName, totalRead.toFloat() / fileLength.toFloat())
                                }
                                onFileReceived(targetFile.absolutePath)
                                output.flush()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }.start()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    actual fun stopReceiving() {
        isRunning = false
        serverSocket?.close()
        serverSocket = null
    }
}