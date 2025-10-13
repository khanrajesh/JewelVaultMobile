package com.velox.jewelvault.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.velox.jewelvault.data.roomdb.entity.users.UsersEntity
import com.velox.jewelvault.data.bluetooth.PrinterInfo
import com.velox.jewelvault.utils.ioScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
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

        // Printer Settings
        val SAVED_PRINTERS_JSON = stringPreferencesKey("saved_printers_json")
        val DEFAULT_PRINTER_ADDRESS = stringPreferencesKey("default_printer_address")


        private val CL_USER_NAME_KEY = stringPreferencesKey("cl_user_name")
        private val CL_USER_ID_KEY = stringPreferencesKey("cl_user_id")
        private val CL_USER_MOBILE_KEY = stringPreferencesKey("cl_user_mobile")
        private val CL_USER_ROLE_KEY = stringPreferencesKey("cl_user_role")

    }

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

    // Printer management methods
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Read all saved printers from DataStore
     */
    fun readPrinters(): Flow<List<PrinterInfo>> {
        return dataStore.data.map { prefs ->
            try {
                val jsonString = prefs[SAVED_PRINTERS_JSON] ?: "[]"
                kotlinx.serialization.json.Json.decodeFromString<List<PrinterInfo>>(jsonString)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    /**
     * Save list of printers to DataStore
     */
    suspend fun savePrinters(printers: List<PrinterInfo>) {
        try {
            val jsonString = kotlinx.serialization.json.Json.encodeToString(kotlinx.serialization.serializer<List<PrinterInfo>>(), printers)
            setValue(SAVED_PRINTERS_JSON, jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Add or update a printer in the saved list
     */
    suspend fun upsertPrinter(printer: PrinterInfo) {
        try {
            val currentPrinters = readPrinters().first().toMutableList()
            
            // Remove existing printer with same address
            currentPrinters.removeAll { it.address == printer.address }
            
            // Add updated printer
            currentPrinters.add(printer)
            
            savePrinters(currentPrinters)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Remove a printer from saved list
     */
    suspend fun removePrinter(address: String) {
        try {
            val currentPrinters = readPrinters().first().toMutableList()
            currentPrinters.removeAll { it.address == address }
            savePrinters(currentPrinters)
            
            // If removed printer was default, clear default
            val currentDefault = getValue(DEFAULT_PRINTER_ADDRESS).first()
            if (currentDefault == address) {
                setValue(DEFAULT_PRINTER_ADDRESS, "")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Set a printer as default (clears others)
     */
    suspend fun setDefaultPrinter(address: String) {
        try {
            var currentPrinters = readPrinters().first().toMutableList()
            
            // Clear all default flags
            currentPrinters = currentPrinters.map { it.copy(isDefault = false) }.toMutableList()
            
            // Set specified printer as default
            val printerIndex = currentPrinters.indexOfFirst { it.address == address }
            if (printerIndex >= 0) {
                currentPrinters[printerIndex] = currentPrinters[printerIndex].copy(isDefault = true)
                savePrinters(currentPrinters)
                setValue(DEFAULT_PRINTER_ADDRESS, address)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Get the default printer
     */
    fun getDefaultPrinter(): Flow<PrinterInfo?> {
        return combine(
            readPrinters(),
            getValue(DEFAULT_PRINTER_ADDRESS)
        ) { printers, defaultAddress ->
            printers.find { it.address == defaultAddress }
        }
    }

    /**
     * Add a supported language to a printer
     */
    suspend fun addSupportedLanguage(printerAddress: String, language: String) {
        try {
            val currentPrinters = readPrinters().first().toMutableList()
            val printerIndex = currentPrinters.indexOfFirst { it.address == printerAddress }
            
            if (printerIndex >= 0) {
                val printer = currentPrinters[printerIndex]
                val updatedSupportedLanguages = if (language in printer.supportedLanguages) {
                    printer.supportedLanguages
                } else {
                    printer.supportedLanguages + language
                }
                
                currentPrinters[printerIndex] = printer.copy(
                    supportedLanguages = updatedSupportedLanguages,
                    currentLanguage = language
                )
                savePrinters(currentPrinters)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Set current language for a printer
     */
    suspend fun setPrinterCurrentLanguage(printerAddress: String, language: String) {
        try {
            val currentPrinters = readPrinters().first().toMutableList()
            val printerIndex = currentPrinters.indexOfFirst { it.address == printerAddress }
            
            if (printerIndex >= 0) {
                val printer = currentPrinters[printerIndex]
                currentPrinters[printerIndex] = printer.copy(currentLanguage = language)
                savePrinters(currentPrinters)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Remove a supported language from a printer
     */
    suspend fun removeSupportedLanguage(printerAddress: String, language: String) {
        try {
            val currentPrinters = readPrinters().first().toMutableList()
            val printerIndex = currentPrinters.indexOfFirst { it.address == printerAddress }
            
            if (printerIndex >= 0) {
                val printer = currentPrinters[printerIndex]
                val updatedSupportedLanguages = printer.supportedLanguages.filter { it != language }
                val updatedCurrentLanguage = if (printer.currentLanguage == language) null else printer.currentLanguage
                
                currentPrinters[printerIndex] = printer.copy(
                    supportedLanguages = updatedSupportedLanguages,
                    currentLanguage = updatedCurrentLanguage
                )
                savePrinters(currentPrinters)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

//