package com.velox.jewelvault.ui.screen.setting

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.velox.jewelvault.data.roomdb.AppDatabase
import com.velox.jewelvault.data.DataStoreManager
import com.velox.jewelvault.utils.SecurityUtils
import com.velox.jewelvault.utils.ioLaunch
import com.velox.jewelvault.utils.ioScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class SettingViewModel @Inject constructor(
    private val _dataStoreManager: DataStoreManager,
    private val _appDatabase: AppDatabase,
    private val _auth: FirebaseAuth,
    private val _snackBarState: MutableState<String>
) : ViewModel() {

    val snackBarState = _snackBarState

    // Functional settings that are actually implemented
    val continuousNetworkCheck = mutableStateOf(true)
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

    private fun loadSettings() {
        ioLaunch {
            try {
                continuousNetworkCheck.value = _dataStoreManager.getValue(DataStoreManager.CONTINUOUS_NETWORK_CHECK, true).first() ?: true
                networkSpeedMonitoring.value = _dataStoreManager.getValue(DataStoreManager.NETWORK_SPEED_MONITORING, true).first() ?: true
                autoRefreshMetalRates.value = _dataStoreManager.getValue(DataStoreManager.AUTO_REFRESH_METAL_RATES, true).first() ?: true
                sessionTimeoutMinutes.value = _dataStoreManager.getValue(DataStoreManager.SESSION_TIMEOUT_MINUTES, 30).first() ?: 30
                autoLogoutInactivity.value = _dataStoreManager.getValue(DataStoreManager.AUTO_LOGOUT_INACTIVITY, true).first() ?: true
                biometricAuth.value = _dataStoreManager.getValue(DataStoreManager.BIOMETRIC_AUTH, false).first() ?: false
                defaultCgst.value = _dataStoreManager.getValue(DataStoreManager.DEFAULT_CGST, "1.5").first() ?: "1.5"
                defaultSgst.value = _dataStoreManager.getValue(DataStoreManager.DEFAULT_SGST, "1.5").first() ?: "1.5"
                defaultIgst.value = _dataStoreManager.getValue(DataStoreManager.DEFAULT_IGST, "0.0").first() ?: "0.0"
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
                        _dataStoreManager.setValue(DataStoreManager.CONTINUOUS_NETWORK_CHECK, value as Boolean)
                        continuousNetworkCheck.value = value as Boolean
                        _snackBarState.value = if (value as Boolean) "Network monitoring enabled" else "Network monitoring disabled"
                    }
                    "network_speed_monitoring" -> {
                        _dataStoreManager.setValue(DataStoreManager.NETWORK_SPEED_MONITORING, value as Boolean)
                        networkSpeedMonitoring.value = value as Boolean
                        _snackBarState.value = if (value as Boolean) "Speed monitoring enabled" else "Speed monitoring disabled"
                    }
                    "auto_refresh_metal_rates" -> {
                        _dataStoreManager.setValue(DataStoreManager.AUTO_REFRESH_METAL_RATES, value as Boolean)
                        autoRefreshMetalRates.value = value as Boolean
                        _snackBarState.value = if (value as Boolean) "Auto-refresh enabled" else "Auto-refresh disabled"
                    }
                    "session_timeout_minutes" -> {
                        _dataStoreManager.setValue(DataStoreManager.SESSION_TIMEOUT_MINUTES, value as Int)
                        sessionTimeoutMinutes.value = value as Int
                        _snackBarState.value = "Session timeout updated to ${value as Int} minutes"
                    }
                    "auto_logout_inactivity" -> {
                        _dataStoreManager.setValue(DataStoreManager.AUTO_LOGOUT_INACTIVITY, value as Boolean)
                        autoLogoutInactivity.value = value as Boolean
                        _snackBarState.value = if (value as Boolean) "Auto-logout enabled" else "Auto-logout disabled"
                    }
                    "biometric_auth" -> {
                        _dataStoreManager.setValue(DataStoreManager.BIOMETRIC_AUTH, value as Boolean)
                        biometricAuth.value = value as Boolean
                        _snackBarState.value = if (value as Boolean) "Biometric authentication enabled" else "Biometric authentication disabled"
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
                val userId = _dataStoreManager.userId.first()
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
                val userId = _dataStoreManager.userId.first()
                val currentUser = _appDatabase.userDao().getUserById(userId)
                if (currentUser != null) {
                    val phoneNumber = currentUser.mobileNo
                    if (phoneNumber.isNotEmpty()) {
                        val options = PhoneAuthOptions.newBuilder(_auth)
                            .setPhoneNumber(phoneNumber)
                            .setTimeout(60L, TimeUnit.SECONDS)
                            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
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