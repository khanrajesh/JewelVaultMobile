package com.velox.jewelvault.ui.screen.bluetooth.components

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
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.velox.jewelvault.data.bluetooth.PrinterInfo

@Composable
fun SavedPrinterCard(
    printer: PrinterInfo,
    isConnected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.Companion
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.Companion
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Companion.CenterVertically
        ) {
            Box(
                modifier = Modifier.Companion
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isConnected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Companion.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Print,
                    contentDescription = null,
                    modifier = Modifier.Companion.size(24.dp),
                    tint = if (isConnected)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.Companion.width(16.dp))

            Column(modifier = Modifier.Companion.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.Companion.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = printer.name ?: "Saved Printer",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isConnected)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (printer.isDefault) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Default Printer",
                            modifier = Modifier.Companion.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text(
                    text = printer.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isConnected)
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                Text(
                    text = "${printer.method} • ${if (isConnected) "Connected" else "Disconnected"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isConnected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                
                // Language support information
                if (printer.supportedLanguages.isNotEmpty() || printer.currentLanguage != null) {
                    Spacer(modifier = Modifier.Companion.size(4.dp))
                    
                    Row(
                        verticalAlignment = Alignment.Companion.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Languages: ",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isConnected)
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        
                        if (printer.supportedLanguages.isNotEmpty()) {
                            Text(
                                text = printer.supportedLanguages.joinToString(", "),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isConnected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        } else {
                            Text(
                                text = "None tested",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isConnected)
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                        
                        if (printer.currentLanguage != null) {
                            Text(
                                text = "• Current: ${printer.currentLanguage}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isConnected)
                                    MaterialTheme.colorScheme.secondary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}