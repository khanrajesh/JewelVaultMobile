package com.velox.jewelvault.ui.screen.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DeviceUnknown
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.NetworkWifi
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tablet
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.velox.jewelvault.data.bluetooth.ConnectionState
import com.velox.jewelvault.data.bluetooth.DeviceType
import com.velox.jewelvault.data.bluetooth.DeviceUiModel
import com.velox.jewelvault.ui.components.PermissionRequester
import com.velox.jewelvault.utils.LocalBaseViewModel
import com.velox.jewelvault.utils.LocalSubNavController
import com.velox.jewelvault.utils.log
import com.velox.jewelvault.utils.permissions.getBluetoothPermissions

/**
 * Screen for scanning and connecting to Bluetooth devices
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanAndConnectScreen(
    onNavigateToPrinterManagement: () -> Unit, 
    viewModel: BluetoothViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    val connectionStatus by viewModel.connectionStatus.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val isDiscovering by viewModel.isDiscovering.collectAsStateWithLifecycle()
    val bluetoothAdapterState by viewModel.bluetoothAdapterState.collectAsStateWithLifecycle()
    val connectedDevice by viewModel.connectedDevice.collectAsStateWithLifecycle()
    
    val baseViewModel = LocalBaseViewModel.current
    LocalSubNavController.current
    val context = LocalContext.current

    // Permission state - use ViewModel's permission check
    val hasPermissions = viewModel.hasBluetoothPermissions()
    val isBluetoothAvailable = viewModel.isBluetoothAvailable()

    // Set screen heading
    LaunchedEffect(Unit) {
        baseViewModel.currentScreenHeading = "Bluetooth Scan"
    }

    // Handle errors with snackbar
    LaunchedEffect(errorMessage) {
        errorMessage?.let { error ->
            baseViewModel.snackBarState = error
            viewModel.clearError()
        }
    }

    // Check if we should navigate to printer management
    LaunchedEffect(connectionStatus) {
        if (connectionStatus == ConnectionState.CONNECTED) {
            onNavigateToPrinterManagement()
        }
    }

    // Handle Bluetooth state changes
    LaunchedEffect(bluetoothAdapterState) {
        when (bluetoothAdapterState) {
            BluetoothAdapter.STATE_OFF -> {
                log("Bluetooth is OFF - stopping discovery")
                viewModel.stopDiscovery()
            }
            BluetoothAdapter.STATE_ON -> {
                log("Bluetooth is ON - ready for operations")
            }
        }
    }

    // Debug logging for discovery state changes
    LaunchedEffect(isDiscovering) {
        log("ScanAndConnectScreen: Current discovery state - isDiscovering: $isDiscovering, hasPermissions: $hasPermissions, isBluetoothAvailable: $isBluetoothAvailable")
    }

    // Debug logging for device list changes
    LaunchedEffect(devices) {
        log("ScanAndConnectScreen: devices list changed - count: ${devices.size}")
        devices.forEachIndexed { index, device ->
            log("ScanAndConnectScreen: Device $index - Name: '${device.name}', Address: ${device.address}, Type: ${device.deviceType}, Printer: ${device.isPrinterCandidate}")
        }
    }
    
    // Debug logging for permissions and Bluetooth availability
    LaunchedEffect(hasPermissions, isBluetoothAvailable) {
        log("ScanAndConnectScreen: Permissions or Bluetooth availability changed - hasPermissions: $hasPermissions, isBluetoothAvailable: $isBluetoothAvailable")
    }

    // Permission requester
    PermissionRequester(
        permissions = getBluetoothPermissions()
    ) {
        // Permissions granted - no action needed as hasPermissions will update
    }

    // Show permission denied message if permissions are not granted
    if (!hasPermissions) {
        Box(
            modifier = Modifier.fillMaxSize(), 
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.padding(16.dp), 
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.BluetoothDisabled,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Bluetooth Permissions Required",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Please grant Bluetooth permissions to scan and connect to devices.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
        return
    }

    // Show Bluetooth disabled message if Bluetooth is not available
    if (!isBluetoothAvailable) {
        Box(
            modifier = Modifier.fillMaxSize(), 
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.padding(16.dp), 
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.BluetoothDisabled,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Bluetooth Not Available",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Please enable Bluetooth to scan and connect to devices.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Title
            Text(
                text = "All Bluetooth Devices",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
            
            // Header with controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Scan/Stop button
                Button(
                    onClick = {
                        log("ScanAndConnectScreen: Scan/Stop button clicked - isDiscovering: $isDiscovering, hasPermissions: $hasPermissions, isBluetoothAvailable: $isBluetoothAvailable")
                        if (isDiscovering) {
                            log("ScanAndConnectScreen: Stopping discovery")
                            viewModel.stopDiscovery()
                        } else {
                log("ScanAndConnectScreen: Starting discovery")
                viewModel.startDiscovery()
                        }
                    }, 
                    modifier = Modifier, 
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDiscovering) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (isDiscovering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onError
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Stop Scanning")
                    } else {
                        Icon(Icons.Default.Search, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Scanning")
                    }
                }

            }

            // Connection status indicator
            if (connectedDevice != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Connected to: ${connectedDevice?.name ?: "Unknown Device"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }


            // Device list
            if (devices.isEmpty() && !isDiscovering) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Bluetooth,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No devices found",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap 'Start Scan' to discover nearby Bluetooth devices (paired and unpaired)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                val categorizedDevices = categorizeAndSortDevices(devices)

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Connected devices
                    if (categorizedDevices.connected.isNotEmpty()) {
                        item {
                            CategoryHeader("Connected Devices", categorizedDevices.connected.size)
                        }
                        items(categorizedDevices.connected) { device ->
                            DeviceItem(
                                device = device, 
                                onConnect = {
                                    if (hasPermissions && isBluetoothAvailable) {
                                        viewModel.connect(device.address)
                                    } else {
                                        baseViewModel.snackBarState = "Bluetooth permissions required or Bluetooth not available"
                                    }
                                }, 
                                onDisconnect = {
                                    if (hasPermissions && isBluetoothAvailable) {
                                        viewModel.disconnect()
                                    } else {
                                        baseViewModel.snackBarState = "Bluetooth permissions required or Bluetooth not available"
                                    }
                                }, 
                                onProbe = {
                                    if (hasPermissions && isBluetoothAvailable) {
                                        viewModel.probeDevice(device)
                                    } else {
                                        baseViewModel.snackBarState = "Bluetooth permissions required or Bluetooth not available"
                                    }
                                }, 
                                deviceCategory = DeviceCategory.CONNECTED
                            )
                        }
                    }


                    // Known devices
                    if (categorizedDevices.known.isNotEmpty()) {
                        item {
                            CategoryHeader("Known Devices", categorizedDevices.known.size)
                        }
                        items(categorizedDevices.known) { device ->
                            DeviceItem(
                                device = device, 
                                onConnect = {
                                    if (hasPermissions && isBluetoothAvailable) {
                                        viewModel.connect(device.address)
                                    } else {
                                        baseViewModel.snackBarState = "Bluetooth permissions required or Bluetooth not available"
                                    }
                                }, 
                                onDisconnect = {
                                    if (hasPermissions && isBluetoothAvailable) {
                                        viewModel.disconnect()
                                    } else {
                                        baseViewModel.snackBarState = "Bluetooth permissions required or Bluetooth not available"
                                    }
                                }, 
                                onProbe = {
                                    if (hasPermissions && isBluetoothAvailable) {
                                        viewModel.probeDevice(device)
                                    } else {
                                        baseViewModel.snackBarState = "Bluetooth permissions required or Bluetooth not available"
                                    }
                                }, 
                                deviceCategory = DeviceCategory.KNOWN
                            )
                        }
                    }

                    // Unknown devices
                    if (categorizedDevices.unknown.isNotEmpty()) {
                        item {
                            CategoryHeader("Unknown Devices", categorizedDevices.unknown.size)
                        }
                        items(categorizedDevices.unknown) { device ->
                            DeviceItem(
                                device = device, 
                                onConnect = {
                                    if (hasPermissions && isBluetoothAvailable) {
                                        viewModel.connect(device.address)
                                    } else {
                                        baseViewModel.snackBarState = "Bluetooth permissions required or Bluetooth not available"
                                    }
                                }, 
                                onDisconnect = {
                                    if (hasPermissions && isBluetoothAvailable) {
                                        viewModel.disconnect()
                                    } else {
                                        baseViewModel.snackBarState = "Bluetooth permissions required or Bluetooth not available"
                                    }
                                }, 
                                onProbe = {
                                    if (hasPermissions && isBluetoothAvailable) {
                                        viewModel.probeDevice(device)
                                    } else {
                                        baseViewModel.snackBarState = "Bluetooth permissions required or Bluetooth not available"
                                    }
                                }, 
                                deviceCategory = DeviceCategory.UNKNOWN
                            )
                        }
                    }

                    // Paired devices
                    if (categorizedDevices.paired.isNotEmpty()) {
                        item {
                            CategoryHeader("Paired Devices", categorizedDevices.paired.size)
                        }
                        items(categorizedDevices.paired) { device ->
                            DeviceItem(
                                device = device, 
                                onConnect = {
                                    if (hasPermissions && isBluetoothAvailable) {
                                        viewModel.connect(device.address)
                                    } else {
                                        baseViewModel.snackBarState = "Bluetooth permissions required or Bluetooth not available"
                                    }
                                }, 
                                onDisconnect = {
                                    if (hasPermissions && isBluetoothAvailable) {
                                        viewModel.disconnect()
                                    } else {
                                        baseViewModel.snackBarState = "Bluetooth permissions required or Bluetooth not available"
                                    }
                                }, 
                                onProbe = {
                                    if (hasPermissions && isBluetoothAvailable) {
                                        viewModel.probeDevice(device)
                                    } else {
                                        baseViewModel.snackBarState = "Bluetooth permissions required or Bluetooth not available"
                                    }
                                }, 
                                deviceCategory = DeviceCategory.PAIRED
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceItem(
    device: DeviceUiModel,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onProbe: () -> Unit,
    deviceCategory: DeviceCategory
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (deviceCategory) {
                DeviceCategory.CONNECTED -> MaterialTheme.colorScheme.primaryContainer
                DeviceCategory.PAIRED -> MaterialTheme.colorScheme.secondaryContainer
                DeviceCategory.KNOWN -> MaterialTheme.colorScheme.tertiaryContainer
                DeviceCategory.UNKNOWN -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with device info and actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)
                ) {
                    // Device icon with category indicator
                    Box {
                        Icon(
                            imageVector = getDeviceIcon(device.deviceType),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = getDeviceIconColor(device)
                        )
                        // Category indicator dot
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(
                                    color = when (deviceCategory) {
                                        DeviceCategory.CONNECTED -> MaterialTheme.colorScheme.primary
                                        DeviceCategory.PAIRED -> MaterialTheme.colorScheme.secondary
                                        DeviceCategory.KNOWN -> MaterialTheme.colorScheme.tertiary
                                        DeviceCategory.UNKNOWN -> MaterialTheme.colorScheme.outline
                                    }, shape = RoundedCornerShape(6.dp)
                                )
                                .align(Alignment.TopEnd)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = device.name ?: "Unknown Device",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                )
                                // Signal strength indicator with color variation
                                if (device.rssi != null) {
                                    val rssi = device.rssi
                                    // Color logic: strong = green, medium = yellow, weak = red
                                    val signalColor = when {
                                        rssi >= -60 -> MaterialTheme.colorScheme.primary // strong
                                        rssi >= -80 -> MaterialTheme.colorScheme.secondary // medium
                                        else -> MaterialTheme.colorScheme.error // weak
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.NetworkWifi,
                                            contentDescription = "Signal Strength",
                                            tint = signalColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "${rssi} dBm",
                                            fontSize = 12.sp,
                                            color = signalColor,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                if (device.isPrinterCandidate) {
                                    PrinterCandidateChip()
                                }
                            }
                        }
                        Text(
                            text = "Address: ${device.address}, Hash: ${device.hashCode()} ",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Device Type: ${device.deviceType.name}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Smart action button
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SmartConnectButton(
                        connectionStatus = device.connectionState,
                        onConnect = onConnect,
                        onDisconnect = onDisconnect
                    )

                    if (device.isPrinterCandidate) {
                        Button(
                            onClick = onProbe, colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            ), modifier = Modifier.height(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Probe",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Probe", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Device details section
            DeviceDetailsSection(device = device)

            Spacer(modifier = Modifier.height(8.dp))


        }
    }
}

@Composable
private fun DeviceDetailsSection(device: DeviceUiModel) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {

        Spacer(modifier = Modifier.height(4.dp))
        Row {

            Text(
                text = "Supported Services (${device.supportedServices.size}):",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (device.supportedServices.isNotEmpty()) {

                device.supportedServices.forEach { service ->
                    Text(
                        text = "• $service",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
        // Service information

        // Printer probe results
        device.probeResult?.let { result ->
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    if (result.success) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (result.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                Text(
                    text = if (result.success) "Printer Detected" else "Not a Printer",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (result.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }

            if (result.success) {
                result.language?.let { language ->
                    InfoRow("Detected Language", language.displayName)
                }
                InfoRow("Response Time", "${result.responseTime}ms")
            } else {
                result.error?.let { error ->
                    InfoRow("Probe Error", error)
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

enum class DeviceCategory(val displayName: String) {
    CONNECTED("Connected"), PAIRED("Paired"), KNOWN("Known"), UNKNOWN("Unknown")
}


@Composable
private fun DeviceTypeChip(deviceType: DeviceType) {
    val (text, color) = when (deviceType) {
        DeviceType.PRINTER -> "Printer" to MaterialTheme.colorScheme.primary
        DeviceType.PHONE -> "Phone" to MaterialTheme.colorScheme.secondary
        DeviceType.TABLET -> "Tablet" to MaterialTheme.colorScheme.secondary
        DeviceType.COMPUTER -> "Computer" to MaterialTheme.colorScheme.tertiary
        DeviceType.LAPTOP -> "Laptop" to MaterialTheme.colorScheme.tertiary
        DeviceType.EARPHONE -> "Earphone" to MaterialTheme.colorScheme.primaryContainer
        DeviceType.HEADPHONE -> "Headphone" to MaterialTheme.colorScheme.primaryContainer
        DeviceType.SPEAKER -> "Speaker" to MaterialTheme.colorScheme.primaryContainer
        DeviceType.WATCH -> "Watch" to MaterialTheme.colorScheme.secondaryContainer
        DeviceType.KEYBOARD -> "Keyboard" to MaterialTheme.colorScheme.tertiaryContainer
        DeviceType.MOUSE -> "Mouse" to MaterialTheme.colorScheme.tertiaryContainer
        DeviceType.GAMEPAD -> "Gamepad" to MaterialTheme.colorScheme.tertiaryContainer
        DeviceType.CAMERA -> "Camera" to MaterialTheme.colorScheme.errorContainer
        DeviceType.SMART_TV -> "Smart TV" to MaterialTheme.colorScheme.secondaryContainer
        DeviceType.ROUTER -> "Router" to MaterialTheme.colorScheme.tertiaryContainer
        DeviceType.CAR -> "Car" to MaterialTheme.colorScheme.primaryContainer
        DeviceType.FITNESS_TRACKER -> "Fitness" to MaterialTheme.colorScheme.secondaryContainer
        DeviceType.UNKNOWN -> "Unknown" to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.1f)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun SmartConnectButton(
    connectionStatus: ConnectionState, onConnect: () -> Unit, onDisconnect: () -> Unit
) {
    val text = when (connectionStatus) {
        ConnectionState.CONNECTED -> "Disconnect"
        ConnectionState.CONNECTING -> "Connecting..."
        ConnectionState.DISCONNECTING -> "Disconnecting..."
        ConnectionState.DISCONNECTED -> "Connect"
    }

    val icon = when (connectionStatus) {
        ConnectionState.CONNECTED -> Icons.Default.Close
        ConnectionState.CONNECTING -> Icons.Default.HourglassEmpty
        ConnectionState.DISCONNECTING -> Icons.Default.HourglassEmpty
        ConnectionState.DISCONNECTED -> Icons.Default.Bluetooth
    }

    val containerColor = when (connectionStatus) {
        ConnectionState.CONNECTED -> MaterialTheme.colorScheme.error
        ConnectionState.CONNECTING -> MaterialTheme.colorScheme.tertiary
        ConnectionState.DISCONNECTING -> MaterialTheme.colorScheme.tertiary
        ConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.primary
    }

    val enabled = when (connectionStatus) {
        ConnectionState.CONNECTED -> true
        ConnectionState.CONNECTING -> false
        ConnectionState.DISCONNECTING -> false
        ConnectionState.DISCONNECTED -> true
    }

    Button(
        onClick = {
            when (connectionStatus) {
                ConnectionState.CONNECTED -> onDisconnect()
                ConnectionState.CONNECTING -> { /* Do nothing */
                }  ConnectionState.DISCONNECTING -> { /* Do nothing */
                }

                ConnectionState.DISCONNECTED -> onConnect()
            }
        }, enabled = enabled, colors = ButtonDefaults.buttonColors(
            containerColor = containerColor
        ), modifier = Modifier.height(32.dp)
    ) {
        if (connectionStatus == ConnectionState.CONNECTING) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onTertiary
            )
        } else {
            Icon(
                imageVector = icon, contentDescription = text, modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = text, fontSize = 12.sp)
    }
}

