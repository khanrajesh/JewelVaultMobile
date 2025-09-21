package com.velox.jewelvault.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velox.jewelvault.data.bluetooth.ConnectionState
import com.velox.jewelvault.utils.PrinterUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Printer status component that can be used across the app
 * Shows printer connection status and provides connect button
 */
@Composable
fun PrinterStatusComponent(
    modifier: Modifier = Modifier,
    onConnectPrinter: () -> Unit = {},
    showConnectButton: Boolean = true
) {
    var connectionStatus by remember { mutableStateOf<ConnectionState?>(null) }
    var printerName by remember { mutableStateOf<String?>(null) }
    var isChecking by remember { mutableStateOf(true) }
    
    val context = LocalContext.current
    
    // Check printer connection status
    LaunchedEffect(Unit) {
        try {
            connectionStatus = PrinterUtils.getConnectionStatus()
            printerName = PrinterUtils.getConnectedPrinterName()
        } catch (e: Exception) {
            connectionStatus = ConnectionState.DISCONNECTED
        } finally {
            isChecking = false
        }
    }
    
    if (isChecking) {
        // Loading state
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp
            )
            Text(
                text = "Checking printer...",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        when (connectionStatus) {
            ConnectionState.CONNECTED -> {
                // Printer connected
                Row(
                    modifier = modifier,
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.Green
                    )
                    Text(
                        text = "Printer: ${printerName ?: "Connected"}",
                        fontSize = 12.sp,
                        color = Color.Green,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            ConnectionState.CONNECTING -> {
                // Printer connecting
                Row(
                    modifier = modifier,
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "Connecting to printer...",
                        fontSize = 12.sp,
                        color = Color(0xFFFF9800)
                    )
                }
            }
            else -> {
                // Printer not connected
                Row(
                    modifier = modifier,
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.Red
                    )
                    Text(
                        text = "No printer connected",
                        fontSize = 12.sp,
                        color = Color.Red
                    )
                    if (showConnectButton) {
                        TextButton(
                            onClick = onConnectPrinter,
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                text = "Connect",
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Printer status chip component
 * Compact version for use in app bars or small spaces
 */
@Composable
fun PrinterStatusChip(
    modifier: Modifier = Modifier,
    onConnectPrinter: () -> Unit = {}
) {
    var connectionStatus by remember { mutableStateOf<ConnectionState?>(null) }
    var printerName by remember { mutableStateOf<String?>(null) }
    var isChecking by remember { mutableStateOf(true) }
    
    // Check printer connection status
    LaunchedEffect(Unit) {
        try {
            connectionStatus = PrinterUtils.getConnectionStatus()
            printerName = PrinterUtils.getConnectedPrinterName()
        } catch (e: Exception) {
            connectionStatus = ConnectionState.DISCONNECTED
        } finally {
            isChecking = false
        }
    }
    
    when {
        isChecking -> {
            Surface(
                modifier = modifier,
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 1.dp
                    )
                    Text(
                        text = "Checking...",
                        fontSize = 10.sp
                    )
                }
            }
        }
        connectionStatus == ConnectionState.CONNECTED -> {
            Surface(
                modifier = modifier,
                shape = RoundedCornerShape(16.dp),
                color = Color.Green.copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = Color.Green
                    )
                    Text(
                        text = "Printer: ${printerName?.take(10) ?: "Connected"}",
                        fontSize = 10.sp,
                        color = Color.Green
                    )
                }
            }
        }
        else -> {
            Surface(
                modifier = modifier,
                shape = RoundedCornerShape(16.dp),
                color = Color.Red.copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = Color.Red
                    )
                    Text(
                        text = "No Printer",
                        fontSize = 10.sp,
                        color = Color.Red
                    )
                    TextButton(
                        onClick = onConnectPrinter,
                        modifier = Modifier.height(24.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = "Connect",
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Print button with printer connection check
 * Shows different states based on printer connection
 */
@Composable
fun SmartPrintButton(
    text: String = "Print",
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onConnectPrinter: () -> Unit = {}
) {
    var connectionStatus by remember { mutableStateOf<ConnectionState?>(null) }
    var isChecking by remember { mutableStateOf(true) }
    
    // Check printer connection status
    LaunchedEffect(Unit) {
        try {
            connectionStatus = PrinterUtils.getConnectionStatus()
        } catch (e: Exception) {
            connectionStatus = ConnectionState.DISCONNECTED
        } finally {
            isChecking = false
        }
    }
    
    when {
        isChecking -> {
            Button(
                onClick = { },
                modifier = modifier,
                enabled = false
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Checking...")
            }
        }
        connectionStatus == ConnectionState.CONNECTED -> {
            Button(
                onClick = onClick,
                modifier = modifier
            ) {
                Icon(Icons.Default.Print, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text)
            }
        }
        else -> {
            Button(
                onClick = onConnectPrinter,
                modifier = modifier,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Warning, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Connect Printer")
            }
        }
    }
}

/**
 * Print action with error handling
 * Wraps print operations with proper error handling
 */
@Composable
fun PrintAction(
    onPrint: () -> Unit,
    onError: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isPrinting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Show error dialog if there's an error
    errorMessage?.let { error ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("Print Error") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) {
                    Text("OK")
                }
            }
        )
    }
    
    Button(
        onClick = {
            if (!isPrinting) {
                isPrinting = true
                try {
                    onPrint()
                } catch (e: Exception) {
                    errorMessage = e.message ?: "Unknown error occurred"
                    onError(errorMessage!!)
                } finally {
                    isPrinting = false
                }
            }
        },
        modifier = modifier,
        enabled = !isPrinting
    ) {
        if (isPrinting) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Printing...")
        } else {
            Icon(Icons.Default.Print, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Print")
        }
    }
}
