package com.velox.jewelvault.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.velox.jewelvault.utils.ioScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DataStoreManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        // Existing keys
        val USER_NAME_KEY = stringPreferencesKey("user_name")
        val LOGIN_USER_MOBILE_KEY = stringPreferencesKey("login_user_mobile")
        private val ADMIN_USER_ID_KEY = stringPreferencesKey("admin_user_id")
        private val ADMIN_USER_NAME_KEY = stringPreferencesKey("admin_user_name")
        private val ADMIN_USER_MOBILE_KEY = stringPreferencesKey("admin_user_mobile")
        private val SELECTED_STORE_ID_KEY = stringPreferencesKey("selected_store_id")
        private val SELECTED_STORE_UPI_ID = stringPreferencesKey("selected_store_upi_id")
        private val SELECTED_STORE_NAME = stringPreferencesKey("selected_store_name")
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

        // Backup Settings
        val BACKUP_FREQUENCY = stringPreferencesKey("backup_frequency")
    }

    val userName: Flow<String> = dataStore.data.map { prefs -> prefs[USER_NAME_KEY] ?: "" }

    fun saveAdminInfo(userName: String, userId: String, mobileNo: String) {
        ioScope {
            setValue(ADMIN_USER_NAME_KEY, userName)
            setValue(ADMIN_USER_ID_KEY, userId)
            setValue(ADMIN_USER_MOBILE_KEY, mobileNo)
        }
    }

    /**
     * return Triple of Flow<String> for userId, userName, mobileNo
     * */
    fun getAdminInfo(): Triple<Flow<String>, Flow<String>, Flow<String>> {
        val id: Flow<String> = dataStore.data.map { prefs -> prefs[ADMIN_USER_ID_KEY] ?: "" }
        val name: Flow<String> = dataStore.data.map { prefs -> prefs[ADMIN_USER_NAME_KEY] ?: "" }
        val mobile: Flow<String> =
            dataStore.data.map { prefs -> prefs[ADMIN_USER_MOBILE_KEY] ?: "" }
        return Triple(id, name, mobile)
    }

    val loginUserName: Flow<String> =
        dataStore.data.map { prefs -> prefs[LOGIN_USER_MOBILE_KEY] ?: "" }


    /**
     * return Triple of Flow<String> for storeId, upiId, storeName
     * */
    fun getSelectedStoreInfo(): Triple<Flow<String>, Flow<String>, Flow<String>> {
        val storeId: Flow<String> =
            dataStore.data.map { prefs -> prefs[SELECTED_STORE_ID_KEY] ?: "" }
        val upiId: Flow<String> = dataStore.data.map { prefs -> prefs[SELECTED_STORE_UPI_ID] ?: "" }
        val storeName: Flow<String> =
            dataStore.data.map { prefs -> prefs[SELECTED_STORE_NAME] ?: "" }
        return Triple(storeId, upiId, storeName)
    }

    fun saveSelectedStoreInfo(storeId: String, upiId: String, storeName: String) {
        ioScope {
            setValue(SELECTED_STORE_ID_KEY, storeId)
            setValue(SELECTED_STORE_UPI_ID, upiId)
            setValue(SELECTED_STORE_NAME, storeName)
        }
    }

//    val selectedStoreId: Flow<String> =
//        dataStore.data.map { prefs -> prefs[SELECTED_STORE_ID_KEY] ?: "" }
//    val upiId: Flow<String> = dataStore.data.map { prefs -> prefs[SELECTED_STORE_UPI_ID] ?: "" }
//    val storeName: Flow<String> =
//        dataStore.data.map { prefs -> prefs[SELECTED_STORE_NAME] ?: "Merchant" }


    val backupFrequency: Flow<String> =
        dataStore.data.map { prefs -> prefs[BACKUP_FREQUENCY] ?: "WEEKLY" }

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

    suspend fun setUpiId(upiId: String) {
        setValue(SELECTED_STORE_UPI_ID, upiId)
    }

    suspend fun setMerchantName(name: String) {
        setValue(SELECTED_STORE_NAME, name)
    }

    suspend fun setBackupFrequency(frequency: String) {
        setValue(BACKUP_FREQUENCY, frequency)
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