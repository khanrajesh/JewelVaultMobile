package com.velox.jewelvault.utils.fcm

import android.content.Context
import com.google.firebase.messaging.FirebaseMessaging
import com.velox.jewelvault.data.DataStoreManager
import com.velox.jewelvault.utils.log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FCMTokenManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStoreManager: DataStoreManager
) {

    companion object {
        private const val FCM_TOKEN_KEY = "fcm_token"
    }

    /**
     * Get the current FCM token
     */
    suspend fun getFCMToken(): String? {
        return try {
            val token = FirebaseMessaging.getInstance().token.await()
            log("FCM: Retrieved token: $token")
            token
        } catch (e: Exception) {
            log("FCM: Failed to get token: ${e.message}")
            null
        }
    }

    /**
     * Save FCM token to DataStore
     */
    suspend fun saveFCMToken(token: String) {
        try {
            dataStoreManager.setValue(FCM_TOKEN_KEY, token)
            log("FCM: Token saved to DataStore: $token")
        } catch (e: Exception) {
            log("FCM: Failed to save token to DataStore: ${e.message}")
        }
    }

    /**
     * Get saved FCM token from DataStore
     */
    suspend fun getSavedFCMToken(): String? {
        return try {
            val token = dataStoreManager.getValue(FCM_TOKEN_KEY, "").first()
            if (token.isBlank()) null else token
        } catch (e: Exception) {
            log("FCM: Failed to get saved token: ${e.message}")
            null
        }
    }

    /**
     * Get FCM token and save it if not already saved
     */
    suspend fun getAndSaveFCMToken(): String? {
        return try {
            // First check if we have a saved token
            val savedToken = getSavedFCMToken()
            if (savedToken != null) {
                log("FCM: Using saved token: $savedToken")
                return savedToken
            }

            // Get fresh token from Firebase
            val token = getFCMToken()
            if (token != null) {
                saveFCMToken(token)
                log("FCM: Fresh token obtained and saved: $token")
            }
            token
        } catch (e: Exception) {
            log("FCM: Failed to get and save token: ${e.message}")
            null
        }
    }

    /**
     * Delete saved FCM token
     */
    suspend fun deleteFCMToken() {
        try {
            dataStoreManager.setValue(FCM_TOKEN_KEY, "")
            log("FCM: Token deleted from DataStore")
        } catch (e: Exception) {
            log("FCM: Failed to delete token: ${e.message}")
        }
    }
}
