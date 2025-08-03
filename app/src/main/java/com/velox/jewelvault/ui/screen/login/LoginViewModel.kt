
package com.velox.jewelvault.ui.screen.login

import android.app.Activity
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.velox.jewelvault.data.roomdb.AppDatabase
import com.velox.jewelvault.data.roomdb.entity.users.UsersEntity
import com.velox.jewelvault.data.DataStoreManager
import com.velox.jewelvault.utils.ioLaunch
import com.velox.jewelvault.utils.ioScope
import com.velox.jewelvault.utils.SecurityUtils
import com.velox.jewelvault.utils.SessionManager
import com.velox.jewelvault.utils.InputValidator
import com.velox.jewelvault.utils.BiometricAuthManager
import com.velox.jewelvault.utils.log
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val _appDatabase: AppDatabase,
    private val _dataStoreManager: DataStoreManager,
    private val _sessionManager: SessionManager,
    private val _loadingState: MutableState<Boolean>,
    private val _snackBarState: MutableState<String>,
    private val _auth: FirebaseAuth,
    private val _fireStore: FirebaseFirestore
) : ViewModel() {

    val snackBarState = _snackBarState
    val isOtpGenerated = mutableStateOf(false)
    val isOtpVerified = mutableStateOf(false)

    val firebaseUser = mutableStateOf<FirebaseUser?>(null)

    val otpVerificationId = mutableStateOf<String?>(null)
    
    // Biometric authentication state
    val isBiometricAvailable = mutableStateOf(false)
    val biometricAuthEnabled = mutableStateOf(false)


    suspend fun userExits():Boolean{
        return _appDatabase.userDao().getUserCount() >= 1
    }
    
    // Check biometric availability
    fun checkBiometricAvailability(context: android.content.Context) {
        val biometricAuthManager = BiometricAuthManager(context)
        val isAvailable = biometricAuthManager.isBiometricAvailable()
        log("Biometric availability check: $isAvailable")
        isBiometricAvailable.value = isAvailable
    }
    
    // Save phone number to DataStore
    suspend fun savePhoneNumber(phoneNumber: String) {
        try {
            _dataStoreManager.setValue(DataStoreManager.SAVED_PHONE_NUMBER, phoneNumber)
            log("Phone number saved: $phoneNumber")
        } catch (e: Exception) {
            log("Failed to save phone number: ${e.message}")
            _snackBarState.value = "Failed to save phone number: ${e.message}"
        }
    }
    
    // Get saved phone number from DataStore
    suspend fun getSavedPhoneNumber(): String? {
        return try {
            val phone = _dataStoreManager.getValue(DataStoreManager.SAVED_PHONE_NUMBER, "").first()
            log("Retrieved saved phone number: $phone")
            phone
        } catch (e: Exception) {
            log("Failed to get saved phone number: ${e.message}")
            null
        }
    }
    
    // Get biometric setting from DataStore
    suspend fun getBiometricSetting(): Boolean {
        return try {
            val enabled = _dataStoreManager.getValue(DataStoreManager.BIOMETRIC_AUTH, false).first() ?: false
            log("Biometric setting retrieved: $enabled")
            enabled
        } catch (e: Exception) {
            log("Failed to get biometric setting: ${e.message}")
            false
        }
    }
    
    // Authenticate with biometric using callback approach
    fun authenticateWithBiometric(
        context: android.content.Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onCancel: () -> Unit
    ) {
        log("authenticateWithBiometric called")
        val biometricAuthManager = BiometricAuthManager(context)
        
        // Check if biometric is enabled in settings and start authentication on main thread
        ioScope {
            val biometricEnabled = _dataStoreManager.getValue(DataStoreManager.BIOMETRIC_AUTH, false).first() ?: false
            log("Biometric enabled in settings: $biometricEnabled")
            if (!biometricEnabled) {
                log("Biometric disabled in settings, calling onError")
                onError("Biometric authentication is disabled in settings")
                return@ioScope
            }
            
            // Switch to main thread for biometric authentication
            kotlinx.coroutines.MainScope().launch {
                try {
                    // Check if context is FragmentActivity
                    if (context is androidx.fragment.app.FragmentActivity) {
                        log("Context is FragmentActivity, using authenticateWithBiometric")
                        biometricAuthManager.authenticateWithBiometric(
                            activity = context,
                            onSuccess = onSuccess,
                            onError = onError,
                            onCancel = onCancel
                        )
                    } else {
                        log("Context is ComponentActivity, using authenticateWithBiometricCallback")
                        // Fallback for ComponentActivity
                        biometricAuthManager.authenticateWithBiometricCallback(
                            activity = context as androidx.activity.ComponentActivity,
                            onSuccess = onSuccess,
                            onError = onError,
                            onCancel = onCancel
                        )
                    }
                } catch (e: Exception) {
                    log("Exception in biometric authentication: ${e.message}")
                    onError("Failed to start biometric authentication: ${e.message}")
                }
            }
        }
    }
    
    // Integrated login with PIN validation first, then biometric, then Firebase verification
    fun loginWithBiometricAndPin(
        phone: String, 
        pin: String, 
        savePhone: Boolean = false,
        context: android.content.Context,
        onSuccess: () -> Unit, 
        onFailure: (String) -> Unit,
        onCancel: () -> Unit
    ) {
        log("loginWithBiometricAndPin called with phone: $phone, pin length: ${pin.length}, savePhone: $savePhone")
        
        // Step 1: Validate PIN length first
        if (!InputValidator.isValidPin(pin)) {
            log("PIN validation failed: $pin (length: ${pin.length})")
            onFailure("PIN must be 4-6 digits")
            return
        }
        
        // Step 2: Validate phone number
        if (!InputValidator.isValidPhoneNumber(phone)) {
            log("Invalid phone number format: $phone")
            onFailure("Invalid phone number format")
            return
        }
        
        log("Input validation passed, proceeding with biometric authentication")
        
        // Step 3: Authenticate with biometric
        authenticateWithBiometric(
            context = context,
            onSuccess = {
                log("Biometric authentication successful, proceeding with Firebase PIN verification")
                // Biometric successful, now verify PIN with Firebase
                ioScope {
                    adminLoginWithPin(phone, pin, savePhone, onSuccess, onFailure)
                }
            },
            onError = { error ->
                log("Biometric authentication failed: $error")
                onFailure(error)
            },
            onCancel = {
                log("Biometric authentication cancelled")
                onCancel()
            }
        )
    }

    fun startPhoneVerification(
        activity: Activity,
        phoneNumber: String,
        onCodeSent: (String, PhoneAuthProvider.ForceResendingToken) -> Unit = { _, _ -> },
        onVerificationCompleted: (PhoneAuthCredential) -> Unit = {},
        onVerificationFailed: (FirebaseException) -> Unit = {}
    ) {
        // Validate phone number
        if (!InputValidator.isValidPhoneNumber(phoneNumber)) {
            _snackBarState.value = "Invalid phone number format"
            return
        }
        
        // Check if user already exists with different phone number
        ioLaunch {
            val userCount = _appDatabase.userDao().getUserCount()
            if (userCount > 0) {
                val existingUser = _appDatabase.userDao().getUserByMobile(phoneNumber)
                if (existingUser == null) {
                    // User exists but with different phone number - prevent OTP generation
                    _snackBarState.value = "This device is already registered with a different phone number. Cannot proceed with this phone number."
                    return@ioLaunch
                }
            }
            
            // Proceed with OTP generation
        _loadingState.value = true
        val options = PhoneAuthOptions.newBuilder(_auth).setPhoneNumber("+91$phoneNumber")
            .setTimeout(60L, TimeUnit.SECONDS).setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onCodeSent(
                    verificationId: String, token: PhoneAuthProvider.ForceResendingToken
                ) {
                    isOtpGenerated.value = true
                    otpVerificationId.value = verificationId
                    _loadingState.value = false
                    onCodeSent(verificationId, token)
                }

                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    isOtpVerified.value = true
                    onVerificationCompleted(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                        _loadingState.value = false
                    onVerificationFailed(e)
                }
            }).build()
        PhoneAuthProvider.verifyPhoneNumber(options)
        }
    }


    fun verifyOtpAndSignIn(
        otp: String, onSuccess: (FirebaseUser) -> Unit = {}, onFailure: (String) -> Unit = {}
    ) {
        if (!otpVerificationId.value.isNullOrBlank()) {
            val credential = PhoneAuthProvider.getCredential(otpVerificationId.value!!, otp)
            _loadingState.value = true
            _auth.signInWithCredential(credential).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    task.result?.user?.let { user ->
                        isOtpVerified.value = true
                        firebaseUser.value = user
                        onSuccess(user)
                    }
                    _loadingState.value = false
                } else {
                    _loadingState.value = false
                    onFailure(task.exception?.message ?: "OTP verification failed")
                }
            }

        }
    }

    fun uploadAdminUser(
        pin: String, onSuccess: () -> Unit, onFailure: () -> Unit
    ) {
        // Validate PIN
        if (!InputValidator.isValidPin(pin)) {
            _snackBarState.value = "PIN must be 4-6 digits"
            onFailure()
            return
        }
        


        val uid = firebaseUser.value?.uid
        val phone = firebaseUser.value?.phoneNumber?.replace("+91", "")?.filter { it.isDigit() }

        if (uid != null && phone != null) {
            // Hash the PIN before storing
            val hashedPin = SecurityUtils.hashPin(pin)
            val userMap = hashMapOf(
                "phone" to phone,
                "uid" to uid,
                "pin" to hashedPin,
                "otpVerifiedAt" to System.currentTimeMillis(),
                "role" to "admin"
            )
            _loadingState.value = true
            _fireStore.collection("users").document(phone).set(userMap).addOnSuccessListener {
                ioScope {
                    try {
                        // Check if user already exists in database
                        val existingUser = _appDatabase.userDao().getUserByMobile(phone)
                        val userCount = _appDatabase.userDao().getUserCount()
                        
                        if (existingUser != null) {
                            // User exists with same phone, update the existing user
                            val updatedUser = existingUser.copy(
                                pin = hashedPin
                            )
                            val updateResult = _appDatabase.userDao().updateUser(updatedUser)
                            if (updateResult > 0) {
                                onSuccess()
                            } else {
                                onFailure()
                            }
                        } else if (userCount == 0) {
                            // No user exists, create new user
                            val newUser = UsersEntity(
                                userId = uid,
                                name = phone,
                                mobileNo = phone,
                                pin = hashedPin,
                                role = "admin"
                            )
                            val insertResult = _appDatabase.userDao().insertUser(newUser)
                            if (insertResult != -1L) {
                                onSuccess()
                            } else {
                                onFailure()
                            }
                        } else {
                            // User exists but with different phone number - this should not happen due to OTP check
                            _snackBarState.value = "This device is already registered with a different phone number. Cannot proceed."
                            onFailure()
                        }
                        _loadingState.value = false
                    } catch (e: Exception) {
                        _loadingState.value = false
                        onFailure()
                    }
                }
            }.addOnFailureListener { it ->
                _loadingState.value = false
                _snackBarState.value = "Fire store upload failed"
                onFailure()
            }
        }
    }

    fun adminLoginWithPin(
        phone: String, pin: String, savePhone: Boolean = false, onSuccess: () -> Unit, onFailure: (String) -> Unit
    ) {
        // Validate inputs
        if (!InputValidator.isValidPhoneNumber(phone)) {
            onFailure("Invalid phone number format")
            return
        }
        
        if (!InputValidator.isValidPin(pin)) {
            onFailure("PIN must be 4-6 digits")
            return
        }
        ioLaunch {

            val userCount = _appDatabase.userDao().getUserCount()

            if (userCount > 0) {
                val usr = _appDatabase.userDao().getUserByMobile(phone)
                if (usr == null) {
                    onFailure("This device is already registered with a different phone number. Cannot proceed.")
                    return@ioLaunch
                }
            }

            _loadingState.value = true
            _fireStore.collection("users").document(phone).get().addOnSuccessListener { document ->
                if (document.exists()) {
                    val storedPin = document.getString("pin")
                    if (storedPin != null && SecurityUtils.verifyPin(pin, storedPin)) {
                        ioScope {
                            val result = _appDatabase.userDao().getUserByMobile(phone)
                            if (result != null) {
                                try {

                                    _dataStoreManager.saveAdminInfo(
                                        phone,
                                        result.userId,
                                        phone
                                    )

                                    _sessionManager.startSession(result.userId)
                                    
                                    // Save phone number if requested
                                    if (savePhone) {
                                        ioLaunch {
                                            savePhoneNumber(phone)
                                        }
                                    }
                                    
                                    onSuccess()
                                    _loadingState.value = false
                                } catch (e: Exception) {
                                    _loadingState.value = false
                                    onFailure("Unable to store the user data")
                                }
                            } else {
                                _loadingState.value = false
                                onFailure("User not found, please sign up first")
                            }
                        }
                    } else {
                        _loadingState.value = false
                        onFailure("Invalid PIN")
                    }
                } else {
                    _loadingState.value = false
                    onFailure("User not found")
                }
            }.addOnFailureListener {
                _loadingState.value = false
                onFailure(it.message ?: "Login failed")
            }

        }
    }

    fun logout() {
        ioLaunch {
            try {
                _sessionManager.endSession()
                _auth.signOut()
                firebaseUser.value = null
                isOtpGenerated.value = false
                isOtpVerified.value = false
                otpVerificationId.value = null
            } catch (e: Exception) {
                _snackBarState.value = "Logout failed: ${e.message}"
            }
        }
    }
    
    fun extendSession() {
        ioLaunch {
            try {
                _sessionManager.updateLastActivity()
                _snackBarState.value = "Session extended successfully"
            } catch (e: Exception) {
                _snackBarState.value = "Failed to extend session: ${e.message}"
            }
        }
    }
    
    fun resetPin(
        pin: String,  onSuccess: () -> Unit, onFailure: () -> Unit
    ) {
        // Validate PIN
        if (!InputValidator.isValidPin(pin)) {
            _snackBarState.value = "PIN must be 4-6 digits"
            onFailure()
            return
        }

        val uid = firebaseUser.value?.uid
        val phone = firebaseUser.value?.phoneNumber?.replace("+91", "")?.filter { it.isDigit() }

        if (uid != null && phone != null) {
            // Hash the PIN before storing
            val hashedPin = SecurityUtils.hashPin(pin)
            val userMap = hashMapOf(
                "phone" to phone,
                "uid" to uid,
                "pin" to hashedPin,
                "pinResetAt" to System.currentTimeMillis(),
                "role" to "admin"
            )
            _loadingState.value = true
            _fireStore.collection("users").document(phone).set(userMap).addOnSuccessListener {
                ioScope {
                    try {
                        // Update existing user in local database
                        val existingUser = _appDatabase.userDao().getUserByMobile(phone)
                        val userCount = _appDatabase.userDao().getUserCount()
                        
                        if (existingUser != null) {
                            // User exists with same phone, update the PIN
                            val updatedUser = existingUser.copy(
                                pin = hashedPin
                            )
                            val updateResult = _appDatabase.userDao().updateUser(updatedUser)
                            if (updateResult > 0) {
                                onSuccess()
                            } else {
                                onFailure()
                            }
                        } else if (userCount == 0) {
                            // No user exists, create new user
                            val newUser = UsersEntity(
                                userId = uid,
                                name = phone, 
                                mobileNo = phone,
                                pin = hashedPin,
                                role = "admin"
                            )
                            val insertResult = _appDatabase.userDao().insertUser(newUser)
                            if (insertResult != -1L) {
                                onSuccess()
                            } else {
                                onFailure()
                            }
                        } else {
                            // User exists but with different phone number - this should not happen due to OTP check
                            _snackBarState.value = "Another user is already registered. Only one user can be registered per device."
                            onFailure()
                        }
                        _loadingState.value = false
                    } catch (e: Exception) {
                        _loadingState.value = false
                        onFailure()
                    }
                }
            }.addOnFailureListener { it ->
                _loadingState.value = false
                _snackBarState.value = "Failed to reset PIN"
                onFailure()
            }
        }
    }
    
    fun resetOtpStates() {
        isOtpGenerated.value = false
        isOtpVerified.value = false
        otpVerificationId.value = null
        firebaseUser.value = null
    }

    
    // Function to check if current phone matches registered user
    suspend fun isCurrentPhoneRegistered(phoneNumber: String): Boolean {
        return try {
            val userCount = _appDatabase.userDao().getUserCount()
            if (userCount > 0) {
                val existingUser = _appDatabase.userDao().getUserByMobile(phoneNumber)
                existingUser != null
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    


}