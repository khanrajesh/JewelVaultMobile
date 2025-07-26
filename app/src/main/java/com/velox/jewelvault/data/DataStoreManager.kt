package com.velox.jewelvault.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DataStoreManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        // Existing keys
        val USER_NAME_KEY = stringPreferencesKey("user_name")
        val USER_ID_KEY = intPreferencesKey("user_id")
        val STORE_ID_KEY = intPreferencesKey("store_id")
        val SHOW_SEPARATE_CHARGE = booleanPreferencesKey("show_separate_charge")

        // Network & Connectivity Settings
        val CONTINUOUS_NETWORK_CHECK = booleanPreferencesKey("continuous_network_check")
        val NETWORK_SPEED_MONITORING = booleanPreferencesKey("network_speed_monitoring")
        val AUTO_REFRESH_METAL_RATES = booleanPreferencesKey("auto_refresh_metal_rates")
        val CONNECTION_TIMEOUT = intPreferencesKey("connection_timeout")

        // Security Settings
        val SESSION_TIMEOUT_MINUTES = intPreferencesKey("session_timeout_minutes")
        val AUTO_LOGOUT_INACTIVITY = booleanPreferencesKey("auto_logout_inactivity")
        val BIOMETRIC_AUTH = booleanPreferencesKey("biometric_auth")
        val SAVED_PHONE_NUMBER = stringPreferencesKey("saved_phone_number")

        // Display & UI Settings
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val FONT_SIZE = stringPreferencesKey("font_size")

        // Business Settings
        val DEFAULT_CGST = stringPreferencesKey("default_cgst")
        val DEFAULT_SGST = stringPreferencesKey("default_sgst")
        val DEFAULT_IGST = stringPreferencesKey("default_igst")
        val CURRENCY_FORMAT = stringPreferencesKey("currency_format")
        val DATE_FORMAT = stringPreferencesKey("date_format")

        // Notification Settings
        val SESSION_WARNING_ENABLED = booleanPreferencesKey("session_warning_enabled")
        val LOW_STOCK_ALERTS = booleanPreferencesKey("low_stock_alerts")
        val PRICE_CHANGE_NOTIFICATIONS = booleanPreferencesKey("price_change_notifications")
        val BACKUP_REMINDERS = booleanPreferencesKey("backup_reminders")

        // Performance Settings
        val AUTO_SAVE_INTERVAL = intPreferencesKey("auto_save_interval")
        val CACHE_ENABLED = booleanPreferencesKey("cache_enabled")

        // Privacy Settings
        val DATA_COLLECTION = booleanPreferencesKey("data_collection")
        val ANALYTICS_ENABLED = booleanPreferencesKey("analytics_enabled")
        val CRASH_REPORTING = booleanPreferencesKey("crash_reporting")

        // Advanced Settings
        val DEBUG_MODE = booleanPreferencesKey("debug_mode")
        val LOG_EXPORT_ENABLED = booleanPreferencesKey("log_export_enabled")
    }

    val userName: Flow<String> = dataStore.data.map { prefs -> prefs[USER_NAME_KEY] ?: "" }
    val userId: Flow<Int> = dataStore.data.map { prefs -> prefs[USER_ID_KEY] ?: -1 }
    val storeId: Flow<Int> = dataStore.data.map { prefs -> prefs[STORE_ID_KEY] ?: -1 }

    suspend fun <T> setValue(key: Preferences.Key<T>, value: T) {
        try {
        dataStore.edit { prefs ->
            prefs[key] = value
            }
        } catch (e: Exception) {
            // Handle DataStore errors gracefully
            e.printStackTrace()
        }
    }

    fun <T> getValue(key: Preferences.Key<T>, default: T? = null): Flow<T?> {
        return dataStore.data.map { prefs ->
            try {
            prefs[key] ?: default
            } catch (e: Exception) {
                default
    }
        }
    }

    // Convenience methods for common settings
    suspend fun setContinuousNetworkCheck(enabled: Boolean) {
        setValue(CONTINUOUS_NETWORK_CHECK, enabled)
    }

    suspend fun setNetworkSpeedMonitoring(enabled: Boolean) {
        setValue(NETWORK_SPEED_MONITORING, enabled)
    }

    suspend fun setSessionTimeout(minutes: Int) {
        setValue(SESSION_TIMEOUT_MINUTES, minutes)
    }

    suspend fun setThemeMode(mode: String) {
        setValue(THEME_MODE, mode)
    }

    suspend fun setDefaultTaxRates(cgst: String, sgst: String, igst: String) {
        setValue(DEFAULT_CGST, cgst)
        setValue(DEFAULT_SGST, sgst)
        setValue(DEFAULT_IGST, igst)
    }

    suspend fun clearAllData() {
        try {
            dataStore.edit { prefs ->
                prefs.clear()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}