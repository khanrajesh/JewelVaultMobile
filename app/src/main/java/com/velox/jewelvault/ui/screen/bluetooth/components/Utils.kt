package com.velox.jewelvault.ui.screen.bluetooth.components

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.velox.jewelvault.data.bluetooth.BluetoothDeviceDetails

// Helper function to check if device is a printer

fun isPrinterDevice(device: BluetoothDeviceDetails): Boolean {
    // Simple check based on device name or type
    return device.name?.contains("printer", ignoreCase = true) == true ||
            device.name?.contains("print", ignoreCase = true) == true ||
            device.bluetoothClass?.contains("printer", ignoreCase = true) == true
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
fun TypeTags(device: BluetoothDeviceDetails) {
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
