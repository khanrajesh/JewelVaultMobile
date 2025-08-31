package com.velox.jewelvault.utils

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.firebase.FirebaseApp
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.gson.Gson
import com.velox.jewelvault.data.DataStoreManager
import com.velox.jewelvault.data.UpdateInfo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteConfigManager @Inject constructor(
    private val context: Context,
    private val dataStoreManager: DataStoreManager
) {
    private val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
    private val gson = Gson()
    
    companion object {
        private const val CONFIG_CACHE_EXPIRATION = 0L // 0 seconds for development (allows immediate fetches)
        private const val FORCE_UPDATE_KEY = "force_update"
        private const val LATEST_VERSION_CODE_KEY = "latest_version_code"
        private const val LATEST_VERSION_NAME_KEY = "latest_version_name"
        private const val PLAY_STORE_URL_KEY = "play_store_url"
        private const val UPDATE_MESSAGE_KEY = "update_message"
        private const val UPDATE_TITLE_KEY = "update_title"
        private const val MIN_REQUIRED_VERSION_KEY = "min_required_version"
        private const val LAST_UPDATE_CHECK_KEY = "last_update_check"
        private const val CACHED_UPDATE_INFO_KEY = "cached_update_info"
    }
    
    init {
        setupRemoteConfig()
    }
    
    private fun setupRemoteConfig() {
        log("🔧 Setting up Firebase Remote Config...")
        
        // Use 0 seconds for development to allow immediate fetches
        val cacheExpiration = 0L
        log("🔧 Cache expiration set to: ${cacheExpiration}s (development mode)")
        
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(cacheExpiration)
            .build()
        remoteConfig.setConfigSettingsAsync(configSettings)
        
//        // Set default values to null/empty to force Firebase values
//        val defaultValues = mapOf(
//            FORCE_UPDATE_KEY to null,
//            LATEST_VERSION_CODE_KEY to null,
//            LATEST_VERSION_NAME_KEY to "",
//            PLAY_STORE_URL_KEY to "",
//            UPDATE_MESSAGE_KEY to "",
//            UPDATE_TITLE_KEY to "",
//            MIN_REQUIRED_VERSION_KEY to null
//        )
//        remoteConfig.setDefaultsAsync(defaultValues)
        log("🔧 Default values set to null/empty to force Firebase values")
        log("🔧 Remote Config setup completed with cache expiration: ${CONFIG_CACHE_EXPIRATION}s")
    }
    
    suspend fun fetchAndActivate(): Result<Unit> {
        return try {
            log("🔄 Starting Firebase Remote Config fetch and activate...")
            
            // Check internet connectivity first
            checkInternetConnectivity()
            
            // Log initial status
            logRemoteConfigStatus()
            
            // List all available parameters
            listAllParameters()
            
            // Force fetch by setting a very short minimum fetch interval
            val configSettings = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(0L)
                .build()
            remoteConfig.setConfigSettingsAsync(configSettings)
            log("🔧 Config settings updated with 0s fetch interval")
            
            log("🔄 Calling remoteConfig.fetchAndActivate()...")
            val result = remoteConfig.fetchAndActivate().await()
            log("🔄 Remote Config fetch result: $result")
            
            if (result) {
                log("✅ Remote Config fetched and activated successfully")
            } else {
                log("⚠️ Remote Config fetched but not activated (cached data used)")
                log("⚠️ This means the fetch succeeded but no new data was available")
            }
            
            // Log the current config state
            log("🔍 Current Remote Config state:")
            log("   - Last fetch time: ${remoteConfig.info.fetchTimeMillis}")
            log("   - Last fetch status: ${remoteConfig.info.lastFetchStatus}")
            log("   - Config settings: ${remoteConfig.info.configSettings}")
            
            Result.success(Unit)
        } catch (e: Exception) {
            log("❌ Remote Config fetch failed!")
            log("❌ Exception type: ${e.javaClass.simpleName}")
            log("❌ Exception message: ${e.message}")
            log("❌ Exception cause: ${e.cause?.message}")
            log("❌ Stack trace:")
            e.printStackTrace()
            
            // Check if it's a network-related error
            val isNetworkError = e.message?.contains("Unable to resolve host") == true ||
                    e.message?.contains("No address associated with hostname") == true ||
                    e.message?.contains("Network is unreachable") == true
            
            if (isNetworkError) {
                log("🌐 Network error detected - this is likely a connectivity issue")
                log("🌐 Please check:")
                log("   - Internet connection")
                log("   - WiFi/Cellular data")
                log("   - VPN/Proxy settings")
                log("   - Firewall settings")
            }
            
            Result.failure(e)
        }
    }
    
    suspend fun getUpdateInfo(): UpdateInfo {
        return try {
            log("📋 Getting update info from Remote Config...")
            log("📋 Using keys without prefix (direct parameter names)")
            
            log("📋 Attempting to get boolean value for: $FORCE_UPDATE_KEY")
            val forceUpdate = remoteConfig.getBoolean(FORCE_UPDATE_KEY)
            log("📋 Force update value retrieved: $forceUpdate")
            
            log("📋 Attempting to get long value for: $LATEST_VERSION_CODE_KEY")
            val latestVersionCode = remoteConfig.getLong(LATEST_VERSION_CODE_KEY)
            log("📋 Latest version code retrieved: $latestVersionCode")
            
            log("📋 Attempting to get string value for: $LATEST_VERSION_NAME_KEY")
            val latestVersionName = remoteConfig.getString(LATEST_VERSION_NAME_KEY)
            log("📋 Latest version name retrieved: '$latestVersionName'")
            
            log("📋 Attempting to get string value for: $PLAY_STORE_URL_KEY")
            val playStoreUrl = remoteConfig.getString(PLAY_STORE_URL_KEY)
            log("📋 Play store URL retrieved: '$playStoreUrl'")
            
            log("📋 Attempting to get string value for: $UPDATE_MESSAGE_KEY")
            val updateMessage = remoteConfig.getString(UPDATE_MESSAGE_KEY)
            log("📋 Update message retrieved: '$updateMessage'")
            
            log("📋 Attempting to get string value for: $UPDATE_TITLE_KEY")
            val updateTitle = remoteConfig.getString(UPDATE_TITLE_KEY)
            log("📋 Update title retrieved: '$updateTitle'")
            
            log("📋 Attempting to get long value for: $MIN_REQUIRED_VERSION_KEY")
            val minRequiredVersion = remoteConfig.getLong(MIN_REQUIRED_VERSION_KEY)
            log("📋 Min required version retrieved: $minRequiredVersion")
            
            log("📋 Remote Config values:")
            log("   - Force Update: $forceUpdate")
            log("   - Latest Version Code: $latestVersionCode")
            log("   - Latest Version Name: $latestVersionName")
            log("   - Play Store URL: $playStoreUrl")
            log("   - Update Message: $updateMessage")
            log("   - Update Title: $updateTitle")
            log("   - Min Required Version: $minRequiredVersion")
            
            val updateInfo = UpdateInfo.fromRemoteConfig(
                forceUpdate = forceUpdate,
                latestVersionCode = latestVersionCode,
                latestVersionName = latestVersionName,
                playStoreUrl = playStoreUrl,
                updateMessage = updateMessage,
                updateTitle = updateTitle,
                minRequiredVersion = minRequiredVersion
            )
            
            log("📋 Created UpdateInfo: $updateInfo")
            updateInfo
        } catch (e: Exception) {
            log("❌ Failed to get update info from Remote Config!")
            log("❌ Exception type: ${e.javaClass.simpleName}")
            log("❌ Exception message: ${e.message}")
            log("❌ Exception cause: ${e.cause?.message}")
            log("❌ Stack trace:")
            e.printStackTrace()
            
            // Return default values if remote config fails
            val defaultInfo = UpdateInfo()
            log("📋 Returning default UpdateInfo: $defaultInfo")
            defaultInfo
        }
    }
    
    fun getCurrentAppVersion(): Int {
        return try {
            val packageInfo: PackageInfo = context.packageManager.getPackageInfo(
                context.packageName, 0
            )
            val versionCode = packageInfo.longVersionCode.toInt()
            log("📱 Current app version code: $versionCode")
            versionCode
        } catch (e: PackageManager.NameNotFoundException) {
            log("❌ Failed to get current app version: ${e.message}")
            1
        }
    }
    
    fun getCurrentAppVersionName(): String {
        return try {
            val packageInfo: PackageInfo = context.packageManager.getPackageInfo(
                context.packageName, 0
            )
            val versionName = packageInfo.versionName ?: "1.0"
            log("📱 Current app version name: $versionName")
            versionName
        } catch (e: PackageManager.NameNotFoundException) {
            log("❌ Failed to get current app version name: ${e.message}")
            "1.0"
        }
    }
    
    suspend fun isUpdateAvailable(): Boolean {
        val currentVersion = getCurrentAppVersion()
        val updateInfo = getUpdateInfo()
        val isAvailable = updateInfo.latestVersionCode > currentVersion
        log("🔍 Update available check:")
        log("   - Current version: $currentVersion")
        log("   - Latest version: ${updateInfo.latestVersionCode}")
        log("   - Update available: $isAvailable")
        return isAvailable
    }
    
    suspend fun isForceUpdateRequired(): Boolean {
        val currentVersion = getCurrentAppVersion()
        val updateInfo = getUpdateInfo()
        val isForceRequired = updateInfo.forceUpdate && updateInfo.latestVersionCode > currentVersion
        log("🔍 Force update check:")
        log("   - Current version: $currentVersion")
        log("   - Latest version: ${updateInfo.latestVersionCode}")
        log("   - Force update flag: ${updateInfo.forceUpdate}")
        log("   - Force update required: $isForceRequired")
        return isForceRequired
    }
    
    suspend fun isMinimumVersionRequired(): Boolean {
        val currentVersion = getCurrentAppVersion()
        val updateInfo = getUpdateInfo()
        return currentVersion < updateInfo.minRequiredVersion
    }
    
    suspend fun shouldCheckForUpdate(): Boolean {
        val lastCheck = dataStoreManager.getValue(longPreferencesKey(LAST_UPDATE_CHECK_KEY), 0L).first()
        val currentTime = System.currentTimeMillis()
        val timeSinceLastCheck = currentTime - (lastCheck ?: 0L)
        val shouldCheck = timeSinceLastCheck > (6 * 60 * 60 * 1000)
        
        log("⏰ Should check for update:")
        log("   - Last check time: ${lastCheck ?: 0L}")
        log("   - Current time: $currentTime")
        log("   - Time since last check: ${timeSinceLastCheck}ms (${timeSinceLastCheck / (1000 * 60 * 60)} hours)")
        log("   - Should check: $shouldCheck")
        
        // Check every 6 hours
        return shouldCheck
    }
    
    suspend fun updateLastCheckTime() {
        dataStoreManager.setValue(longPreferencesKey(LAST_UPDATE_CHECK_KEY), System.currentTimeMillis())
    }
    
    suspend fun cacheUpdateInfo(updateInfo: UpdateInfo) {
        // Cache the update info as JSON string using Gson
        val jsonString = gson.toJson(updateInfo)
        dataStoreManager.setValue(stringPreferencesKey(CACHED_UPDATE_INFO_KEY), jsonString)
    }
    
    suspend fun getCachedUpdateInfo(): UpdateInfo? {
        val cachedJson = dataStoreManager.getValue(stringPreferencesKey(CACHED_UPDATE_INFO_KEY), "").first()
        return if (cachedJson?.isNotEmpty() == true) {
            try {
                // Parse cached data using Gson
                gson.fromJson(cachedJson, UpdateInfo::class.java)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }
    
    // Method to list all available Remote Config parameters
    private fun listAllParameters() {
        log("📋 Listing all available Remote Config parameters...")
        try {
            // Get all parameter keys (this is a workaround since Firebase doesn't provide direct access)
            val allKeys = remoteConfig.all
            log("📋 Total parameters available: ${allKeys.size}")
            
            // Try to get some common parameter patterns
            val testKeys = listOf(
                "force_update",
                "latest_version_code", 
                "latest_version_name",
                "play_store_url",
                "update_message",
                "update_title",
                "min_required_version",
                "app_update_param/force_update",
                "app_update_param/latest_version_code",
                "app_update_param/latest_version_name",
                "app_update_param/play_store_url",
                "app_update_param/update_message",
                "app_update_param/update_title",
                "app_update_param/min_required_version"
            )
            
            log("📋 Testing parameter existence:")
            testKeys.forEach { key ->
                try {
                    val value = remoteConfig.getString(key)
                    if (value.isNotEmpty()) {
                        log("   ✅ Found: $key = '$value'")
                    } else {
                        log("   ❌ Empty: $key")
                    }
                } catch (e: Exception) {
                    log("   ❌ Not found: $key")
                }
            }
        } catch (e: Exception) {
            log("❌ Error listing parameters: ${e.message}")
        }
    }
    
    // Method to check internet connectivity
    private fun checkInternetConnectivity() {
        log("🌐 Checking internet connectivity...")
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            
            if (capabilities != null) {
                val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                val hasValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                
                log("🌐 Internet connectivity check:")
                log("   - Has internet capability: $hasInternet")
                log("   - Has validated connection: $hasValidated")
                log("   - Network type: ${getNetworkType(capabilities)}")
                
                if (!hasInternet || !hasValidated) {
                    log("⚠️ No valid internet connection detected!")
                }
            } else {
                log("⚠️ No active network found!")
            }
        } catch (e: Exception) {
            log("❌ Error checking internet connectivity: ${e.message}")
        }
    }
    
    private fun getNetworkType(capabilities: NetworkCapabilities): String {
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Unknown"
        }
    }
    
    // Method to clear all cached data for testing
    suspend fun clearAllCache() {
        log("🧹 Clearing all cached data...")
        dataStoreManager.setValue(stringPreferencesKey(CACHED_UPDATE_INFO_KEY), "")
        dataStoreManager.setValue(longPreferencesKey(LAST_UPDATE_CHECK_KEY), 0L)
        log("🧹 All cached data cleared")
    }
    
    // Method to force reinitialize Remote Config
    suspend fun forceReinitialize() {
        log("🔄 Force reinitializing Remote Config...")
        
        // Clear all cached data
        clearAllCache()
        
        // Re-setup Remote Config with fresh settings
        setupRemoteConfig()
        
        // Force a fresh fetch
        val result = fetchAndActivate()
        if (result.isSuccess) {
            log("✅ Remote Config reinitialized successfully")
        } else {
            log("❌ Remote Config reinitialization failed: ${result.exceptionOrNull()?.message}")
        }
    }
    
    // Method to check Remote Config status
    fun logRemoteConfigStatus() {
        log("🔍 Remote Config Status Check:")
        log("   - Instance: ${remoteConfig}")
        log("   - Info: ${remoteConfig.info}")
        log("   - Last fetch time: ${remoteConfig.info.fetchTimeMillis}")
        log("   - Last fetch status: ${remoteConfig.info.lastFetchStatus}")
        log("   - Config settings: ${remoteConfig.info.configSettings}")
        log("   - Minimum fetch interval: ${remoteConfig.info.configSettings.minimumFetchIntervalInSeconds}")
        log("   - Fetch timeout: ${remoteConfig.info.configSettings.fetchTimeoutInSeconds}")
        
        // Check if we can get any values at all
        log("🔍 Testing value retrieval:")
        try {
            val testValue = remoteConfig.getString("test_key_that_doesnt_exist")
            log("   - Non-existent key test: '$testValue'")
        } catch (e: Exception) {
            log("   - Error getting non-existent key: ${e.message}")
        }
        
        // Check Firebase app configuration
        log("🔍 Firebase App Check:")
        try {
            val firebaseApp = FirebaseApp.getInstance()
            log("   - Firebase App: ${firebaseApp.name}")
            log("   - Firebase Project ID: ${firebaseApp.options.projectId}")
            log("   - Firebase App ID: ${firebaseApp.options.applicationId}")
        } catch (e: Exception) {
            log("   - Firebase App error: ${e.message}")
        }
    }
} 