package com.velox.jewelvault.utils.sync

import android.app.*
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.compose.material.MaterialTheme
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.toArgb
import com.velox.jewelvault.MainActivity
import com.velox.jewelvault.R
import com.velox.jewelvault.data.DataStoreManager
import com.velox.jewelvault.data.metalRates
import com.velox.jewelvault.data.remort.RepositoryImpl
import com.velox.jewelvault.ui.theme.primaryLight
import com.velox.jewelvault.utils.log
import com.velox.jewelvault.utils.logJvSync
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Foreground service for handling sync and restore operations
 */
@AndroidEntryPoint
class SyncService : Service() {
    
    @Inject
    lateinit var database: com.velox.jewelvault.data.roomdb.AppDatabase
    
    @Inject
    lateinit var storage: com.google.firebase.storage.FirebaseStorage
    
    @Inject
    lateinit var dataStoreManager: com.velox.jewelvault.data.DataStoreManager

    @Inject
    lateinit var firestore: com.google.firebase.firestore.FirebaseFirestore

    @Inject
    lateinit var metalRatesRepository: RepositoryImpl
    
    @Inject
    lateinit var syncManager: SyncManager
    private var serviceJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var monitorJob: Job? = null
    private var metalRatesJob: Job? = null
    private var activeDeviceJob: Job? = null
    private var autoSyncJob: Job? = null
    private var monitorActive: Boolean = false
    private var syncInProgress: Boolean = false
    private var syncProgress: Int = 0
    private var syncMessage: String = ""
    private var gold24kRate: Double? = null
    private var silverKgRate: Double? = null
    private var lastSyncAt: Long? = null
    private var lastSyncDevice: String? = null
    private var activeDeviceLabel: String? = null
    private var activeDeviceAt: Long? = null
    
    companion object {
        private const val CHANNEL_ID = "sync_service_channel"
        private const val CHANNEL_NAME = "Sync Service"
        private const val NOTIFICATION_ID = 1001
        private const val COMPLETION_NOTIFICATION_ID = 1002
        
        const val ACTION_BACKUP = "com.velox.jewelvault.BACKUP"
        const val ACTION_RESTORE = "com.velox.jewelvault.RESTORE"
        const val ACTION_START_MONITOR = "com.velox.jewelvault.START_MONITOR"
        const val ACTION_STOP_MONITOR = "com.velox.jewelvault.STOP_MONITOR"
        const val ACTION_REFRESH_METAL_RATES = "com.velox.jewelvault.REFRESH_METAL_RATES"
        
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
            val intent = Intent(context, SyncService::class.java).apply {
                action = ACTION_BACKUP
            }
            logJvSync("SyncService startBackup requested")
            context.startForegroundService(intent)
        }

        fun startMonitoring(context: android.content.Context) {
            val intent = Intent(context, SyncService::class.java).apply {
                action = ACTION_START_MONITOR
            }
            logJvSync("SyncService startMonitoring requested")
            context.startForegroundService(intent)
        }

        fun stopMonitoring(context: android.content.Context) {
            val intent = Intent(context, SyncService::class.java).apply {
                action = ACTION_STOP_MONITOR
            }
            logJvSync("SyncService stopMonitoring requested")
            context.startService(intent)
        }
        
        fun startRestore(context: android.content.Context, userMobile: String, restoreMode: RestoreMode = RestoreMode.MERGE) {
            val intent = Intent(context, SyncService::class.java).apply {
                action = ACTION_RESTORE
                putExtra("userMobile", userMobile)
                putExtra("restoreMode", restoreMode.name)
            }
            context.startForegroundService(intent)
        }

