package com.velox.jewelvault.ui.components

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Bluetooth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.velox.jewelvault.data.bluetooth.BluetoothStateChange
import com.velox.jewelvault.utils.LocalBaseViewModel
import com.velox.jewelvault.utils.log

@Composable
fun BluetoothToggleIcon(
    modifier: Modifier = Modifier,
    onStateChanged: (Int) -> Unit = {},
) {
    val context = LocalContext.current
    val baseViewModel = LocalBaseViewModel.current

    val bluetoothState by baseViewModel.bluetoothReceiver.bluetoothStateChanged.collectAsStateWithLifecycle()

    // Debug logging for state changes
    LaunchedEffect(bluetoothState) {
        log("BluetoothToggleIcon: State changed to $bluetoothState (${getStateName(bluetoothState.currentState)})")
    }

    val bluetoothLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        log("BluetoothToggleIcon: launcher resultCode=${result.resultCode}")
        val state =
            baseViewModel.bluetoothReceiver.bluetoothAdapter?.state ?: BluetoothAdapter.STATE_OFF
        log("BluetoothToggleIcon: current adapter state after launcher = $state")
        onStateChanged(state)
    }

    val iconColor = when (bluetoothState.currentState) {
        BluetoothAdapter.STATE_ON -> Color.Blue
        BluetoothAdapter.STATE_OFF -> Color(0xFF55565B)
        BluetoothAdapter.STATE_TURNING_ON, BluetoothAdapter.STATE_TURNING_OFF -> Color(0xFFFAC03A)
        else -> Color.Gray
    }

    // Debug logging for icon color changes
    LaunchedEffect(iconColor) {
        log("BluetoothToggleIcon: Icon color changed to $iconColor for state $bluetoothState")
    }

    IconButton(
        onClick = {
            log(
                "BluetoothToggleIcon: clicked, current state = $bluetoothState (${
                    getStateName(
                        bluetoothState.currentState
                    )
                })"
            )
            when (bluetoothState.currentState) {
                BluetoothAdapter.STATE_OFF -> {
                    // Launch system dialog to enable
                    log("BluetoothToggleIcon: launching enable dialog")
                    val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    bluetoothLauncher.launch(enableIntent)
                }

                BluetoothAdapter.STATE_ON -> {
                    log("BluetoothToggleIcon: attempting to disable Bluetooth")
                    try {
                        val adapter = baseViewModel.bluetoothReceiver.bluetoothAdapter
                        if (adapter != null) {
                            val canDisable =
                                Build.VERSION.SDK_INT < Build.VERSION_CODES.S || (ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.BLUETOOTH_CONNECT
                                ) == PackageManager.PERMISSION_GRANTED)

                            if (canDisable) {
                                val disabled = adapter.disable()
                                if (disabled) {
                                    log("BluetoothToggleIcon: Bluetooth disabled successfully")
                                    onStateChanged(BluetoothAdapter.STATE_OFF)
                                } else {
                                    baseViewModel.snackBarState = "Failed to disable Bluetooth"
                                }
                            } else {
                                baseViewModel.snackBarState =
                                    "Permission required to disable Bluetooth"
                            }
                        } else {
                            baseViewModel.snackBarState = "Bluetooth adapter not available"
                        }
                    } catch (_: Exception) {
                        baseViewModel.snackBarState = "Failed to disable Bluetooth"
                    }
                }

                else -> {
                    baseViewModel.snackBarState =
                        "Bluetooth is currently ${getStateDescription(bluetoothState.currentState)}"
                }
            }
        }, modifier = modifier.size(25.dp).aspectRatio(1f)
    ) {
        Icon(
            imageVector = Icons.TwoTone.Bluetooth,
            contentDescription = getStateDescription(bluetoothState.currentState),
            tint = iconColor,
            modifier = Modifier.fillMaxSize()
        )
    }
}

private fun getStateName(state: Int): String {
    return when (state) {
        BluetoothAdapter.STATE_ON -> "ON"
        BluetoothAdapter.STATE_OFF -> "OFF"
        BluetoothAdapter.STATE_TURNING_ON -> "TURNING_ON"
        BluetoothAdapter.STATE_TURNING_OFF -> "TURNING_OFF"
        else -> "UNKNOWN($state)"
    }
}

private fun getStateDescription(state: Int): String {
    return when (state) {
        BluetoothAdapter.STATE_ON -> "Bluetooth On - Tap to disable"
        BluetoothAdapter.STATE_OFF -> "Bluetooth Off - Tap to enable"
        BluetoothAdapter.STATE_TURNING_ON -> "Bluetooth Turning On"
        BluetoothAdapter.STATE_TURNING_OFF -> "Bluetooth Turning Off"
        else -> "Bluetooth"
    }
}