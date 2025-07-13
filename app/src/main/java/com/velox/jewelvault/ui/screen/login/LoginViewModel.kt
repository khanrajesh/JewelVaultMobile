
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
import com.velox.jewelvault.data.roomdb.entity.UsersEntity
import com.velox.jewelvault.utils.DataStoreManager
import com.velox.jewelvault.utils.ioLaunch
import com.velox.jewelvault.utils.ioScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val _appDatabase: AppDatabase,
    private val _dataStoreManager: DataStoreManager,
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


    suspend fun userExits():Boolean{
        return _appDatabase.userDao().getUserCount() == 1
    }

    fun startPhoneVerification(
        activity: Activity,
        phoneNumber: String,
        onCodeSent: (String, PhoneAuthProvider.ForceResendingToken) -> Unit = { _, _ -> },
        onVerificationCompleted: (PhoneAuthCredential) -> Unit = {},
        onVerificationFailed: (FirebaseException) -> Unit = {}
    ) {
        _loadingState.value = true
        val options = PhoneAuthOptions.newBuilder(_auth).setPhoneNumber(phoneNumber)
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
                    onVerificationFailed(e)
                }
            }).build()
        PhoneAuthProvider.verifyPhoneNumber(options)
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

    fun uploadUser(
        pin: String, email: String? = null, onSuccess: () -> Unit, onFailure: () -> Unit
    ) {

        val uid = firebaseUser.value?.uid
        val phone = firebaseUser.value?.phoneNumber

        if (uid != null && phone != null) {
            val userMap = hashMapOf(
                "phone" to phone,
                "uid" to uid,
                "pin" to pin,
                "email" to email,
                "otpVerifiedAt" to System.currentTimeMillis()
            )
            _loadingState.value = true
            _fireStore.collection("users").document(phone).set(userMap).addOnSuccessListener {
                ioScope {
                    val user = UsersEntity(
                        name = phone, email = email, mobileNo = phone
                    )
                    try {
                        val result = _appDatabase.userDao().insertUser(user)
                        if (result != -1L) onSuccess() else onFailure()
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

    fun loginWithPin(
        phone: String, pin: String, onSuccess: () -> Unit, onFailure: (String) -> Unit
    ) {
        ioLaunch {

            val userCount = _appDatabase.userDao().getUserCount()

            if (userCount > 0) {
                val usr = _appDatabase.userDao().getUserByMobile(phone)
                if (usr == null) {
                    onFailure("You are not registered, Please try again with valid user")
                    return@ioLaunch
                }
            }

            _loadingState.value = true
            _fireStore.collection("users").document(phone).get().addOnSuccessListener { document ->
                if (document.exists()) {
                    val storedPin = document.getString("pin")
                    if (storedPin == pin) {
                        ioScope {
                            val result = _appDatabase.userDao().getUserByMobile(phone)
                            if (result != null) {
                                try {
                                    _dataStoreManager.setValue(
                                        DataStoreManager.USER_ID_KEY, result.id
                                    )
                                    onSuccess()
                                    _loadingState.value = false
                                } catch (e: Exception) {
                                    _loadingState.value = false
                                    onFailure("Unable to store the user data")
                                }
                            } else {
                                _loadingState.value = false
                                onFailure("Welcome back, Please verify")
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


}