        fun startRestoreWithSource(
            context: android.content.Context,
            userMobile: String,
            restoreSource: RestoreSource,
            localFileUri: Uri? = null,
            restoreMode: RestoreMode = RestoreMode.MERGE
        ) {
            log("SyncService: startRestoreWithSource called for user: $userMobile with source: $restoreSource, mode: $restoreMode")
            logJvSync("SyncService startRestoreWithSource requested for $userMobile via $restoreSource")
            val intent = Intent(context, SyncService::class.java).apply {
                action = "START_RESTORE_WITH_SOURCE"
                putExtra("userMobile", userMobile)
                putExtra("restoreSource", restoreSource.name)
                putExtra("localFileUri", localFileUri?.toString())
                putExtra("restoreMode", restoreMode.name)
            }
            context.startForegroundService(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            startMonitoringInternal()
            return START_STICKY
        }
        when (intent.action) {
            ACTION_BACKUP -> {
                logJvSync("SyncService received ACTION_BACKUP")
                ensureForeground()
                performBackup()
            }
            ACTION_RESTORE -> {
                logJvSync("SyncService received ACTION_RESTORE")
                val userMobile = intent.getStringExtra("userMobile") ?: ""
                val restoreModeStr = intent.getStringExtra("restoreMode") ?: RestoreMode.MERGE.name
                val restoreMode = RestoreMode.valueOf(restoreModeStr)
                ensureForeground()
                performRestore(userMobile, restoreMode)
            }
            "START_RESTORE_WITH_SOURCE" -> {
                logJvSync("SyncService received START_RESTORE_WITH_SOURCE")
                val userMobile = intent.getStringExtra("userMobile") ?: ""
                val restoreSourceStr = intent.getStringExtra("restoreSource") ?: RestoreSource.FIREBASE.name
                val restoreSource = RestoreSource.valueOf(restoreSourceStr)
                val localFileUriStr = intent.getStringExtra("localFileUri")
                val localFileUri = if (localFileUriStr != null) Uri.parse(localFileUriStr) else null
                val restoreModeStr = intent.getStringExtra("restoreMode") ?: RestoreMode.MERGE.name
                val restoreMode = RestoreMode.valueOf(restoreModeStr)
                
                ensureForeground()
                performRestoreWithSource(userMobile, restoreSource, localFileUri, restoreMode)
            }
            ACTION_START_MONITOR -> {
                logJvSync("SyncService received ACTION_START_MONITOR")
                startMonitoringInternal()
            }
            ACTION_STOP_MONITOR -> {
                logJvSync("SyncService received ACTION_STOP_MONITOR")
                stopMonitoringInternal()
            }
            ACTION_REFRESH_METAL_RATES -> {
                logJvSync("SyncService received ACTION_REFRESH_METAL_RATES")
                serviceScope.launch {
                    refreshMetalRates()
                    updateMonitorNotification()
                }
            }
        }
        
        return if (monitorActive) START_STICKY else START_NOT_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        serviceJob?.cancel()
        serviceScope.cancel()
        monitorJob?.cancel()
        metalRatesJob?.cancel()
        activeDeviceJob?.cancel()
        autoSyncJob?.cancel()
    }
    
    private fun performBackup() {
        log("SyncService: performBackup started")
        logJvSync("SyncService performBackup initiated")
        if (syncInProgress) {
            log("SyncService: performBackup skipped - sync already in progress")
            return
        }
        serviceJob = serviceScope.launch {
            try {
                setSyncProgress("Starting sync...", 0)
                updateMonitorNotification()
                val result = syncManager.performBackup { message, progress ->
                    updateNotification(message, progress)
                }
                
                if (result.isSuccess) {
                    saveLastSyncInfo()
                    showCompletionNotification("Sync completed successfully!", true)
                    // Send completion update via SharedFlow
                    CoroutineScope(Dispatchers.Main).launch {
                        try {
                            _progressFlow.emit(BackupProgress("Sync completed successfully!", 100, true, true))
                            log("SyncService: Completion update sent via SharedFlow - success: true")
                        } catch (e: Exception) {
                            log("SyncService: Error sending completion update: ${e.message}")
                        }
                    }
                    logJvSync("SyncService performBackup succeeded")
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    showCompletionNotification("Sync failed: $error", false)
                    // Send completion update via SharedFlow
                    CoroutineScope(Dispatchers.Main).launch {
                        try {
                            _progressFlow.emit(BackupProgress("Sync failed: $error", 100, true, false))
                            log("SyncService: Completion update sent via SharedFlow - success: false")
                        } catch (e: Exception) {
                            log("SyncService: Error sending completion update: ${e.message}")
                        }
                    }
                    logJvSync("SyncService performBackup reported failure: $error")
                }
                
            } catch (e: Exception) {
                log("Backup service error: ${e.message}")
                logJvSync("SyncService performBackup exception: ${e.message}")
                showCompletionNotification("Sync failed: ${e.message}", false)
                    sendBroadcast(Intent("com.velox.jewelvault.BACKUP_COMPLETED").apply {
                        putExtra("success", false)
                        putExtra("message", "Sync failed: ${e.message}")
                    })
            } finally {
                log("SyncService: performBackup finished")
                logJvSync("SyncService performBackup finished")
                // Add a delay to ensure completion notification is visible
                delay(2000) // 2 seconds delay
                clearSyncProgress()
                if (!monitorActive) {
                    stopSelf()
                } else {
                    updateMonitorNotification()
                }
            }
        }
    }
    
