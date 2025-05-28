package com.velox.jewelvault.ui.screen.login

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
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
import com.velox.jewelvault.R
import com.velox.jewelvault.data.roomdb.entity.UsersEntity
import com.velox.jewelvault.ui.components.CusOutlinedTextField
import com.velox.jewelvault.ui.components.InputFieldState
import com.velox.jewelvault.ui.nav.Screens
import com.velox.jewelvault.utils.LocalBaseViewModel
import com.velox.jewelvault.utils.LocalNavController
import com.velox.jewelvault.utils.VaultPreview
import com.velox.jewelvault.utils.ioScope
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
    val navHost = LocalNavController.current
    val baseViewModel = LocalBaseViewModel.current

    val isLogin = remember { mutableStateOf(true) }

    val mobileNo = remember { InputFieldState(textState = baseViewModel.phone) }
    val password = remember { InputFieldState() }
    val confirmPassword = remember { InputFieldState() }
    val email = remember { InputFieldState() }

    password.text = "0000"

    val savePhoneChecked = remember { mutableStateOf(false) }


    val onAuthFunction: () -> Unit = onAuthFunction@{
        if (mobileNo.text.isBlank()) {
            mobileNo.error = "Mobile No can't be empty"
            return@onAuthFunction
        }

        if (password.text.isBlank()) {
            password.error = "Password can't be empty"
            return@onAuthFunction
        }

        if (!isLogin.value) {
            if (email.text.isBlank()) {
                email.error = "Email can't be empty"
                return@onAuthFunction
            }

            if (password.text != confirmPassword.text) {
                confirmPassword.error = "Password didn't matched!"
                return@onAuthFunction
            }
        }


        if (isLogin.value) {

            loginViewModel.login(mobileNo.text, pass = password.text, onFailure = {
                baseViewModel.snackMessage = it
            }, onSuccess = {usersEntity->
                ioScope {
                    //todo save the user id in datastore

                    mainScope {

                        navHost.navigate(Screens.Main.route) {
                            popUpTo(Screens.Login.route) {
                                inclusive = true
                            }
                        }
                    }
                }
            })


        } else {

//            navHost.navigate(Screens.Login.route){
//                popUpTo(Screens.Login.route){
//                    inclusive = true
//                }
//            }
            val user = UsersEntity(
                name = mobileNo.text,
                email = email.text,
                mobileNo = mobileNo.text,
                pin = password.text
            )
            loginViewModel.signup(user, onFailure = {
                mainScope {
                    baseViewModel.snackMessage = "Unable to create the user"
                }
            }, onSuccess = {

                baseViewModel.snackMessage = "Signed up successfully"
                isLogin.value = !isLogin.value
            })


        }
    }


    if (isLandscape())
        LandscapeLoginScreen(
            Modifier,
            isLogin,
            mobileNo,
            password,
            confirmPassword,
            email,
            savePhoneChecked,
            onAuthFunction
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
            onAuthFunction
        )
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
    onAuthFunction: () -> Unit
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
                .padding(26.dp),
            elevation = CardDefaults.cardElevation()
        ) {
            Column(Modifier.padding(16.dp)
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
                    onAuthFunction
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
    onAuthFunction: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
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
                .padding(26.dp),
            elevation = CardDefaults.cardElevation()
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
                    onAuthFunction
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
    onAuthFunction: () -> Unit
) {
    val forgotPassClick = remember { mutableStateOf(false) }

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
            CusOutlinedTextField(
                modifier = Modifier
                    .padding(vertical = 5.dp)
                    .fillMaxWidth(),
                state = mobileNo,
                placeholderText = "Mobile Number",
                keyboardType = KeyboardType.Phone
            )

            if (!isLogin.value)
                CusOutlinedTextField(
                    modifier = Modifier
                        .padding(vertical = 5.dp)
                        .fillMaxWidth(),
                    state = email,
                    placeholderText = "Email",
                    keyboardType = KeyboardType.Email
                )

            CusOutlinedTextField(
                modifier = Modifier
                    .padding(vertical = 5.dp)
                    .fillMaxWidth(),
                state = password,
                placeholderText = "Password",
                keyboardType = KeyboardType.Password
            )

            if (!isLogin.value)
                CusOutlinedTextField(
                    modifier = Modifier
                        .padding(vertical = 5.dp)
                        .fillMaxWidth(),
                    state = confirmPassword,
                    placeholderText = "Confirm Password",
                    keyboardType = KeyboardType.Password
                )



            if (isLogin.value) {
                Row(Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = savePhoneChecked.value,
                            onCheckedChange = { savePhoneChecked.value = it }
                        )
                        Text("Save phone number")
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(
                        onClick = {
                            forgotPassClick.value = true
                        }) {
                        Text("Forgot Password?")
                    }
                }
            }

            Button(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth(),
                onClick = onAuthFunction
            ) {
                Text(text = if (isLogin.value) "Login" else "Sign Up")
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
