package com.velox.jewelvault.ui.screen.bluetooth

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.Label
import androidx.compose.material.icons.twotone.CheckCircle
import androidx.compose.material.icons.twotone.Delete
import androidx.compose.material.icons.twotone.Link
import androidx.compose.material.icons.twotone.LinkOff
import androidx.compose.material.icons.twotone.Print
import androidx.compose.material.icons.twotone.Star
import androidx.compose.material.icons.twotone.StarBorder
import androidx.compose.material.icons.twotone.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.velox.jewelvault.utils.LocalSubNavController
import com.velox.jewelvault.ui.components.baseBackground0

@Composable
fun ManagePrintersScreen(
    viewModel: ManagePrintersViewModel
) {
    val subNavController = LocalSubNavController.current
    val savedPrinters by viewModel.savedPrinters.collectAsStateWithLifecycle()
    val defaultPrinter by viewModel.defaultPrinter.collectAsStateWithLifecycle()
    val printerStatuses by viewModel.printerStatuses.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .baseBackground0()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Manage Printers",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold
            )
            
            // Navigate to Label Templates
            Button(
                onClick = { subNavController.navigate("label_template_list") }
            ) {
                Icon(Icons.AutoMirrored.TwoTone.Label, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Templates")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (savedPrinters.isEmpty()) {
            // Empty state
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.TwoTone.Print,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "No saved printers yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

        Text(
                        text = "Connect to a printer to save it here",
            style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Text(
                text = "Saved Printers (${savedPrinters.size})",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(savedPrinters) { printer ->
                    val isConnected = printerStatuses[printer.address] ?: false
                    val isDefault = printer.address == defaultPrinter?.address
                    
                    PrinterManagementCard(
                        printer = printer,
                        isConnected = isConnected,
                        isDefault = isDefault,
                        onSetDefault = { viewModel.setDefaultPrinter(printer.address) },
                        onRemove = { viewModel.removePrinter(printer.address) },
                        onCheckConnection = { viewModel.checkConnection(printer.address, printer.method) },
                        onConnect = { viewModel.connectToPrinter(printer.address, printer.method) },
                        onDisconnect = { viewModel.disconnectPrinter(printer.address) },
                        onTestPrint = { protocol -> viewModel.testPrint(printer.address, protocol) },
                        onTestResult = { protocol, passed -> viewModel.handleTestResult(printer.address, protocol, passed) }
                    )
                }
            }
        }
    }
}

@Composable
fun PrinterManagementCard(
    printer: com.velox.jewelvault.data.roomdb.entity.printer.PrinterEntity,
    isConnected: Boolean,
    isDefault: Boolean,
    onSetDefault: () -> Unit,
    onRemove: () -> Unit,
    onCheckConnection: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onTestPrint: (String) -> Unit,
    onTestResult: (String, Boolean) -> Unit
) {
    // Language support information
    val supportedLanguagesList = printer.supportedLanguages?.let { 
        try {
            kotlinx.serialization.json.Json.decodeFromString<List<String>>(it)
        } catch (e: Exception) {
            emptyList()
        }
    } ?: emptyList()
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with printer info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isConnected) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.TwoTone.Print,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (isConnected) 
                            MaterialTheme.colorScheme.onPrimary 
                            else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = printer.name ?: "Saved Printer",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isConnected) 
                                MaterialTheme.colorScheme.onPrimaryContainer 
                                else 
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        if (isDefault) {
                            Icon(
                                imageVector = Icons.TwoTone.Star,
                                contentDescription = "Default Printer",
                                modifier = Modifier.size(20.dp),
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
                    
                    if (supportedLanguagesList.isNotEmpty() || printer.currentLanguage != null) {
                        Spacer(modifier = Modifier.size(4.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
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
                            
                            if (supportedLanguagesList.isNotEmpty()) {
                                Text(
                                    text = supportedLanguagesList.joinToString(", "),
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

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // First row of buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Set as Default button
                    if (!isDefault) {
                        OutlinedButton(
                            onClick = onSetDefault,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.TwoTone.StarBorder,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Set Default", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    // Check Connection button
                    OutlinedButton(
                        onClick = onCheckConnection,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.TwoTone.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Check", style = MaterialTheme.typography.labelSmall)
                    }

                    // Connect/Disconnect button
                    Button(
                        onClick = if (isConnected) onDisconnect else onConnect,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isConnected) 
                                MaterialTheme.colorScheme.error 
                                else 
                                MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = if (isConnected) Icons.TwoTone.LinkOff else Icons.TwoTone.Link,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            if (isConnected) "Disconnect" else "Connect",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    // Remove button
                    OutlinedButton(
                        onClick = onRemove,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.TwoTone.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Remove", style = MaterialTheme.typography.labelSmall)
                    }
                }
                
                // Test Print section (always show)
                TestPrintSection(
                    printerAddress = printer.address,
                    supportedLanguages = supportedLanguagesList,
                    onTestPrint = onTestPrint,
                    onTestResult = onTestResult
                )
            }
        }
    }
}


