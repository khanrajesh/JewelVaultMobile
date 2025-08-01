package com.velox.jewelvault.utils.backup

import android.content.Context
import androidx.work.*
import com.velox.jewelvault.utils.log
import java.util.concurrent.TimeUnit

/**
 * Backup frequency options
 */
enum class BackupFrequency {

    DAILY,
    WEEKLY,
    MONTHLY,
    DISABLED;
    
    val displayName: String
        get() = when (this) {
            DAILY -> "Daily"
            WEEKLY -> "Weekly"
            MONTHLY -> "Monthly"
            DISABLED -> "Never"
        }
    
    val description: String
        get() = when (this) {
            DISABLED -> "No automatic backups"
            DAILY -> "Backup every day at midnight"
            WEEKLY -> "Backup every Sunday at midnight"
            MONTHLY -> "Backup on the 1st of every month"

        }
}

/**
 * Handles scheduling of automatic backups using WorkManager
 */
class BackupScheduler(private val context: Context) {
    
    companion object {
        private const val BACKUP_WORK_NAME = "automatic_backup_work"
        private const val BACKUP_WORK_TAG = "backup"
    }
    
    /**
     * Schedule automatic backup based on frequency
     */
    fun scheduleAutomaticBackup(frequency: BackupFrequency) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
        
//        val backupRequest = when (frequency) {
//            BackupFrequency.DAILY -> {
//                PeriodicWorkRequestBuilder<BackupWorker>(1, TimeUnit.DAYS)
//                    .setConstraints(constraints)
//                    .addTag(BACKUP_WORK_TAG)
//                    .build()
//            }
//            BackupFrequency.WEEKLY -> {
//                PeriodicWorkRequestBuilder<BackupWorker>(7, TimeUnit.DAYS)
//                    .setConstraints(constraints)
//                    .addTag(BACKUP_WORK_TAG)
//                    .build()
//            }
//            BackupFrequency.MONTHLY -> {
//                PeriodicWorkRequestBuilder<BackupWorker>(30, TimeUnit.DAYS)
//                    .setConstraints(constraints)
//                    .addTag(BACKUP_WORK_TAG)
//                    .build()
//            }
//            BackupFrequency.DISABLED -> {
//                cancelAutomaticBackup()
//                return
//            }
//
//        }
        
//        WorkManager.getInstance(context)
//            .enqueueUniquePeriodicWork(
//                BACKUP_WORK_NAME,
//                ExistingPeriodicWorkPolicy.REPLACE,
//                backupRequest
//            )
        
        log("Automatic backup scheduled: $frequency")
    }
    
    /**
     * Cancel automatic backup
     */
    fun cancelAutomaticBackup() {
        WorkManager.getInstance(context)
            .cancelUniqueWork(BACKUP_WORK_NAME)
        
        log("Automatic backup cancelled")
    }
    
    /**
     * Check if automatic backup is scheduled
     */
    suspend fun isAutomaticBackupScheduled(): Boolean {
        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(BACKUP_WORK_NAME)
            .get()
        
        return workInfos.any { workInfo ->
            workInfo.state == WorkInfo.State.ENQUEUED || workInfo.state == WorkInfo.State.RUNNING
        }
    }
    
    /**
     * Get the status of the last backup work
     */
    suspend fun getLastBackupStatus(): WorkInfo.State? {
        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosByTag(BACKUP_WORK_TAG)
            .get()
        
        return workInfos.lastOrNull()?.state
    }
    
    /**
     * Trigger immediate backup
     */
    fun triggerImmediateBackup() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
//        val immediateBackupRequest = OneTimeWorkRequestBuilder<BackupWorker>()
//            .setConstraints(constraints)
//            .addTag(BACKUP_WORK_TAG)
//            .build()
//
//        WorkManager.getInstance(context)
//            .enqueue(immediateBackupRequest)
        
        log("Immediate backup triggered")
    }
}

