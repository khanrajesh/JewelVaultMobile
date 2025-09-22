package com.velox.jewelvault

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.velox.jewelvault.data.DataStoreManager
import com.velox.jewelvault.data.bluetooth.BluetoothReceiver
import com.velox.jewelvault.ui.nav.AppNavigation
import com.velox.jewelvault.ui.nav.Screens
import com.velox.jewelvault.ui.theme.JewelVaultTheme
import com.velox.jewelvault.utils.SessionManager
import com.velox.jewelvault.utils.log
import com.velox.jewelvault.utils.mainScope
import com.velox.jewelvault.utils.monitorInternetConnection
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.sql.Timestamp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val speedMonitorJob: MutableState<Job?> = mutableStateOf(null)
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Inject
    lateinit var bluetoothBroadcastReceiver: BluetoothReceiver

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var dataStoreManager: DataStoreManager


    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bluetoothBroadcastReceiver.registerReceiver()
        enableEdgeToEdge()
        setContent {

            val navController = rememberNavController()
            val baseViewModel: BaseViewModel = hiltViewModel()
            val networkCheckEnabled = remember { mutableStateOf(true) }

            // Check if network monitoring is enabled before starting it
            LaunchedEffect(Unit) {
                monitorInternetConnection(
                    baseViewModel,
                    speedMonitorJob,
                    coroutineScope,
                    this@MainActivity,
                    dataStoreManager
                )
            }


            LaunchedEffect(
                dataStoreManager.getValue(
                    DataStoreManager.CONTINUOUS_NETWORK_CHECK,
                    true
                )
            ) {
                networkCheckEnabled.value =
                    dataStoreManager.getValue(DataStoreManager.CONTINUOUS_NETWORK_CHECK, true)
                        .first() ?: true
            }

            // Check session validity on app start
            LaunchedEffect(Unit) {
                coroutineScope.launch {
                    if (!sessionManager.isSessionValid()) {
                        // Session expired, navigate to login
                        mainScope {
                            navController.navigate(Screens.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                }
            }

            // Monitor session during app usage
            LaunchedEffect(Unit) {
                coroutineScope.launch {
                    while (true) {
                        delay(300000) // Check every 5 minutes
                        if (!sessionManager.isSessionValid()) {
                            mainScope {
                                // Session expired during app usage
                                baseViewModel.snackBarState = "Session expired. Please login again."
                                navController.navigate(Screens.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                            break
                        }

                        // Check if session is expiring soon
                        if (sessionManager.isSessionExpiringSoon()) {
                            val timeRemaining = sessionManager.getSessionTimeRemaining()
                            val minutesRemaining = TimeUnit.MILLISECONDS.toMinutes(timeRemaining)
                            baseViewModel.snackBarState =
                                "Session expires in $minutesRemaining minutes. Please save your work."
                        }

                        sessionManager.updateLastActivity() // Extend session on activity
                    }
                }
            }

            JewelVaultTheme {
                LaunchedEffect(baseViewModel.snackBarState) {
                    log("snackMessage: ${baseViewModel.snackBarState}")
                    if (baseViewModel.snackBarState.isNotBlank()) delay(5000)
                    baseViewModel.snackBarState = ""
                }

                LaunchedEffect(baseViewModel.loading) {
                    log("loading: ${baseViewModel.loading}. time: ${Timestamp(System.currentTimeMillis())}")
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
                            navController, baseViewModel, startDestination = Screens.Splash.route
                        )


                        // Safe loading state observation to prevent crashes from IO scope updates
                        if (baseViewModel.loading) {
                            Dialog(
                                properties = DialogProperties(
                                    dismissOnBackPress = false, dismissOnClickOutside = false
                                ), onDismissRequest = { /* Handle dismiss */ }) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.wrapContentSize(),
                                ) {
                                    Box(
                                        modifier = Modifier.padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                        }

                        if (baseViewModel.snackBarState.isNotEmpty()) {
                            Text(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                                    .padding(10.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(16.dp),
                                text = baseViewModel.snackBarState,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.surfaceBright,
                                fontSize = 16.sp

                            )
                        }

                        if (!baseViewModel.isConnectedState.value && networkCheckEnabled.value) {
                            AlertDialog(
                                onDismissRequest = {},
                                confirmButton = {},
                                title = { Text("No Internet Connection") },
                                text = { Text("Please check your connection and try again.") })
                        }
                    }
                }
            }
        }
    }
    override fun onResume() {
        super.onResume()
        // Ensure broadcast receiver is working when app comes to foreground
        if (!bluetoothBroadcastReceiver.isRegistered()) {
            bluetoothBroadcastReceiver.registerReceiver()
        }
        bluetoothBroadcastReceiver.refreshSystemState()
    }

    override fun onPause() {
        super.onPause()
        // Note: We don't unregister on pause to maintain background functionality
        // The receiver will continue to work in background for Bluetooth events
    }

    override fun onStart() {
        super.onStart()
        if (!bluetoothBroadcastReceiver.isRegistered()) {
            bluetoothBroadcastReceiver.registerReceiver()
        }
        bluetoothBroadcastReceiver.refreshSystemState()
    }

    override fun onStop() {
        super.onStop()
        // App is going to background, but receiver should still work
        log("MainActivity: App stopped, BluetoothBroadcastReceiver status: ${bluetoothBroadcastReceiver.isRegistered()}")
    }

    override fun onRestart() {
        super.onRestart()
        // App is coming back from background
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop Bluetooth service and unregister receiver when activity is destroyed
        bluetoothBroadcastReceiver.unregister()
        log("MainActivity: BluetoothService stopped and BluetoothBroadcastReceiver unregistered")
    }
}

