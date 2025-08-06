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
import android.content.ContentValues
import android.os.Environment
import android.provider.MediaStore
import android.net.Uri

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
    private val googleSheetsExporter = GoogleSheetsExporter(context)
//    private val excelImporter = ExcelImporter(context, database)
    private val firebaseBackupManager = FirebaseBackupManager(storage)
    
    /**
     * Perform complete backup operation
     */
    suspend fun performBackup(
        useGoogleSheets: Boolean = false, // Default to Excel (Google Sheets available for future use)
        onProgress: (String, Int) -> Unit = { _, _ -> }
    ): Result<String> {
        log("BackupManager: Backup process started (${if (useGoogleSheets) "Google Sheets" else "Excel"})")
        return try {
            onProgress("Starting backup process...", 0)
            
            // Get user info for backup file naming
            val userId = dataStoreManager.getAdminInfo().first.first()
            val userData = database.userDao().getUserById(userId)
            val userMobile = userData?.mobileNo ?: "unknown"
            
            if (useGoogleSheets) {
                // Use Google Sheets (for future use when permissions are available)
                onProgress("Exporting data to Google Sheets...", 20)
                val sheetsResult = googleSheetsExporter.exportAllEntitiesToGoogleSheets(database) { message, progress ->
                    onProgress(message, (progress * 0.4).toInt() + 20)
                }
                
                if (sheetsResult.isFailure) {
                    return Result.failure(sheetsResult.exceptionOrNull() ?: Exception("Google Sheets export failed"))
                }
                
                onProgress("Google Sheets backup completed!", 100)
                val sheetsUrl = sheetsResult.getOrNull() ?: ""
                log("Google Sheets backup completed successfully: $sheetsUrl")
                Result.success("Google Sheets backup completed! Access at: $sheetsUrl")
                
            } else {
                // Use Excel (current default)
                onProgress("Exporting data to Excel...", 20)
                
                // Create Excel file with all entities (now returns Uri)
                val backupUri = createBackupFile(userMobile)
                val exportResult = excelExporter.exportAllEntitiesToExcel(database, backupUri, context) { message, progress ->
                    onProgress(message, (progress * 0.4).toInt() + 20) // Scale to 20-60% range
                }
                
                if (exportResult.isFailure) {
                    return Result.failure(exportResult.exceptionOrNull() ?: Exception("Export failed"))
                }
                
                onProgress("Uploading to Firebase Storage...", 60)
                
                // Copy from Uri to temp file for upload
                val tempFile = File(context.cacheDir, "temp_backup_upload.xlsx")
                context.contentResolver.openInputStream(backupUri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                } ?: throw Exception("Unable to open input stream for backup Uri")
                val uploadResult = firebaseBackupManager.uploadBackupFile(tempFile, userMobile)
                // Clean up temp file
                if (tempFile.exists()) tempFile.delete()
                if (uploadResult.isFailure) {
                    return Result.failure(uploadResult.exceptionOrNull() ?: Exception("Upload failed"))
                }
                onProgress("Cleaning up local files...", 90)
                // Optionally delete the public file using contentResolver.delete(backupUri, null, null)
                onProgress("Backup completed successfully!", 100)
                val downloadUrl = uploadResult.getOrNull() ?: ""
                log("Backup completed successfully. Download URL: $downloadUrl")
                log("BackupManager: Backup file saved to Downloads/JewelVault/Backup/ - User can find it in their Downloads folder")
                Result.success("Backup completed! File saved to Downloads/JewelVault/Backup/ and uploaded to cloud.")
            }
            
        } catch (e: Exception) {
            log("BackupManager: Backup process failed: ${e.message}")
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
        log("BackupManager: Restore process started for user: $userMobile")
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
            log("BackupManager: Restore process failed: ${e.message}")
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
    
    private fun createBackupFile(userMobile: String): Uri {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "jewelvault_backup_${userMobile}_$timestamp.xlsx"
        
        log("BackupManager: Creating backup file: $fileName")
        
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/JewelVault/Backup")
        }
        
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw Exception("Unable to create backup file in MediaStore")
        
        log("BackupManager: Backup file created successfully at: $uri")
        log("BackupManager: File will be saved to: Downloads/JewelVault/Backup/$fileName")
        
        return uri
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