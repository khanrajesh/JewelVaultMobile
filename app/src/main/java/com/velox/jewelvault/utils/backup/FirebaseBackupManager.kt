package com.velox.jewelvault.utils.backup

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.velox.jewelvault.utils.log
import kotlinx.coroutines.tasks.await
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Manages backup operations with Firebase Storage
 */
class FirebaseBackupManager(private val storage: FirebaseStorage) {
    
    companion object {
        private const val BACKUP_FOLDER = "database_backups"
        private const val MAX_BACKUP_AGE_DAYS = 30
    }
    
    /**
     * Upload backup file to Firebase Storage
     */
    suspend fun uploadBackupFile(backupFile: File, userMobile: String): Result<String> {
        return try {
            // Delete previous backup files for this user
            cleanupPreviousBackups(userMobile)
            
            val fileName = backupFile.name
            val storageRef = storage.reference
                .child(BACKUP_FOLDER)
                .child(userMobile)
                .child(fileName)
            
            // Upload file
            val uploadTask = storageRef.putFile(Uri.fromFile(backupFile)).await()
            val downloadUrl = storageRef.downloadUrl.await()
            
            log("Backup uploaded successfully: ${downloadUrl}")
            Result.success(downloadUrl.toString())
            
        } catch (e: Exception) {
            log("Failed to upload backup: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Download latest backup file from Firebase Storage
     */
    suspend fun downloadLatestBackup(userMobile: String): Result<File> {
        return try {
            val userBackupRef = storage.reference
                .child(BACKUP_FOLDER)
                .child(userMobile)
            
            // List all backup files for the user
            val listResult = userBackupRef.listAll().await()
            
            if (listResult.items.isEmpty()) {
                return Result.failure(Exception("No backup files found for user: $userMobile"))
            }
            
            // Find the latest backup file (by name timestamp)
            val latestBackup = listResult.items
                .sortedByDescending { it.name }
                .firstOrNull()
                ?: return Result.failure(Exception("No backup files found"))
            
            // Create temporary file for download
            val tempFile = File.createTempFile("backup_download", ".xlsx")
            
            // Download the file
            latestBackup.getFile(tempFile).await()
            
            log("Backup downloaded successfully: ${tempFile.absolutePath}")
            Result.success(tempFile)
            
        } catch (e: Exception) {
            log("Failed to download backup: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Get list of available backups for a user
     */
    suspend fun getBackupList(userMobile: String): Result<List<BackupInfo>> {
        return try {
            val userBackupRef = storage.reference
                .child(BACKUP_FOLDER)
                .child(userMobile)
            
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
                    log("Error getting metadata for backup file ${item.name}: ${e.message}")
                }
            }
            
            // Sort by upload date (newest first)
            val sortedList = backupInfoList.sortedByDescending { it.uploadDate }
            
            Result.success(sortedList)
            
        } catch (e: Exception) {
            log("Failed to get backup list: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Clean up old backup files (keep only the specified number of recent backups)
     */
    suspend fun cleanupOldBackups(userMobile: String, keepCount: Int): Result<Int> {
        return try {
            val userBackupRef = storage.reference
                .child(BACKUP_FOLDER)
                .child(userMobile)
            
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
                    log("Deleted old backup: ${item.name}")
                } catch (e: Exception) {
                    log("Failed to delete backup ${item.name}: ${e.message}")
                }
            }
            
            log("Cleanup completed. Deleted $deletedCount old backup files.")
            Result.success(deletedCount)
            
        } catch (e: Exception) {
            log("Failed to cleanup old backups: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Delete all previous backup files for a user before uploading new one
     */
    private suspend fun cleanupPreviousBackups(userMobile: String) {
        try {
            val userBackupRef = storage.reference
                .child(BACKUP_FOLDER)
                .child(userMobile)
            
            val listResult = userBackupRef.listAll().await()
            
            // Delete all existing backup files
            for (item in listResult.items) {
                try {
                    item.delete().await()
                    log("Deleted previous backup: ${item.name}")
                } catch (e: Exception) {
                    log("Failed to delete previous backup ${item.name}: ${e.message}")
                }
            }
            
        } catch (e: Exception) {
            log("Failed to cleanup previous backups: ${e.message}")
        }
    }
    
    /**
     * Delete a specific backup file
     */
    suspend fun deleteBackup(userMobile: String, fileName: String): Result<Unit> {
        return try {
            val backupRef = storage.reference
                .child(BACKUP_FOLDER)
                .child(userMobile)
                .child(fileName)
            
            backupRef.delete().await()
            
            log("Backup deleted successfully: $fileName")
            Result.success(Unit)
            
        } catch (e: Exception) {
            log("Failed to delete backup: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Check if backup exists for a user
     */
    suspend fun hasBackup(userMobile: String): Boolean {
        return try {
            val userBackupRef = storage.reference
                .child(BACKUP_FOLDER)
                .child(userMobile)
            
            val listResult = userBackupRef.listAll().await()
            listResult.items.isNotEmpty()
            
        } catch (e: Exception) {
            log("Failed to check backup existence: ${e.message}")
            false
        }
    }
}