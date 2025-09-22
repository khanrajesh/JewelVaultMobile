package com.velox.jewelvault.ui.screen.bluetooth

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.velox.jewelvault.data.printing.BluetoothManager
import com.velox.jewelvault.ui.components.PermissionRequester
import com.velox.jewelvault.ui.nav.SubScreens
import com.velox.jewelvault.ui.theme.JewelVaultTheme
import com.velox.jewelvault.utils.LocalSubNavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothScanConnectScreen(
    viewModel: BluetoothScanConnectViewModel = hiltViewModel()
) {
    val navController = LocalSubNavController.current
    LocalContext.current
    rememberCoroutineScope()

    val uiState by viewModel.uiState.collectAsState()
    val bluetoothManager = viewModel.bluetoothManager
    val discoveredDevices by bluetoothManager.discoveredDevices.collectAsState()
    val bondedDevices by bluetoothManager.bondedDevices.collectAsState()
    val isBluetoothEnabled by bluetoothManager.isBluetoothEnabled.collectAsState()

    // Separate bonded devices into connected and not connected
    val connectedDevices = bondedDevices.filter { bluetoothManager.isDeviceConnected(it.address)}
    val unconnectedPairedDevices = bondedDevices.filter { !bluetoothManager.isDeviceConnected(it.address) }

    // Request Bluetooth permissions
    PermissionRequester(
        permissions = listOf(
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_SCAN,
            android.Manifest.permission.BLUETOOTH,
            android.Manifest.permission.BLUETOOTH_ADMIN,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ), onAllPermissionsGranted = {
            viewModel.startScanning()
        })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(topStart = 18.dp))
            .padding(5.dp)
    ) {

        Row(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(
                modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(
                    containerColor = when {
                        !isBluetoothEnabled -> MaterialTheme.colorScheme.errorContainer
                        uiState.isScanning -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Row(
                    modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when {
                            !isBluetoothEnabled -> Icons.Default.BluetoothDisabled
                            uiState.isScanning -> Icons.AutoMirrored.Filled.BluetoothSearching
                            else -> Icons.Default.Bluetooth
                        }, contentDescription = null, modifier = Modifier.size(24.dp), tint = when {
                            !isBluetoothEnabled -> MaterialTheme.colorScheme.onErrorContainer
                            uiState.isScanning -> MaterialTheme.colorScheme.onPrimaryContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = when {
                            !isBluetoothEnabled -> "Disabled - Please enable Bluetooth"
                            uiState.isScanning -> "Scanning for devices..."
                            else -> "Ready to scan"
                        }, style = MaterialTheme.typography.bodyMedium, color = when {
                            !isBluetoothEnabled -> MaterialTheme.colorScheme.onErrorContainer
                            uiState.isScanning -> MaterialTheme.colorScheme.onPrimaryContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )

                }
            }

                Button(
                    onClick = { if (uiState.isScanning) viewModel.stopScanning() else viewModel.startScanning() },
                    modifier = Modifier,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.isScanning) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = if (uiState.isScanning) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = if (uiState.isScanning) "Stop Scan" else "Start Scan"
                    )

                }

                OutlinedButton(
                    onClick = { viewModel.refreshDevices() }, modifier = Modifier
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { navController.navigate(SubScreens.BluetoothManagePrinters.route) }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Manage Printers",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Connected Devices (at the top)
            if (connectedDevices.isNotEmpty()) {
                item {
                    Text(
                        text = "Connected Devices (${connectedDevices.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }


                items(connectedDevices) { device ->
                    ConnectedDeviceCard(device = device, onManagePrinter = {
                        if (bluetoothManager.isPrinterDevice(device)) {
                            navController.navigate(SubScreens.BluetoothManagePrinters.route)
                        }
                    }, onDisconnect = { viewModel.disconnectDevice(device.address) },
                        bluetoothManager
                        )
                }


            }

            // Paired Devices (not connected)
            if (unconnectedPairedDevices.isNotEmpty()) {

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Paired Devices (${unconnectedPairedDevices.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                }

                items(unconnectedPairedDevices) { device ->
                    PairedDeviceCard(
                        device = device,
                        onConnect = { viewModel.connectToDevice(device.address) },
                        onManagePrinter = {
                            if (bluetoothManager.isPrinterDevice(device)) {
                                navController.navigate(SubScreens.BluetoothManagePrinters.route)
                            }
                        },

                        bluetoothManager)
                }


            }

            // Discovered Devices
            if (discoveredDevices.isNotEmpty()) {

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Discovered Devices (${discoveredDevices.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }


                items(discoveredDevices) { device ->
                    DeviceCard(
                        bluetoothManager=bluetoothManager,
                        device = device,
                        onConnect = { viewModel.connectToDevice(device.address) },
                        onDisconnect = { viewModel.disconnectDevice(device.address) },
                        onPair = { viewModel.pairDevice(device.address) })
                }

            } else if (connectedDevices.isEmpty()) {

                item {
                    // No devices message
                    Card(
                        modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.BluetoothSearching,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = if (uiState.isScanning) "Scanning for devices..." else "No devices found",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (!uiState.isScanning) {
                                Text(
                                    text = "Tap 'Start Scan' to discover nearby devices",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

            }
        }


        // Loading indicator
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }

    // Show snackbar for messages
    uiState.message?.let { message ->
        LaunchedEffect(message) {
            // Show snackbar
            viewModel.clearMessage()
        }
    }
}


@Composable
fun DeviceCard(
    bluetoothManager: BluetoothManager, device: BluetoothDevice, onConnect: () -> Unit, onDisconnect: () -> Unit, onPair: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Device icon with type indicator
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when {
                            bluetoothManager.isDeviceConnected(device.address) -> MaterialTheme.colorScheme.primary
                            device.bondState == BluetoothDevice.BOND_BONDED -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ), contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getDeviceIcon(device.type),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = when {
                        bluetoothManager.isDeviceConnected(device.address)-> MaterialTheme.colorScheme.onPrimary
                        device.bondState == BluetoothDevice.BOND_BONDED -> MaterialTheme.colorScheme.onSecondary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Device info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = device.name ?: "Unknown Device",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = device.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Connection status indicator
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                when {
                                    bluetoothManager.isDeviceConnected(device.address) -> Color.Green
                                    device.bondState == BluetoothDevice.BOND_BONDED -> Color.Blue
                                    else -> Color.Gray
                                }
                            )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = when {
                            bluetoothManager.isDeviceConnected(device.address) -> "Connected"
                            device.bondState == BluetoothDevice.BOND_BONDED -> "Paired"
                            else -> "Available"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Device type indicator
                    if (bluetoothManager.isPrinterDevice(device)) {
                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "• Printer",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

//                // RSSI indicator
//                device.rssi?.let { rssi ->
//                    Text(
//                        text = "Signal: ${rssi}dBm",
//                        style = MaterialTheme.typography.bodySmall,
//                        color = MaterialTheme.colorScheme.onSurfaceVariant
//                    )
//                }
            }

            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (bluetoothManager.isDeviceConnected(device.address)) {
                    IconButton(
                        onClick = onDisconnect
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Disconnect",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                } else if (device.bondState == BluetoothDevice.BOND_BONDED) {
                    IconButton(
                        onClick = onConnect
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Connect",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    IconButton(
                        onClick = onPair
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = "Pair",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun getDeviceIcon(type: Int): ImageVector {
    return when {
        else -> Icons.Default.DeviceHub
    }
}

@Composable
fun ConnectedDeviceCard(
    device: BluetoothDevice,
    onManagePrinter: () -> Unit,
    onDisconnect: () -> Unit,
    bluetoothManager: BluetoothManager
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (bluetoothManager.isPrinterDevice(device)) {
                    onManagePrinter()
                }
            }, colors = CardDefaults.cardColors(
            containerColor = if (bluetoothManager.isPrinterDevice(device)) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getDeviceIcon(device.type),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (bluetoothManager.isPrinterDevice(device)) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = device.name ?: "Unknown Device",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (bluetoothManager.isPrinterDevice(device)) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = device.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (bluetoothManager.isPrinterDevice(device)) MaterialTheme.colorScheme.onPrimaryContainer.copy(
                        alpha = 0.7f
                    )
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                if (bluetoothManager.isPrinterDevice(device)) {
                    Text(
                        text = "Tap to manage printer settings",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }

            // Connection status indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Green)
                )

                Text(
                    text = "Connected",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (bluetoothManager.isPrinterDevice(device)) MaterialTheme.colorScheme.onPrimaryContainer.copy(
                        alpha = 0.7f
                    )
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                IconButton(
                    onClick = onDisconnect
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Disconnect",
                        tint = if (bluetoothManager.isPrinterDevice(device)) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun PairedDeviceCard(
    device: BluetoothDevice,
    onConnect: () -> Unit,
    onManagePrinter: () -> Unit,
    bluetoothManager: BluetoothManager
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (bluetoothManager.isPrinterDevice(device)) {
                    onManagePrinter()
                } else {
                    onConnect()
                }
            }, colors = CardDefaults.cardColors(
            containerColor = if (bluetoothManager.isPrinterDevice(device)) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getDeviceIcon(device.type),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (bluetoothManager.isPrinterDevice(device)) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = device.name ?: "Unknown Device",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (bluetoothManager.isPrinterDevice(device)) MaterialTheme.colorScheme.onSecondaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = device.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (bluetoothManager.isPrinterDevice(device)) MaterialTheme.colorScheme.onSecondaryContainer.copy(
                        alpha = 0.7f
                    )
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                if (bluetoothManager.isPrinterDevice(device)) {
                    Text(
                        text = "Tap to manage printer settings",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                } else {
                    Text(
                        text = "Tap to connect",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            // Paired status indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Red)
                )

                Text(
                    text = "Paired",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (bluetoothManager.isPrinterDevice(device)) MaterialTheme.colorScheme.onSecondaryContainer.copy(
                        alpha = 0.7f
                    )
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                if (!bluetoothManager.isPrinterDevice(device)) {
                    IconButton(
                        onClick = onConnect
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Connect",
                            tint = if (bluetoothManager.isPrinterDevice(device)) MaterialTheme.colorScheme.onSecondaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BluetoothScanConnectScreenPreview() {
    JewelVaultTheme {
        BluetoothScanConnectScreen()
    }
}
