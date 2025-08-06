package com.velox.jewelvault.utils.backup

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.velox.jewelvault.MainActivity
import com.velox.jewelvault.R
import com.velox.jewelvault.utils.log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

/**
 * Foreground service for handling backup and restore operations
 */
@AndroidEntryPoint
class BackupService : Service() {
    
    @Inject
    lateinit var database: com.velox.jewelvault.data.roomdb.AppDatabase
    
    @Inject
    lateinit var storage: com.google.firebase.storage.FirebaseStorage
    
    @Inject
    lateinit var dataStoreManager: com.velox.jewelvault.data.DataStoreManager
    
    private lateinit var backupManager: BackupManager
    private var serviceJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    companion object {
        private const val CHANNEL_ID = "backup_service_channel"
        private const val CHANNEL_NAME = "Backup Service"
        private const val NOTIFICATION_ID = 1001
        private const val COMPLETION_NOTIFICATION_ID = 1002
        
        const val ACTION_BACKUP = "com.velox.jewelvault.BACKUP"
        const val ACTION_RESTORE = "com.velox.jewelvault.RESTORE"
        
        // SharedFlow for progress updates
        private val _progressFlow = MutableSharedFlow<BackupProgress>()
        val progressFlow = _progressFlow.asSharedFlow()
        
        data class BackupProgress(
            val message: String,
            val progress: Int,
            val isComplete: Boolean = false,
            val isSuccess: Boolean = false
        )
        
        fun startBackup(context: android.content.Context) {
            val intent = Intent(context, BackupService::class.java).apply {
                action = ACTION_BACKUP
            }
            context.startForegroundService(intent)
        }
        
        fun startRestore(context: android.content.Context, userMobile: String) {
            val intent = Intent(context, BackupService::class.java).apply {
                action = ACTION_RESTORE
                putExtra("userMobile", userMobile)
            }
            context.startForegroundService(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        backupManager = BackupManager(this, database, storage, dataStoreManager)
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_BACKUP -> {
                startForeground(NOTIFICATION_ID, createNotification("Starting backup...", 0))
                performBackup()
            }
            ACTION_RESTORE -> {
                val userMobile = intent.getStringExtra("userMobile") ?: ""
                startForeground(NOTIFICATION_ID, createNotification("Starting restore...", 0))
                performRestore(userMobile)
            }
        }
        
        return START_NOT_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        serviceJob?.cancel()
        serviceScope.cancel()
    }
    
    private fun performBackup() {
        log("BackupService: performBackup started")
        serviceJob = serviceScope.launch {
            try {
                val result = backupManager.performBackup { message, progress ->
                    updateNotification(message, progress)
                }
                
                if (result.isSuccess) {
                    showCompletionNotification("Backup completed successfully!", true)
                    // Send completion update via SharedFlow
                    CoroutineScope(Dispatchers.Main).launch {
                        try {
                            _progressFlow.emit(BackupProgress("Backup completed successfully!", 100, true, true))
                            log("BackupService: Completion update sent via SharedFlow - success: true")
                        } catch (e: Exception) {
                            log("BackupService: Error sending completion update: ${e.message}")
                        }
                    }
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    showCompletionNotification("Backup failed: $error", false)
                    // Send completion update via SharedFlow
                    CoroutineScope(Dispatchers.Main).launch {
                        try {
                            _progressFlow.emit(BackupProgress("Backup failed: $error", 100, true, false))
                            log("BackupService: Completion update sent via SharedFlow - success: false")
                        } catch (e: Exception) {
                            log("BackupService: Error sending completion update: ${e.message}")
                        }
                    }
                }
                
            } catch (e: Exception) {
                log("Backup service error: ${e.message}")
                showCompletionNotification("Backup failed: ${e.message}", false)
                sendBroadcast(Intent("com.velox.jewelvault.BACKUP_COMPLETED").apply {
                    putExtra("success", false)
                    putExtra("message", "Backup failed: ${e.message}")
                })
            } finally {
                log("BackupService: performBackup finished")
                // Add a delay to ensure completion notification is visible
                delay(2000) // 2 seconds delay
                stopSelf()
            }
        }
    }
    
    private fun performRestore(userMobile: String) {
        log("BackupService: performRestore started for user: $userMobile")
        serviceJob = serviceScope.launch {
            try {
                val result = backupManager.performRestore(userMobile) { message, progress ->
                    updateRestoreNotification(message, progress)
                }
                
                if (result.isSuccess) {
                    showCompletionNotification("Restore completed successfully!", true)
                    sendBroadcast(Intent("com.velox.jewelvault.RESTORE_COMPLETED").apply {
                        putExtra("success", true)
                        putExtra("message", "Restore completed successfully!")
                    })
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    showCompletionNotification("Restore failed: $error", false)
                    sendBroadcast(Intent("com.velox.jewelvault.RESTORE_COMPLETED").apply {
                        putExtra("success", false)
                        putExtra("message", "Restore failed: $error")
                    })
                }
                
            } catch (e: Exception) {
                log("Restore service error: ${e.message}")
                showCompletionNotification("Restore failed: ${e.message}", false)
                sendBroadcast(Intent("com.velox.jewelvault.RESTORE_COMPLETED").apply {
                    putExtra("success", false)
                    putExtra("message", "Restore failed: ${e.message}")
                })
            } finally {
                log("BackupService: performRestore finished")
                // Add a delay to ensure completion notification is visible
                delay(2000) // 2 seconds delay
                stopSelf()
            }
        }
    }
    
    private fun createNotificationChannel() {
        log("BackupService: Creating notification channel: $CHANNEL_ID")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT  // Changed from LOW to DEFAULT for better visibility
            ).apply {
                description = "Channel for backup and restore operations"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            log("BackupService: Notification channel created successfully")
        } else {
            log("BackupService: Notification channel not needed for API level < 26")
        }
    }
    
