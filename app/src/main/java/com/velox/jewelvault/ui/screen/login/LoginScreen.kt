package com.velox.jewelvault.ui.screen.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.velox.jewelvault.R
import com.velox.jewelvault.ui.components.CusOutlinedTextField
import com.velox.jewelvault.ui.components.ForceUpdateDialog
import com.velox.jewelvault.ui.components.InputFieldState
import com.velox.jewelvault.ui.nav.Screens
import com.velox.jewelvault.utils.LocalNavController
import com.velox.jewelvault.utils.VaultPreview
import com.velox.jewelvault.utils.isLandscape
import com.velox.jewelvault.utils.mainScope


@Composable
@VaultPreview
fun LoginScreenPreview() {
    LoginScreen(hiltViewModel<LoginViewModel>())
}


@Composable
fun LoginScreen(loginViewModel: LoginViewModel) {
    val context = LocalContext.current

    val isLogin = remember { mutableStateOf(true) }

    val mobileNo = remember { InputFieldState() }
    val password = remember { InputFieldState() }
    val confirmPassword = remember { InputFieldState() }

    val savePhoneChecked = remember { mutableStateOf(false) }

    LaunchedEffect(true) {
        // Check for force updates first
        loginViewModel.checkForForceUpdates(context)

        val adminUser = loginViewModel.adminUserExits()

        if (adminUser == null) {
            isLogin.value = false
        } else {
            // Check biometric availability
            loginViewModel.checkBiometricAvailability(context)

            // Load saved phone number if available
            val savedPhone = loginViewModel.getSavedPhoneNumber()
            if (!savedPhone.isNullOrBlank()) {
                mobileNo.text = savedPhone
                savePhoneChecked.value = true
            }

            // Check if biometric is enabled in settings
            val biometricEnabled = loginViewModel.getBiometricSetting()
            loginViewModel.biometricAuthEnabled.value = biometricEnabled
        }
    }

    Box(Modifier.fillMaxSize()) {

        if (isLandscape()) LandscapeLoginScreen(
            Modifier, isLogin, mobileNo, password, confirmPassword, savePhoneChecked, loginViewModel
        )
        else {
            PortraitLoginScreen(
                Modifier,
                isLogin,
                mobileNo,
                password,
                confirmPassword,
                savePhoneChecked,
                loginViewModel
            )
        }

        // Show force update dialog if needed
        if (loginViewModel.showForceUpdateDialog.value) {
            loginViewModel.updateInfo.value?.let { updateInfo ->
                ForceUpdateDialog(
                    updateInfo = updateInfo,
                    onUpdateClick = { loginViewModel.onUpdateClick(context) })
            }
        }

    }

}


@Composable
private fun LandscapeLoginScreen(
    modifier: Modifier = Modifier,
    isLogin: MutableState<Boolean>,
    mobileNo: InputFieldState,
    password: InputFieldState,
    confirmPassword: InputFieldState,
    savePhoneChecked: MutableState<Boolean>,
    loginViewModel: LoginViewModel
) {

    val scrollState = rememberScrollState()


    Row(
        modifier = modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {

        Image(
            painter = painterResource(id = R.drawable.logo_1),
            contentDescription = "Splash Logo",
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(0.3f),
        )

        Card(
            modifier = Modifier
                .weight(1f)
                .padding(30.dp)
                .wrapContentHeight()
                .fillMaxWidth()
                .padding(26.dp), elevation = CardDefaults.cardElevation()
        ) {
            Column(
                Modifier
                    .padding(16.dp)
                    .verticalScroll(scrollState)
            ) {
                Text("Welcome", fontWeight = FontWeight.Bold, fontSize = 32.sp)
                Spacer(Modifier.height(10.dp))
                AuthScreen(
                    isLogin, mobileNo, password, confirmPassword, savePhoneChecked, loginViewModel
                )
            }
        }
    }
}

@Composable
private fun PortraitLoginScreen(
    modifier: Modifier = Modifier,
    isLogin: MutableState<Boolean>,
    mobileNo: InputFieldState,
    password: InputFieldState,
    confirmPassword: InputFieldState,
    savePhoneChecked: MutableState<Boolean>,
    loginViewModel: LoginViewModel
) {
    Column(
        modifier = modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Image(
            painter = painterResource(id = R.drawable.logo_1),
            contentDescription = "Splash Logo",
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(0.3f),
        )

        Card(
            modifier = Modifier
                .weight(1f)
                .padding(30.dp)
                .wrapContentHeight()
                .fillMaxWidth()
                .padding(26.dp), elevation = CardDefaults.cardElevation()
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Welcome", fontWeight = FontWeight.Bold, fontSize = 32.sp)
                Spacer(Modifier.height(10.dp))
                AuthScreen(
                    isLogin, mobileNo, password, confirmPassword, savePhoneChecked, loginViewModel
                )
            }
        }
    }
}

