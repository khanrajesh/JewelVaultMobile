package com.velox.jewelvault

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.Window
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.velox.jewelvault.ui.nav.AppNavigation
import com.velox.jewelvault.ui.nav.Screens
import com.velox.jewelvault.ui.screen.LoginScreen
import com.velox.jewelvault.ui.theme.JewelVaultTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {


            val navController = rememberNavController()
            val baseViewModel: BaseViewModel = hiltViewModel()

            // Check if we have permission for MANAGE_EXTERNAL_STORAGE



            JewelVaultTheme{

                LaunchedEffect(baseViewModel.snackMessage) {
                    if (baseViewModel.snackMessage.isNotBlank())
                        delay(5000)
                    baseViewModel.snackMessage = ""
                }


                Surface(
                    modifier = Modifier
                        .padding(
                            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                        )
                        .fillMaxSize()
                ) {
                    Box(Modifier.fillMaxSize()) {

                        AppNavigation(
                            navController,
                            baseViewModel,
                            startDestination = Screens.Splash.route
                        )

                        if (baseViewModel.loading) {
                            Dialog(
                                properties = DialogProperties(
                                    dismissOnBackPress = false,
                                    dismissOnClickOutside = false
                                ),
                                onDismissRequest = { /* Handle dismiss */ }) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .wrapContentSize(),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                        }

                        if (baseViewModel.snackMessage.isNotBlank()) {
                            Text(

                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                                    .padding(10.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.onSurface,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(16.dp),
                                text = baseViewModel.snackMessage,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.surfaceBright,
                                fontSize = 16.sp

                            )
                        }
                    }
                }
            }
        }
    }
}