    private fun performRestore(userMobile: String, restoreMode: RestoreMode) {
        log("SyncService: performRestore started for user: $userMobile with mode: $restoreMode")
        logJvSync("SyncService performRestore initiated for $userMobile (mode=$restoreMode)")
        if (syncInProgress) {
            log("SyncService: performRestore skipped - sync already in progress")
            return
        }
        serviceJob = serviceScope.launch {
            try {
                setSyncProgress("Starting restore...", 0)
                updateMonitorNotification()
                val result = syncManager.performRestore(userMobile, restoreMode) { message, progress ->
                    updateRestoreNotification(message, progress)
                }
                
                if (result.isSuccess) {
                    val restoreResult = result.getOrNull()!!
                    val summary = restoreResult.summary
                    val message = "${restoreResult.message}\n\nSummary: $summary"
                    
                    saveLastSyncInfo()
                    showCompletionNotification("Restore completed successfully!", true)
                    sendBroadcast(Intent("com.velox.jewelvault.RESTORE_COMPLETED").apply {
                        putExtra("success", true)
                        putExtra("message", message)
                        putExtra("summary", summary.toString())
                        putExtra("restoreMode", restoreMode.name)
                    })
                    logJvSync("SyncService performRestore succeeded for $userMobile")
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    showCompletionNotification("Restore failed: $error", false)
                    sendBroadcast(Intent("com.velox.jewelvault.RESTORE_COMPLETED").apply {
                        putExtra("success", false)
                        putExtra("message", "Restore failed: $error")
                    })
                    logJvSync("SyncService performRestore reported failure for $userMobile: $error")
                }
                
            } catch (e: Exception) {
                log("Restore service error: ${e.message}")
                logJvSync("SyncService performRestore exception: ${e.message}")
                showCompletionNotification("Restore failed: ${e.message}", false)
                    sendBroadcast(Intent("com.velox.jewelvault.RESTORE_COMPLETED").apply {
                        putExtra("success", false)
                        putExtra("message", "Restore failed: ${e.message}")
                    })
            } finally {
                log("SyncService: performRestore finished")
                // Add a delay to ensure completion notification is visible
                delay(2000) // 2 seconds delay
                clearSyncProgress()
                if (!monitorActive) {
                    stopSelf()
                } else {
                    updateMonitorNotification()
                }
                logJvSync("SyncService performRestore finished for $userMobile")
            }
        }
    }

