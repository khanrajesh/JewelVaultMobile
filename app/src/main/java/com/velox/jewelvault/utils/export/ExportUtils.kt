package com.velox.jewelvault.utils.export

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.velox.jewelvault.utils.ExportFormat
import com.velox.jewelvault.utils.ioScope
import com.velox.jewelvault.utils.logJvSync
import com.velox.jewelvault.utils.mainScope
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStreamWriter
import android.content.ContentResolver
import android.net.Uri


fun enqueueExportWorker(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    fileName: String,
    headers: List<String>,
    rows: List<List<String>>,
    format: ExportFormat = ExportFormat.XLSX,
    onExportComplete: ((String?) -> Unit)? = null
) {
    // Create notification channel for export operations
    createExportNotificationChannel(context)
    
    logJvSync("enqueueExportWorker requested for $fileName, format: ${format.name}")
    mainScope {
        val flatRows = rows.flatten().toTypedArray()
        val inputData = workDataOf(
            "fileName" to fileName,
            "headers" to headers.toTypedArray(),
            "rows" to flatRows,
            "format" to format.name
        )

        val request = OneTimeWorkRequestBuilder<ExportWorker>()
            .setInputData(inputData)
            .build()

        val workerManager = WorkManager.getInstance(context)

        workerManager.enqueue(request)
        logJvSync("ExportWorker enqueued for $fileName")
        workerManager.getWorkInfoByIdLiveData(request.id).observe(lifecycleOwner) { workInfo ->
            if (workInfo?.state == WorkInfo.State.SUCCEEDED) {
                val fileUri = workInfo.outputData.getString("fileUri")
                onExportComplete?.invoke(fileUri)
                logJvSync("ExportWorker completed successfully for $fileName")
                Toast.makeText(context, "Export successful", Toast.LENGTH_SHORT).show()
            } else if (workInfo?.state == WorkInfo.State.FAILED) {
                onExportComplete?.invoke(null)
                logJvSync("ExportWorker failed for $fileName")
                Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
            }
        }

        Toast.makeText(context, "Export started...", Toast.LENGTH_SHORT).show()
    }
}

private fun createExportNotificationChannel(context: Context) {
    logJvSync("Creating export notification channel")
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
       
        // Check if channel already exists
        if (notificationManager.getNotificationChannel("export_channel") == null) {
            val exportChannel = NotificationChannel(
                "export_channel",
                "Export Operations",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for export operations"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(exportChannel)
            logJvSync("Export notification channel created")
        }
    }
}


fun exportItemListAndGetUri(
    context: Context,
    fileName: String,
    dataRows: List<List<String>>,
    headers: List<String>,
    format: ExportFormat
): String? {
    logJvSync("exportItemListAndGetUri starting for $fileName")
    return try {
        // Validate column sizes
        if (dataRows.any { it.size != headers.size }) {
            throw IllegalArgumentException("Row size must match headers size")
        }

        val mimeType = when (format) {
            ExportFormat.CSV -> "text/csv"
            ExportFormat.XLS -> "application/vnd.ms-excel"
            ExportFormat.XLSX -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(
                MediaStore.Downloads.RELATIVE_PATH,
                Environment.DIRECTORY_DOWNLOADS + "/JewelVault/ItemExport"
            )
        }

        val resolver = context.contentResolver
        deleteExistingExportFile(resolver, fileName, Environment.DIRECTORY_DOWNLOADS + "/JewelVault/ItemExport")
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw IOException("Unable to create file in MediaStore")

        resolver.openOutputStream(uri)?.use { outputStream ->
            when (format) {
                ExportFormat.CSV -> {
                    val writer = BufferedWriter(OutputStreamWriter(outputStream))
                    writer.write(headers.joinToString(","))
                    writer.newLine()
                    dataRows.forEach { row ->
                        writer.write(row.joinToString(","))
                        writer.newLine()
                    }
                    writer.flush()
                }

                ExportFormat.XLS, ExportFormat.XLSX -> {
                    val workbook =
                        if (format == ExportFormat.XLS) HSSFWorkbook() else XSSFWorkbook()
                    val sheet = workbook.createSheet("Inventory")

                    val headerRow = sheet.createRow(0)
                    headers.forEachIndexed { col, title ->
                        headerRow.createCell(col).setCellValue(title)
                    }

                    dataRows.forEachIndexed { rowIndex, rowData ->
                        val row = sheet.createRow(rowIndex + 1)
                        rowData.forEachIndexed { colIndex, value ->
                            row.createCell(colIndex).setCellValue(value)
                        }
                    }

                    workbook.write(outputStream)
                    workbook.close()
                }
            }
        }

        logJvSync("exportItemListAndGetUri succeeded for $fileName ($uri)")
        uri.toString()
    } catch (e: Exception) {
        logJvSync("exportItemListAndGetUri failed for $fileName: ${e.message}")
        e.printStackTrace()
        null
    }
}

