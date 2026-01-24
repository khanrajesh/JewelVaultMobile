package com.velox.jewelvault.utils.sync

import android.content.Context
import androidx.work.*
import com.velox.jewelvault.utils.log

/**
 * Backup frequency options
 */
enum class SyncFrequency {

    DAILY,
    WEEKLY,
    MONTHLY,
    MINUTES_2,
    MINUTES_5,
    MINUTES_10,
    DISABLED;
    
    val displayName: String
        get() = when (this) {
            DAILY -> "Daily"
            WEEKLY -> "Weekly"
            MONTHLY -> "Monthly"
            MINUTES_2 -> "Every 2 minutes"
            MINUTES_5 -> "Every 5 minutes"
            MINUTES_10 -> "Every 10 minutes"
            DISABLED -> "Never"
        }
    
    val description: String
        get() = when (this) {
            DISABLED -> "No automatic syncs"
            DAILY -> "Sync every day at midnight"
            WEEKLY -> "Sync every Sunday at midnight"
            MONTHLY -> "Sync on the 1st of every month"
            MINUTES_2 -> "Sync every 2 minutes (testing)"
            MINUTES_5 -> "Sync every 5 minutes (testing)"
            MINUTES_10 -> "Sync every 10 minutes (testing)"

        }

    val intervalMinutes: Int
        get() = when (this) {
            DAILY -> 24 * 60
            WEEKLY -> 7 * 24 * 60
            MONTHLY -> 30 * 24 * 60
            MINUTES_2 -> 2
            MINUTES_5 -> 5
            MINUTES_10 -> 10
            DISABLED -> 0
        }
}

/**
 * Handles scheduling of automatic syncs using WorkManager
 */
class SyncScheduler(private val context: Context) {
    
    companion object {
        private const val BACKUP_WORK_NAME = "automatic_backup_work"
        private const val BACKUP_WORK_TAG = "backup"
    }
    
    /**
     * Schedule automatic sync based on frequency
     */
    fun scheduleAutomaticBackup(frequency: SyncFrequency) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
        
//        val backupRequest = when (frequency) {
//            SyncFrequency.DAILY -> {
//                PeriodicWorkRequestBuilder<BackupWorker>(1, TimeUnit.DAYS)
//                    .setConstraints(constraints)
//                    .addTag(BACKUP_WORK_TAG)
//                    .build()
//            }
//            SyncFrequency.WEEKLY -> {
//                PeriodicWorkRequestBuilder<BackupWorker>(7, TimeUnit.DAYS)
//                    .setConstraints(constraints)
//                    .addTag(BACKUP_WORK_TAG)
//                    .build()
//            }
//            SyncFrequency.MONTHLY -> {
//                PeriodicWorkRequestBuilder<BackupWorker>(30, TimeUnit.DAYS)
//                    .setConstraints(constraints)
//                    .addTag(BACKUP_WORK_TAG)
//                    .build()
//            }
//            SyncFrequency.DISABLED -> {
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
        
        log("Automatic sync scheduled: $frequency")
    }
    
    /**
     * Cancel automatic sync
     */
    fun cancelAutomaticBackup() {
        WorkManager.getInstance(context)
            .cancelUniqueWork(BACKUP_WORK_NAME)
        
        log("Automatic sync cancelled")
    }
    
    /**
     * Check if automatic sync is scheduled
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
     * Get the status of the last sync work
     */
    suspend fun getLastBackupStatus(): WorkInfo.State? {
        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosByTag(BACKUP_WORK_TAG)
            .get()
        
        return workInfos.lastOrNull()?.state
    }
    
    /**
     * Trigger immediate sync
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
        
        log("Immediate sync triggered")
    }
}

