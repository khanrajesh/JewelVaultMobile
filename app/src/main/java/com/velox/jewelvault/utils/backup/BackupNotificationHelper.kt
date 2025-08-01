package com.velox.jewelvault.utils.backup

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.velox.jewelvault.R

/**
 * Helper class for backup/restore notifications
 */
object BackupNotificationHelper {
    
    private const val CHANNEL_ID = "backup_channel"
    private const val CHANNEL_NAME = "Backup & Restore"
    private const val CHANNEL_DESCRIPTION = "Notifications for backup and restore operations"
    
    const val BACKUP_NOTIFICATION_ID = 1001
    const val RESTORE_NOTIFICATION_ID = 1002
    
    /**
     * Create notification channel for backup operations
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = CHANNEL_DESCRIPTION
                setShowBadge(false)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Create backup progress notification
     */
    fun createBackupNotification(
        context: Context,
        title: String,
        message: String,
        progress: Int,
        isIndeterminate: Boolean = false
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setProgress(100, progress, isIndeterminate)
            .setOngoing(true)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
    }
    
    /**
     * Create restore progress notification
     */
    fun createRestoreNotification(
        context: Context,
        title: String,
        message: String,
        progress: Int,
        isIndeterminate: Boolean = false
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, isIndeterminate)
            .setOngoing(true)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
    }
    
    /**
     * Create completion notification
     */
    fun createCompletionNotification(
        context: Context,
        title: String,
        message: String,
        isSuccess: Boolean
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(if (isSuccess) android.R.drawable.stat_notify_sync else android.R.drawable.stat_notify_error)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
    }
    
    /**
     * Update notification progress
     */
    fun updateNotificationProgress(
        context: Context,
        notificationId: Int,
        notification: NotificationCompat.Builder,
        progress: Int,
        message: String
    ) {
        notification
            .setProgress(100, progress, false)
            .setContentText(message)
        
//        with(NotificationManagerCompat.from(context)) {
//            notify(notificationId, notification.build())
//        }
    }
    
    /**
     * Cancel notification
     */
    fun cancelNotification(context: Context, notificationId: Int) {
        with(NotificationManagerCompat.from(context)) {
            cancel(notificationId)
        }
    }
}