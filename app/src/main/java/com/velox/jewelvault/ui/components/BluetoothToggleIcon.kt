package com.velox.jewelvault.ui.components

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
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
import com.velox.jewelvault.utils.LocalBaseViewModel
import com.velox.jewelvault.utils.log

@Composable
fun BluetoothToggleIcon(
    modifier: Modifier = Modifier,
    onStateChanged: (Int) -> Unit = {},
    onError: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val baseViewModel = LocalBaseViewModel.current

    val bluetoothState by baseViewModel.bluetoothService.bluetoothAdapterState.collectAsStateWithLifecycle()

    // Debug logging for state changes
    LaunchedEffect(bluetoothState) {
        log("BluetoothToggleIcon: State changed to $bluetoothState (${getStateName(bluetoothState)})")
    }

    val bluetoothLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        log("BluetoothToggleIcon: launcher resultCode=${result.resultCode}")
        val state =
            baseViewModel.bluetoothService.bluetoothAdapter?.state ?: BluetoothAdapter.STATE_OFF
        log("BluetoothToggleIcon: current adapter state after launcher = $state")
        onStateChanged(state)
    }

    val iconColor = when (bluetoothState) {
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
                        bluetoothState
                    )
                })"
            )
            when (bluetoothState) {
                BluetoothAdapter.STATE_OFF -> {
                    // Launch system dialog to enable
                    log("BluetoothToggleIcon: launching enable dialog")
                    val enableIntent = baseViewModel.bluetoothService.requestBluetoothEnableIntent()
                    bluetoothLauncher.launch(enableIntent)
                }

                BluetoothAdapter.STATE_ON -> {
                    log("BluetoothToggleIcon: attempting to disable Bluetooth")
                    try {
                        val adapter = baseViewModel.bluetoothService.bluetoothAdapter
                        if (adapter != null) {
                            // Check if we have permission to disable
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
                                    log("BluetoothToggleIcon: programmatic disable failed, opening settings")
                                    onError("Unable to disable Bluetooth programmatically")
                                    try {
                                        val settings =
                                            baseViewModel.bluetoothService.openBluetoothSettingsIntent()
                                        context.startActivity(settings)
                                    } catch (ex: Exception) {
                                        log("BluetoothToggleIcon: Cannot open settings: ${ex.message}")
                                    }
                                }
                            } else {
                                log("BluetoothToggleIcon: No permission to disable Bluetooth, opening settings")
                                onError("Permission required to disable Bluetooth")
                                try {
                                    val settings =
                                        baseViewModel.bluetoothService.openBluetoothSettingsIntent()
                                    context.startActivity(settings)
                                } catch (ex: Exception) {
                                    log("BluetoothToggleIcon: Cannot open settings: ${ex.message}")
                                }
                            }
                        } else {
                            log("BluetoothToggleIcon: Bluetooth adapter not available")
                            onError("Bluetooth adapter not available")
                        }
                    } catch (e: Exception) {
                        log("BluetoothToggleIcon: Exception during disable: ${e.message}")
                        onError("Error disabling Bluetooth: ${e.message}")
                    }
                }

                else -> {
                    log("BluetoothToggleIcon: Bluetooth in transition state: $bluetoothState")
                    onError("Bluetooth is currently ${getStateDescription(bluetoothState)}")
                }
            }
        }, modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Default.Bluetooth,
            contentDescription = getStateDescription(bluetoothState),
            tint = iconColor,
            modifier = Modifier.size(24.dp)
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