    /**
     * Perform restore with source selection (Firebase or Local)
     */
    private fun performRestoreWithSource(
        userMobile: String, 
        restoreSource: RestoreSource,
        localFileUri: Uri? = null,
        restoreMode: RestoreMode
    ) {
        log("SyncService: performRestoreWithSource started for user: $userMobile with source: $restoreSource, mode: $restoreMode")
        logJvSync("SyncService performRestoreWithSource initiated for $userMobile via $restoreSource (mode=$restoreMode)")
        if (syncInProgress) {
            log("SyncService: performRestoreWithSource skipped - sync already in progress")
            return
        }
        serviceJob = serviceScope.launch {
            try {
                setSyncProgress("Starting restore...", 0)
                updateMonitorNotification()
                val result = syncManager.performRestoreWithSource(
                    userMobile, 
                    restoreSource, 
                    localFileUri, 
                    restoreMode
                ) { message, progress ->
                    updateRestoreNotification(message, progress)
                }
                
                if (result.isSuccess) {
                    val restoreResult = result.getOrNull()!!
                    val summary = restoreResult.summary
                    val message = "${restoreResult.message}\n\nSummary: $summary"
                    
                    saveLastSyncInfo()
                    showCompletionNotification("Restore completed successfully!", true)
                    
                    // Send completion update via SharedFlow (same as sync)
                    CoroutineScope(Dispatchers.Main).launch {
                        try {
                            _progressFlow.emit(BackupProgress("Restore completed successfully!", 100, true, true))
                            log("SyncService: Restore completion update sent via SharedFlow - success: true")
                        } catch (e: Exception) {
                            log("SyncService: Error sending restore completion update: ${e.message}")
                        }
                    }
                    
                    sendBroadcast(Intent("com.velox.jewelvault.RESTORE_COMPLETED").apply {
                        putExtra("success", true)
                        putExtra("message", message)
                        putExtra("summary", summary.toString())
                        putExtra("restoreMode", restoreMode.name)
                        putExtra("restoreSource", restoreSource.name)
                    })
                    logJvSync("SyncService performRestoreWithSource succeeded for $userMobile via $restoreSource")
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    showCompletionNotification("Restore failed: $error", false)
                    
                    // Send completion update via SharedFlow (same as sync)
                    CoroutineScope(Dispatchers.Main).launch {
                        try {
                            _progressFlow.emit(BackupProgress("Restore failed: $error", 100, true, false))
                            log("SyncService: Restore completion update sent via SharedFlow - success: false")
                        } catch (e: Exception) {
                            log("SyncService: Error sending restore completion update: ${e.message}")
                        }
                    }
                    
                    sendBroadcast(Intent("com.velox.jewelvault.RESTORE_COMPLETED").apply {
                        putExtra("success", false)
                        putExtra("message", "Restore failed: $error")
                        putExtra("restoreSource", restoreSource.name)
                    })
                    logJvSync("SyncService performRestoreWithSource reported failure for $userMobile via $restoreSource: $error")
                }
                
            } catch (e: Exception) {
                log("Restore service error: ${e.message}")
                logJvSync("SyncService performRestoreWithSource exception for $userMobile: ${e.message}")
                showCompletionNotification("Restore failed: ${e.message}", false)
                
                // Send completion update via SharedFlow (same as sync)
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        _progressFlow.emit(BackupProgress("Restore failed: ${e.message}", 100, true, false))
                        log("SyncService: Restore completion update sent via SharedFlow - success: false (exception)")
                    } catch (e2: Exception) {
                        log("SyncService: Error sending restore completion update: ${e2.message}")
                    }
                }
                
                sendBroadcast(Intent("com.velox.jewelvault.RESTORE_COMPLETED").apply {
                    putExtra("success", false)
                    putExtra("message", "Restore failed: ${e.message}")
                    putExtra("restoreSource", restoreSource.name)
                })
            } finally {
                log("SyncService: performRestoreWithSource finished")
                // Add a delay to ensure completion notification is visible
                delay(2000) // 2 seconds delay
                clearSyncProgress()
                if (!monitorActive) {
                    stopSelf()
                } else {
                    updateMonitorNotification()
                }
                logJvSync("SyncService performRestoreWithSource finished for $userMobile via $restoreSource")
            }
        }
    }
    
    private fun createNotificationChannel() {
        logJvSync("SyncService: Creating notification channel: $CHANNEL_ID (API ${Build.VERSION.SDK_INT})")
        log("SyncService: Creating notification channel: $CHANNEL_ID")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT  // Changed from LOW to DEFAULT for better visibility
            ).apply {
                description = "Channel for sync and restore operations"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            log("SyncService: Notification channel created successfully")
            logJvSync("SyncService: Notification channel created ($CHANNEL_ID)")
        } else {
            log("SyncService: Notification channel not needed for API level < 26")
            logJvSync("SyncService: Notification channel creation skipped for API level < 26")
        }
    }
    
    private fun createNotification(message: String, progress: Int): Notification {
        log("SyncService: Creating notification with message: '$message', progress: $progress")
        logJvSync("SyncService: Building notification (progress=$progress) with message: $message")
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Jewel Vault")
            .setContentText(message)
            .setSmallIcon(R.drawable.logo) // Changed to logo which should exist
            .setColor(primaryLight.toArgb())
            .setContentIntent(pendingIntent)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // Added priority
            .build()
            .apply {
                flags = flags or Notification.FLAG_NO_CLEAR
            }
        
        log("SyncService: Notification created successfully")
        logJvSync("SyncService: Notification built successfully (progress=$progress)")
        return notification
    }

    private fun ensureForeground() {
        val notification = if (monitorActive) {
            buildMonitorNotification()
        } else {
            createNotification("Sync in progress...", 0)
        }
        startForeground(NOTIFICATION_ID, notification)
    }
    
    private fun updateNotification(message: String, progress: Int) {
        log("SyncService: Updating notification with message: '$message', progress: $progress")
        logJvSync("SyncService: Updating notification (progress=$progress) - $message")
        setSyncProgress(message, progress)
        val notification = if (monitorActive) {
            buildMonitorNotification()
        } else {
            createNotification(message, progress)
        }
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
        log("SyncService: Notification updated successfully")
        logJvSync("SyncService: Notification update sent (progress=$progress)")
        
        // Send progress update via SharedFlow
        CoroutineScope(Dispatchers.Main).launch {
            try {
                _progressFlow.emit(BackupProgress(message, progress))
                log("SyncService: Progress update sent via SharedFlow - message: '$message', progress: $progress")
            } catch (e: Exception) {
                log("SyncService: Error sending progress update: ${e.message}")
            }
        }
    }
    
    private fun updateRestoreNotification(message: String, progress: Int) {
        log("SyncService: Updating restore notification with message: '$message', progress: $progress")
        logJvSync("SyncService: Updating restore notification (progress=$progress) - $message")
        setSyncProgress(message, progress)
        val notification = if (monitorActive) {
            buildMonitorNotification()
        } else {
            createNotification(message, progress)
        }
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
        log("SyncService: Restore notification updated successfully")
        logJvSync("SyncService: Restore notification update applied (progress=$progress)")
        
        // Send progress update via SharedFlow (same as sync)
        CoroutineScope(Dispatchers.Main).launch {
            try {
                _progressFlow.emit(BackupProgress(message, progress))
                log("SyncService: Restore progress update sent via SharedFlow - message: '$message', progress: $progress")
            } catch (e: Exception) {
                log("SyncService: Error sending restore progress update: ${e.message}")
            }
        }
        
        // Also send progress broadcast to update UI (for backward compatibility)
        val broadcastIntent = Intent("com.velox.jewelvault.RESTORE_PROGRESS").apply {
            putExtra("message", message)
            putExtra("progress", progress)
        }
        log("SyncService: Sending RESTORE_PROGRESS broadcast - message: '$message', progress: $progress")
        logJvSync("SyncService: RESTORE_PROGRESS broadcast triggered (progress=$progress)")
        sendBroadcast(broadcastIntent)
        log("SyncService: RESTORE_PROGRESS broadcast sent successfully")
    }
    
    private fun showCompletionNotification(message: String, isSuccess: Boolean) {
        log("SyncService: Showing completion notification - success: $isSuccess, message: '$message'")
        logJvSync("SyncService: Completion notification shown (success=$isSuccess)")
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Jewel Vault")
            .setContentText(message)
            .setSmallIcon(R.drawable.logo) // Use the same logo icon
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // Added priority
            .build()
            .apply {
                flags = flags or Notification.FLAG_NO_CLEAR
            }

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(COMPLETION_NOTIFICATION_ID, notification)
        log("SyncService: Completion notification shown successfully")
    }

    private fun startMonitoringInternal() {
        logJvSync("SyncService: startMonitoringInternal called")
        if (monitorActive) {
            updateMonitorNotification()
            return
        }
        monitorActive = true
        ensureForeground()
        startMonitorJobs()
        updateMonitorNotification()
    }

    private fun stopMonitoringInternal() {
        logJvSync("SyncService: stopMonitoringInternal called")
        monitorActive = false
        monitorJob?.cancel()
        metalRatesJob?.cancel()
        activeDeviceJob?.cancel()
        autoSyncJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startMonitorJobs() {
        logJvSync("SyncService: startMonitorJobs hooking background observers")
        if (monitorJob?.isActive == true) return
        monitorJob = serviceScope.launch {
            loadCachedSyncInfo()
            updateMonitorNotification()
        }

        metalRatesJob?.cancel()
        metalRatesJob = serviceScope.launch {
            while (isActive) {
                refreshMetalRates()
                delay(TimeUnit.MINUTES.toMillis(30))
            }
        }

        activeDeviceJob?.cancel()
        activeDeviceJob = serviceScope.launch {
            while (isActive) {
                refreshActiveDeviceInfo()
                delay(TimeUnit.MINUTES.toMillis(1))
            }
        }

        autoSyncJob?.cancel()
        autoSyncJob = serviceScope.launch {
            dataStoreManager.syncFrequency.collectLatest { frequencyString ->
                val frequency = try {
                    SyncFrequency.valueOf(frequencyString)
                } catch (_: Exception) {
                    SyncFrequency.WEEKLY
                }

                dataStoreManager.setValue(DataStoreManager.SYNC_INTERVAL_MINUTES, frequency.intervalMinutes)
                if (frequency == SyncFrequency.DISABLED || frequency.intervalMinutes <= 0) {
                    return@collectLatest
                }

                val intervalMs = TimeUnit.MINUTES.toMillis(frequency.intervalMinutes.toLong())
                while (isActive) {
                    delay(intervalMs)
                    if (!syncInProgress) {
                        val storeId = dataStoreManager.getSelectedStoreInfo().first.first()
                        val adminMobile = dataStoreManager.getAdminInfo().third.first()
                        if (storeId.isNotBlank() && adminMobile.isNotBlank()) {
                            logJvSync("SyncService: Auto sync triggered for store $storeId")
                            performBackup()
                        }
                    }
                }
            }
        }
    }

    private suspend fun refreshMetalRates() {
        logJvSync("SyncService: refreshMetalRates called")
        try {
            val loading = mutableStateOf(false)
            val rates = metalRates(loading, applicationContext, dataStoreManager, metalRatesRepository)
            val gold = rates.firstOrNull {
                it.metal.equals("Gold", true) && it.caratOrPurity.equals("24K", true)
            }?.price?.let { parsePrice(it) }
            val silver = rates.firstOrNull {
                it.metal.equals("Silver", true) && it.caratOrPurity.contains("1 Kg", true)
            }?.price?.let { parsePrice(it) }

            val goldFallback = dataStoreManager.getValue(DataStoreManager.METAL_GOLD_24K).first()
            val silverFallback = dataStoreManager.getValue(DataStoreManager.METAL_SILVER_KG).first()

            gold24kRate = gold ?: goldFallback ?: 0.0
            silverKgRate = silver ?: silverFallback ?: 0.0
            updateMonitorNotification()
            logJvSync("SyncService: Metal rates refreshed (gold=$gold24kRate silver=$silverKgRate)")
        } catch (_: Exception) {
            gold24kRate = dataStoreManager.getValue(DataStoreManager.METAL_GOLD_24K).first() ?: 0.0
            silverKgRate = dataStoreManager.getValue(DataStoreManager.METAL_SILVER_KG).first() ?: 0.0
            updateMonitorNotification()
            logJvSync("SyncService: Metal rate refresh fallback used (gold=$gold24kRate silver=$silverKgRate)")
        }
    }

    private suspend fun refreshActiveDeviceInfo() {
        logJvSync("SyncService: refreshActiveDeviceInfo started")
        try {
            val adminMobile = dataStoreManager.getAdminInfo().third.first()
            if (adminMobile.isBlank()) return
            val snapshot = firestore.collection("users")
                .document(adminMobile)
                .collection("devices")
                .get()
                .await()

            val activeDevice = snapshot.documents
                .mapNotNull { doc ->
                    val isActive = doc.getBoolean("isActive") == true
                    if (!isActive) return@mapNotNull null
                    val label = buildDeviceLabel(
                        doc.getString("manufacturer"),
                        doc.getString("model")
                    )
                    val lastSeen = doc.getLong("lastSeenAt") ?: doc.getLong("lastLoginAt") ?: 0L
                    Triple(label, lastSeen, doc.id)
                }.maxByOrNull { it.second }

            if (activeDevice != null) {
                activeDeviceLabel = activeDevice.first
                activeDeviceAt = activeDevice.second
                dataStoreManager.setValue(DataStoreManager.ACTIVE_DEVICE_LABEL, activeDeviceLabel ?: "")
                dataStoreManager.setValue(DataStoreManager.ACTIVE_DEVICE_AT, activeDeviceAt ?: 0L)
            } else {
                activeDeviceLabel = null
                activeDeviceAt = null
                dataStoreManager.setValue(DataStoreManager.ACTIVE_DEVICE_LABEL, "")
                dataStoreManager.setValue(DataStoreManager.ACTIVE_DEVICE_AT, 0L)
            }
            updateMonitorNotification()
            logJvSync("SyncService: Active device info updated (label=$activeDeviceLabel at=${activeDeviceAt ?: "null"})")
        } catch (_: Exception) {
            // Keep last known values.
            logJvSync("SyncService: refreshActiveDeviceInfo failed, keeping cached values")
        }
    }

    private fun updateMonitorNotification() {
        logJvSync("SyncService: updateMonitorNotification called (monitorActive=$monitorActive)")
        if (!monitorActive) return
        val notification = buildMonitorNotification()
        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
    }

    private fun buildMonitorNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val refreshIntent = Intent(this, SyncService::class.java).apply {
            action = ACTION_REFRESH_METAL_RATES
        }
        val refreshPendingIntent = PendingIntent.getService(
            this, 1, refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val gold = gold24kRate ?: 0.0
        val silver = silverKgRate ?: 0.0
        val lastSyncTime = formatTime(lastSyncAt)
        val lastSyncDeviceLabel = lastSyncDevice ?: "No Active Device"
        val activeLabel = activeDeviceLabel ?: "No Active Device"
        val activeTime = formatTime(activeDeviceAt)

        val line1 = "Gold 24k, 1g: ₹${formatNumber(gold)} | Silver 1kg: ₹${formatNumber(silver)}"
        val line2 = "Last sync: $lastSyncTime | Device: $lastSyncDeviceLabel"
        val line3 = if (activeDeviceAt != null) {
            "Active on: $activeLabel at $activeTime"
        } else {
            "Active on: $activeLabel"
        }

        val baseText = activeLabel

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(line1)
            .setContentText(baseText)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$line1\n$line3\n$line2"))
            .setSmallIcon(R.drawable.logo)
            .setColor(primaryLight.toArgb())
            .setContentIntent(pendingIntent)
            .addAction(
                NotificationCompat.Action(
                    0,
                    "Refresh Rates",
                    refreshPendingIntent
                )
            )
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        if (syncInProgress) {
            builder.setProgress(100, syncProgress.coerceIn(0, 100), syncProgress <= 0)
        } else {
            builder.setProgress(0, 0, false)
        }

        return builder.build().apply {
            flags = flags or Notification.FLAG_NO_CLEAR
        }
    }

    private suspend fun loadCachedSyncInfo() {
        logJvSync("SyncService: loadCachedSyncInfo fetching previous sync metadata")
        lastSyncAt = dataStoreManager.getValue(DataStoreManager.LAST_SYNC_AT).first()
        lastSyncDevice = dataStoreManager.getValue(DataStoreManager.LAST_SYNC_DEVICE).first()
            ?.takeIf { it.isNotBlank() }
        activeDeviceLabel = dataStoreManager.getValue(DataStoreManager.ACTIVE_DEVICE_LABEL).first()
            ?.takeIf { it.isNotBlank() }
        activeDeviceAt = dataStoreManager.getValue(DataStoreManager.ACTIVE_DEVICE_AT).first()
            ?.takeIf { it > 0L }
        logJvSync("SyncService: Cached sync info loaded (lastSyncAt=$lastSyncAt device=$activeDeviceLabel)")
    }

    private fun setSyncProgress(message: String, progress: Int) {
        syncInProgress = true
        syncMessage = message
        syncProgress = progress
    }

    private fun clearSyncProgress() {
        syncInProgress = false
        syncMessage = ""
        syncProgress = 0
    }

    private fun formatTime(value: Long?): String {
        if (value == null || value <= 0L) return "--"
        val formatter = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        return formatter.format(Date(value))
    }

    private fun formatNumber(value: Double): String {
        return String.format(Locale.getDefault(), "%.0f", value)
    }

    private fun parsePrice(raw: String): Double? {
        val cleaned = raw.replace(Regex("[^0-9.]"), "")
        return cleaned.toDoubleOrNull()
    }

    private suspend fun saveLastSyncInfo() {
        logJvSync("SyncService: saveLastSyncInfo storing sync timestamp")
        val now = System.currentTimeMillis()
        val deviceLabel = buildDeviceLabel(Build.MANUFACTURER, Build.MODEL)
        dataStoreManager.setValue(DataStoreManager.LAST_SYNC_AT, now)
        dataStoreManager.setValue(DataStoreManager.LAST_SYNC_DEVICE, deviceLabel)
        lastSyncAt = now
        lastSyncDevice = deviceLabel
        updateMonitorNotification()
        logJvSync("SyncService: Last sync info updated (lastSyncAt=$lastSyncAt device=$lastSyncDevice)")
    }

    private fun buildDeviceLabel(manufacturer: String?, model: String?): String {
        val maker = manufacturer?.ifBlank { "Unknown" } ?: "Unknown"
        val modelName = model?.ifBlank { "Unknown" } ?: "Unknown"
        return "$maker $modelName".trim()
    }
}
