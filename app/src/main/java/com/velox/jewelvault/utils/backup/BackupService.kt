package com.velox.jewelvault.utils.backup

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.firebase.storage.FirebaseStorage
import com.velox.jewelvault.MainActivity
import com.velox.jewelvault.R
import com.velox.jewelvault.data.DataStoreManager
import com.velox.jewelvault.data.roomdb.AppDatabase
import com.velox.jewelvault.utils.log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * Foreground service for handling backup and restore operations
 */
@AndroidEntryPoint
class BackupService : Service() {
    
    @Inject
    lateinit var database: AppDatabase
    
    @Inject
    lateinit var storage: FirebaseStorage
    
    @Inject
    lateinit var dataStoreManager: DataStoreManager
    
    private lateinit var backupManager: BackupManager
    private var serviceJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    companion object {
        const val ACTION_BACKUP = "com.velox.jewelvault.BACKUP"
        const val ACTION_RESTORE = "com.velox.jewelvault.RESTORE"
        const val EXTRA_USER_MOBILE = "user_mobile"
        
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "backup_service_channel"
        private const val CHANNEL_NAME = "Backup Service"
        
        fun startBackup(context: Context) {
            val intent = Intent(context, BackupService::class.java).apply {
                action = ACTION_BACKUP
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun startRestore(context: Context, userMobile: String) {
            val intent = Intent(context, BackupService::class.java).apply {
                action = ACTION_RESTORE
                putExtra(EXTRA_USER_MOBILE, userMobile)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
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
                val userMobile = intent.getStringExtra(EXTRA_USER_MOBILE) ?: ""
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
        serviceJob = serviceScope.launch {
            try {
                val result = backupManager.performBackup { message, progress ->
                    updateNotification(message, progress)
                }
                
                if (result.isSuccess) {
                    showCompletionNotification("Backup completed successfully!", true)
                    sendBroadcast(Intent("com.velox.jewelvault.BACKUP_COMPLETED").apply {
                        putExtra("success", true)
                        putExtra("message", "Backup completed successfully!")
                    })
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    showCompletionNotification("Backup failed: $error", false)
                    sendBroadcast(Intent("com.velox.jewelvault.BACKUP_COMPLETED").apply {
                        putExtra("success", false)
                        putExtra("message", "Backup failed: $error")
                    })
                }
                
            } catch (e: Exception) {
                log("Backup service error: ${e.message}")
                showCompletionNotification("Backup failed: ${e.message}", false)
                sendBroadcast(Intent("com.velox.jewelvault.BACKUP_COMPLETED").apply {
                    putExtra("success", false)
                    putExtra("message", "Backup failed: ${e.message}")
                })
            } finally {
                stopSelf()
            }
        }
    }
    
    private fun performRestore(userMobile: String) {
        serviceJob = serviceScope.launch {
            try {
                val result = backupManager.performRestore(userMobile) { message, progress ->
                    updateNotification(message, progress)
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
                stopSelf()
            }
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Channel for backup and restore operations"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(message: String, progress: Int): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("JewelVault Backup")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Make sure this icon exists
            .setContentIntent(pendingIntent)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
    }
    
    private fun updateNotification(message: String, progress: Int) {
        val notification = createNotification(message, progress)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun showCompletionNotification(message: String, isSuccess: Boolean) {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("JewelVault Backup")
            .setContentText(message)
            .setSmallIcon(if (isSuccess) android.R.drawable.ic_dialog_info else android.R.drawable.ic_dialog_alert)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOngoing(false)
            .build()
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }
}