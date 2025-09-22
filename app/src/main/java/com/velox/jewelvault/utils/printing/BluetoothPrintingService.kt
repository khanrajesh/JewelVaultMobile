package com.velox.jewelvault.utils.printing

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.velox.jewelvault.MainActivity
import com.velox.jewelvault.R
import com.velox.jewelvault.data.printing.BluetoothManager
import com.velox.jewelvault.utils.log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service to maintain Bluetooth printer connections
 * when the app is backgrounded
 */
@AndroidEntryPoint
class BluetoothPrintingService : Service() {
    
    @Inject
    lateinit var bluetoothManager: BluetoothManager
    
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var connectionJob: Job? = null
    
    companion object {
        const val CHANNEL_ID = "bluetooth_printing_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START_SERVICE = "start_service"
        const val ACTION_STOP_SERVICE = "stop_service"
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        log("BluetoothPrintingService created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SERVICE -> {
                startForegroundService()
                startConnectionMonitoring()
            }
            ACTION_STOP_SERVICE -> {
                stopConnectionMonitoring()
                stopSelf()
            }
        }
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun startForegroundService() {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        log("BluetoothPrintingService started in foreground")
    }
    
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Bluetooth Printing Active")
            .setContentText("Maintaining printer connections")
            .setSmallIcon(R.drawable.logo_1) // Use your app icon
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Bluetooth Printing Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Maintains Bluetooth printer connections"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun startConnectionMonitoring() {
        connectionJob = serviceScope.launch {
            try {
                // Monitor bonded devices and maintain connections
         /*       bluetoothManager.bondedDevices.collect { bondedDevices ->
                    val printerCount = bondedDevices.count { it.isPrinter && it.isConnected }
                    
                    if (printerCount > 0) {
                        log("Monitoring $printerCount connected printer(s)")
                        // Update notification with printer count
                        updateNotification(printerCount)
                    } else {
                        log("No printers connected, stopping service")
                        stopSelf()
                    }
                }*/
            } catch (e: Exception) {
                log("Error in connection monitoring: ${e.message}")
                stopSelf()
            }
        }
    }
    
    private fun stopConnectionMonitoring() {
        connectionJob?.cancel()
        connectionJob = null
        log("Stopped connection monitoring")
    }
    
    private fun updateNotification(printerCount: Int) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Bluetooth Printing Active")
            .setContentText("$printerCount printer(s) connected")
            .setSmallIcon(R.drawable.logo_1)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopConnectionMonitoring()
        log("BluetoothPrintingService destroyed")
    }
}
