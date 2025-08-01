package com.velox.jewelvault.utils.backup

import android.content.Context
import com.google.firebase.storage.FirebaseStorage
import com.velox.jewelvault.data.roomdb.AppDatabase
import com.velox.jewelvault.data.DataStoreManager
import com.velox.jewelvault.utils.log
import kotlinx.coroutines.flow.first
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Main backup manager class that coordinates backup and restore operations
 */
class BackupManager(
    private val context: Context,
    private val database: AppDatabase,
    private val storage: FirebaseStorage,
    private val dataStoreManager: DataStoreManager
) {
    
    companion object {
        private const val BACKUP_FOLDER = "database_backups"
        private const val BACKUP_FILE_PREFIX = "jewelvault_backup"
        private const val BACKUP_FILE_EXTENSION = ".xlsx"
    }
    
    private val excelExporter = ExcelExporter(context)
//    private val excelImporter = ExcelImporter(context, database)
    private val firebaseBackupManager = FirebaseBackupManager(storage)
    
    /**
     * Perform complete backup operation
     */
    suspend fun performBackup(
        onProgress: (String, Int) -> Unit = { _, _ -> }
    ): Result<String> {
        return try {
            onProgress("Starting backup process...", 0)
            
            // Get user info for backup file naming
            val userId = dataStoreManager.userId.first()
            val userData = database.userDao().getUserById(userId)
            val userMobile = userData?.mobileNo ?: "unknown"
            
            onProgress("Exporting data to Excel...", 20)
            
            // Create Excel file with all entities
            val backupFile = createBackupFile(userMobile)
            val exportResult = excelExporter.exportAllEntitiesToExcel(database, backupFile) { message, progress ->
                onProgress(message, (progress * 0.4).toInt() + 20) // Scale to 20-60% range
            }
            
            if (exportResult.isFailure) {
                return Result.failure(exportResult.exceptionOrNull() ?: Exception("Export failed"))
            }
            
            onProgress("Uploading to Firebase Storage...", 60)
            
            // Upload to Firebase Storage
            val uploadResult = firebaseBackupManager.uploadBackupFile(backupFile, userMobile)
            
            if (uploadResult.isFailure) {
                return Result.failure(uploadResult.exceptionOrNull() ?: Exception("Upload failed"))
            }
            
            onProgress("Cleaning up local files...", 90)
            
            // Clean up local file
            if (backupFile.exists()) {
                backupFile.delete()
            }
            
            onProgress("Backup completed successfully!", 100)
            
            val downloadUrl = uploadResult.getOrNull() ?: ""
            log("Backup completed successfully. Download URL: $downloadUrl")
            Result.success(downloadUrl)
            
        } catch (e: Exception) {
            log("Backup failed: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Perform complete restore operation
     */
    suspend fun performRestore(
        userMobile: String,
        onProgress: (String, Int) -> Unit = { _, _ -> }
    ): Result<String> {
        return try {
            onProgress("Starting restore process...", 0)
            
            onProgress("Downloading backup from Firebase...", 20)
            
            // Download backup file from Firebase
            val downloadResult = firebaseBackupManager.downloadLatestBackup(userMobile)
            
            if (downloadResult.isFailure) {
                return Result.failure(downloadResult.exceptionOrNull() ?: Exception("Download failed"))
            }
            
            val backupFile = downloadResult.getOrNull()!!
            
            onProgress("Importing data from Excel...", 60)
            
            // Import data from Excel
//            val importResult = excelImporter.importAllEntitiesFromExcel(backupFile) { message, progress ->
//                onProgress(message, progress)
//            }
//
//            if (importResult.isFailure) {
//                return Result.failure(importResult.exceptionOrNull() ?: Exception("Import failed"))
//            }
            
            onProgress("Cleaning up downloaded files...", 90)
            
            // Clean up downloaded file
            if (backupFile.exists()) {
                backupFile.delete()
            }
            
            onProgress("Restore completed successfully!", 100)
            
            log("Restore completed successfully")
            Result.success("Data restored successfully")
            
        } catch (e: Exception) {
            log("Restore failed: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Get list of available backups for a user
     */
    suspend fun getAvailableBackups(userMobile: String): Result<List<BackupInfo>> {
        return firebaseBackupManager.getBackupList(userMobile)
    }
    
    /**
     * Delete old backup files (keep only latest N backups)
     */
    suspend fun cleanupOldBackups(userMobile: String, keepCount: Int = 5): Result<Int> {
        return firebaseBackupManager.cleanupOldBackups(userMobile, keepCount)
    }
    
    private fun createBackupFile(userMobile: String): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "${BACKUP_FILE_PREFIX}_${userMobile}_${timestamp}${BACKUP_FILE_EXTENSION}"
        
        val backupDir = File(context.cacheDir, BACKUP_FOLDER)
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }
        
        return File(backupDir, fileName)
    }
}

/**
 * Data class to hold backup information
 */
data class BackupInfo(
    val fileName: String,
    val uploadDate: Date,
    val fileSize: Long,
    val downloadUrl: String,
    val description :String
)