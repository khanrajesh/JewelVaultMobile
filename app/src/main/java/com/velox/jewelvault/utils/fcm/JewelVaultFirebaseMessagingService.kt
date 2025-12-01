package com.velox.jewelvault.utils.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.velox.jewelvault.MainActivity
import com.velox.jewelvault.R
import com.velox.jewelvault.utils.ioScope
import com.velox.jewelvault.utils.log
import com.velox.jewelvault.utils.fcm.NotificationConstants.EXTRA_CHANNEL_ID
import com.velox.jewelvault.utils.fcm.NotificationConstants.EXTRA_TARGET_ARG
import com.velox.jewelvault.utils.fcm.NotificationConstants.EXTRA_TARGET_ROUTE
import dagger.hilt.android.AndroidEntryPoint
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

@AndroidEntryPoint
class JewelVaultFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var fcmTokenManager: FCMTokenManager

    companion object {
        private const val DEFAULT_CHANNEL_ID = "jewel_vault_general"
        private const val ALERTS_CHANNEL_ID = "jewel_vault_alerts"
        private const val MARKETING_CHANNEL_ID = "jewel_vault_marketing"
        private const val DEFAULT_CHANNEL_NAME = "General"
        private const val ALERTS_CHANNEL_NAME = "Alerts"
        private const val MARKETING_CHANNEL_NAME = "Marketing"
        private const val CHANNEL_DESCRIPTION = "Notifications for JewelVault app"
        private const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        log("FCM: Message received from: ${remoteMessage.from}")
        val data = remoteMessage.data
        log("FCM: Data payload: $data")

        val channelId = data["channelId"] ?: data["channel"] ?: DEFAULT_CHANNEL_ID
        val title =
            data["title"] ?: remoteMessage.notification?.title ?: "JewelVault"
        val body =
            data["body"] ?: remoteMessage.notification?.body ?: "You have a new notification"
        val imageUrl = data["imageUrl"] ?: remoteMessage.notification?.imageUrl?.toString()
        val targetRoute = data["targetRoute"] ?: data["target"]
        val targetArg = data["targetArg"] ?: data["id"]

        ioScope {
            val imageBitmap = loadBitmapFromUrl(imageUrl)
            showNotification(
                channelId = channelId,
                title = title,
                body = body,
                imageBitmap = imageBitmap,
                targetRoute = targetRoute,
                targetArg = targetArg
            )
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        log("FCM: New token generated: $token")

        ioScope {
            // Save the new token
            fcmTokenManager.saveFCMToken(token)
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelConfigs = listOf(
                Triple(DEFAULT_CHANNEL_ID, DEFAULT_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT),
                Triple(ALERTS_CHANNEL_ID, ALERTS_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH),
                Triple(MARKETING_CHANNEL_ID, MARKETING_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW)
            )

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            channelConfigs.forEach { (id, name, importance) ->
                val channel = NotificationChannel(id, name, importance).apply {
                    description = CHANNEL_DESCRIPTION
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    private fun showNotification(
        channelId: String,
        title: String,
        body: String,
        imageBitmap: Bitmap?,
        targetRoute: String?,
        targetArg: String?
    ) {
        val resolvedChannel = when (channelId) {
            ALERTS_CHANNEL_ID, MARKETING_CHANNEL_ID -> channelId
            else -> DEFAULT_CHANNEL_ID
        }
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            if (!targetRoute.isNullOrBlank()) {
                putExtra(EXTRA_TARGET_ROUTE, targetRoute)
            }
            if (!targetArg.isNullOrBlank()) {
                putExtra(EXTRA_TARGET_ARG, targetArg)
            }
            putExtra(EXTRA_CHANNEL_ID, resolvedChannel)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            resolvedChannel.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, resolvedChannel)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)

        if (imageBitmap != null) {
            builder.setLargeIcon(imageBitmap)
            builder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(imageBitmap)
            )
        } else {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(body))
        }

        NotificationManagerCompat.from(this)
            .notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), builder.build())
    }

    private fun loadBitmapFromUrl(imageUrl: String?): Bitmap? {
        if (imageUrl.isNullOrBlank()) return null
        return try {
            val url = URL(imageUrl)
            (url.openConnection() as? HttpURLConnection)?.run {
                connectTimeout = 5000
                readTimeout = 5000
                doInput = true
                connect()
                inputStream.use { BitmapFactory.decodeStream(it) }
            }
        } catch (e: Exception) {
            log("FCM: Failed to load image: ${e.message}")
            null
        }
    }
}
