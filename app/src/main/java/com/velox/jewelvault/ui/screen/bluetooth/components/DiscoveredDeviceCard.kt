package com.velox.jewelvault.ui.screen.bluetooth.components

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.velox.jewelvault.data.bluetooth.BluetoothDeviceDetails

@Composable
fun DiscoveredDeviceCard(
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