package fr.uge.mobistorytra

import android.content.Context
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URL
import java.util.zip.GZIPInputStream
import javax.net.ssl.HttpsURLConnection

class EventsDownloader(private val context: Context, private val urlString: String) {

    fun downloadAndDecompressFile() {
        val fileName = "events.txt.gz"
        val outputFile = File(context.filesDir, fileName)
        val decompressedFileName = fileName.removeSuffix(".gz")
        val decompressedFile = File(context.filesDir, decompressedFileName)

        if (!decompressedFile.exists()) {
            GlobalScope.launch {
                downloadFile(outputFile)
                decompressGzipFile(outputFile, decompressedFile)

            }
        }
    }


    private fun decompressGzipFile(gzipFile: File, decompressedFile: File) {
        try {
            FileInputStream(gzipFile).use { fileInputStream ->
                GZIPInputStream(fileInputStream).use { gzipInputStream ->
                    FileOutputStream(decompressedFile).use { fileOutputStream ->
                        gzipInputStream.copyTo(fileOutputStream)
                        println("Décompression terminée. Fichier décompressé en tant que ${decompressedFile.path}")
                    }
                }
            }
            // Optionally, delete the gz file if not needed
            gzipFile.delete()
        } catch (e: Exception) {
            e.printStackTrace()
            println("Erreur lors de la décompression du fichier : ${e.message}")
        }
    }

    suspend fun downloadFile(outputFile: File) = withContext(Dispatchers.IO) { // Utilise le dispatcher IO pour les opérations de réseau
        try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpsURLConnection
            connection.requestMethod = "GET"

            val responseCode = connection.responseCode
            if (responseCode != HttpsURLConnection.HTTP_OK) {
                throw RuntimeException("Response Code: $responseCode")
            }

            BufferedInputStream(url.openStream()).use { inputStream ->
                FileOutputStream(outputFile).use { outputStream ->
                    val dataBuffer = ByteArray(1024)
                    var bytesRead: Int
                    while (inputStream.read(dataBuffer, 0, 1024).also { bytesRead = it } != -1) {
                        outputStream.write(dataBuffer, 0, bytesRead)
                    }
                }
            }

            println("Téléchargement terminé. Fichier enregistré en tant que ${outputFile.path}")

        } catch (e: Exception) {
            e.printStackTrace()
            println("Erreur lors du téléchargement du fichier : ${e.message}")
        }
    }
}