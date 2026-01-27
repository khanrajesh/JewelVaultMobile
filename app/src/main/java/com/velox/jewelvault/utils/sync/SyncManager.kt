package com.velox.jewelvault.utils.sync

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.velox.jewelvault.data.roomdb.AppDatabase
import com.velox.jewelvault.data.DataStoreManager
import com.velox.jewelvault.data.firebase.FirebaseUtils
import com.velox.jewelvault.utils.log
import com.velox.jewelvault.utils.logJvSync
import kotlinx.coroutines.flow.first
import java.io.File
import java.util.Date
import android.content.ContentValues
import android.os.Environment
import android.provider.MediaStore
import android.net.Uri
import android.os.Build

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
 * Main sync manager class that coordinates sync and restore operations
 */
class SyncManager(
    private val context: Context,
    private val database: AppDatabase,
    private val storage: FirebaseStorage,
    private val dataStoreManager: DataStoreManager,
    private val firestore: FirebaseFirestore
) {
    
    companion object {
        private const val BACKUP_FOLDER = "database_backups"
        private const val FIXED_SYNC_FILE_NAME = "backup_file.xlsx"
    }
    
    private val excelExporter = ExcelExporter(context)
    private val googleSheetsExporter = GoogleSheetsExporter(context)
    private val excelImporter = ExcelImporter(context, database)
    private val firebaseBackupManager = FirebaseBackupManager(storage)
    
    /**
     * Perform complete sync operation
     */
    suspend fun performBackup(
        useGoogleSheets: Boolean = false, // Default to Excel (Google Sheets available for future use)
        onProgress: (String, Int) -> Unit = { _, _ -> }
    ): Result<String> {
        log("SyncManager: Sync process started (${if (useGoogleSheets) "Google Sheets" else "Excel"})")
        logJvSync("performBackup initiated (useGoogleSheets=$useGoogleSheets)")
        return try {
            onProgress("Starting sync process...", 0)
            
            // Get user info for sync file naming
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
                
                onProgress("Google Sheets sync completed!", 100)
                val sheetsUrl = sheetsResult.getOrNull() ?: ""
                log("Google Sheets sync completed successfully: $sheetsUrl")
                logJvSync("performBackup completed via Google Sheets: $sheetsUrl")
                Result.success("Google Sheets sync completed! Access at: $sheetsUrl")
                
            } else {
                // Use Excel (current default)
                onProgress("Exporting data to Excel...", 20)
                
                // Create Excel file with all entities (now returns Uri)
                val backupUri = createBackupFile()
                val exportResult = excelExporter.exportAllEntitiesToExcel(database, backupUri, context) { message, progress ->
                    onProgress(message, (progress * 0.4).toInt() + 20) // Scale to 20-60% range
                }
                
                if (exportResult.isFailure) {
                    return Result.failure(exportResult.exceptionOrNull() ?: Exception("Export failed"))
                }
                
                onProgress("Uploading to cloud sync...", 60)
                
                val storeId = dataStoreManager.getSelectedStoreInfo().first.first()
                if (storeId.isBlank()) {
                    return Result.failure(Exception("Store ID not found. Please set up a store before syncing."))
                }

                // Copy from Uri to temp file for upload (used as sync filename in Firebase)
                val tempFile = File(context.cacheDir, FIXED_SYNC_FILE_NAME)
                context.contentResolver.openInputStream(backupUri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                } ?: throw Exception("Unable to open input stream for sync file")
                val uploadResult = firebaseBackupManager.uploadBackupFile(tempFile, userMobile, storeId)
                // Clean up temp file
                if (tempFile.exists()) tempFile.delete()
                if (uploadResult.isFailure) {
                    return Result.failure(uploadResult.exceptionOrNull() ?: Exception("Upload failed"))
                }
                val deviceLabel = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
                FirebaseUtils.updateStoreBackupTime(
                    firestore,
                    userMobile,
                    storeId,
                    System.currentTimeMillis(),
                    deviceLabel
                )
                onProgress("Cleaning up local files...", 90)
                // Optionally delete the public file using contentResolver.delete(backupUri, null, null)
                onProgress("Sync completed successfully!", 100)
                val downloadUrl = uploadResult.getOrNull() ?: ""
                log("Sync completed successfully. Download URL: $downloadUrl")
                log("SyncManager: Sync file saved to Downloads/JewelVault/Sync/ - User can find it in their Downloads folder")
                logJvSync("performBackup completed via Excel: Upload URL $downloadUrl")
                Result.success("Sync completed! File saved to Downloads/JewelVault/Sync/ and uploaded to cloud.")
            }
            
        } catch (e: Exception) {
            log("SyncManager: Sync process failed: ${e.message}")
            logJvSync("performBackup failed: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Export data to a local Excel file (no Firebase upload).
     */
    suspend fun performLocalExport(
        onProgress: (String, Int) -> Unit = { _, _ -> }
    ): Result<Uri> {
        logJvSync("performLocalExport initiated")
        return try {
            onProgress("Starting local export...", 0)
            val userId = dataStoreManager.getAdminInfo().first.first()
            val userData = database.userDao().getUserById(userId)
            val userMobile = userData?.mobileNo ?: "unknown"
            val storeId = dataStoreManager.getSelectedStoreInfo().first.first()

            val exportUri = createBackupFile()
            val exportResult = excelExporter.exportAllEntitiesToExcel(
                database,
                exportUri,
                context
            ) { message, progress ->
                onProgress(message, progress)
            }

            if (exportResult.isFailure) {
                return Result.failure(exportResult.exceptionOrNull() ?: Exception("Export failed"))
            }

            onProgress("Export completed successfully!", 100)
            logJvSync("performLocalExport succeeded: $exportUri")
            Result.success(exportUri)
        } catch (e: Exception) {
            log("SyncManager: Local export failed: ${e.message}")
            logJvSync("performLocalExport failed: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Import data from a local Excel file (no Firebase download).
     */
    suspend fun performLocalImport(
        localFileUri: Uri,
        restoreMode: RestoreMode = RestoreMode.MERGE,
        onProgress: (String, Int) -> Unit = { _, _ -> }
    ): Result<RestoreResult> {
        logJvSync("performLocalImport started for ${localFileUri.path ?: localFileUri}")
        return try {
            onProgress("Validating local file...", 10)
            val validationResult = validateExcelFile(localFileUri)
            if (!validationResult.isValid) {
                return Result.failure(Exception(validationResult.message))
            }

            val currentUserId = dataStoreManager.getAdminInfo().first.first()
            val currentStoreId = dataStoreManager.getSelectedStoreInfo().first.first()

            onProgress("Importing data from Excel...", 60)
            val importResult = excelImporter.importAllEntitiesFromExcel(
                validationResult.file!!,
                currentUserId,
                currentStoreId,
                restoreMode
            ) { message, progress ->
                onProgress(message, (progress * 0.3).toInt() + 60)
            }

            if (importResult.isFailure) {
                return Result.failure(importResult.exceptionOrNull() ?: Exception("Import failed"))
            }

            onProgress("Cleaning up temporary files...", 90)
            val tempFile = validationResult.file
            if (tempFile != null && tempFile.exists() && tempFile.name.startsWith("validation")) {
                tempFile.delete()
            }

            onProgress("Import completed successfully!", 100)
            val summary = importResult.getOrNull()!!
            logJvSync("performLocalImport succeeded for ${localFileUri.path ?: localFileUri}")
            Result.success(
                RestoreResult(
                    success = true,
                    message = "Local import completed successfully",
                    summary = summary,
                    restoreMode = restoreMode
                )
            )
        } catch (e: Exception) {
            log("SyncManager: Local import failed: ${e.message}")
            logJvSync("performLocalImport failed: ${e.message}")
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
        log("SyncManager: Restore process started for user: $userMobile with mode: $restoreMode")
        logJvSync("performRestore initiated for $userMobile (mode=$restoreMode)")
        return try {
            onProgress("Starting restore process...", 0)
            
            // Get current user and store info
            val currentUserId = dataStoreManager.getAdminInfo().first.first()
            val currentStoreId = dataStoreManager.getSelectedStoreInfo().first.first()
            
            if (currentUserId.isEmpty()) {
                return Result.failure(Exception("No current user found. Please login first."))
            }
            if (currentStoreId.isBlank()) {
                return Result.failure(Exception("No store selected. Please set up a store before restoring."))
            }
            if (currentStoreId.isBlank()) {
                return Result.failure(Exception("No store selected. Please set up a store before restoring."))
            }
            
            onProgress("Downloading sync data from cloud...", 20)
            
            // Download sync file from Firebase
            val downloadResult = firebaseBackupManager.downloadLatestBackup(userMobile, currentStoreId)
            
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
            logJvSync("performRestore succeeded for $userMobile with summary: $summary")
            Result.success(result)
            
        } catch (e: Exception) {
            log("SyncManager: Restore process failed: ${e.message}")
            logJvSync("performRestore failed for $userMobile: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Get list of available sync files for a user
     */
    suspend fun getAvailableBackups(userMobile: String): Result<List<BackupInfo>> {
        val storeId = dataStoreManager.getSelectedStoreInfo().first.first()
        if (storeId.isBlank()) {
            return Result.failure(Exception("Store ID not found. Please set up a store first."))
        }
        return firebaseBackupManager.getBackupList(userMobile, storeId)
    }
    
    /**
     * Delete old sync files (keep only latest N syncs)
     */
    suspend fun cleanupOldBackups(userMobile: String, keepCount: Int = 5): Result<Int> {
        val storeId = dataStoreManager.getSelectedStoreInfo().first.first()
        if (storeId.isBlank()) {
            return Result.failure(Exception("Store ID not found. Please set up a store first."))
        }
        return firebaseBackupManager.cleanupOldBackups(userMobile, storeId, keepCount)
    }
    
    private fun createBackupFile(): Uri {
        val baseFileName = FIXED_SYNC_FILE_NAME
        
        log("SyncManager: Creating sync file: $baseFileName")
        logJvSync("SyncManager: createBackupFile generating base filename $baseFileName")

        val resolver = context.contentResolver
        val relativePath = Environment.DIRECTORY_DOWNLOADS + "/JewelVault/Sync"

        val existingUri = findExistingDownload(resolver, baseFileName, relativePath)
        if (existingUri != null) {
            logJvSync("SyncManager: Existing sync file detected at $existingUri; cleaning up before inserting new one")
        }
        purgeExistingDownloads(resolver, baseFileName, relativePath)
        deleteLocalSyncFile(relativePath, baseFileName)

        val uri = insertDownload(resolver, baseFileName, relativePath)
            ?: run {
                val fallbackName = "backup_${System.currentTimeMillis()}.xlsx"
                insertDownload(resolver, fallbackName, relativePath)
                    ?: throw Exception("Unable to create sync file in MediaStore")
            }

        log("SyncManager: Sync file created successfully at: $uri")
        log("SyncManager: File will be saved to: Downloads/JewelVault/Sync/$baseFileName")
        logJvSync("SyncManager: createBackupFile success (uri=$uri) for user/base $baseFileName")

        return uri
    }

    private fun insertDownload(
        resolver: android.content.ContentResolver,
        fileName: String,
        relativePath: String
    ): Uri? {
        logJvSync("SyncManager: insertDownload called for file $fileName at $relativePath")
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            put(MediaStore.Downloads.RELATIVE_PATH, relativePath)
        }
        return try {
            resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        } catch (e: Exception) {
            log("SyncManager: insertDownload failed: ${e.message}")
            logJvSync("SyncManager: insertDownload exception for $fileName - ${e.message}")
            null
        }
    }

    private fun findExistingDownload(
        resolver: android.content.ContentResolver,
        fileName: String,
        relativePath: String
    ): Uri? {
        logJvSync("SyncManager: findExistingDownload checking for $fileName under $relativePath")
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
                logJvSync("SyncManager: Existing download found with id $id for $fileName")
                return Uri.withAppendedPath(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id.toString())
            }
        }
        return null
    }

    private fun purgeExistingDownloads(
        resolver: android.content.ContentResolver,
        fileName: String,
        relativePath: String
    ) {
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
            while (cursor.moveToNext()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                val existing = Uri.withAppendedPath(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id.toString())
                resolver.delete(existing, null, null)
                logJvSync("SyncManager: purgeExistingDownloads deleted $existing")
            }
        }
    }

    private fun deleteLocalSyncFile(relativePath: String, fileName: String) {
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val trimmed = relativePath.removePrefix("${Environment.DIRECTORY_DOWNLOADS}/")
            val targetFolder = File(downloadsDir, trimmed)
            if (targetFolder.exists()) {
                val targetFile = File(targetFolder, fileName)
                if (targetFile.exists() && targetFile.delete()) {
                    logJvSync("SyncManager: deleteLocalSyncFile removed stale file ${targetFile.absolutePath}")
                }
            }
        } catch (e: Exception) {
            logJvSync("SyncManager: deleteLocalSyncFile failed - ${e.message}")
        }
    }

    /**
     * Check if Firebase sync exists for user
     */
    suspend fun checkFirebaseBackupExists(userMobile: String): Result<Boolean> {
        logJvSync("checkFirebaseBackupExists started for $userMobile")
        return try {
            val storeId = dataStoreManager.getSelectedStoreInfo().first.first()
            if (storeId.isBlank()) {
                return Result.success(false)
            }
            val backupList = firebaseBackupManager.getBackupList(userMobile, storeId)
            if (backupList.isSuccess) {
                val backups = backupList.getOrNull() ?: emptyList()
                logJvSync("checkFirebaseBackupExists result for $userMobile: ${backups.isNotEmpty()}")
                Result.success(backups.isNotEmpty())
            } else {
                logJvSync("checkFirebaseBackupExists failed to list backups for $userMobile")
                Result.success(false)
            }
        } catch (e: Exception) {
            log("SyncManager: Error checking Firebase sync: ${e.message}")
            logJvSync("checkFirebaseBackupExists exception for $userMobile: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Validate Excel file for import
     */
    suspend fun validateExcelFile(fileUri: Uri): FileValidationResult {
        return try {
            val fileName = getFileNameFromUri(fileUri)
            logJvSync("SyncManager: validateExcelFile checking $fileName")
            if (!fileName.lowercase().endsWith(".xlsx")) {
                return FileValidationResult(false, "Invalid file format. Please select an Excel (.xlsx) file.")
            }

            val inputStream = context.contentResolver.openInputStream(fileUri)
                ?: return FileValidationResult(false, "Cannot open file. Please check file permissions.")

            val tempFile = File.createTempFile("validation", ".xlsx")
            inputStream.use { input ->
                tempFile.outputStream().use { outputStream ->
                    input.copyTo(outputStream)
                }
            }

            val validationResult = excelImporter.validateExcelStructure(tempFile)
            if (validationResult.isSuccess) {
                logJvSync("SyncManager: validateExcelFile succeeded for $fileName")
                FileValidationResult(true, "File is valid and ready for import.", tempFile)
            } else {
                tempFile.delete()
                logJvSync("SyncManager: validateExcelFile failed for $fileName – ${validationResult.exceptionOrNull()?.message}")
                FileValidationResult(
                    false,
                    validationResult.exceptionOrNull()?.message ?: "Invalid Excel structure."
                )
            }

        } catch (e: Exception) {
            log("SyncManager: File validation failed: ${e.message}")
            logJvSync("SyncManager: validateExcelFile exception for ${fileUri.path ?: fileUri} – ${e.message}")
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
     * Get default sync folder path
     */
    fun getDefaultBackupFolder(): String {
        val downloadsDir = context.getExternalFilesDir(null)?.absolutePath ?: context.filesDir.absolutePath
        return "$downloadsDir/JewelVault/Sync"
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
        log("SyncManager: Restore process started for user: $userMobile with source: $restoreSource, mode: $restoreMode")
        logJvSync("performRestoreWithSource initiated for $userMobile via $restoreSource (mode=$restoreMode)")
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
                    onProgress("Checking cloud sync availability...", 10)
                    
                    // Check if sync exists in Firebase
                    val backupExists = checkFirebaseBackupExists(userMobile)
                    if (backupExists.isFailure) {
                        return Result.failure(Exception("Failed to check cloud sync: ${backupExists.exceptionOrNull()?.message}"))
                    }
                    
                    if (!backupExists.getOrNull()!!) {
                        return Result.failure(Exception("No sync files found in cloud for user: $userMobile"))
                    }
                    
                    onProgress("Downloading sync data from cloud...", 20)
                    
                    // Download sync file from Firebase
                    val downloadResult = firebaseBackupManager.downloadLatestBackup(userMobile, currentStoreId)
                    
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
            if (backupFile.exists() && (backupFile.name.startsWith("validation") || backupFile.name.startsWith("backup_file"))) {
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
            logJvSync("performRestoreWithSource succeeded for $userMobile via $restoreSource: $summary")
            Result.success(result)
            
        } catch (e: Exception) {
            log("SyncManager: Restore process failed: ${e.message}")
            logJvSync("performRestoreWithSource failed for $userMobile: ${e.message}")
            Result.failure(e)
        }
    }
}

/**
 * Data class to hold sync file information
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
