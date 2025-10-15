package com.velox.jewelvault.ui.screen.bluetooth.components

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.velox.jewelvault.data.bluetooth.BluetoothDeviceDetails

@Composable
fun DeviceDetailsDialog(
    device: BluetoothDeviceDetails?,
    onDismiss: () -> Unit
) {
    if (device == null) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Device Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Basic Info
                DetailRow("Name", device.name ?: "Unknown Device")
                DetailRow("MAC Address", device.address)
                
                // Bond State
                val bondStateText = when (device.bondState) {
                    10 -> "Not Paired" // BluetoothDevice.BOND_NONE
                    11 -> "Pairing..." // BluetoothDevice.BOND_BONDING
                    12 -> "Paired" // BluetoothDevice.BOND_BONDED
                    else -> "Unknown (${device.bondState})"
                }
                DetailRow("Bond State", bondStateText)
                
                // Connection State
                DetailRow("Connection State", device.state ?: "Unknown")
                
                // Device Type
                val deviceTypeText = when (device.type) {
                    1 -> "Classic Bluetooth" // BluetoothDevice.DEVICE_TYPE_CLASSIC
                    2 -> "Bluetooth Low Energy" // BluetoothDevice.DEVICE_TYPE_LE
                    3 -> "Dual Mode (Classic + LE)" // BluetoothDevice.DEVICE_TYPE_DUAL
                    0 -> "Unknown Type" // BluetoothDevice.DEVICE_TYPE_UNKNOWN
                    else -> "Unknown (${device.type})"
                }
                DetailRow("Device Type", deviceTypeText)
                
                // Connection Method (if available)
                device.extraInfo["connectionMethod"]?.let { method ->
                    DetailRow("Connection Method", method.toString())
                }
                
                // RSSI (if available)
                device.extraInfo["rssi"]?.let { rssi ->
                    DetailRow("Signal Strength", "${rssi} dBm")
                }
                
                // UUID (if available)
                device.extraInfo["uuid"]?.let { uuid ->
                    DetailRow("UUID", uuid.toString())
                }
                
                // Bluetooth Class (if available)
                device.bluetoothClass?.let { bluetoothClass ->
                    DetailRow("Bluetooth Class", bluetoothClass)
                }
                
                // Service UUIDs (if available)
                device.extraInfo["serviceUuids"]?.let { serviceUuids ->
                    DetailRow("Service UUIDs", serviceUuids.toString())
                }
                
                // Action (if available)
                device.action?.let { action ->
                    DetailRow("Action", action)
                }
                
                // Extra Info (all remaining key-value pairs)
                val extraInfoKeys = device.extraInfo.keys.filter { key ->
                    key !in listOf("connectionMethod", "rssi", "uuid", "serviceUuids")
                }
                
                if (extraInfoKeys.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Additional Info",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    extraInfoKeys.forEach { key ->
                        DetailRow(key, device.extraInfo[key]?.toString() ?: "null")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun DetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(120.dp)
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}
