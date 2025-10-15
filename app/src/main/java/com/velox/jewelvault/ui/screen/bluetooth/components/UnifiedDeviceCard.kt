package com.velox.jewelvault.ui.screen.bluetooth.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.velox.jewelvault.data.bluetooth.BluetoothDeviceDetails

@Composable
fun UnifiedDeviceCard(
    device: BluetoothDeviceDetails,
    category: DeviceCategory,
    isSavedPrinter: Boolean,
    rssi: Int?,
    connectionMethod: String?,
    onLeftButtonClick: (() -> Unit)?,
    onCenterButtonClick: (() -> Unit)?,
    onRightButtonClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = when {
                    isSavedPrinter -> MaterialTheme.colorScheme.primaryContainer
                    category == DeviceCategory.DISCOVERED && isPrinterDevice(device) -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Device Icon
            Icon(
                imageVector = getDeviceIcon(device),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = when {
                    isSavedPrinter -> MaterialTheme.colorScheme.onPrimaryContainer
                    category == DeviceCategory.DISCOVERED && isPrinterDevice(device) -> MaterialTheme.colorScheme.onSecondaryContainer
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Device Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Name + Status indicators on same line
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = device.name ?: "Unknown Device",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            isSavedPrinter -> MaterialTheme.colorScheme.onPrimaryContainer
                            category == DeviceCategory.DISCOVERED && isPrinterDevice(device) -> MaterialTheme.colorScheme.onSecondaryContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    
                    // Connecting indicator
                    if (category == DeviceCategory.CONNECTING) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    // Saved Printer Badge
                    if (isSavedPrinter) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Saved Printer",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    // RSSI
                    rssi?.let { signal ->
                        Text(
                            text = "• ${signal} dBm",
                            style = MaterialTheme.typography.bodySmall,
                            color = when {
                                isSavedPrinter -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                category == DeviceCategory.DISCOVERED && isPrinterDevice(device) -> MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            }
                        )
                    }
                }

                // MAC Address
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        isSavedPrinter -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        category == DeviceCategory.DISCOVERED && isPrinterDevice(device) -> MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    }
                )

                // Connection Status for Connecting Devices
                if (category == DeviceCategory.CONNECTING) {
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Connection method being tried
                    connectionMethod?.let { method ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Trying: $method",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    // Connection stage if available
                    device.extraInfo["stage"]?.let { stage ->
                        Text(
                            text = "Stage: $stage",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                    
                    // Connection state
                    device.state?.let { state ->
                        if (state != "CONNECTING") {
                            Text(
                                text = "Status: $state",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                // Connection Method + Device Type + UUID
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Connection Method Badge
                    connectionMethod?.let { method ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = method,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    // Device Type Badge
                    val deviceTypeText = when (device.type) {
                        1 -> "Classic" // BluetoothDevice.DEVICE_TYPE_CLASSIC
                        2 -> "LE"      // BluetoothDevice.DEVICE_TYPE_LE
                        3 -> "Dual"    // BluetoothDevice.DEVICE_TYPE_DUAL
                        else -> "Unknown"
                    }
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = deviceTypeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    // UUID (if available, truncated)
                    device.extraInfo["uuid"]?.toString()?.let { uuid ->
                        if (uuid.length > 8) {
                            Text(
                                text = "UUID: ${uuid.take(8)}...",
                                style = MaterialTheme.typography.labelSmall,
                                color = when {
                                    isSavedPrinter -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                                    category == DeviceCategory.DISCOVERED && isPrinterDevice(device) -> MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                }
                            )
                        }
                    }
                }
            }

            // Action Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Left Button
                when (category) {
                    DeviceCategory.CONNECTED -> {
                        // Gone/Invisible
                    }
                    DeviceCategory.CONNECTING -> {
                        // Disabled/Invisible
                    }
                    DeviceCategory.SAVED_PRINTER -> {
                        // Disabled/Invisible
                    }
                    DeviceCategory.PAIRED -> {
                        // Add as Printer
                        IconButton(
                            onClick = { onLeftButtonClick?.invoke() },
                            enabled = true
                        ) {
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = "Add as Printer",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    DeviceCategory.DISCOVERED -> {
                        // Disabled/Invisible
                    }
                }

                // Center Button (Main Action)
                IconButton(
                    onClick = { onCenterButtonClick?.invoke() },
                    enabled = onCenterButtonClick != null
                ) {
                    Icon(
                        imageVector = when (category) {
                            DeviceCategory.CONNECTED -> Icons.Default.Bluetooth
                            DeviceCategory.CONNECTING -> Icons.Default.Close
                            DeviceCategory.SAVED_PRINTER -> Icons.Default.Bluetooth
                            DeviceCategory.PAIRED -> Icons.Default.Bluetooth
                            DeviceCategory.DISCOVERED -> Icons.Default.Add
                        },
                        contentDescription = when (category) {
                            DeviceCategory.CONNECTED -> "Manage Device"
                            DeviceCategory.CONNECTING -> "Cancel Connection"
                            DeviceCategory.SAVED_PRINTER -> "Connect Printer"
                            DeviceCategory.PAIRED -> "Connect Device"
                            DeviceCategory.DISCOVERED -> "Pair Device"
                        },
                        tint = when (category) {
                            DeviceCategory.CONNECTED -> MaterialTheme.colorScheme.primary
                            DeviceCategory.CONNECTING -> MaterialTheme.colorScheme.error
                            DeviceCategory.SAVED_PRINTER -> MaterialTheme.colorScheme.primary
                            DeviceCategory.PAIRED -> MaterialTheme.colorScheme.primary
                            DeviceCategory.DISCOVERED -> MaterialTheme.colorScheme.primary
                        }
                    )
                }

                // Right Button
                when (category) {
                    DeviceCategory.CONNECTED -> {
                        // Disconnect
                        IconButton(
                            onClick = { onRightButtonClick?.invoke() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Disconnect",
                                tint = when {
                                    isSavedPrinter -> MaterialTheme.colorScheme.onPrimaryContainer
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                    DeviceCategory.CONNECTING -> {
                        // Cancel
                        IconButton(
                            onClick = { onRightButtonClick?.invoke() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel Connection",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    DeviceCategory.SAVED_PRINTER -> {
                        // Disabled/Invisible
                    }
                    DeviceCategory.PAIRED -> {
                        // Forget
                        IconButton(
                            onClick = { onRightButtonClick?.invoke() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.LinkOff,
                                contentDescription = "Forget Device",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    DeviceCategory.DISCOVERED -> {
                        // Disabled/Invisible
                    }
                }
            }
        }
    }
}
