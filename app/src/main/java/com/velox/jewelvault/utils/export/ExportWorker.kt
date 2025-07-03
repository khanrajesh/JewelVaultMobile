package com.velox.jewelvault.utils.export

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.velox.jewelvault.R
import com.velox.jewelvault.utils.ExportFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow

class ExportWorker(
    private val context: Context, workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val fileName = inputData.getString("fileName") ?: return Result.failure()
        val headers = inputData.getStringArray("headers")?.toList() ?: return Result.failure()
        val rawData = inputData.getStringArray("rows")?.toList() ?: return Result.failure()
        val formatName = inputData.getString("format") ?: ExportFormat.XLS.name
        val format = ExportFormat.valueOf(formatName)

        // Convert flat list into List<List<String>> by chunking it using header size
        val rows: List<List<String>> = rawData.chunked(headers.size)

        // Show initial progress notification
//        setForeground(createForegroundInfo(0))

        return try {
            // Collect progress from the flow
            var lastProgress = -1
            exportItemListWithProgress(
                context,
                fileName,
                rows,
                headers,
                format
            ).collect { progress ->
                    if (progress != lastProgress) {
                        lastProgress = progress
                        setProgress(workDataOf("progress" to progress))
                        setForeground(createForegroundInfo(progress))
                    }
                }


            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    // Emit progress updates using Flow
    private fun exportItemListWithProgress(
        context: Context,
        fileName: String,
        dataRows: List<List<String>>,
        headers: List<String>,
        format: ExportFormat
    ): Flow<Int> = channelFlow {
        val total = dataRows.size
        var count = 0

        exportItemListInBackground(context = context,
            fileName = fileName,
            dataRows = dataRows,
            headers = headers,
            format = format,
            onSuccess = {},
            onFailure = { throw it },
            onProgress = {
                count++
                trySend((count * 100) / total)
            })
    }

    private fun createForegroundInfo(progress: Int): ForegroundInfo {
        val notificationId = 1234
        val notification =
            NotificationCompat.Builder(context, "export_channel").setContentTitle("Exporting Items")
                .setContentText("$progress% complete").setSmallIcon(R.drawable.logo_1)
                .setProgress(100, progress, false).setOngoing(true).build()

        return ForegroundInfo(notificationId, notification)
    }
}
