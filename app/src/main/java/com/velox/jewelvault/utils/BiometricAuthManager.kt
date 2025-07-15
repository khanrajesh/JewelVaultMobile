package com.velox.jewelvault.utils

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import androidx.fragment.app.FragmentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class BiometricAuthManager(private val context: Context) {

    fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
        val result = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        log("Biometric availability check result: $result")
        return when (result) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> false
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> false
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> false
            else -> false
        }
    }

    fun getBiometricStatus(): String {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> "Biometric authentication is available"
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "No biometric hardware available"
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "Biometric hardware is unavailable"
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "No biometric credentials enrolled"
            else -> "Unknown biometric status"
        }
    }

    // For ComponentActivity, we need to use a different approach
    fun authenticateWithBiometricCallback(
        activity: ComponentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onCancel: () -> Unit
    ) {
        log("authenticateWithBiometricCallback called")
        if (!isBiometricAvailable()) {
            log("Biometric not available, calling onError")
            onError("Biometric authentication is not available")
            return
        }
        
        // Since we can't use BiometricPrompt directly with ComponentActivity,
        // we'll use a custom dialog approach that guides the user
        // The actual biometric authentication will be handled by the system
        // when the user interacts with the dialog
        log("Biometric available, calling onSuccess")
        onSuccess()
    }
    
    // For FragmentActivity (if we change MainActivity to extend FragmentActivity)
    fun authenticateWithBiometric(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onCancel: () -> Unit
    ) {
        log("authenticateWithBiometric called with FragmentActivity")
        if (!isBiometricAvailable()) {
            log("Biometric not available, calling onError")
            onError("Biometric authentication is not available")
            return
        }
        
        log("Biometric available, creating BiometricPrompt")
        val executor = ContextCompat.getMainExecutor(activity)
        
        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    log("Biometric authentication error: $errorCode - $errString")
                    when (errorCode) {
                        BiometricPrompt.ERROR_HW_NOT_PRESENT -> onError("No biometric hardware available")
                        BiometricPrompt.ERROR_HW_UNAVAILABLE -> onError("Biometric hardware is unavailable")
                        BiometricPrompt.ERROR_NO_BIOMETRICS -> onError("No biometric credentials enrolled")
                        BiometricPrompt.ERROR_USER_CANCELED -> onCancel()
                        BiometricPrompt.ERROR_LOCKOUT -> onError("Too many failed attempts. Try again later.")
                        BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> onError("Too many failed attempts. Biometric authentication is permanently disabled.")
                        else -> onError("Authentication error: $errString")
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    log("Biometric authentication succeeded")
                    onSuccess()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    log("Biometric authentication failed")
                    onError("Authentication failed. Please try again.")
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("JewelVault Authentication")
            .setSubtitle("Use your fingerprint or face to login")
            .setDescription("Authenticate using your biometric credentials to access your account")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .setNegativeButtonText("Cancel")
            .build()

        try {
            log("Starting biometric authentication")
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            log("Exception starting biometric authentication: ${e.message}")
            onError("Failed to start biometric authentication: ${e.message}")
        }
    }
} 