@Composable
private fun PrinterCandidateChip() {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    ) {
        Text(
            text = "Printer",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

private fun getDeviceIcon(deviceType: DeviceType): ImageVector {
    return when (deviceType) {
        DeviceType.PRINTER -> Icons.Default.Print
        DeviceType.PHONE -> Icons.Default.Phone
        DeviceType.TABLET -> Icons.Default.Tablet
        DeviceType.COMPUTER -> Icons.Default.Computer
        DeviceType.LAPTOP -> Icons.Default.Laptop
        DeviceType.EARPHONE -> Icons.Default.Headphones
        DeviceType.HEADPHONE -> Icons.Default.Headphones
        DeviceType.SPEAKER -> Icons.Default.Speaker
        DeviceType.WATCH -> Icons.Default.Watch
        DeviceType.KEYBOARD -> Icons.Default.Keyboard
        DeviceType.MOUSE -> Icons.Default.Mouse
        DeviceType.GAMEPAD -> Icons.Default.SportsEsports
        DeviceType.CAMERA -> Icons.Default.CameraAlt
        DeviceType.SMART_TV -> Icons.Default.Tv
        DeviceType.ROUTER -> Icons.Default.Router
        DeviceType.CAR -> Icons.Default.DirectionsCar
        DeviceType.FITNESS_TRACKER -> Icons.Default.FitnessCenter
        DeviceType.UNKNOWN -> Icons.Default.DeviceUnknown
    }
}

@Composable
private fun getDeviceIconColor(device: DeviceUiModel): Color {
    return when {
        device.isPrinterCandidate -> MaterialTheme.colorScheme.primary
        device.connectionState == ConnectionState.CONNECTED -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val formatter = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault())
    return formatter.format(java.util.Date(timestamp))
}

data class CategorizedDevices(
    val connected: List<DeviceUiModel>,
    val paired: List<DeviceUiModel>,
    val known: List<DeviceUiModel>,
    val unknown: List<DeviceUiModel>
)

private fun categorizeAndSortDevices(devices: List<DeviceUiModel>): CategorizedDevices {
    val connected = devices.filter { it.connectionState == ConnectionState.CONNECTED }
        .sortedByDescending { it.lastSeenMillis }

    val paired = devices.filter {
        it.connectionState != ConnectionState.CONNECTED && isPairedDevice(it)
    }.sortedByDescending { it.lastSeenMillis }

    val known = devices.filter {
        it.connectionState != ConnectionState.CONNECTED && !isPairedDevice(it) && isKnownDevice(it)
    }.sortedByDescending { it.lastSeenMillis }

    val unknown = devices.filter {
        it.connectionState != ConnectionState.CONNECTED && !isPairedDevice(it) && !isKnownDevice(it)
    }.sortedByDescending { it.lastSeenMillis }

    return CategorizedDevices(connected, paired, known, unknown)
}

private fun isPairedDevice(device: DeviceUiModel): Boolean {
    // Check if device is paired based on bond state
    return device.bondState == android.bluetooth.BluetoothDevice.BOND_BONDED
}

private fun isKnownDevice(device: DeviceUiModel): Boolean {
    // Check if device is known (has been seen before, is a printer candidate, or has services)
    return device.isPrinterCandidate || 
           device.supportedServices.isNotEmpty() || 
           (device.name != null && device.name != "Unknown Device")
}

@Composable
private fun CategoryHeader(title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        ) {
            Text(
                text = count.toString(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}
