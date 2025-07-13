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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.velox.jewelvault.MainActivity
import com.velox.jewelvault.R
import com.velox.jewelvault.ui.components.CusOutlinedTextField
import com.velox.jewelvault.ui.components.InputFieldState
import com.velox.jewelvault.ui.nav.Screens
import com.velox.jewelvault.utils.LocalNavController
import com.velox.jewelvault.utils.VaultPreview
import com.velox.jewelvault.utils.ioScope
import com.velox.jewelvault.utils.isAppInstalled
import com.velox.jewelvault.utils.isLandscape
import com.velox.jewelvault.utils.mainScope
import kotlinx.coroutines.delay


@Composable
@VaultPreview
fun LoginScreenPreview() {
    LoginScreen(hiltViewModel<LoginViewModel>())
}


@Composable
fun LoginScreen(loginViewModel: LoginViewModel) {
    val context = LocalContext.current

    val isLogin = remember { mutableStateOf(true) }

    val mobileNo = remember { InputFieldState(initValue = "+91") }
    val password = remember { InputFieldState() }
    val confirmPassword = remember { InputFieldState() }
    val email = remember { InputFieldState() }

//    password.text = "0000"

    val savePhoneChecked = remember { mutableStateOf(false) }


    LaunchedEffect(true) {

        ioScope {
            val userExist = loginViewModel.userExits()
//            val isAppLockInstalled = isAppInstalled(context, "com.domobile.applock.ind")

            if (!userExist){
                isLogin.value = false
            }

//            if (!userExist && !isAppLockInstalled){
//                //new user
//
//            }
//
//            if (userExist && !isAppLockInstalled){
//                //old user data wipe warning
//
//            }
        }

    }

    Box(Modifier.fillMaxSize()) {

        if (isLandscape()) LandscapeLoginScreen(
            Modifier,
            isLogin,
            mobileNo,
            password,
            confirmPassword,
            email,
            savePhoneChecked,
            loginViewModel
        )
        else {
            PortraitLoginScreen(
                Modifier,
                isLogin,
                mobileNo,
                password,
                confirmPassword,
                email,
                savePhoneChecked,
                loginViewModel
            )
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
    email: InputFieldState,
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
                    isLogin,
                    mobileNo,
                    password,
                    confirmPassword,
                    email,
                    savePhoneChecked,
                    loginViewModel
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
    email: InputFieldState,
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
                    isLogin,
                    mobileNo,
                    password,
                    confirmPassword,
                    email,
                    savePhoneChecked,
                    loginViewModel
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
    email: InputFieldState,
    savePhoneChecked: MutableState<Boolean>,
    loginViewModel: LoginViewModel
) {
    val navHost = LocalNavController.current
    val forgotPassClick = remember { mutableStateOf(false) }
    val otp = remember { InputFieldState() }
    val activity = LocalContext.current as MainActivity

    LaunchedEffect(forgotPassClick.value) {
        if (forgotPassClick.value) {

            delay(7000)
            forgotPassClick.value = false
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
                        .fillMaxWidth(),
                    state = mobileNo,
                    placeholderText = "Mobile Number",
                    keyboardType = KeyboardType.Phone
                )
                CusOutlinedTextField(
                    modifier = Modifier
                        .padding(vertical = 5.dp)
                        .fillMaxWidth(),
                    state = password,
                    placeholderText = "Password",
                    keyboardType = KeyboardType.Password
                )
            } else {


                CusOutlinedTextField(
                    modifier = Modifier
                        .padding(vertical = 5.dp)
                        .fillMaxWidth(),
                    state = mobileNo,
                    placeholderText = "Mobile Number",
                    keyboardType = KeyboardType.Phone
                )

                CusOutlinedTextField(
                    modifier = Modifier
                        .padding(vertical = 5.dp)
                        .fillMaxWidth(),
                    state = email,
                    placeholderText = "Email (optional)",
                    keyboardType = KeyboardType.Email
                )

                if (loginViewModel.isOtpGenerated.value) {
                    CusOutlinedTextField(
                        modifier = Modifier
                            .padding(vertical = 5.dp)
                            .fillMaxWidth(),
                        state = otp,
                        placeholderText = "OTP",
                        keyboardType = KeyboardType.Number,
                        trailingIcon = if (loginViewModel.isOtpVerified.value) Icons.Default.Done else null
                    )

                }

                if (loginViewModel.isOtpGenerated.value && loginViewModel.isOtpVerified.value) {
                    CusOutlinedTextField(
                        modifier = Modifier
                            .padding(vertical = 5.dp)
                            .fillMaxWidth(),
                        state = password,
                        placeholderText = "Password",
                        keyboardType = KeyboardType.Password
                    )
                    CusOutlinedTextField(
                        modifier = Modifier
                            .padding(vertical = 5.dp)
                            .fillMaxWidth(),
                        state = confirmPassword,
                        placeholderText = "Confirm Password",
                        keyboardType = KeyboardType.Password
                    )

                }
            }



            if (isLogin.value) {
                Row(Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = savePhoneChecked.value,
                            onCheckedChange = { savePhoneChecked.value = it })
                        Text("Save phone number")
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = {
                        forgotPassClick.value = true
                    }) {
                        Text("Forgot Password?")
                    }
                }
            }

            Button(modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth(), onClick = {
                if (isLogin.value) {
                    // "Log In"
                    loginViewModel.loginWithPin(phone = mobileNo.text,
                        pin = password.text,
                        onSuccess = {
                            mainScope {
                                navHost.navigate(Screens.Main.route) {
                                    popUpTo(Screens.Login.route) {
                                        inclusive = true
                                    }
                                }
                            }
                        },
                        onFailure = {
                            loginViewModel.snackBarState.value = it
                            if (it == "Welcome back, Please verify") {
                                isLogin.value = false
                            }
                        })
                } else {
                    if (loginViewModel.isOtpGenerated.value && loginViewModel.isOtpVerified.value) {
                        //"Sign Up"
                        loginViewModel.uploadUser(pin = password.text,
                            email = email.text,
                            onSuccess = {
                                loginViewModel.snackBarState.value = "Signed up successfully"
                                isLogin.value = !isLogin.value
                            },
                            onFailure = {
                                //todo provide fall back mechanism
                            })

                    } else if (loginViewModel.isOtpGenerated.value && !loginViewModel.isOtpVerified.value) {
                        //"Verify OTP"
                        loginViewModel.verifyOtpAndSignIn(otp.text)
                    } else {
                        // "Get OTP"
                        loginViewModel.startPhoneVerification(
                            activity = activity, phoneNumber = mobileNo.text
                        )
                    }
                }
            }) {
                Text(
                    text = if (isLogin.value) {
                        "Log In"
                    } else {
                        if (loginViewModel.isOtpGenerated.value && loginViewModel.isOtpVerified.value) {
                            "Verify & Set PIN"
                        } else if (loginViewModel.isOtpGenerated.value && !loginViewModel.isOtpVerified.value) {
                            "Verify OTP"
                        } else {
                            "Get OTP"
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(onClick = {
                isLogin.value = !isLogin.value
            }) {
                Text(
                    text = if (isLogin.value) "Don't have an account? Sign Up" else "Already have an account? Login"
                )
            }
        }

    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(30.dp),
                text = "We have sent temporary password to your Email, \nPlease update the password within 24 hours.\nYou will be redirected to Login Screen shortly, Thank You",
                textAlign = TextAlign.Center
            )
        }
    }
}
