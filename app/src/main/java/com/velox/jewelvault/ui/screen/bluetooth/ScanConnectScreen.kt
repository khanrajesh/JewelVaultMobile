package com.velox.jewelvault.ui.screen.bluetooth

import android.bluetooth.BluetoothAdapter
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.velox.jewelvault.ui.components.PermissionRequester
import com.velox.jewelvault.ui.nav.SubScreens
import com.velox.jewelvault.ui.screen.bluetooth.components.ConnectedDeviceCard
import com.velox.jewelvault.ui.screen.bluetooth.components.ConnectingDeviceCard
import com.velox.jewelvault.ui.screen.bluetooth.components.DiscoveredDeviceCard
import com.velox.jewelvault.ui.screen.bluetooth.components.PairedDeviceCard
import com.velox.jewelvault.ui.screen.bluetooth.components.PossiblePrinterCard
import com.velox.jewelvault.ui.screen.bluetooth.components.SavedPrinterCard
import com.velox.jewelvault.ui.screen.bluetooth.components.isPrinterDevice
import com.velox.jewelvault.utils.LocalSubNavController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanConnectScreen(
    viewModel: ScanConnectViewModel = hiltViewModel()
) {
    val navController = LocalSubNavController.current
    LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val uiState by viewModel.uiState.collectAsState()
    val bluetoothManager = viewModel.bluetoothReceiver

    // Device lists from ViewModel
    val connectedDevices by viewModel.connectedDevices.collectAsStateWithLifecycle()
    val connectingDevices by viewModel.connectingDevices.collectAsStateWithLifecycle()
    val unconnectedPairedDevices by viewModel.unconnectedPairedDevices.collectAsStateWithLifecycle()
    val allDiscoveredDevices by viewModel.allDiscoveredDevices.collectAsStateWithLifecycle()
    val savedPrintersWithStatus by viewModel.savedPrintersWithStatus.collectAsStateWithLifecycle()

    val bleState by bluetoothManager.bluetoothStateChanged.collectAsStateWithLifecycle()
    val isBluetoothEnabled = (bleState.currentState == BluetoothAdapter.STATE_ON)

    val isDiscovering = bluetoothManager.isDiscovering.collectAsState().value

    // Start/stop screen-specific monitoring
    LaunchedEffect(Unit) {
        // Screen is active - start 5-second monitoring
        bluetoothManager.setScanConnectScreenActive(true)
    }

    // Cleanup when screen is disposed
    DisposableEffect(Unit) {
        onDispose {
            bluetoothManager.setScanConnectScreenActive(false)
        }
    }

    // Request Bluetooth permissions
    PermissionRequester(
        permissions = listOf(
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_SCAN,
            android.Manifest.permission.BLUETOOTH,
            android.Manifest.permission.BLUETOOTH_ADMIN,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ), onAllPermissionsGranted = {
            // Permissions granted - scanning is now manual via Start Scan button
        })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(topStart = 18.dp))
            .padding(5.dp)
    ) {

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
            ) {
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
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
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
                onClick = { viewModel.refreshAllDeviceLists() }, modifier = Modifier
            ) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = "Refresh Connected Devices",
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        LazyColumn(
            state = listState, verticalArrangement = Arrangement.spacedBy(8.dp)
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
                        savedPrinters = savedPrintersWithStatus.map { it.first },
                        onClick = {
                            if (isPrinterDevice(device, savedPrintersWithStatus.map { it.first })) {
                                navController.navigate(SubScreens.BluetoothManagePrinters.route)
                            }
                        }, 
                        onDisconnect = { viewModel.disconnectDevice(device.address) },
                        onAddAsPrinter = { device ->
                            viewModel.addDeviceAsPrinter(device)
                        }
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

            // Saved Printers (not already shown in connected)
            val savedPrintersNotConnected = savedPrintersWithStatus.filter { (printer, isConnected) ->
                !isConnected && !connectedDevices.any { it.address == printer.address }
            }
            if (savedPrintersNotConnected.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Saved Printers (${savedPrintersNotConnected.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(savedPrintersNotConnected) { (printer, isConnected) ->
                    SavedPrinterCard(
                        printer = printer,
                        isConnected = isConnected,
                        onClick = {
                            navController.navigate(SubScreens.BluetoothManagePrinters.route)
                        }
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
                    PairedDeviceCard(device = device, onConnect = {
                        viewModel.connectToDevice(device.address, isPrinterDevice(device))
                        scope.launch { listState.animateScrollToItem(0) }
                    }, onForget = { viewModel.forgetDevice(device.address) })
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
                val possiblePrinters = allDiscoveredDevices.filter { isPrinterDevice(it, savedPrintersWithStatus.map { it.first }) }
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
                            device = device, onPairAndConnect = {
                                viewModel.pairDevice(device.address,isPrinterDevice(device, savedPrintersWithStatus.map { it.first }))
                                scope.launch { listState.animateScrollToItem(0) }
                            })
                    }

                    // Divider space before non-printers
                    item { Spacer(modifier = Modifier.height(12.dp)) }
                }

                val nonPrinters = allDiscoveredDevices.filterNot { isPrinterDevice(it, savedPrintersWithStatus.map { it.first }) }
                items(nonPrinters) { device ->
                    DiscoveredDeviceCard(
                        device = device, onPairAndConnect = {
                            viewModel.pairDevice(device.address,isPrinterDevice(device, savedPrintersWithStatus.map { it.first }))
                            scope.launch { listState.animateScrollToItem(0) }
                        })
                }
            } else if (connectedDevices.isEmpty() && connectingDevices.isEmpty() && unconnectedPairedDevices.isEmpty()) {
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





