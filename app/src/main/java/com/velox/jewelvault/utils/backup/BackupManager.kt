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
import com.velox.jewelvault.utils.backup.ExcelExporter
import com.velox.jewelvault.utils.backup.ExcelImporter
import com.velox.jewelvault.utils.backup.FirebaseBackupManager

/**
 * Enum for restore source options
 */
enum class RestoreSource {
    FIREBASE,   // Download from Firebase Storage
    LOCAL       // Select from local file
}

/**
 * Data class for file validation result
 */
data class FileValidationResult(
    val isValid: Boolean,
    val message: String,
    val file: File? = null
)

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
    private val excelImporter = ExcelImporter(context, database)
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
     * Perform complete restore operation with smart conflict resolution
     */
    suspend fun performRestore(
        userMobile: String,
        restoreMode: RestoreMode = RestoreMode.MERGE,
        onProgress: (String, Int) -> Unit = { _, _ -> }
    ): Result<RestoreResult> {
        log("BackupManager: Restore process started for user: $userMobile with mode: $restoreMode")
        return try {
            onProgress("Starting restore process...", 0)
            
            // Get current user and store info
            val currentUserId = dataStoreManager.getAdminInfo().first.first()
            val currentStoreId = dataStoreManager.getSelectedStoreInfo().first.first()
            
            if (currentUserId.isEmpty()) {
                return Result.failure(Exception("No current user found. Please login first."))
            }
            
            onProgress("Downloading backup from Firebase...", 20)
            
            // Download backup file from Firebase
            val downloadResult = firebaseBackupManager.downloadLatestBackup(userMobile)
            
            if (downloadResult.isFailure) {
                return Result.failure(downloadResult.exceptionOrNull() ?: Exception("Download failed"))
            }
            
            val backupFile = downloadResult.getOrNull()!!
            
            onProgress("Importing data from Excel...", 60)
            
            // Import data from Excel with smart conflict resolution
            val importResult = excelImporter.importAllEntitiesFromExcel(
                backupFile, 
                currentUserId, 
                currentStoreId, 
                restoreMode
            ) { message, progress ->
                onProgress(message, (progress * 0.3).toInt() + 60) // Scale to 60-90% range
            }
            
            if (importResult.isFailure) {
                return Result.failure(importResult.exceptionOrNull() ?: Exception("Import failed"))
            }
            
            onProgress("Cleaning up downloaded files...", 90)
            
            // Clean up downloaded file
            if (backupFile.exists()) {
                backupFile.delete()
            }
            
            onProgress("Restore completed successfully!", 100)
            
            val summary = importResult.getOrNull()!!
            val result = RestoreResult(
                success = true,
                message = "Data restored successfully",
                summary = summary,
                restoreMode = restoreMode
            )
            
            log("Restore completed successfully: $summary")
            Result.success(result)
            
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

    /**
     * Check if Firebase backup exists for user
     */
    suspend fun checkFirebaseBackupExists(userMobile: String): Result<Boolean> {
        return try {
            val backupList = firebaseBackupManager.getBackupList(userMobile)
            if (backupList.isSuccess) {
                val backups = backupList.getOrNull() ?: emptyList()
                Result.success(backups.isNotEmpty())
            } else {
                Result.success(false)
            }
        } catch (e: Exception) {
            log("BackupManager: Error checking Firebase backup: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Validate Excel file for import
     */
    suspend fun validateExcelFile(fileUri: Uri): FileValidationResult {
        return try {
            val inputStream = context.contentResolver.openInputStream(fileUri)
            if (inputStream == null) {
                return FileValidationResult(false, "Cannot open file. Please check file permissions.")
            }

            // Check file extension
            val fileName = getFileNameFromUri(fileUri)
            if (!fileName.lowercase().endsWith(".xlsx")) {
                return FileValidationResult(false, "Invalid file format. Please select an Excel (.xlsx) file.")
            }

            // Create temporary file for validation
            val tempFile = File.createTempFile("validation", ".xlsx")
            tempFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }

            // Validate Excel structure
            val validationResult = excelImporter.validateExcelStructure(tempFile)
            
            if (validationResult.isSuccess) {
                FileValidationResult(true, "File is valid and ready for import.", tempFile)
            } else {
                tempFile.delete()
                FileValidationResult(false, "Invalid Excel structure: ${validationResult.exceptionOrNull()?.message ?: "Unknown error"}")
            }

        } catch (e: Exception) {
            log("BackupManager: File validation failed: ${e.message}")
            FileValidationResult(false, "File validation failed: ${e.message}")
        }
    }

    /**
     * Get file name from URI
     */
    private fun getFileNameFromUri(uri: Uri): String {
        return try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val displayNameIndex = it.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                    if (displayNameIndex != -1) {
                        return it.getString(displayNameIndex)
                    }
                }
            }
            uri.lastPathSegment ?: "unknown_file"
        } catch (e: Exception) {
            uri.lastPathSegment ?: "unknown_file"
        }
    }

    /**
     * Get default backup folder path
     */
    fun getDefaultBackupFolder(): String {
        val downloadsDir = context.getExternalFilesDir(null)?.absolutePath ?: context.filesDir.absolutePath
        return "$downloadsDir/JewelVault/Backup"
    }

    /**
     * Perform complete restore operation with source selection
     */
    suspend fun performRestoreWithSource(
        userMobile: String,
        restoreSource: RestoreSource,
        localFileUri: Uri? = null,
        restoreMode: RestoreMode = RestoreMode.MERGE,
        onProgress: (String, Int) -> Unit = { _, _ -> }
    ): Result<RestoreResult> {
        log("BackupManager: Restore process started for user: $userMobile with source: $restoreSource, mode: $restoreMode")
        return try {
            onProgress("Starting restore process...", 0)
            
            // Get current user and store info
            val currentUserId = dataStoreManager.getAdminInfo().first.first()
            val currentStoreId = dataStoreManager.getSelectedStoreInfo().first.first()
            
            if (currentUserId.isEmpty()) {
                return Result.failure(Exception("No current user found. Please login first."))
            }

            val backupFile: File = when (restoreSource) {
                RestoreSource.FIREBASE -> {
                    onProgress("Checking Firebase backup availability...", 10)
                    
                    // Check if backup exists in Firebase
                    val backupExists = checkFirebaseBackupExists(userMobile)
                    if (backupExists.isFailure) {
                        return Result.failure(Exception("Failed to check Firebase backup: ${backupExists.exceptionOrNull()?.message}"))
                    }
                    
                    if (!backupExists.getOrNull()!!) {
                        return Result.failure(Exception("No backup files found in Firebase for user: $userMobile"))
                    }
                    
                    onProgress("Downloading backup from Firebase...", 20)
                    
                    // Download backup file from Firebase
                    val downloadResult = firebaseBackupManager.downloadLatestBackup(userMobile)
                    
                    if (downloadResult.isFailure) {
                        return Result.failure(downloadResult.exceptionOrNull() ?: Exception("Download failed"))
                    }
                    
                    downloadResult.getOrNull()!!
                }
                
                RestoreSource.LOCAL -> {
                    if (localFileUri == null) {
                        return Result.failure(Exception("Local file URI is required for local restore"))
                    }
                    
                    onProgress("Validating local file...", 10)
                    
                    // Validate the local file
                    val validationResult = validateExcelFile(localFileUri)
                    if (!validationResult.isValid) {
                        return Result.failure(Exception(validationResult.message))
                    }
                    
                    onProgress("Local file validated successfully", 20)
                    validationResult.file!!
                }
            }
            
            onProgress("Importing data from Excel...", 60)
            
            // Import data from Excel with smart conflict resolution
            val importResult = excelImporter.importAllEntitiesFromExcel(
                backupFile, 
                currentUserId, 
                currentStoreId, 
                restoreMode
            ) { message, progress ->
                onProgress(message, (progress * 0.3).toInt() + 60) // Scale to 60-90% range
            }
            
            if (importResult.isFailure) {
                return Result.failure(importResult.exceptionOrNull() ?: Exception("Import failed"))
            }
            
            onProgress("Cleaning up temporary files...", 90)
            
            // Clean up temporary file
            if (backupFile.exists() && (backupFile.name.startsWith("validation") || backupFile.name.startsWith("backup_download"))) {
                backupFile.delete()
            }
            
            onProgress("Restore completed successfully!", 100)
            
            val summary = importResult.getOrNull()!!
            val result = RestoreResult(
                success = true,
                message = "Data restored successfully from ${restoreSource.name.lowercase()}",
                summary = summary,
                restoreMode = restoreMode
            )
            
            log("Restore completed successfully: $summary")
            Result.success(result)
            
        } catch (e: Exception) {
            log("BackupManager: Restore process failed: ${e.message}")
            Result.failure(e)
        }
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

/**
 * Data class to hold restore operation result
 */
data class RestoreResult(
    val success: Boolean,
    val message: String,
    val summary: ImportSummary,
    val restoreMode: RestoreMode
)