package com.velox.jewelvault

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.velox.jewelvault.data.MetalRate
import com.velox.jewelvault.data.UpdateInfo
import com.velox.jewelvault.data.fetchAllMetalRates
import com.velox.jewelvault.data.roomdb.AppDatabase
import com.velox.jewelvault.data.DataStoreManager
import com.velox.jewelvault.data.bluetooth.BleManager
import com.velox.jewelvault.utils.AppUpdateManager
import com.velox.jewelvault.utils.FileManager
import com.velox.jewelvault.data.firebase.RemoteConfigManager
import com.velox.jewelvault.utils.SecurityUtils
import com.velox.jewelvault.utils.backup.BackupManager
import com.velox.jewelvault.utils.backup.BackupService
import com.velox.jewelvault.utils.ioLaunch
import com.velox.jewelvault.utils.log
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class BaseViewModel @Inject constructor(
    private val _dataStoreManager: DataStoreManager,
    private val _loadingState : MutableState<Boolean>,
    @Named("snackMessage") private val _snackBarState: MutableState<String>,
    @Named("currentScreenHeading") private val _currentScreenHeadingState: MutableState<String>,
    private val _metalRates: SnapshotStateList<MetalRate>,
    private val _appDatabase: AppDatabase,
    private val _remoteConfigManager: RemoteConfigManager,
    private val _appUpdateManager: AppUpdateManager,
    private val _backupManager: BackupManager,
    private val _auth: FirebaseAuth,
    private val _Internal_bluetoothManager: BleManager
    ) : ViewModel() {

    var loading by _loadingState
    var snackBarState by _snackBarState
    var currentScreenHeading by _currentScreenHeadingState
    val dataStoreManager = _dataStoreManager
    val metalRates = _metalRates
    val bluetoothReceiver = _Internal_bluetoothManager
    val metalRatesLoading = mutableStateOf(false)
    val isConnectedState = mutableStateOf(true)
    val storeImage = mutableStateOf<String?>(null)
    val storeName = mutableStateOf<String?>(null)
    val localLogoUri = mutableStateOf<Uri?>(null)
    
    // Update management
    val remoteConfigManager = _remoteConfigManager
    val appUpdateManager = _appUpdateManager
    val backupManager = _backupManager
    val updateInfo = mutableStateOf<UpdateInfo?>(null)
    val showUpdateDialog = mutableStateOf(false)
    val showForceUpdateDialog = mutableStateOf(false)
    val updateCheckLoading = mutableStateOf(false)

    /**
     * return Triple of Flow<String> for userId, userName, mobileNo
     * */

    val admin: Triple<Flow<String>, Flow<String>, Flow<String>> = _dataStoreManager.getAdminInfo()

    // Functional settings that are actually implemented
    val continuousNetworkCheck = mutableStateOf(false)
    val networkSpeedMonitoring = mutableStateOf(true)
    val autoRefreshMetalRates = mutableStateOf(true)
    val sessionTimeoutMinutes = mutableStateOf(30)
    val autoLogoutInactivity = mutableStateOf(true)
    val biometricAuth = mutableStateOf(false)
    val defaultCgst = mutableStateOf("1.5")
    val defaultSgst = mutableStateOf("1.5")
    val defaultIgst = mutableStateOf("0.0")

    // Data wipe verification state
    val showDataWipeConfirmation = mutableStateOf(false)
    val showPinVerificationDialog = mutableStateOf(false)
    val showOtpVerificationDialog = mutableStateOf(false)
    val pinForWipe = mutableStateOf("")
    val otpForWipe = mutableStateOf("")
    val isWipeInProgress = mutableStateOf(false)
    val otpVerificationId = mutableStateOf<String?>(null)

    init {
        loadSettings()
    }

    suspend fun refreshMetalRates(state: String = "visakhapatnam", context: Context) {
        metalRates.clear()
        metalRates.addAll(fetchAllMetalRates(state, context,metalRatesLoading))
    }

    fun loadStoreImage() {
        ioLaunch {
            try {
                val storeId = _dataStoreManager.getSelectedStoreInfo().first.first()
                val store = _appDatabase.storeDao().getStoreById(storeId)
                log("Loading store image: ${store?.image}")
                storeImage.value = store?.image
                
                // Load local logo file if available
                loadLocalLogo()
                
                // If no local logo but there's a URL in database, download and cache it
                if (!hasLocalLogo() && !store?.image.isNullOrBlank()) {
                    log("No local logo found, downloading from URL: ${store?.image}")
                    downloadAndCacheLogo(android.app.Application(), store?.image ?: "")
                }
            } catch (e: Exception) {
                log("Error loading store image: ${e.message}")
            }
        }
    }
    
    /**
     * Load local logo file URI
     */
    fun loadLocalLogo() {
        ioLaunch {
            try {
                val logoUri = FileManager.getLogoFileUri(android.app.Application())
                localLogoUri.value = logoUri
                log("Local logo URI loaded: $logoUri")
            } catch (e: Exception) {
                log("Error loading local logo: ${e.message}")
                localLogoUri.value = null
            }
        }
    }
    
    /**
     * Download and cache logo from URL
     */
    fun downloadAndCacheLogo(context: Context, imageUrl: String) {
        ioLaunch {
            try {
                log("Starting logo download and cache from: $imageUrl")
                val result = FileManager.downloadAndSaveLogo(context, imageUrl)
                
                if (result.isSuccess) {
                    localLogoUri.value = result.getOrNull()
                    log("Logo downloaded and cached successfully: ${result.getOrNull()}")
                } else {
                    log("Failed to download logo: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                log("Error downloading logo: ${e.message}")
            }
        }
    }
    
    /**
     * Get the logo URI to use (local file if available, otherwise null)
     */
    fun getLogoUri(): Uri? {
        return localLogoUri.value
    }
    
    /**
     * Check if local logo exists
     */
    fun hasLocalLogo(): Boolean {
        return localLogoUri.value != null
    }
    
    /**
     * Force refresh logo - download from URL even if local logo exists
     */
    fun forceRefreshLogo(context: Context) {
        ioLaunch {
            try {
                val storeId = _dataStoreManager.getSelectedStoreInfo().first.first()
                val store = _appDatabase.storeDao().getStoreById(storeId)
                
                if (!store?.image.isNullOrBlank()) {
                    log("Force refreshing logo from URL: ${store?.image}")
                    downloadAndCacheLogo(context, store?.image ?: "")
                }
            } catch (e: Exception) {
                log("Error force refreshing logo: ${e.message}")
            }
        }
    }

    fun loadStoreName() {
        ioLaunch {
            try {
                val storeId = _dataStoreManager.getSelectedStoreInfo().first.first()
                val store = _appDatabase.storeDao().getStoreById(storeId)
                log("Loading store image: ${store?.name}")
                storeName.value = store?.name
            } catch (e: Exception) {
                log("Error loading store image: ${e.message}")
            }
        }
    }
    
    // Update management functions
    fun checkForUpdates(context: Context, forceCheck: Boolean = false) {
        log("🚀 Starting update check process...")
        log("🚀 Force check: $forceCheck")
        
        ioLaunch {
            try {
                updateCheckLoading.value = true
                log("🚀 Update check loading set to true")
                
                // Check if we should check for updates
                if (!forceCheck && !remoteConfigManager.shouldCheckForUpdate()) {
                    log("⏰ Skipping update check - using cached data")
                    // Use cached update info if available
                    val cachedInfo = remoteConfigManager.getCachedUpdateInfo()
                    if (cachedInfo != null) {
                        log("📋 Using cached update info: $cachedInfo")
                        handleUpdateInfo(cachedInfo)
                    } else {
                        log("📋 No cached update info available")
                    }
                    updateCheckLoading.value = false
                    return@ioLaunch
                }
                
                log("🔄 Fetching fresh update info from Remote Config...")
                // Fetch fresh update info
                val fetchResult = remoteConfigManager.fetchAndActivate()
                if (fetchResult.isSuccess) {
                    log("✅ Remote Config fetch successful")
                    val info = remoteConfigManager.getUpdateInfo()
                    log("📋 Got update info: $info")
                    remoteConfigManager.cacheUpdateInfo(info)
                    log("💾 Cached update info")
                    remoteConfigManager.updateLastCheckTime()
                    log("⏰ Updated last check time")
                    handleUpdateInfo(info)
                } else {
                    log("❌ Remote Config fetch failed: ${fetchResult.exceptionOrNull()?.message}")
                    // Use cached info if fetch fails
                    val cachedInfo = remoteConfigManager.getCachedUpdateInfo()
                    if (cachedInfo != null) {
                        log("📋 Using cached update info after fetch failure: $cachedInfo")
                        handleUpdateInfo(cachedInfo)
                    } else {
                        log("📋 No cached update info available after fetch failure")
                    }
                }
            } catch (e: Exception) {
                log("❌ Error checking for updates: ${e.message}")
                log("❌ Exception details: ${e.javaClass.simpleName}")
                e.printStackTrace()
            } finally {
                updateCheckLoading.value = false
                log("🚀 Update check loading set to false")
            }
        }
    }
    
    private suspend fun handleUpdateInfo(info: UpdateInfo) {
        log("🎯 Handling update info: $info")
        updateInfo.value = info
        
        // Check if force update is required
        val isForceRequired = remoteConfigManager.isForceUpdateRequired()
        log("🔍 Force update required: $isForceRequired")
        if (isForceRequired) {
            log("🚨 Showing force update dialog")
            showForceUpdateDialog.value = true
            return
        }
        
        // Check if update is available
        val isUpdateAvailable = remoteConfigManager.isUpdateAvailable()
        log("🔍 Update available: $isUpdateAvailable")
        if (isUpdateAvailable) {
            log("📱 Showing update dialog")
            showUpdateDialog.value = true
        } else {
            log("📱 No update available - not showing dialog")
        }
    }
    
    fun onUpdateClick(context: Context) {
        log("🔄 Update button clicked")
        val info = updateInfo.value
        if (info != null) {
            try {
                BackupService.startBackup(context)
            }catch (e: Exception){

            }
            log("📱 Opening Play Store with update info: $info")
            appUpdateManager.openPlayStore(context, info)
        } else {
            log("📱 Opening Play Store without update info")
            appUpdateManager.openPlayStore(context)
        }
    }
    
    fun dismissUpdateDialog() {
        showUpdateDialog.value = false
    }
    
    fun dismissForceUpdateDialog() {
        showForceUpdateDialog.value = false
    }

    private fun loadSettings() {
        ioLaunch {
            try {
                continuousNetworkCheck.value =
                    _dataStoreManager.getValue(DataStoreManager.CONTINUOUS_NETWORK_CHECK, true)
                        .first() ?: true
                networkSpeedMonitoring.value =
                    _dataStoreManager.getValue(DataStoreManager.NETWORK_SPEED_MONITORING, false)
                        .first() ?: true
                autoRefreshMetalRates.value =
                    _dataStoreManager.getValue(DataStoreManager.AUTO_REFRESH_METAL_RATES, true)
                        .first() ?: true
                sessionTimeoutMinutes.value =
                    _dataStoreManager.getValue(DataStoreManager.SESSION_TIMEOUT_MINUTES, 30).first()
                        ?: 30
                autoLogoutInactivity.value =
                    _dataStoreManager.getValue(DataStoreManager.AUTO_LOGOUT_INACTIVITY, true)
                        .first() ?: true
                biometricAuth.value =
                    _dataStoreManager.getValue(DataStoreManager.BIOMETRIC_AUTH, false).first()
                        ?: false
                defaultCgst.value =
                    _dataStoreManager.getValue(DataStoreManager.DEFAULT_CGST, "1.5").first()
                        ?: "1.5"
                defaultSgst.value =
                    _dataStoreManager.getValue(DataStoreManager.DEFAULT_SGST, "1.5").first()
                        ?: "1.5"
                defaultIgst.value =
                    _dataStoreManager.getValue(DataStoreManager.DEFAULT_IGST, "0.0").first()
                        ?: "0.0"
            } catch (e: Exception) {
                _snackBarState.value = "Failed to load settings: ${e.message}"
            }
        }
    }

    fun updateSetting(key: String, value: Any) {
        ioLaunch {
            try {
                when (key) {
                    "continuous_network_check" -> {
                        _dataStoreManager.setValue(
                            DataStoreManager.CONTINUOUS_NETWORK_CHECK,
                            value as Boolean
                        )
                        continuousNetworkCheck.value = value as Boolean
                        _snackBarState.value =
                            if (value as Boolean) "Network monitoring enabled" else "Network monitoring disabled"
                    }

                    "network_speed_monitoring" -> {
                        _dataStoreManager.setValue(
                            DataStoreManager.NETWORK_SPEED_MONITORING,
                            value as Boolean
                        )
                        networkSpeedMonitoring.value = value as Boolean
                        _snackBarState.value =
                            if (value as Boolean) "Speed monitoring enabled" else "Speed monitoring disabled"
                    }

                    "auto_refresh_metal_rates" -> {
                        _dataStoreManager.setValue(
                            DataStoreManager.AUTO_REFRESH_METAL_RATES,
                            value as Boolean
                        )
                        autoRefreshMetalRates.value = value as Boolean
                        _snackBarState.value =
                            if (value as Boolean) "Auto-refresh enabled" else "Auto-refresh disabled"
                    }

                    "session_timeout_minutes" -> {
                        _dataStoreManager.setValue(
                            DataStoreManager.SESSION_TIMEOUT_MINUTES,
                            value as Int
                        )
                        sessionTimeoutMinutes.value = value as Int
                        _snackBarState.value = "Session timeout updated to ${value as Int} minutes"
                    }

                    "auto_logout_inactivity" -> {
                        _dataStoreManager.setValue(
                            DataStoreManager.AUTO_LOGOUT_INACTIVITY,
                            value as Boolean
                        )
                        autoLogoutInactivity.value = value as Boolean
                        _snackBarState.value =
                            if (value as Boolean) "Auto-logout enabled" else "Auto-logout disabled"
                    }

                    "biometric_auth" -> {
                        _dataStoreManager.setValue(
                            DataStoreManager.BIOMETRIC_AUTH,
                            value as Boolean
                        )
                        biometricAuth.value = value as Boolean
                        _snackBarState.value =
                            if (value as Boolean) "Biometric authentication enabled" else "Biometric authentication disabled"
                    }

                    "default_cgst" -> {
                        _dataStoreManager.setValue(DataStoreManager.DEFAULT_CGST, value as String)
                        defaultCgst.value = value as String
                        _snackBarState.value = "CGST rate updated to ${value as String}%"
                    }

                    "default_sgst" -> {
                        _dataStoreManager.setValue(DataStoreManager.DEFAULT_SGST, value as String)
                        defaultSgst.value = value as String
                        _snackBarState.value = "SGST rate updated to ${value as String}%"
                    }

                    "default_igst" -> {
                        _dataStoreManager.setValue(DataStoreManager.DEFAULT_IGST, value as String)
                        defaultIgst.value = value as String
                        _snackBarState.value = "IGST rate updated to ${value as String}%"
                    }
                }
            } catch (e: Exception) {
                _snackBarState.value = "Failed to update setting: ${e.message}"
            }
        }
    }

    fun initiateDataWipe() {
        showPinVerificationDialog.value = true
    }

    fun verifyPinForWipe(pin: String) {
        ioLaunch {
            try {
                val userId = admin.first.first()
                val currentUser = _appDatabase.userDao().getUserById(userId)
                if (currentUser != null) {
                    val hashedPin = SecurityUtils.hashPin(pin)
                    if (currentUser.pin == hashedPin) {
                        pinForWipe.value = pin
                        showPinVerificationDialog.value = false
                        sendOtpForWipe()
                    } else {
                        _snackBarState.value = "Incorrect PIN"
                    }
                } else {
                    _snackBarState.value = "User not found"
                }
            } catch (e: Exception) {
                _snackBarState.value = "Error verifying PIN: ${e.message}"
            }
        }
    }

    private fun sendOtpForWipe() {
        ioLaunch {
            try {
                val userId = admin.first.first()
                val currentUser = _appDatabase.userDao().getUserById(userId)
                if (currentUser != null) {
                    val phoneNumber = currentUser.mobileNo
                    if (phoneNumber.isNotEmpty()) {
                        val options = PhoneAuthOptions.newBuilder(_auth)
                            .setPhoneNumber(phoneNumber)
                            .setTimeout(60L, TimeUnit.SECONDS)
                            .setCallbacks(object :
                                PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                                    // Auto-verification completed
                                    verifyOtpForWipe(credential.smsCode ?: "")
                                }

                                override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                                    _snackBarState.value = "OTP verification failed: ${e.message}"
                                    showOtpVerificationDialog.value = false
                                }

                                override fun onCodeSent(
                                    verificationId: String,
                                    token: PhoneAuthProvider.ForceResendingToken
                                ) {
                                    otpVerificationId.value = verificationId
                                    showOtpVerificationDialog.value = true
                                }
                            })
                            .build()
                        PhoneAuthProvider.verifyPhoneNumber(options)
                    } else {
                        _snackBarState.value = "Phone number not found"
                    }
                } else {
                    _snackBarState.value = "User not found"
                }
            } catch (e: Exception) {
                _snackBarState.value = "Error sending OTP: ${e.message}"
            }
        }
    }

    fun verifyOtpForWipe(otp: String) {
        try {
            val verificationId = otpVerificationId.value
            if (verificationId != null) {
                val credential = PhoneAuthProvider.getCredential(verificationId, otp)
                _auth.signInWithCredential(credential)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            performDataWipe()
                        } else {
                            _snackBarState.value = "Invalid OTP"
                        }
                    }
            } else {
                _snackBarState.value = "OTP verification ID not found"
            }
        } catch (e: Exception) {
            _snackBarState.value = "Error verifying OTP: ${e.message}"
        }
    }

    private fun performDataWipe() {
        isWipeInProgress.value = true
        ioLaunch {
            try {
                // Clear all database tables
                _appDatabase.clearAllTables()

                // Clear DataStore preferences
                _dataStoreManager.clearAllData()

                // Reset all state variables
                resetAllSettings()

                _snackBarState.value = "All data wiped successfully"
                isWipeInProgress.value = false
                showOtpVerificationDialog.value = false

                // Navigate to login screen (this will be handled by the UI)

            } catch (e: Exception) {
                _snackBarState.value = "Error wiping data: ${e.message}"
                isWipeInProgress.value = false
            }
        }
    }

    private fun resetAllSettings() {
        continuousNetworkCheck.value = true
        networkSpeedMonitoring.value = true
        autoRefreshMetalRates.value = true
        sessionTimeoutMinutes.value = 30
        autoLogoutInactivity.value = true
        biometricAuth.value = false
        defaultCgst.value = "1.5"
        defaultSgst.value = "1.5"
        defaultIgst.value = "0.0"
    }

    fun getAppVersion(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    fun resetAppPreferences() {
        ioLaunch {
            try {
                resetAllSettings()
                loadSettings()
                _snackBarState.value = "App preferences reset successfully"
            } catch (e: Exception) {
                _snackBarState.value = "Error resetting preferences: ${e.message}"
            }
        }
    }
}