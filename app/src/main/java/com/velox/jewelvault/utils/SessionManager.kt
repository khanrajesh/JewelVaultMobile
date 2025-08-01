package com.velox.jewelvault.utils

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.velox.jewelvault.data.DataStoreManager
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    private val dataStoreManager: DataStoreManager
) {
    companion object {
        private const val DEFAULT_SESSION_TIMEOUT_MINUTES = 30L
        private const val WARNING_THRESHOLD_MINUTES = 25L // Warn 5 minutes before expiry
        private const val LAST_ACTIVITY_KEY = "last_activity"
        private const val SESSION_TOKEN_KEY = "session_token"
    }
    
    private val lastActivityKey = longPreferencesKey(LAST_ACTIVITY_KEY)
    private val sessionTokenKey = stringPreferencesKey(SESSION_TOKEN_KEY)
    
    suspend fun startSession(userId: String) {
        val token = generateSessionToken()
        dataStoreManager.setValue(sessionTokenKey, token)
        updateLastActivity()
    }
    
    suspend fun isSessionValid(): Boolean {
        val lastActivity = dataStoreManager.getValue(lastActivityKey, 0L).first() ?: 0L
        val currentTime = System.currentTimeMillis()
        val sessionTimeoutMinutes = dataStoreManager.getValue(DataStoreManager.SESSION_TIMEOUT_MINUTES, DEFAULT_SESSION_TIMEOUT_MINUTES.toInt()).first() ?: DEFAULT_SESSION_TIMEOUT_MINUTES.toInt()
        val sessionTimeout = TimeUnit.MINUTES.toMillis(sessionTimeoutMinutes.toLong())
        
        return (currentTime - lastActivity) < sessionTimeout
    }
    
    suspend fun isSessionExpiringSoon(): Boolean {
        val lastActivity = dataStoreManager.getValue(lastActivityKey, 0L).first() ?: 0L
        val currentTime = System.currentTimeMillis()
        val sessionTimeoutMinutes = dataStoreManager.getValue(DataStoreManager.SESSION_TIMEOUT_MINUTES, DEFAULT_SESSION_TIMEOUT_MINUTES.toInt()).first() ?: DEFAULT_SESSION_TIMEOUT_MINUTES.toInt()
        val warningThreshold = TimeUnit.MINUTES.toMillis((sessionTimeoutMinutes - 5).toLong()) // Warn 5 minutes before expiry
        
        return (currentTime - lastActivity) >= warningThreshold
    }
    
    suspend fun getSessionTimeRemaining(): Long {
        val lastActivity = dataStoreManager.getValue(lastActivityKey, 0L).first() ?: 0L
        val currentTime = System.currentTimeMillis()
        val sessionTimeoutMinutes = dataStoreManager.getValue(DataStoreManager.SESSION_TIMEOUT_MINUTES, DEFAULT_SESSION_TIMEOUT_MINUTES.toInt()).first() ?: DEFAULT_SESSION_TIMEOUT_MINUTES.toInt()
        val sessionTimeout = TimeUnit.MINUTES.toMillis(sessionTimeoutMinutes.toLong())
        
        return sessionTimeout - (currentTime - lastActivity)
    }
    
    suspend fun updateLastActivity() {
        dataStoreManager.setValue(lastActivityKey, System.currentTimeMillis())
    }
    
    suspend fun endSession() {
        dataStoreManager.setValue(sessionTokenKey, "")
        dataStoreManager.setValue(lastActivityKey, 0L)
    }
    
    private fun generateSessionToken(): String {
        val timestamp = System.currentTimeMillis()
        val random = (Math.random() * 1000000).toLong()
        return "$timestamp-$random"
    }
    
    suspend fun getSessionToken(): String? {
        return dataStoreManager.getValue(sessionTokenKey, "").first()
    }
} 