@Composable
private fun AuthScreen(
    isLogin: MutableState<Boolean>,
    mobileNo: InputFieldState,
    password: InputFieldState,
    confirmPassword: InputFieldState,
    savePhoneChecked: MutableState<Boolean>,
    loginViewModel: LoginViewModel
) {
    val navHost = LocalNavController.current
    val forgotPassClick = remember { mutableStateOf(false) }
    val otp = remember { InputFieldState() }
    val activity = LocalContext.current as FragmentActivity
    val keyboardController = LocalSoftwareKeyboardController.current


    // Create focus requesters for focus management
    val mobileNoFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val confirmPasswordFocusRequester = remember { FocusRequester() }
    val otpFocusRequester = remember { FocusRequester() }

    // Reset OTP states when switching between login/signup/forgot PIN
    LaunchedEffect(isLogin.value, forgotPassClick.value) {
        if (isLogin.value || forgotPassClick.value) {
            loginViewModel.resetOtpStates()
        }
    }


    val buttonText = if (isLogin.value) {
        if (loginViewModel.isBiometricAvailable.value && loginViewModel.biometricAuthEnabled.value) {
            "Login with Biometric & PIN"
        } else {
            "Log In"
        }
    } else if (forgotPassClick.value) {
        if (loginViewModel.isOtpGenerated.value && loginViewModel.isOtpVerified.value) {
            "Reset PIN"
        } else if (loginViewModel.isOtpGenerated.value && !loginViewModel.isOtpVerified.value) {
            "Verify OTP"
        } else {
            "Get OTP"
        }
    } else {
        if (loginViewModel.isOtpGenerated.value && loginViewModel.isOtpVerified.value) {
            "Verify & Set PIN"
        } else if (loginViewModel.isOtpGenerated.value && !loginViewModel.isOtpVerified.value) {
            "Verify OTP"
        } else {
            "Get OTP"
        }
    }

    val onDone = onDone@{
        if (isLogin.value) {
            // "Log In" - Check if biometric is available and enabled
            loginAction(
                loginViewModel,
                mobileNo,
                password,
                savePhoneChecked,
                activity,
                navHost,
                isLogin,
                keyboardController
            )
        } else if (forgotPassClick.value) {
            // "Forgot PIN Flow"
            if (loginViewModel.isOtpGenerated.value && loginViewModel.isOtpVerified.value) {
                // Validate PIN confirmation
                if (password.text != confirmPassword.text) {
                    loginViewModel.snackBarState.value = "PINs do not match"
                    return@onDone
                }

                //"Reset PIN"
                loginViewModel.resetPin(pin = password.text, onSuccess = {
                    loginViewModel.snackBarState.value = "PIN reset successfully"
                    forgotPassClick.value = false
                    isLogin.value = true
                    // Clear all fields
//                        mobileNo.clear()
                    password.clear()
                    confirmPassword.clear()
                    otp.clear()
                    loginViewModel.resetOtpStates() // Reset timer after successful PIN reset
                }, onFailure = {
                    loginViewModel.snackBarState.value = "Failed to reset PIN"
                })

            } else if (loginViewModel.isOtpGenerated.value && !loginViewModel.isOtpVerified.value) {
                //"Verify OTP"
                loginViewModel.verifyOtpAndSignIn(otp.text)
            } else {
                // "Get OTP"
                loginViewModel.startPhoneVerification(
                    activity = activity, phoneNumber = mobileNo.text
                ) { smsCode ->
                    smsCode?.let { code ->
                        otp.textChange(code)
                    }
                }
            }
        } else {
            // "Sign Up Flow"
            if (loginViewModel.isOtpGenerated.value && loginViewModel.isOtpVerified.value) {
                // Validate PIN confirmation
                if (password.text != confirmPassword.text) {
                    loginViewModel.snackBarState.value = "PINs do not match"
                    return@onDone
                }
                //"Sign Up"
                loginViewModel.uploadAdminUser(pin = password.text, onSuccess = {
                    loginViewModel.snackBarState.value = "Signed up successfully"
                    isLogin.value = true
                    // Clear all fields
//                        mobileNo.clear()
                    password.clear()
                    confirmPassword.clear()
                    otp.clear()
                    loginViewModel.resetOtpStates() // Reset timer after successful signup
                }, onFailure = {
                    loginViewModel.snackBarState.value = "Failed to sign up"
                })

            } else if (loginViewModel.isOtpGenerated.value && !loginViewModel.isOtpVerified.value) {
                //"Verify OTP"
                loginViewModel.verifyOtpAndSignIn(otp.text)
            } else {
                // "Get OTP"
                loginViewModel.startPhoneVerification(
                    activity = activity, phoneNumber = mobileNo.text
                ) { smsCode ->
                    smsCode?.let { code ->
                        otp.textChange(code)
                    }
                }
            }
        }
    }


    if (!forgotPassClick.value) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            if (isLogin.value) {
                CusOutlinedTextField(
                    modifier = Modifier
                        .padding(vertical = 5.dp)
                        .fillMaxWidth()
                        .focusRequester(mobileNoFocusRequester),
                    state = mobileNo,
                    placeholderText = "Mobile Number",
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next,
                    nextFocusRequester = passwordFocusRequester,
                    validation = { input -> if (input.length != 10) "Please Enter Valid Number" else null })
                CusOutlinedTextField(
                    modifier = Modifier
                        .padding(vertical = 5.dp)
                        .fillMaxWidth()
                        .focusRequester(passwordFocusRequester),
                    state = password,
                    placeholderText = "PIN",
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                    keyboardActions = KeyboardActions(onDone = {
                        loginAction(
                            loginViewModel,
                            mobileNo,
                            password,
                            savePhoneChecked,
                            activity,
                            navHost,
                            isLogin,
                            keyboardController
                        )
                    })
                )
            } else if (forgotPassClick.value) {
                // Forgot PIN Flow - Same as signup
                CusOutlinedTextField(
                    modifier = Modifier
                        .padding(vertical = 5.dp)
                        .fillMaxWidth()
                        .focusRequester(mobileNoFocusRequester),
                    state = mobileNo,
                    placeholderText = "Mobile Number",
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next,
                    nextFocusRequester = if (loginViewModel.isOtpGenerated.value) otpFocusRequester else null,
                    validation = { input -> if (input.length != 10) "Please Enter Valid Number" else null }

                )



                if (loginViewModel.isOtpGenerated.value) {
                    CusOutlinedTextField(
                        modifier = Modifier
                            .padding(vertical = 5.dp)
                            .fillMaxWidth()
                            .focusRequester(otpFocusRequester),
                        state = otp,
                        placeholderText = "OTP",
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next,
                        nextFocusRequester = if (loginViewModel.isOtpVerified.value) passwordFocusRequester else null,
                        trailingIcon = if (loginViewModel.isOtpVerified.value) Icons.Default.Done else null
                    )
                }

                if (loginViewModel.isOtpGenerated.value && loginViewModel.isOtpVerified.value) {
                    CusOutlinedTextField(
                        modifier = Modifier
                            .padding(vertical = 5.dp)
                            .fillMaxWidth()
                            .focusRequester(passwordFocusRequester),
                        state = password,
                        placeholderText = "New PIN",
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next,
                        nextFocusRequester = confirmPasswordFocusRequester
                    )
                    CusOutlinedTextField(
                        modifier = Modifier
                            .padding(vertical = 5.dp)
                            .fillMaxWidth()
                            .focusRequester(confirmPasswordFocusRequester),
                        state = confirmPassword,
                        placeholderText = "Confirm New PIN",
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    )
                }
            } else {
                CusOutlinedTextField(
                    modifier = Modifier
                        .padding(vertical = 5.dp)
                        .fillMaxWidth()
                        .focusRequester(mobileNoFocusRequester),
                    state = mobileNo,
                    placeholderText = "Mobile Number",
                    keyboardType = KeyboardType.Phone,
                    validation = { input -> if (input.length != 10) "Please Enter Valid Number" else null },
                    imeAction = ImeAction.Next,
                    nextFocusRequester = if (loginViewModel.isOtpGenerated.value) otpFocusRequester else null,
                    keyboardActions = KeyboardActions(onNext = {
                        loginViewModel.startPhoneVerification(
                            activity = activity, phoneNumber = mobileNo.text
                        ) { smsCode ->
                            smsCode?.let {
                                otp.textChange(it)
                            }
                        }
                    })
                )


                if (loginViewModel.isOtpGenerated.value) {
                    CusOutlinedTextField(
                        modifier = Modifier
                            .padding(vertical = 5.dp)
                            .fillMaxWidth()
                            .focusRequester(otpFocusRequester),
                        state = otp,
                        placeholderText = "OTP",
                        keyboardType = KeyboardType.Number,
                        trailingIcon = if (loginViewModel.isOtpVerified.value) Icons.Default.Done else null,
                        imeAction = ImeAction.Next,
                        nextFocusRequester = if (loginViewModel.isOtpVerified.value) passwordFocusRequester else null,
                        keyboardActions = KeyboardActions(onNext = {
                            loginViewModel.verifyOtpAndSignIn(otp.text)
                        })
                    )
                }

                if (loginViewModel.isOtpGenerated.value && loginViewModel.isOtpVerified.value) {
                    CusOutlinedTextField(
                        modifier = Modifier
                            .padding(vertical = 5.dp)
                            .fillMaxWidth()
                            .focusRequester(passwordFocusRequester),
                        state = password,
                        placeholderText = "PIN",
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next,
                        nextFocusRequester = confirmPasswordFocusRequester
                    )
                    CusOutlinedTextField(
                        modifier = Modifier
                            .padding(vertical = 5.dp)
                            .fillMaxWidth()
                            .focusRequester(confirmPasswordFocusRequester),
                        state = confirmPassword,
                        placeholderText = "Confirm PIN",
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                        keyboardActions = KeyboardActions(onDone = {
                            // Validate PIN confirmation
                            if (password.text != confirmPassword.text) {
                                loginViewModel.snackBarState.value = "PINs do not match"
                                return@KeyboardActions
                            }
                            //"Sign Up"
                            loginViewModel.uploadAdminUser(pin = password.text, onSuccess = {
                                loginViewModel.snackBarState.value = "Signed up successfully"
                                isLogin.value = true
                                // Clear all fields
//                                    mobileNo.clear()
                                password.clear()
                                confirmPassword.clear()
                                otp.clear()
                                loginViewModel.resetOtpStates() // Reset timer after successful signup
                            }, onFailure = {
                                loginViewModel.snackBarState.value = "Failed to sign up"
                            })
                        })
                    )
                }
            }

            if (isLogin.value) {
                Row(Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = savePhoneChecked.value,
                            onCheckedChange = { savePhoneChecked.value = it })
                        Text("Save phone number")
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = {
                        forgotPassClick.value = true
                    }) {
                        Text("Forgot PIN?")
                    }
                }
            }

            // Timer display and resend button
            if (loginViewModel.isOtpGenerated.value && !loginViewModel.isOtpVerified.value) {
                Row(
                    modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End
                ) {
                    if (loginViewModel.isTimerRunning.value) {
                        val minutes = loginViewModel.timerValue.value / 60
                        val seconds = loginViewModel.timerValue.value % 60
                        val timeText = if (minutes > 0) {
                            "Resend OTP in ${minutes}m ${seconds}s"
                        } else {
                            "Resend OTP in ${seconds}s"
                        }
                        val textColor = when {
                            loginViewModel.timerValue.value <= 10 -> androidx.compose.ui.graphics.Color.Red
                            loginViewModel.timerValue.value <= 30 -> androidx.compose.ui.graphics.Color(
                                0xFFFF8C00
                            ) // Orange
                            else -> androidx.compose.ui.graphics.Color.Gray
                        }
                        Text(
                            text = timeText,
                            modifier = Modifier.padding(top = 8.dp),
                            fontSize = 14.sp,
                            color = textColor
                        )
                    } else {
                        TextButton(
                            onClick = {
                                loginViewModel.startPhoneVerification(
                                    activity = activity, phoneNumber = mobileNo.text
                                ) { smsCode ->
                                    smsCode?.let {
                                        otp.textChange(it)
                                    }
                                }
                            }, modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("Resend OTP")
                        }
                    }
                }
            }

            Button(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth(), onClick = {
                    onDone()
                }) {
                if (isLogin.value && loginViewModel.isBiometricAvailable.value && loginViewModel.biometricAuthEnabled.value) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Biometric",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                Text(
                    text = buttonText
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!forgotPassClick.value) {
                TextButton(onClick = {
                    isLogin.value = !isLogin.value
                    loginViewModel.resetOtpStates() // Reset timer when switching modes
                }) {
                    Text(
                        text = if (isLogin.value) "Don't have an account? Sign Up" else "Already have an account? Login"
                    )
                }
            } else {
                TextButton(onClick = {
                    forgotPassClick.value = false
                    isLogin.value = true
                    loginViewModel.resetOtpStates() // Reset timer when going back to login
                }) {
                    Text("Back to Login")
                }
            }
        }
    }
}