    private fun createNotification(message: String, progress: Int): Notification {
        log("BackupService: Creating notification with message: '$message', progress: $progress")
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("JewelVault Backup")
            .setContentText(message)
            .setSmallIcon(R.drawable.logo_1) // Changed to logo_1 which should exist
            .setContentIntent(pendingIntent)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // Added priority
            .build()
        
        log("BackupService: Notification created successfully")
        return notification
    }
    
    private fun updateNotification(message: String, progress: Int) {
        log("BackupService: Updating notification with message: '$message', progress: $progress")
        val notification = createNotification(message, progress)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
        log("BackupService: Notification updated successfully")
        
        // Send progress update via SharedFlow
        CoroutineScope(Dispatchers.Main).launch {
            try {
                _progressFlow.emit(BackupProgress(message, progress))
                log("BackupService: Progress update sent via SharedFlow - message: '$message', progress: $progress")
            } catch (e: Exception) {
                log("BackupService: Error sending progress update: ${e.message}")
            }
        }
    }
    
    private fun updateRestoreNotification(message: String, progress: Int) {
        log("BackupService: Updating restore notification with message: '$message', progress: $progress")
        val notification = createNotification(message, progress)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
        log("BackupService: Restore notification updated successfully")
        
        // Send progress broadcast to update UI
        val broadcastIntent = Intent("com.velox.jewelvault.RESTORE_PROGRESS").apply {
            putExtra("message", message)
            putExtra("progress", progress)
        }
        log("BackupService: Sending RESTORE_PROGRESS broadcast - message: '$message', progress: $progress")
        sendBroadcast(broadcastIntent)
        log("BackupService: RESTORE_PROGRESS broadcast sent successfully")
    }
    
    private fun showCompletionNotification(message: String, isSuccess: Boolean) {
        log("BackupService: Showing completion notification - success: $isSuccess, message: '$message'")
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("JewelVault Backup")
            .setContentText(message)
            .setSmallIcon(R.drawable.logo_1) // Use the same logo icon
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOngoing(false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // Added priority
            .build()
        
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(COMPLETION_NOTIFICATION_ID, notification)
        log("BackupService: Completion notification shown successfully")
    }
}