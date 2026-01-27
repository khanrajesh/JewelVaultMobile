package com.velox.jewelvault.utils.sync

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.velox.jewelvault.utils.log
import com.velox.jewelvault.utils.logJvSync
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.*

/**
 * Manages sync operations with Firebase Storage
 */
class FirebaseBackupManager(private val storage: FirebaseStorage) {
    
    companion object {
        private const val BACKUP_FOLDER = "database_backups"
        private const val MAX_BACKUP_AGE_DAYS = 30
    }
    
    /**
     * Upload sync file to Firebase Storage
     */
    suspend fun uploadBackupFile(backupFile: File, userMobile: String, storeId: String): Result<String> {
        log("FirebaseBackupManager: Starting upload for user: $userMobile, store: $storeId, file: ${backupFile.name}")
        logJvSync("FirebaseBackupManager upload started for ${backupFile.name}")
        return try {
            // Delete previous sync files for this user
            cleanupPreviousBackups(userMobile, storeId)
            
            val fileName = backupFile.name
            val storageRef = storage.reference
                .child(BACKUP_FOLDER)
                .child(userMobile)
                .child(storeId)
                .child(fileName)
            
            // Upload file
            val uploadTask = storageRef.putFile(Uri.fromFile(backupFile)).await()
            val downloadUrl = storageRef.downloadUrl.await()
            
            log("Sync uploaded successfully: ${downloadUrl}")
            log("FirebaseBackupManager: Upload completed successfully")
            logJvSync("FirebaseBackupManager upload succeeded: $downloadUrl")
            Result.success(downloadUrl.toString())
            
        } catch (e: Exception) {
            log("FirebaseBackupManager: Upload failed: ${e.message}")
            logJvSync("FirebaseBackupManager upload failed: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Download latest sync file from Firebase Storage
     */
    suspend fun downloadLatestBackup(userMobile: String, storeId: String): Result<File> {
        log("FirebaseBackupManager: Starting download for user: $userMobile, store: $storeId")
        logJvSync("FirebaseBackupManager download started for user $userMobile")
        return try {
            val userBackupRef = storage.reference
                .child(BACKUP_FOLDER)
                .child(userMobile)
                .child(storeId)
            
            // List all sync files for the user
            val listResult = userBackupRef.listAll().await()
            
            if (listResult.items.isEmpty()) {
                return Result.failure(Exception("No sync files found for user: $userMobile"))
            }
            
            // Find the latest sync file (by name timestamp)
            val latestBackup = listResult.items
                .sortedByDescending { it.name }
                .firstOrNull()
                ?: return Result.failure(Exception("No sync files found"))
            
            // Create temporary file for download
            val tempFile = File.createTempFile("backup_file", ".xlsx")
            
            // Download the file
            latestBackup.getFile(tempFile).await()
            
            log("Sync downloaded successfully: ${tempFile.absolutePath}")
            log("FirebaseBackupManager: Download completed successfully")
            logJvSync("FirebaseBackupManager download succeeded: ${tempFile.absolutePath}")
            Result.success(tempFile)
            
        } catch (e: Exception) {
            log("FirebaseBackupManager: Download failed: ${e.message}")
            logJvSync("FirebaseBackupManager download failed: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Get list of available sync files for a user
     */
    suspend fun getBackupList(userMobile: String, storeId: String): Result<List<BackupInfo>> {
        logJvSync("FirebaseBackupManager getBackupList started for $userMobile")
        return try {
            val userBackupRef = storage.reference
                .child(BACKUP_FOLDER)
                .child(userMobile)
                .child(storeId)
            
            val listResult = userBackupRef.listAll().await()
            
            val backupInfoList = mutableListOf<BackupInfo>()
            
            for (item in listResult.items) {
                try {
                    val metadata = item.metadata.await()
                    val downloadUrl = item.downloadUrl.await()
                    
                    val backupInfo = BackupInfo(
                        fileName = item.name,
                        uploadDate = Date(metadata.creationTimeMillis),
                        fileSize = metadata.sizeBytes,
                        downloadUrl = downloadUrl.toString(),
                        description = ""
                    )
                    
                    backupInfoList.add(backupInfo)
                } catch (e: Exception) {
                    log("Error getting metadata for sync file ${item.name}: ${e.message}")
                }
            }
            
            // Sort by upload date (newest first)
            val sortedList = backupInfoList.sortedByDescending { it.uploadDate }
            logJvSync("FirebaseBackupManager getBackupList found ${sortedList.size} items for $userMobile")
            
            Result.success(sortedList)
            
        } catch (e: Exception) {
            log("Failed to get sync list: ${e.message}")
            logJvSync("FirebaseBackupManager getBackupList failed for $userMobile: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Clean up old sync files (keep only the specified number of recent syncs)
     */
    suspend fun cleanupOldBackups(userMobile: String, storeId: String, keepCount: Int): Result<Int> {
        logJvSync("FirebaseBackupManager cleanupOldBackups started for $userMobile keepCount=$keepCount")
        return try {
            val userBackupRef = storage.reference
                .child(BACKUP_FOLDER)
                .child(userMobile)
                .child(storeId)
            
            val listResult = userBackupRef.listAll().await()
            
            if (listResult.items.size <= keepCount) {
                return Result.success(0) // No cleanup needed
            }
            
            // Sort by name (which includes timestamp) and keep only the latest ones
            val sortedItems = listResult.items.sortedByDescending { it.name }
            val itemsToDelete = sortedItems.drop(keepCount)
            
            var deletedCount = 0
            for (item in itemsToDelete) {
                try {
                    item.delete().await()
                    deletedCount++
                    log("Deleted old sync file: ${item.name}")
                } catch (e: Exception) {
                    log("Failed to delete sync file ${item.name}: ${e.message}")
                }
            }
            
            log("Cleanup completed. Deleted $deletedCount old sync files.")
            logJvSync("FirebaseBackupManager cleanupOldBackups deleted $deletedCount files")
            Result.success(deletedCount)
            
        } catch (e: Exception) {
            log("Failed to cleanup old syncs: ${e.message}")
            logJvSync("FirebaseBackupManager cleanupOldBackups failed: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Delete all previous sync files for a user before uploading new one
     */
    private suspend fun cleanupPreviousBackups(userMobile: String, storeId: String) {
        try {
            logJvSync("FirebaseBackupManager cleanupPreviousBackups started for $userMobile")
            val userBackupRef = storage.reference
                .child(BACKUP_FOLDER)
                .child(userMobile)
                .child(storeId)
            
            val listResult = userBackupRef.listAll().await()
            
            // Delete all existing sync files
            for (item in listResult.items) {
                try {
                    item.delete().await()
                    log("Deleted previous sync file: ${item.name}")
                    logJvSync("Deleted previous sync file: ${item.name}")
                } catch (e: Exception) {
                    log("Failed to delete previous sync file ${item.name}: ${e.message}")
                    logJvSync("Failed to delete previous sync file ${item.name}: ${e.message}")
                }
            }
            
        } catch (e: Exception) {
            log("Failed to cleanup previous syncs: ${e.message}")
            logJvSync("FirebaseBackupManager cleanupPreviousBackups failed: ${e.message}")
        }
    }
    
    /**
     * Delete a specific sync file
     */
    suspend fun deleteBackup(userMobile: String, storeId: String, fileName: String): Result<Unit> {
        logJvSync("FirebaseBackupManager deleteBackup requested: $fileName for $userMobile")
        return try {
            val backupRef = storage.reference
                .child(BACKUP_FOLDER)
                .child(userMobile)
                .child(storeId)
                .child(fileName)
            
            backupRef.delete().await()
            
            log("Sync file deleted successfully: $fileName")
            logJvSync("FirebaseBackupManager deleteBackup succeeded: $fileName")
            Result.success(Unit)
            
        } catch (e: Exception) {
            log("Failed to delete sync file: ${e.message}")
            logJvSync("FirebaseBackupManager deleteBackup failed: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Check if sync exists for a user
     */
    suspend fun hasBackup(userMobile: String, storeId: String): Boolean {
        logJvSync("FirebaseBackupManager hasBackup check started for $userMobile")
        return try {
            val userBackupRef = storage.reference
                .child(BACKUP_FOLDER)
                .child(userMobile)
                .child(storeId)
             
            val listResult = userBackupRef.listAll().await()
            val exists = listResult.items.isNotEmpty()
            logJvSync("FirebaseBackupManager hasBackup result for $userMobile: $exists")
            exists
            
        } catch (e: Exception) {
            log("Failed to check sync existence: ${e.message}")
            logJvSync("FirebaseBackupManager hasBackup failed: ${e.message}")
            false
        }
    }
}
