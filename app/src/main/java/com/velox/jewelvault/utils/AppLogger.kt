package com.velox.jewelvault.utils

import android.content.Context
import android.content.Intent
import android.os.Process
import androidx.core.content.FileProvider
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

object AppLogger {
    private const val maxFileSizeBytes = 10L * 1024L * 1024L
    private const val logDirName = "logs"
    private const val filePrefix = "jewelvault_log"
    private const val fileExtension = "log"

    private val lock = Any()
    private var appContext: Context? = null
    private var currentFile: File? = null
    private var currentDate: String? = null
    private val logcatRunning = AtomicBoolean(false)

    fun init(context: Context) {
        appContext = context.applicationContext
        ensureLogDir()
        startLogcatCapture()
    }

    fun log(tag: String, message: String) {
        val line = "${timestamp()} D/$tag: $message"
        writeLine(line)
    }

    fun getLogFiles(context: Context): List<File> {
        val dir = File(context.filesDir, logDirName)
        if (!dir.exists()) return emptyList()
        return dir.listFiles { file -> file.isFile && file.extension == fileExtension }
            ?.sortedBy { it.name }
            ?: emptyList()
    }

    fun shareLogs(context: Context): Boolean {
        val files = getLogFiles(context)
        if (files.isEmpty()) return false

        val uris = files.map { file ->
            FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        }

        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "text/plain"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Share Logs").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
        return true
    }

    private fun ensureLogDir() {
        val dir = appContext?.filesDir?.let { File(it, logDirName) } ?: return
        if (!dir.exists()) {
            dir.mkdirs()
        }
    }

    private fun writeLine(line: String) {
        synchronized(lock) {
            val context = appContext ?: return
            val dir = File(context.filesDir, logDirName)
            if (!dir.exists()) {
                dir.mkdirs()
            }

            val today = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
            val currentSize = currentFile?.length() ?: 0L
            val nextLineSize = line.toByteArray().size + 1L
            if (currentFile == null || currentDate != today || (currentSize + nextLineSize) > maxFileSizeBytes) {
                currentFile = createNextLogFile(dir, today)
                currentDate = today
            }

            currentFile?.appendText(line + "\n")
        }
    }

    private fun startLogcatCapture() {
        if (!logcatRunning.compareAndSet(false, true)) return
        val pid = Process.myPid().toString()
        Thread {
            try {
                val process = ProcessBuilder("logcat", "-v", "time", "--pid", pid)
                    .redirectErrorStream(true)
                    .start()
                InputStreamReader(process.inputStream).use { reader ->
                    reader.forEachLine { line ->
                        writeLine("LOGCAT: $line")
                    }
                }
            } catch (_: Exception) {
                logcatRunning.set(false)
            }
        }.apply {
            name = "AppLogcatCapture"
            isDaemon = true
            start()
        }
    }

    private fun createNextLogFile(dir: File, date: String): File {
        val existing = dir.listFiles { file ->
            file.isFile && file.name.startsWith("${filePrefix}_${date}_") && file.extension == fileExtension
        } ?: emptyArray()

        val nextIndex = (existing.mapNotNull { file ->
            val name = file.nameWithoutExtension
            val parts = name.split("_")
            parts.lastOrNull()?.toIntOrNull()
        }.maxOrNull() ?: 0) + 1

        val indexString = nextIndex.toString().padStart(3, '0')
        return File(dir, "${filePrefix}_${date}_${indexString}.$fileExtension")
    }

    private fun timestamp(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
    }
}
