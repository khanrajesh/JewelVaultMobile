package com.velox.jewelvault.ui.screen.login

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import java.util.Locale
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.velox.jewelvault.data.DataStoreManager
import com.velox.jewelvault.data.UpdateInfo
import com.velox.jewelvault.data.firebase.RemoteConfigManager
import com.velox.jewelvault.data.roomdb.AppDatabase
import com.velox.jewelvault.data.roomdb.entity.users.UsersEntity
import com.velox.jewelvault.utils.AppUpdateManager
import com.velox.jewelvault.utils.BiometricAuthManager
import com.velox.jewelvault.utils.InputValidator
import com.velox.jewelvault.utils.SecurityUtils
import com.velox.jewelvault.utils.SessionManager
import com.velox.jewelvault.utils.fcm.FCMTokenManager
import com.velox.jewelvault.utils.ioLaunch
import com.velox.jewelvault.utils.ioScope
import com.velox.jewelvault.utils.log
import com.velox.jewelvault.utils.mainScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val _appDatabase: AppDatabase,
    private val _dataStoreManager: DataStoreManager,
    private val _sessionManager: SessionManager,
    private val _loadingState: MutableState<Boolean>,
    @Named("snackMessage") private val _snackBarState: MutableState<String>,
    private val _auth: FirebaseAuth,
    private val _fireStore: FirebaseFirestore,
    private val _remoteConfigManager: RemoteConfigManager,
    private val _appUpdateManager: AppUpdateManager,
    private val _fcmTokenManager: FCMTokenManager
) : ViewModel() {

    val snackBarState = _snackBarState
    val isOtpGenerated = mutableStateOf(false)
    val isOtpVerified = mutableStateOf(false)
    val otpSentTo = mutableStateOf<String?>(null)

    val firebaseUser = mutableStateOf<FirebaseUser?>(null)

    val otpVerificationId = mutableStateOf<String?>(null)
    val activeDeviceBlock = mutableStateOf<DeviceSessionInfo?>(null)

    // Biometric authentication state
    val isBiometricAvailable = mutableStateOf(false)
    val biometricAuthEnabled = mutableStateOf(false)

    // Biometric opt-in
    val showBiometricOptInDialog = mutableStateOf(false)

    // Update management state
    val updateInfo = mutableStateOf<UpdateInfo?>(null)
    val showForceUpdateDialog = mutableStateOf(false)

    // Timer state
    val timerValue = mutableStateOf(0)
    val isTimerRunning = mutableStateOf(false)
    private var timerJob: Job? = null


    suspend fun getUserCount(): Int {
        return _appDatabase.userDao().getUserCount()
    }

    suspend fun adminUserExits(): UsersEntity? {
        return _appDatabase.userDao().getAdminUser()
    }

    // Check biometric availability
    fun checkBiometricAvailability(context: android.content.Context) {
        val biometricAuthManager = BiometricAuthManager(context)
        val isAvailable = biometricAuthManager.isBiometricAvailable()
        log("Biometric availability check: $isAvailable")
        isBiometricAvailable.value = isAvailable
    }


    fun showBiometricOptInIfEligible(context: android.content.Context) {
        ioLaunch {
            try {
                val alreadyShown =
                    _dataStoreManager.getValue(DataStoreManager.BIOMETRIC_OPTIN_SHOWN, false)
                        .first() ?: false
                val enabled =
                    _dataStoreManager.getValue(DataStoreManager.BIOMETRIC_AUTH, false).first()
                        ?: false
                val available = BiometricAuthManager(context).isBiometricAvailable()
                if (!alreadyShown && available && !enabled) {
                    showBiometricOptInDialog.value = true
                    log("Showing biometric opt-in dialog")
                }
            } catch (_: Exception) {
            }
        }
    }

    fun handleBiometricOptInDecision(enable: Boolean) {
        ioLaunch {
            try {
                if (enable) {
                    _dataStoreManager.setValue(DataStoreManager.BIOMETRIC_AUTH, true)
                    biometricAuthEnabled.value = true
                }
                _dataStoreManager.setValue(DataStoreManager.BIOMETRIC_OPTIN_SHOWN, true)
            } catch (_: Exception) {
            } finally {
                showBiometricOptInDialog.value = false
            }
        }
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
            val enabled =
                _dataStoreManager.getValue(DataStoreManager.BIOMETRIC_AUTH, false).first() ?: false
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
        ioLaunch {
            val biometricEnabled =
                _dataStoreManager.getValue(DataStoreManager.BIOMETRIC_AUTH, false).first() ?: false
            log("Biometric enabled in settings: $biometricEnabled")
            if (!biometricEnabled) {
                log("Biometric disabled in settings, calling onError")
                onError("Biometric authentication is disabled in settings")
                return@ioLaunch
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
    fun checkBiometric(
        context: android.content.Context,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit,
        onCancel: () -> Unit
    ) {

        log("Input validation passed, proceeding with biometric authentication")
        authenticateWithBiometric(context = context, onSuccess = {
            log("Biometric authentication successful, proceeding with Firebase PIN verification")
            onSuccess()
        }, onError = { error ->
            log("Biometric authentication failed: $error")
            onFailure(error)
        }, onCancel = {
            log("Biometric authentication cancelled")
            onCancel()
        })
    }

    fun startPhoneVerification(
        activity: Activity,
        phoneNumber: String,
        onCodeSent: (String, PhoneAuthProvider.ForceResendingToken) -> Unit = { _, _ -> },
        onVerificationCompleted: (PhoneAuthCredential) -> Unit = {},
        onVerificationFailed: (FirebaseException) -> Unit = {},
        retryCount: Int = 0,
        onOtpReceived: (String?) -> Unit

    ) {
        if (_loadingState.value) {
            log("LOGIN: startPhoneVerification ignored - loading in progress")
            return
        }
        // Validate phone number
        if (!InputValidator.isValidPhoneNumber(phoneNumber)) {
            _snackBarState.value = "Invalid phone number format"
            return
        }

        // Check if activity is still valid
        if (activity.isFinishing || activity.isDestroyed) {
            _snackBarState.value = "Activity is no longer valid"
            return
        }

        // Check if user already exists with different phone number
        ioLaunch {
            val userCount = _appDatabase.userDao().getUserCount()
            if (userCount > 0) {
                val existingUser = _appDatabase.userDao().getUserByMobile(phoneNumber)
                if (existingUser == null) {
                    // User exists but with different phone number - prevent OTP generation
                    _snackBarState.value =
                        "This device is already registered with a different phone number. Cannot proceed with this phone number."
                    return@ioLaunch
                }
            }

            // Proceed with OTP generation
            _loadingState.value = true
            log("Starting phone verification for: +91$phoneNumber")
            log("Activity context: ${activity.javaClass.simpleName}")

            val options = PhoneAuthOptions.newBuilder(_auth).setPhoneNumber("+91$phoneNumber")
                .setTimeout(60L, TimeUnit.SECONDS).setActivity(activity)
                .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                    override fun onCodeSent(
                        verificationId: String, token: PhoneAuthProvider.ForceResendingToken
                    ) {
                        isOtpGenerated.value = true
                        otpVerificationId.value = verificationId
                        otpSentTo.value = phoneNumber
                        _loadingState.value = false
                        _snackBarState.value = "Otp has been sent."
                        startTimer() // Start the timer when OTP is sent
                        onCodeSent(verificationId, token)
                    }

                    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                        credential.smsCode?.let {
                            verifyOtpAndSignIn(it)
                        }
                        mainScope {
                            onOtpReceived(credential.smsCode)
                        }
                        isOtpVerified.value = true
                        stopTimer() // Stop timer when OTP is verified
                        onVerificationCompleted(credential)
                    }

                    override fun onVerificationFailed(e: FirebaseException) {
                        _loadingState.value = false
                        val errorMessage = when {
                            e.message?.contains("reCAPTCHA", ignoreCase = true) == true -> {
                                if (retryCount < 2) {
                                    "reCAPTCHA verification failed. Retrying... (${retryCount + 1}/3)"
                                } else {
                                    "reCAPTCHA verification failed. Please check your internet connection and try again."
                                }
                            }

                            e.message?.contains("network", ignoreCase = true) == true -> {
                                "Network error. Please check your internet connection and try again."
                            }

                            e.message?.contains("quota", ignoreCase = true) == true -> {
                                "Too many attempts. Please wait a while before trying again."
                            }

                            else -> {
                                "OTP verification failed: ${e.message}"
                            }
                        }
                        _snackBarState.value = errorMessage
                        log("Phone verification failed: ${e.message}")

                        // Auto-retry for reCAPTCHA failures
                        if (e.message?.contains(
                                "reCAPTCHA",
                                ignoreCase = true
                            ) == true && retryCount < 2
                        ) {
                            log("Retrying phone verification due to reCAPTCHA failure. Attempt: ${retryCount + 1}")
                            // Wait 2 seconds before retry
                            ioLaunch {
                                delay(2000)
                                startPhoneVerification(
                                    activity = activity,
                                    phoneNumber = phoneNumber,
                                    onCodeSent = onCodeSent,
                                    onVerificationCompleted = onVerificationCompleted,
                                    onVerificationFailed = onVerificationFailed,
                                    retryCount = retryCount + 1,
                                    onOtpReceived = onOtpReceived
                                )
                            }
                        } else {
                            onVerificationFailed(e)
                        }
                    }
                }).build()
            PhoneAuthProvider.verifyPhoneNumber(options)
        }
    }


    fun verifyOtpAndSignIn(
        otp: String, onSuccess: (FirebaseUser) -> Unit = {}, onFailure: (String) -> Unit = {}
    ) {
        if (otpVerificationId.value.isNullOrBlank()) {
            val message = "OTP verification ID not found"
            _snackBarState.value = message
            onFailure(message)
            return
        }
        log("LOGIN: verifyOtpAndSignIn called")
        if (otp.isBlank() || otp.length < 4) {
            val message = "Enter valid OTP"
            log("LOGIN: OTP input invalid (length=${otp.length})")
            _snackBarState.value = message
            onFailure(message)
            return
        }
        val credential = PhoneAuthProvider.getCredential(otpVerificationId.value!!, otp)
        _loadingState.value = true
        _auth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                task.result?.user?.let { user ->
                    isOtpVerified.value = true
                    firebaseUser.value = user
                    stopTimer() // Stop timer when OTP is verified
                    log("LOGIN: OTP verification successful, user=${user.uid}")
                    onSuccess(user)
                }
                _loadingState.value = false
            } else {
                _loadingState.value = false
                val exception = task.exception
                val message = when (exception) {
                    is FirebaseAuthInvalidCredentialsException -> "Invalid OTP"
                    else -> "OTP verification failed. Please try again."
                }
                log("LOGIN: OTP verification failed -> ${exception?.message}")
                _snackBarState.value = message
                onFailure(message)
            }
        }
    }

    fun uploadAdminUser(
        pin: String, onSuccess: () -> Unit, onFailure: () -> Unit
    ) {

        return ioLaunch {
            log("LOGIN: uploadAdminUser called")
            if (_loadingState.value) {
                log("LOGIN: uploadAdminUser ignored - loading in progress")
                return@ioLaunch
            }
            // Validate PIN
            if (!InputValidator.isValidPin(pin)) {
                _snackBarState.value = "PIN must be 4-6 digits"
                onFailure()
                return@ioLaunch
            }


            val uid = firebaseUser.value?.uid
            val phone = firebaseUser.value?.phoneNumber?.replace("+91", "")?.filter { it.isDigit() }

            if (uid != null && phone != null) {
                // Hash the PIN before storing
                val hashedPin = SecurityUtils.hashPin(pin)

                // Get FCM token for first-time login
                val fcmToken = _fcmTokenManager.getAndSaveFCMToken()
                log("LOGIN: FCM token obtained: ${fcmToken?.take(20)}...")

                val userMap = hashMapOf(
                    "phone" to phone,
                    "uid" to uid,
                    "pin" to hashedPin,
                    "otpVerifiedAt" to System.currentTimeMillis(),
                    "role" to "admin",
                    "fcmToken" to (fcmToken ?: "")
                )
                _loadingState.value = true
                _fireStore.collection("users").document(phone).set(userMap).addOnSuccessListener {
                    ioScope {
                        try {
                            // Check if user already exists in database
                            val existingUser = _appDatabase.userDao().getUserByMobile(phone)
                            val userCount = _appDatabase.userDao().getUserCount()

                            if (existingUser != null) {
                                log("LOGIN: updating existing local admin user for $phone")
                                // User exists with same phone, update the existing user
                                val updatedUser = existingUser.copy(
                                    pin = hashedPin,
                                    lastUpdated = System.currentTimeMillis()
                                )
                                val updateResult = _appDatabase.userDao().updateUser(updatedUser)
                                if (updateResult > 0) {
                                    log("LOGIN: local admin user updated for $phone")
                                    onSuccess()
                                } else {
                                    log("LOGIN: local admin user update failed for $phone")
                                    onFailure()
                                }
                            } else if (userCount == 0) {
                                log("LOGIN: inserting new local admin user for $phone")
                                // No user exists, create new user
                                val newUser = UsersEntity(
                                    userId = uid,
                                    name = phone,
                                    mobileNo = phone,
                                    pin = hashedPin,
                                    role = "admin",
                                    lastUpdated = System.currentTimeMillis()
                                )
                                val insertResult = _appDatabase.userDao().insertUser(newUser)
                                if (insertResult != -1L) {
                                    log("LOGIN: local admin user inserted for $phone")
                                    onSuccess()
                                } else {
                                    log("LOGIN: local admin user insert failed for $phone")
                                    onFailure()
                                }
                            } else {
                                // User exists but with different phone number - this should not happen due to OTP check
                                _snackBarState.value =
                                    "This device is already registered with a different phone number. Cannot proceed."
                                log("LOGIN: local admin user conflict for $phone")
                                onFailure()
                            }
                            _loadingState.value = false
                        } catch (e: Exception) {
                            _loadingState.value = false
                            log("LOGIN: uploadAdminUser exception -> ${e.message}")
                            onFailure()
                        }
                    }
                }.addOnFailureListener { it ->
                    _loadingState.value = false
                    _snackBarState.value = "Fire store upload failed"
                    log("LOGIN: Firestore set failed for $phone -> ${it.message}")
                    onFailure()
                }
            }else{
                _loadingState.value = false
                _snackBarState.value = "Fire store upload failed"
                log("LOGIN: Firestore set failed for $phone ,uid: $uid invalid")
                onFailure()
            }
        }

    }

    fun logInUser(
        phone: String,
        pin: String,
        savePhone: Boolean = false,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        activeDeviceBlock.value = null
        // Validate inputs
        if (!InputValidator.isValidPhoneNumber(phone)) {
            log("LOGIN: invalid phone format -> $phone")
            onFailure("Invalid phone number format")
            return
        }

        if (!InputValidator.isValidPin(pin)) {
            log("LOGIN: invalid pin format (length=${pin.length})")
            onFailure("PIN must be 4-6 digits")
            return
        }


        val success = {
            //get the admin user mobile no from the datastore then feature list from firebase
            onSuccess()
        }


        ioScope {
            log("logInUser called with phone: $phone, pin: $pin, savePhone: $savePhone")
            val user = _appDatabase.userDao().getUserByMobile(phone)

            if (user == null) {
                log("LOGIN: user not found for $phone")
                onFailure("User not found")
                return@ioScope
            }

            if (user.role == "admin") {
                log("LOGIN: attempting admin login for $phone")
                adminLoginWithPin(phone, pin, {
                    if (savePhone) {
                        ioLaunch {
                            savePhoneNumber(phone)
                        }
                    }
                    log("LOGIN: admin login success for $phone")
                    success()
                }, onFailure)
            } else {
                log("LOGIN: attempting user login for $phone")
                userLoginWithPin(
                    user, pin, {
                        if (savePhone) {
                            ioLaunch {
                                savePhoneNumber(phone)
                            }
                        }
                        log("LOGIN: user login success for $phone")
                        success()
                    }, onFailure
                )
            }
        }
    }

    @SuppressLint("SuspiciousIndentation")
    private fun userLoginWithPin(
        user: UsersEntity, pin: String, onSuccess: () -> Unit, onFailure: (String) -> Unit
    ) {
        log("LOGIN: userLoginWithPin for userId=${user.userId}")
        if (user.pin != null && SecurityUtils.verifyPin(pin, user.pin)) {
            try {
                ioScope {
                    log("LOGIN: PIN verified for userId=${user.userId}, saving session")
                    _dataStoreManager.saveCurrentLoginUser(user)
                    _sessionManager.startSession(user.userId)
                    log("LOGIN: session started for userId=${user.userId}")
                    onSuccess()
                }
                _loadingState.value = false
            } catch (e: Exception) {
                log("LOGIN: userLoginWithPin exception -> ${e.message}")
                onFailure(e.message ?: "Login failed")
            }
        } else {
            log("LOGIN: invalid PIN for userId=${user.userId}")
            onFailure("Invalid Pin!")
        }
    }

    private fun adminLoginWithPin(
        phone: String, pin: String, onSuccess: () -> Unit, onFailure: (String) -> Unit
    ) {
        log("LOGIN: adminLoginWithPin start for $phone")
        ioLaunch {

            /*     val adminUser = _appDatabase.userDao().getAdminUser()

                 if (adminUser != null && adminUser.mobileNo != phone) {
                     onFailure("This device is already registered with a different phone number. Cannot proceed.")
                     return@ioLaunch
                 }*/

            _loadingState.value = true
            _fireStore.collection("users").document(phone).get().addOnSuccessListener { document ->
                if (document.exists()) {
                    log("LOGIN: Firestore user document exists for $phone")
                    val storedPin = document.getString("pin")
                    if (storedPin != null && SecurityUtils.verifyPin(pin, storedPin)) {
                        checkDeviceSessionAndRegister(
                            adminMobile = phone,
                            onAllowed = {
                                ioScope {
                                    val result = _appDatabase.userDao().getUserByMobile(phone)
                                    if (result != null) {
                                        try {
                                            log("LOGIN: admin PIN verified, saving admin info and session for $phone")

                                            _dataStoreManager.saveAdminInfo(
                                                phone, result.userId, phone
                                            )
                                            _dataStoreManager.saveCurrentLoginUser(result)

                                            _sessionManager.startSession(result.userId)

                                            log("LOGIN: admin login success for $phone")
                                            onSuccess()
                                            _loadingState.value = false
                                        } catch (e: Exception) {
                                            _loadingState.value = false
                                            log("LOGIN: admin save/session error -> ${e.message}")
                                            onFailure("Unable to store the user data")
                                        }
                                    } else {
                                        _loadingState.value = false
                                        log("LOGIN: admin user not found locally for $phone")
                                        onFailure("User not found, please sign up first")
                                    }
                                }
                            },
                            onBlocked = { info ->
                                activeDeviceBlock.value = info
                                _loadingState.value = false
                            },
                            onError = {
                                ioScope {
                                    val result = _appDatabase.userDao().getUserByMobile(phone)
                                    if (result != null) {
                                        try {
                                            log("LOGIN: device check failed, proceeding with login for $phone")
                                            _dataStoreManager.saveAdminInfo(
                                                phone, result.userId, phone
                                            )
                                            _dataStoreManager.saveCurrentLoginUser(result)
                                            _sessionManager.startSession(result.userId)
                                            onSuccess()
                                        } catch (e: Exception) {
                                            _loadingState.value = false
                                            log("LOGIN: admin save/session error -> ${e.message}")
                                            onFailure("Unable to store the user data")
                                        }
                                    } else {
                                        _loadingState.value = false
                                        log("LOGIN: admin user not found locally for $phone")
                                        onFailure("User not found, please sign up first")
                                    }
                                }
                            }
                        )
                    } else {
                        _loadingState.value = false
                        log("LOGIN: admin invalid PIN for $phone")
                        onFailure("Invalid PIN")
                    }
                } else {
                    _loadingState.value = false
                    log("LOGIN: Firestore user document not found for $phone")
                    onFailure("User not found")
                }
            }.addOnFailureListener {
                _loadingState.value = false
                log("LOGIN: Firestore get failed for $phone -> ${it.message}")
                onFailure(it.message ?: "Login failed")
            }

        }
    }

    fun logout() {
        ioLaunch {
            try {
                log("LOGIN: logout called")
                updateDeviceActiveFlag(isActive = false)
                _sessionManager.endSession()
                _auth.signOut()
                firebaseUser.value = null
                isOtpGenerated.value = false
                isOtpVerified.value = false
                otpVerificationId.value = null
                log("LOGIN: logout completed")
            } catch (e: Exception) {
                log("LOGIN: logout failed -> ${e.message}")
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
        pin: String, onSuccess: () -> Unit, onFailure: () -> Unit
    ) {
        log("LOGIN: resetPin called")
        if (_loadingState.value) {
            log("LOGIN: resetPin ignored - loading in progress")
            return
        }
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
                            log("LOGIN: updating local PIN for $phone")
                            // User exists with same phone, update the PIN
                            val updatedUser = existingUser.copy(
                                pin = hashedPin,
                                lastUpdated = System.currentTimeMillis()
                            )
                            val updateResult = _appDatabase.userDao().updateUser(updatedUser)
                            if (updateResult > 0) {
                                log("LOGIN: local PIN updated for $phone")
                                onSuccess()
                            } else {
                                log("LOGIN: local PIN update failed for $phone")
                                onFailure()
                            }
                        } else if (userCount == 0) {
                            log("LOGIN: inserting local admin user during reset for $phone")
                            // No user exists, create new user
                            val newUser = UsersEntity(
                                userId = uid,
                                name = phone,
                                mobileNo = phone,
                                pin = hashedPin,
                                role = "admin",
                                lastUpdated = System.currentTimeMillis()
                            )
                            val insertResult = _appDatabase.userDao().insertUser(newUser)
                            if (insertResult != -1L) {
                                log("LOGIN: local admin user inserted during reset for $phone")
                                onSuccess()
                            } else {
                                log("LOGIN: local insert failed during reset for $phone")
                                onFailure()
                            }
                        } else {
                            // User exists but with different phone number - this should not happen due to OTP check
                            _snackBarState.value =
                                "Another user is already registered. Only one user can be registered per device."
                            log("LOGIN: resetPin conflict for $phone")
                            onFailure()
                        }
                        _loadingState.value = false
                    } catch (e: Exception) {
                        _loadingState.value = false
                        log("LOGIN: resetPin exception -> ${e.message}")
                        onFailure()
                    }
                }
            }.addOnFailureListener { it ->
                _loadingState.value = false
                _snackBarState.value = "Failed to reset PIN"
                log("LOGIN: Firestore set failed during reset for $phone -> ${it.message}")
                onFailure()
            }
        }
    }

    fun resetOtpStates() {
        isOtpGenerated.value = false
        isOtpVerified.value = false
        otpVerificationId.value = null
        otpSentTo.value = null
        firebaseUser.value = null
        stopTimer()
    }

    fun clearActiveDeviceBlock() {
        activeDeviceBlock.value = null
    }

    data class DeviceSessionInfo(
        val deviceId: String,
        val manufacturer: String,
        val model: String,
        val lastLoginAt: Long
    )

    private fun buildDeviceId(adminMobile: String): String {
        val manufacturer = Build.MANUFACTURER.ifBlank { "unknown" }
        val model = Build.MODEL.ifBlank { "unknown" }
        val raw = "${manufacturer}_${model}_${adminMobile}"
        return raw.lowercase(Locale.getDefault()).replace(Regex("[^a-z0-9]+"), "_").trim('_')
    }

    private fun checkDeviceSessionAndRegister(
        adminMobile: String,
        onAllowed: () -> Unit,
        onBlocked: (DeviceSessionInfo) -> Unit,
        onError: () -> Unit
    ) {
        val timeoutMs = TimeUnit.MINUTES.toMillis(5)
        val now = System.currentTimeMillis()
        val deviceId = buildDeviceId(adminMobile)
        val devicesRef = _fireStore.collection("users")
            .document(adminMobile)
            .collection("devices")

        devicesRef.get(Source.SERVER).addOnSuccessListener { snapshot ->
            val activeDocs = mutableListOf<com.google.firebase.firestore.DocumentSnapshot>()
            var activeOther: DeviceSessionInfo? = null

            snapshot.documents.forEach { doc ->
                val isActive = doc.getBoolean("isActive") == true
                if (!isActive || doc.id == deviceId) return@forEach

                val lastSeenAt = doc.getLong("lastSeenAt") ?: doc.getLong("lastLoginAt") ?: 0L
                val isExpired = lastSeenAt > 0 && now - lastSeenAt > timeoutMs

                activeDocs.add(doc)

                if (!isExpired && activeOther == null) {
                    activeOther = DeviceSessionInfo(
                        deviceId = doc.id,
                        manufacturer = doc.getString("manufacturer") ?: "Unknown",
                        model = doc.getString("model") ?: "Unknown",
                        lastLoginAt = doc.getLong("lastLoginAt") ?: 0L
                    )
                }
            }

            if (activeOther != null) {
                onBlocked(activeOther!!)
                return@addOnSuccessListener
            }

            val payload = mapOf(
                "deviceId" to deviceId,
                "manufacturer" to Build.MANUFACTURER,
                "model" to Build.MODEL,
                "isActive" to true,
                "lastLoginAt" to System.currentTimeMillis(),
                "lastSeenAt" to System.currentTimeMillis()
            )

            _fireStore.runBatch { batch ->
                activeDocs.forEach { doc ->
                    batch.update(doc.reference, "isActive", false)
                }
                batch.set(devicesRef.document(deviceId), payload, SetOptions.merge())
            }.addOnSuccessListener {
                onAllowed()
            }.addOnFailureListener {
                log("LOGIN: device register failed -> ${it.message}")
                onError()
            }
        }.addOnFailureListener {
            log("LOGIN: device check failed -> ${it.message}")
            onError()
        }
    }

    private fun updateDeviceActiveFlag(isActive: Boolean) {
        ioScope {
            try {
                val adminMobile = _dataStoreManager.getAdminInfo().third.first()
                if (adminMobile.isBlank()) return@ioScope
                val deviceId = buildDeviceId(adminMobile)
                val devicesRef = _fireStore.collection("users")
                    .document(adminMobile)
                    .collection("devices")
                val payload = mapOf(
                    "isActive" to isActive,
                    "lastSeenAt" to System.currentTimeMillis()
                )
                devicesRef.document(deviceId).set(payload, SetOptions.merge())
            } catch (e: Exception) {
                log("LOGIN: failed to update device active flag -> ${e.message}")
            }
        }
    }

    // Timer functions
    fun startTimer() {
        stopTimer() // Stop any existing timer
        timerValue.value = 60
        isTimerRunning.value = true

        timerJob = CoroutineScope(Dispatchers.IO).launch {
            while (timerValue.value > 0) {
                delay(1000) // Wait for 1 second
                timerValue.value -= 1
            }
            isTimerRunning.value = false
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
        isTimerRunning.value = false
        timerValue.value = 0
    }


    fun resetTimer() {
        stopTimer()
        startTimer()
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

    // Update management functions
    suspend fun checkForForceUpdates(context: android.content.Context) {
        log("🚨 Starting force update check in LoginViewModel...")
        try {
            // Fetch remote config
            log("🔄 Fetching remote config for force update check...")
            val fetchResult = _remoteConfigManager.fetchAndActivate()
            if (fetchResult.isSuccess) {
                log("✅ Remote config fetch successful for force update check")
                val info = _remoteConfigManager.getUpdateInfo()
                log("📋 Got update info for force update check: $info")
                updateInfo.value = info

                // Check if force update is required
                val isForceRequired = _remoteConfigManager.isForceUpdateRequired()
                log("🔍 Force update required check: $isForceRequired")
                if (isForceRequired) {
                    log("🚨 Force update required - showing dialog")
                    showForceUpdateDialog.value = true
                } else {
                    log("✅ No force update required")
                }
            } else {
                log("❌ Remote config fetch failed for force update check: ${fetchResult.exceptionOrNull()?.message}")
            }
        } catch (e: Exception) {
            log("❌ Error checking for force updates: ${e.message}")
            log("❌ Exception details: ${e.javaClass.simpleName}")
        }
    }

    fun onUpdateClick(context: android.content.Context) {
        log("🔄 Update button clicked in LoginViewModel")
        val info = updateInfo.value
        if (info != null) {
            log("📱 Opening Play Store with update info from LoginViewModel: $info")
            _appUpdateManager.openPlayStore(context, info)
        } else {
            log("📱 Opening Play Store without update info from LoginViewModel")
            _appUpdateManager.openPlayStore(context)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer() // Clean up timer when ViewModel is cleared
    }

}