fun exportItemListInBackground(
    context: Context,
    fileName: String,
    dataRows: List<List<String>>,
    headers: List<String>,
    format: ExportFormat,
    onSuccess: () -> Unit,
    onFailure: (Throwable) -> Unit,
    onProgress: (() -> Unit)? = null
) {
    logJvSync("exportItemListInBackground starting for $fileName")
    ioScope {
        try {
            // Validate column sizes
            if (dataRows.any { it.size != headers.size }) {
                throw IllegalArgumentException("Row size must match headers size")
            }

            val mimeType = when (format) {
                ExportFormat.CSV -> "text/csv"
                ExportFormat.XLS -> "application/vnd.ms-excel"
                ExportFormat.XLSX -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            }

            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(
                    MediaStore.Downloads.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS + "/JewelVault/ItemExport"
                )
            }

        val resolver = context.contentResolver
        deleteExistingExportFile(resolver, fileName, Environment.DIRECTORY_DOWNLOADS + "/JewelVault/ItemExport")
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw IOException("Unable to create file in MediaStore")

            resolver.openOutputStream(uri)?.use { outputStream ->
                when (format) {
                    ExportFormat.CSV -> {
                        val writer = BufferedWriter(OutputStreamWriter(outputStream))
                        writer.write(headers.joinToString(","))
                        writer.newLine()
                        dataRows.forEach { row ->
                            writer.write(row.joinToString(","))
                            writer.newLine()
                        }
                        writer.flush()
                    }

                    ExportFormat.XLS, ExportFormat.XLSX -> {
                        val workbook =
                            if (format == ExportFormat.XLS) HSSFWorkbook() else XSSFWorkbook()
                        val sheet = workbook.createSheet("Inventory")

                        val headerRow = sheet.createRow(0)
                        headers.forEachIndexed { col, title ->
                            headerRow.createCell(col).setCellValue(title)
                        }

                        dataRows.forEachIndexed { rowIndex, rowData ->
                            val row = sheet.createRow(rowIndex + 1)
                            rowData.forEachIndexed { colIndex, value ->
                                row.createCell(colIndex).setCellValue(value)
                                if (onProgress != null) {
                                    onProgress()
                                }
                            }
                        }

                        workbook.write(outputStream)
                        workbook.close()
                    }
                }
            }

            logJvSync("exportItemListInBackground succeeded for $fileName")
            mainScope { onSuccess() }

        } catch (e: Exception) {
            logJvSync("exportItemListInBackground failed for $fileName: ${e.message}")
            mainScope { onFailure(e) }
        }
    }
}

private fun deleteExistingExportFile(resolver: ContentResolver, fileName: String, relativePath: String) {
    val projection = arrayOf(MediaStore.MediaColumns._ID)
    val selection = "${MediaStore.MediaColumns.DISPLAY_NAME}=? AND ${MediaStore.MediaColumns.RELATIVE_PATH}=?"
    val selectionArgs = arrayOf(fileName, "$relativePath/")
    resolver.query(
        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        selectionArgs,
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
            val existingUri = Uri.withAppendedPath(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id.toString())
            resolver.delete(existingUri, null, null)
            logJvSync("Deleted existing export file: $existingUri")
        }
    }
}

