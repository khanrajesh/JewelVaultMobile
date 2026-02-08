package com.velox.jewelvault.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.velox.jewelvault.data.roomdb.entity.users.UsersEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
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
        val SESSION_TOKEN_KEY = stringPreferencesKey("session_token")
        val AUTO_LOGOUT_INACTIVITY = booleanPreferencesKey("auto_logout_inactivity")
        val BIOMETRIC_AUTH = booleanPreferencesKey("biometric_auth")
        val SAVED_PHONE_NUMBER = stringPreferencesKey("saved_phone_number")
        val BIOMETRIC_OPTIN_SHOWN = booleanPreferencesKey("biometric_optin_shown")

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
        val SYNC_INTERVAL_MINUTES = intPreferencesKey("sync_interval_minutes")
        val LAST_SYNC_AT = longPreferencesKey("last_sync_at")
        val LAST_SYNC_DEVICE = stringPreferencesKey("last_sync_device")
        val ACTIVE_DEVICE_LABEL = stringPreferencesKey("active_device_label")
        val ACTIVE_DEVICE_AT = longPreferencesKey("active_device_at")


        private val CL_USER_NAME_KEY = stringPreferencesKey("cl_user_name")
        private val CL_USER_ID_KEY = stringPreferencesKey("cl_user_id")
        private val CL_USER_MOBILE_KEY = stringPreferencesKey("cl_user_mobile")
        private val CL_USER_ROLE_KEY = stringPreferencesKey("cl_user_role")

        val FCM_TOKEN_KEY = stringPreferencesKey("fcm_token")


        val METAL_FETCH_DATE = stringPreferencesKey("metal_fetch_date")
        val METAL_GOLD_24K = doublePreferencesKey("metal_gold_24k")
        val METAL_SILVER_KG = doublePreferencesKey("metal_silver_kg")

        val FEATURE_LIST_JSON = stringPreferencesKey("feature_list_json")
        val FEATURE_LIST_UPDATED = longPreferencesKey("feature_list_updated")
        val SUBSCRIPTION_JSON = stringPreferencesKey("subscription_json")
        val SUBSCRIPTION_UPDATED = longPreferencesKey("subscription_updated")
    }

    private val gson = Gson()

    suspend fun saveAdminInfo(userName: String, userId: String, mobileNo: String) {
        setValue(ADMIN_USER_NAME_KEY, userName)
        setValue(ADMIN_USER_ID_KEY, userId)
        setValue(ADMIN_USER_MOBILE_KEY, mobileNo)
    }


    suspend fun saveCurrentLoginUser(user: UsersEntity) {
        setValue(CL_USER_ID_KEY, user.userId)
        setValue(CL_USER_NAME_KEY, user.name)
        setValue(CL_USER_MOBILE_KEY, user.mobileNo)
        setValue(CL_USER_ROLE_KEY, user.role)
    }

    fun getCurrentLoginUser(): UsersEntity{
        val prefs = runBlocking { dataStore.data.first() }
        return UsersEntity(
            userId = prefs[CL_USER_ID_KEY] ?: "",
            name = prefs[CL_USER_NAME_KEY] ?: "",
            mobileNo = prefs[CL_USER_MOBILE_KEY] ?: "",
            role = prefs[CL_USER_ROLE_KEY] ?: ""
        )
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

    suspend fun saveSelectedStoreInfo(storeId: String, upiId: String, storeName: String) {
        setValue(SELECTED_STORE_ID_KEY, storeId)
        setValue(SELECTED_STORE_UPI_ID, upiId)
        setValue(SELECTED_STORE_NAME, storeName)
    }

//    val selectedStoreId: Flow<String> =
//        dataStore.data.map { prefs -> prefs[SELECTED_STORE_ID_KEY] ?: "" }
//    val upiId: Flow<String> = dataStore.data.map { prefs -> prefs[SELECTED_STORE_UPI_ID] ?: "" }
//    val storeName: Flow<String> =
//        dataStore.data.map { prefs -> prefs[SELECTED_STORE_NAME] ?: "Merchant" }


    val syncFrequency: Flow<String> =
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

    suspend fun saveFeatureList(featureList: FeatureListState) {
        try {
            val json = gson.toJson(featureList)
            setValue(FEATURE_LIST_JSON, json)
            setValue(FEATURE_LIST_UPDATED, featureList.lastUpdated)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getFeature(key: String): Boolean {
        return try {
            val featureList = getFeatureList()
            featureList.features[key] ?: false
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getFeatureLastUpdated(): Long {
        return getValue(FEATURE_LIST_UPDATED, 0L).first() ?: 0L
    }

    fun getFeatureListFlow(): Flow<FeatureListState> = dataStore.data.map { prefs ->
        val json = prefs[FEATURE_LIST_JSON] ?: ""
        if (json.isBlank()) {
            FeatureListState()
        } else {
            try {
                val type = object : TypeToken<FeatureListState>() {}.type
                gson.fromJson(json, type) ?: FeatureListState()
            } catch (e: Exception) {
                FeatureListState()
            }
        }
    }

    suspend fun getFeatureList(): FeatureListState {
        return try {
            val json = getValue(FEATURE_LIST_JSON, "").first() ?: ""
            if (json.isBlank()) {
                FeatureListState()
            } else {
                val type = object : TypeToken<FeatureListState>() {}.type
                gson.fromJson(json, type) ?: FeatureListState()
            }
        } catch (e: Exception) {
            FeatureListState()
        }
    }

    suspend fun saveSubscription(subscription: SubscriptionState) {
        try {
            val json = gson.toJson(subscription)
            setValue(SUBSCRIPTION_JSON, json)
            setValue(SUBSCRIPTION_UPDATED, subscription.lastUpdated)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getSubscriptionFlow(): Flow<SubscriptionState> = dataStore.data.map { prefs ->
        val json = prefs[SUBSCRIPTION_JSON] ?: ""
        if (json.isBlank()) {
            SubscriptionState()
        } else {
            try {
                val type = object : TypeToken<SubscriptionState>() {}.type
                gson.fromJson(json, type) ?: SubscriptionState()
            } catch (e: Exception) {
                SubscriptionState()
            }
        }
    }

    suspend fun getSubscription(): SubscriptionState {
        return try {
            val json = getValue(SUBSCRIPTION_JSON, "").first() ?: ""
            if (json.isBlank()) {
                SubscriptionState()
            } else {
                val type = object : TypeToken<SubscriptionState>() {}.type
                gson.fromJson(json, type) ?: SubscriptionState()
            }
        } catch (e: Exception) {
            SubscriptionState()
        }
    }
}