private fun loginAction(
    loginViewModel: LoginViewModel,
    mobileNo: InputFieldState,
    password: InputFieldState,
    savePhoneChecked: MutableState<Boolean>,
    activity: FragmentActivity,
    navHost: NavHostController,
    isLogin: MutableState<Boolean>,
    keyboardController: SoftwareKeyboardController?
) {
    if (loginViewModel.isBiometricAvailable.value && loginViewModel.biometricAuthEnabled.value) {
        // Use biometric authentication directly
        loginViewModel.checkBiometric(context = activity, onSuccess = {
            loginViewModel.logInUser(
                phone = mobileNo.text,
                pin = password.text,
                savePhone = savePhoneChecked.value,
                onSuccess = {
                    mainScope {
                        keyboardController?.hide()
                        navHost.navigate(Screens.Main.route) {
                            popUpTo(Screens.Login.route) {
                                inclusive = true
                            }
                        }
                    }
                },
                onFailure = {
                    loginViewModel.snackBarState.value = it
                    if (it == "User not found, please sign up first") {
                        isLogin.value = false
                    }
                })
        }, onFailure = { error ->
            loginViewModel.snackBarState.value = error
            if (error == "User not found, please sign up first") {
                isLogin.value = false
            }
        }, onCancel = {
            // User cancelled biometric authentication
        })
    } else {
        // Use regular PIN authentication
        loginViewModel.logInUser(
            phone = mobileNo.text,
            pin = password.text,
            savePhone = savePhoneChecked.value,
            onSuccess = {
                mainScope {
                    navHost.navigate(Screens.Main.route) {
                        popUpTo(Screens.Login.route) {
                            inclusive = true
                        }
                    }
                    keyboardController?.hide()
                }
            },
            onFailure = {
                loginViewModel.snackBarState.value = it
                if (it == "User not found, please sign up first") {
                    isLogin.value = false
                }
            })
    }
}
