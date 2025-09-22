package com.velox.jewelvault.ui.screen.bluetooth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.velox.jewelvault.data.printing.BluetoothDeviceInfo
import com.velox.jewelvault.data.printing.LabelFormat
import com.velox.jewelvault.data.printing.PrinterConfig
import com.velox.jewelvault.data.printing.PrinterLanguage
import com.velox.jewelvault.ui.components.CusOutlinedTextField
import com.velox.jewelvault.ui.components.InputFieldState
import com.velox.jewelvault.ui.components.PermissionRequester
import com.velox.jewelvault.ui.theme.JewelVaultTheme
import com.velox.jewelvault.utils.LocalSubNavController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothManagePrintersScreen(
    viewModel: BluetoothManagePrintersViewModel = hiltViewModel()
) {
/*    val navController = LocalSubNavController.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val uiState by viewModel.uiState.collectAsState()
    val bondedDevices by viewModel.bondedDevices.collectAsState()
    val isBluetoothEnabled by viewModel.isBluetoothEnabled.collectAsState()
    
    // Filter connected printers from bonded devices
    val connectedPrinters = bondedDevices.filter { it.isPrinter && it.isConnected }
    
    // Request Bluetooth permissions
    PermissionRequester(
        permissions = listOf(
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_SCAN,
            android.Manifest.permission.BLUETOOTH,
            android.Manifest.permission.BLUETOOTH_ADMIN,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ),
        onAllPermissionsGranted = {
            viewModel.loadConnectedPrinters()
        }
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { navController.popBackStack() }
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                
                Text(
                    text = "Manage Printers",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Bluetooth Status
        BluetoothStatusIndicator(
            isEnabled = isBluetoothEnabled,
            connectedCount = connectedPrinters.size
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Printer Configuration
        if (connectedPrinters.isNotEmpty()) {
            Text(
                text = "Printer Configuration",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(connectedPrinters) { printer ->
                    PrinterConfigCard(
                        printer = printer,
                        onUpdateConfig = { config -> viewModel.updatePrinterConfig(printer.address, config) },
                        onTestPrint = { viewModel.testPrint(printer.address) },
                        onDisconnect = { viewModel.disconnectPrinter(printer.address) }
                    )
                }
            }
        } else {
            // No connected printers message
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
                        imageVector = Icons.Default.Print,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "No Connected Printers",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = "Connect to a printer to configure settings and test printing",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        
        // Loading indicator
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
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
    }*/
}

@Composable
fun BluetoothStatusIndicator(
    isEnabled: Boolean,
    connectedCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                !isEnabled -> MaterialTheme.colorScheme.errorContainer
                connectedCount > 0 -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when {
                    !isEnabled -> Icons.Default.BluetoothDisabled
                    connectedCount > 0 -> Icons.Default.Print
                    else -> Icons.Default.Bluetooth
                },
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = when {
                    !isEnabled -> MaterialTheme.colorScheme.onErrorContainer
                    connectedCount > 0 -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Printer Status",
                    style = MaterialTheme.typography.titleMedium,
                    color = when {
                        !isEnabled -> MaterialTheme.colorScheme.onErrorContainer
                        connectedCount > 0 -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                
                Text(
                    text = when {
                        !isEnabled -> "Bluetooth disabled"
                        connectedCount > 0 -> "$connectedCount printer(s) connected"
                        else -> "No printers connected"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = when {
                        !isEnabled -> MaterialTheme.colorScheme.onErrorContainer
                        connectedCount > 0 -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

@Composable
fun PrinterConfigCard(
    printer: BluetoothDeviceInfo,
    onUpdateConfig: (PrinterConfig) -> Unit,
    onTestPrint: () -> Unit,
    onDisconnect: () -> Unit
) {
    var showConfigDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Printer header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Print,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = printer.name ?: "Unknown Printer",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Text(
                        text = printer.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { showConfigDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configure",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    IconButton(
                        onClick = onTestPrint
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = "Test Print",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    IconButton(
                        onClick = onDisconnect
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Disconnect",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            // Connection status
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Green)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = "Connected",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    
    // Configuration dialog
    if (showConfigDialog) {
        PrinterConfigDialog(
            printer = printer,
            onDismiss = { showConfigDialog = false },
            onSave = { config ->
                onUpdateConfig(config)
                showConfigDialog = false
            }
        )
    }
}

@Composable
fun PrinterConfigDialog(
    printer: BluetoothDeviceInfo,
    onDismiss: () -> Unit,
    onSave: (PrinterConfig) -> Unit
) {
    var labelFormat by remember { mutableStateOf(LabelFormat.THERMAL_100MM) }
    var printerLanguage by remember { mutableStateOf(PrinterLanguage.ESC_POS) }
    var dpi by remember { mutableStateOf(203) }
    var autoConnect by remember { mutableStateOf(true) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Configure Printer")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Label Format
                CusOutlinedTextField(
                    state = InputFieldState(labelFormat.description),
                    placeholderText = "Label Format",
                    dropdownItems = LabelFormat.values().map { it.description },
                    onDropdownItemSelected = { selected ->
                        labelFormat = LabelFormat.values().find { it.description == selected } ?: LabelFormat.THERMAL_100MM
                    },
                    readOnly = true
                )
                
                // Printer Language
                CusOutlinedTextField(
                    state = InputFieldState(printerLanguage.description),
                    placeholderText = "Printer Language",
                    dropdownItems = PrinterLanguage.values().map { it.description },
                    onDropdownItemSelected = { selected ->
                        printerLanguage = PrinterLanguage.values().find { it.description == selected } ?: PrinterLanguage.ESC_POS
                    },
                    readOnly = true
                )
                
                // DPI
                CusOutlinedTextField(
                    state = InputFieldState(dpi.toString()),
                    placeholderText = "DPI",
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                    onTextChange = { value ->
                        dpi = value.toIntOrNull() ?: 203
                    }
                )
                
                // Auto Connect
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = autoConnect,
                        onCheckedChange = { autoConnect = it }
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text("Auto-connect on startup")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val config = PrinterConfig(
                        deviceAddress = printer.address,
                        deviceName = printer.name ?: "Unknown",
                        labelFormat = labelFormat,
                        printerLanguage = printerLanguage,
                        dpi = dpi,
                        autoConnect = autoConnect
                    )
                    onSave(config)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun BluetoothManagePrintersScreenPreview() {
    JewelVaultTheme {
        BluetoothManagePrintersScreen()
    }
}
