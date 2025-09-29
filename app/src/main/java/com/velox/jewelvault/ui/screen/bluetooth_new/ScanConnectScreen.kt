package com.velox.jewelvault.ui.screen.bluetooth_new

import android.bluetooth.BluetoothAdapter
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Watch
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.velox.jewelvault.data.bluetooth.BluetoothDeviceDetails
import com.velox.jewelvault.ui.components.PermissionRequester
import com.velox.jewelvault.ui.nav.SubScreens
import com.velox.jewelvault.utils.LocalSubNavController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanConnectScreen(
    viewModel: ScanConnectViewModel = hiltViewModel()
) {
    val navController = LocalSubNavController.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val uiState by viewModel.uiState.collectAsState()
    val bluetoothManager = viewModel.bluetoothReceiver
    
    // Device lists from ViewModel
    val connectedDevices by viewModel.connectedDevices.collectAsStateWithLifecycle()
    val connectingDevices by viewModel.connectingDevices.collectAsStateWithLifecycle()
    val unconnectedPairedDevices by viewModel.unconnectedPairedDevices.collectAsStateWithLifecycle()
    val allDiscoveredDevices by viewModel.allDiscoveredDevices.collectAsStateWithLifecycle()
    
    val bleState by bluetoothManager.bluetoothStateChanged.collectAsStateWithLifecycle()
    val isBluetoothEnabled = (bleState.currentState == BluetoothAdapter.STATE_ON)

    val isDiscovering = bluetoothManager.isDiscovering.collectAsState().value

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
                        isDiscovering -> MaterialTheme.colorScheme.primaryContainer
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
                            isDiscovering -> Icons.AutoMirrored.Filled.BluetoothSearching
                            else -> Icons.Default.Bluetooth
                        }, contentDescription = null, modifier = Modifier.size(24.dp), tint = when {
                            !isBluetoothEnabled -> MaterialTheme.colorScheme.onErrorContainer
                            isDiscovering -> MaterialTheme.colorScheme.onPrimaryContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = when {
                            !isBluetoothEnabled -> "Disabled - Please enable Bluetooth"
                            isDiscovering -> "Scanning for devices..."
                            else -> "Ready to scan"
                        }, style = MaterialTheme.typography.bodyMedium, color = when {
                            !isBluetoothEnabled -> MaterialTheme.colorScheme.onErrorContainer
                            isDiscovering -> MaterialTheme.colorScheme.onPrimaryContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )

                }
            }

                Button(
                    onClick = { if (isDiscovering) viewModel.stopScanning() else viewModel.startScanning() },
                    modifier = Modifier,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDiscovering) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = if (isDiscovering) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = if (isDiscovering) "Stop Scan" else "Start Scan"
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
                
                OutlinedButton(
                    onClick = { viewModel.refreshConnectedDevices() }, modifier = Modifier
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = "Refresh Connected Devices",
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
            state = listState,
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
                    ConnectedDeviceCard(
                        device = device, 
                        onManagePrinter = {
                            if (isPrinterDevice(device)) {
                                navController.navigate(SubScreens.BluetoothManagePrinters.route)
                            }
                        }, 
                        onDisconnect = { viewModel.disconnectDevice(device.address) }
                    )
                }
            }

            // Connecting Devices
            if (connectingDevices.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Connecting Devices (${connectingDevices.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(connectingDevices) { device ->
                    ConnectingDeviceCard(
                        device = device
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
                        onConnect = { viewModel.connectToDevice(device.address) }
                    )
                }
            }

            // Discovered Devices
            if (allDiscoveredDevices.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Discovered Devices (${allDiscoveredDevices.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Highlight likely printer devices first
                val possiblePrinters = allDiscoveredDevices.filter { isPrinterDevice(it) }
                if (possiblePrinters.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Possible Printers (${possiblePrinters.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    items(possiblePrinters) { device ->
                        PossiblePrinterCard(
                            device = device,
                            onPairAndConnect = {
                                viewModel.pairAndConnectDevice(device.address)
                                scope.launch { listState.animateScrollToItem(0) }
                            }
                        )
                    }

                    // Divider space before non-printers
                    item { Spacer(modifier = Modifier.height(12.dp)) }
                }

                val nonPrinters = allDiscoveredDevices.filterNot { isPrinterDevice(it) }
                items(nonPrinters) { device ->
                    DeviceCard(
                        device = device,
                        onPairAndConnect = {
                            viewModel.pairAndConnectDevice(device.address)
                            scope.launch { listState.animateScrollToItem(0) }
                        }
                    )
                }
            } else if (connectedDevices.isEmpty() && connectingDevices.isEmpty() && unconnectedPairedDevices.isEmpty()) {
                item {
                    // No devices message
                    Card(
                        modifier = Modifier.fillMaxWidth(), 
                        colors = CardDefaults.cardColors(
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
                                imageVector = Icons.AutoMirrored.Filled.BluetoothSearching,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = if (isDiscovering) "Scanning for devices..." else "No devices found",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (!isDiscovering) {
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


// Helper function to check if device is a printer

fun isPrinterDevice(device: BluetoothDeviceDetails): Boolean {
    // Simple check based on device name or type
    return device.name?.contains("printer", ignoreCase = true) == true ||
           device.name?.contains("print", ignoreCase = true) == true ||
           device.bluetoothClass?.contains("printer", ignoreCase = true) == true
}

@Composable
fun DeviceCard(
    device: BluetoothDeviceDetails, 
    onPairAndConnect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onPairAndConnect()
            }
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
                            device.action.contains("CONNECTED") -> MaterialTheme.colorScheme.primary
                            device.bondState == BluetoothDevice.BOND_BONDED -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ), contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getDeviceIcon(device),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = when {
                        device.action.contains("CONNECTED") -> MaterialTheme.colorScheme.onPrimary
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
                                    device.action.contains("CONNECTED") -> Color.Green
                                    device.bondState == BluetoothDevice.BOND_BONDED -> Color.Blue
                                    else -> Color.Gray
                                }
                            )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = when {
                            device.action.contains("CONNECTED") -> "Connected"
                            device.bondState == BluetoothDevice.BOND_BONDED -> "Paired"
                            else -> "Available"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Device type tags
                    Spacer(modifier = Modifier.width(8.dp))
                    TypeTags(device)
                }
            }

            // Action button (explicit)
            OutlinedButton(onClick = onPairAndConnect) {
                Text("Pair & Connect")
            }
        }
    }
}

@Composable
fun PossiblePrinterCard(
    device: BluetoothDeviceDetails,
    onPairAndConnect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPairAndConnect() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.secondary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Print,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSecondary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name ?: "Possible Printer",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun TypeTags(device: BluetoothDeviceDetails) {
    val tags = buildList {
        if (isPrinterDevice(device)) add("Printer")
        when (device.type) {
            BluetoothDevice.DEVICE_TYPE_CLASSIC -> add("Classic")
            BluetoothDevice.DEVICE_TYPE_LE -> add("LE")
            BluetoothDevice.DEVICE_TYPE_DUAL -> add("LE + Classic")
            BluetoothDevice.DEVICE_TYPE_UNKNOWN -> add("Unknown")
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        tags.forEach { tag ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = tag,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun getDeviceIcon(device: BluetoothDeviceDetails): ImageVector {
    val name = device.name?.lowercase() ?: ""
    val cls = device.bluetoothClass?.lowercase() ?: ""
    return when {
        isPrinterDevice(device) -> Icons.Default.Print
        name.contains("headset") || name.contains("headphone") || cls.contains("headset") || cls.contains("headphone") -> Icons.Default.Headset
        name.contains("speaker") || cls.contains("speaker") -> Icons.Default.Speaker
        name.contains("keyboard") || cls.contains("keyboard") -> Icons.Default.Keyboard
        name.contains("mouse") || cls.contains("mouse") -> Icons.Default.Mouse
        name.contains("watch") || cls.contains("watch") -> Icons.Default.Watch
        name.contains("phone") || name.contains("iphone") || cls.contains("phone") -> Icons.Default.Smartphone
        device.type == BluetoothDevice.DEVICE_TYPE_LE -> Icons.AutoMirrored.Filled.BluetoothSearching
        device.type == BluetoothDevice.DEVICE_TYPE_CLASSIC -> Icons.Default.Bluetooth
        device.type == BluetoothDevice.DEVICE_TYPE_DUAL -> Icons.Default.Devices
        device.type == BluetoothDevice.DEVICE_TYPE_UNKNOWN -> Icons.Default.DeviceHub
        else -> Icons.Default.DeviceHub
    }
}

@Composable
fun getDeviceIcon(type: Int): ImageVector {
    return when (type) {
        BluetoothDevice.DEVICE_TYPE_LE -> Icons.AutoMirrored.Filled.BluetoothSearching
        BluetoothDevice.DEVICE_TYPE_CLASSIC -> Icons.Default.Bluetooth
        BluetoothDevice.DEVICE_TYPE_DUAL -> Icons.Default.Devices
        BluetoothDevice.DEVICE_TYPE_UNKNOWN -> Icons.Default.DeviceHub
        else -> Icons.Default.DeviceHub
    }
}

@Composable
fun ConnectedDeviceCard(
    device: BluetoothDeviceDetails,
    onManagePrinter: () -> Unit,
    onDisconnect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (isPrinterDevice(device)) {
                    onManagePrinter()
                }
            }, colors = CardDefaults.cardColors(
            containerColor = if (isPrinterDevice(device)) MaterialTheme.colorScheme.primaryContainer
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
                    imageVector = getDeviceIcon(device),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (isPrinterDevice(device)) MaterialTheme.colorScheme.onPrimaryContainer
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
                    color = if (isPrinterDevice(device)) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = device.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isPrinterDevice(device)) MaterialTheme.colorScheme.onPrimaryContainer.copy(
                        alpha = 0.7f
                    )
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                if (isPrinterDevice(device)) {
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
                    color = if (isPrinterDevice(device)) MaterialTheme.colorScheme.onPrimaryContainer.copy(
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
                        tint = if (isPrinterDevice(device)) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun PairedDeviceCard(
    device: BluetoothDeviceDetails,
    onConnect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onConnect()
            }, colors = CardDefaults.cardColors(
            containerColor = if (isPrinterDevice(device)) MaterialTheme.colorScheme.secondaryContainer
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
                imageVector = getDeviceIcon(device),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (isPrinterDevice(device)) MaterialTheme.colorScheme.onSecondaryContainer
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
                    color = if (isPrinterDevice(device)) MaterialTheme.colorScheme.onSecondaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = device.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isPrinterDevice(device)) MaterialTheme.colorScheme.onSecondaryContainer.copy(
                        alpha = 0.7f
                    )
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                Text(
                    text = "Tap to connect",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
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
                    color = if (isPrinterDevice(device)) MaterialTheme.colorScheme.onSecondaryContainer.copy(
                        alpha = 0.7f
                    )
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                IconButton(
                    onClick = onConnect
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Connect",
                        tint = if (isPrinterDevice(device)) MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

