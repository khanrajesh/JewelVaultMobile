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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.velox.jewelvault.data.bluetooth.LabelSizes
import com.velox.jewelvault.data.bluetooth.PrinterLanguage
import com.velox.jewelvault.data.bluetooth.PrinterStatus
import com.velox.jewelvault.ui.components.CusOutlinedTextField
import com.velox.jewelvault.ui.components.InputFieldState
import com.velox.jewelvault.utils.LocalBaseViewModel
import com.velox.jewelvault.utils.LocalSubNavController
import com.velox.jewelvault.ui.nav.SubScreens
import com.velox.jewelvault.utils.createTestHtml
import com.velox.jewelvault.utils.createJewelryLabelHtml

/**
 * Screen for managing printer settings and printing operations
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrinterManagementScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPreview: (String) -> Unit,
    viewModel: PrinterViewModel = hiltViewModel()
) {
    val printerSettings by viewModel.printerSettings.collectAsStateWithLifecycle()
    val printerStatus by viewModel.printerStatus.collectAsStateWithLifecycle()
    val printPreview by viewModel.printPreview.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val isPrinting by viewModel.isPrinting.collectAsStateWithLifecycle()
    val isGeneratingPreview by viewModel.isGeneratingPreview.collectAsStateWithLifecycle()
    val baseViewModel = LocalBaseViewModel.current
    val subNavController = LocalSubNavController.current
    val context = LocalContext.current

    // Set screen heading
    LaunchedEffect(Unit) {
        baseViewModel.currentScreenHeading = "Printer Management"
    }
    
    // Handle errors with snackbar
    LaunchedEffect(errorMessage) {
        errorMessage?.let { error ->
            baseViewModel.snackBarState = error
            viewModel.clearError()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top App Bar
            TopAppBar(
                title = { Text("Printer Management") },
                navigationIcon = {
                    IconButton(onClick = { 
                        subNavController.navigate(SubScreens.Dashboard.route) {
                            popUpTo(SubScreens.Dashboard.route) {
                                inclusive = true
                            }
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (printerStatus.isConnected) {
                        Button(
                            onClick = { viewModel.disconnect() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Disconnect",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Disconnect", fontSize = 12.sp)
                        }
                    }
                }
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Printer status
                item {
                    PrinterStatusCard(printerStatus = printerStatus)
                }

                // Printer settings
                item {
                    PrinterSettingsCard(
                        settings = printerSettings,
                        onLabelSizeChange = { viewModel.updateLabelSize(it) },
                        onLanguageChange = { viewModel.updateLanguage(it) },
                        onDensityChange = { viewModel.updateDensity(it) },
                        onAutoCutChange = { viewModel.updateAutoCut(it) },
                        onAutoFeedChange = { viewModel.updateAutoFeed(it) }
                    )
                }

                // Print actions
                item {
                    PrintActionsCard(
                        isPrinting = isPrinting,
                        onPrintTestText = { viewModel.printTestText() },
                        onPrintTestBarcode = { viewModel.printTestBarcode() },
                        onPrintSampleHtml = { 
                            val html = createTestHtml(printerSettings.labelSize, "Sample Print")
                            viewModel.printHtml(html, context)
                        },
                        onShowPreview = {
                            val html = createTestHtml(printerSettings.labelSize, "Print Preview")
                            viewModel.generatePreview(html, context)
                            onNavigateToPreview(html)
                        },
                        onCalibrate = { viewModel.calibratePrinter() }
                    )
                }

                // Print preview
                if (printPreview != null) {
                    item {
                        PrintPreviewCard(
                            preview = printPreview!!,
                            onClearPreview = { viewModel.clearPreview() }
                        )
                    }
                }

                // Advanced settings
                item {
                    AdvancedSettingsCard(
                        settings = printerSettings,
                        onUploadLogo = { /* TODO: Implement logo upload */ },
                        onTestVendorApp = { /* TODO: Implement vendor app test */ }
                    )
                }
            }
        }
    }
}

@Composable
private fun PrinterStatusCard(printerStatus: PrinterStatus) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Printer Status",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = printerStatus.deviceName ?: "Unknown Printer",
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                    Text(
                        text = printerStatus.deviceAddress ?: "",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (printerStatus.isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                ) {
                    Text(
                        text = if (printerStatus.isConnected) "Connected" else "Disconnected",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
            
            if (printerStatus.isConnected) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total Prints: ${printerStatus.totalPrints}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    printerStatus.lastPrintTime?.let { lastPrint ->
                        Text(
                            text = "Last Print: ${formatTimestamp(lastPrint)}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrinterSettingsCard(
    settings: com.velox.jewelvault.data.bluetooth.PrinterSettings,
    onLabelSizeChange: (com.velox.jewelvault.data.bluetooth.LabelSize) -> Unit,
    onLanguageChange: (PrinterLanguage) -> Unit,
    onDensityChange: (Int) -> Unit,
    onAutoCutChange: (Boolean) -> Unit,
    onAutoFeedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Printer Settings",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Label size selection
            Text(
                text = "Label Size",
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyColumn(
                modifier = Modifier.height(120.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(LabelSizes.ALL_SIZES) { labelSize ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLabelSizeChange(labelSize) }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = labelSize == settings.labelSize,
                            onClick = { onLabelSizeChange(labelSize) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${labelSize.name} (${labelSize.widthMm}x${labelSize.heightMm}mm)",
                            fontSize = 14.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Language selection
            Text(
                text = "Printer Language",
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyColumn(
                modifier = Modifier.height(120.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(PrinterLanguage.values()) { language ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLanguageChange(language) }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = language == settings.language,
                            onClick = { onLanguageChange(language) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = language.displayName,
                            fontSize = 14.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Print density
            Text(
                text = "Print Density: ${settings.density}",
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Slider(
                value = settings.density.toFloat(),
                onValueChange = { onDensityChange(it.toInt()) },
                valueRange = 1f..8f,
                steps = 6
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Auto settings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Auto Cut",
                    fontSize = 14.sp
                )
                Switch(
                    checked = settings.autoCut,
                    onCheckedChange = onAutoCutChange
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Auto Feed",
                    fontSize = 14.sp
                )
                Switch(
                    checked = settings.autoFeed,
                    onCheckedChange = onAutoFeedChange
                )
            }
        }
    }
}

@Composable
private fun PrintActionsCard(
    isPrinting: Boolean,
    onPrintTestText: () -> Unit,
    onPrintTestBarcode: () -> Unit,
    onPrintSampleHtml: () -> Unit,
    onShowPreview: () -> Unit,
    onCalibrate: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Print Actions",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isPrinting) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Printing...",
                        fontSize = 16.sp
                    )
                }
            } else {
                // Print buttons in a grid
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onPrintTestText,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.TextFields, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Test Text")
                        }
                        
                        Button(
                            onClick = onPrintTestBarcode,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.QrCode, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Test Barcode")
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onPrintSampleHtml,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Print HTML")
                        }
                        
                        Button(
                            onClick = onShowPreview,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Preview, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Preview")
                        }
                    }
                    
                    Button(
                        onClick = onCalibrate,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Calibrate Printer")
                    }
                }
            }
        }
    }
}

@Composable
private fun PrintPreviewCard(
    preview: android.graphics.Bitmap,
    onClearPreview: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Print Preview",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                
                IconButton(onClick = onClearPreview) {
                    Icon(Icons.Default.Close, contentDescription = "Clear Preview")
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Preview image
            androidx.compose.foundation.Image(
                bitmap = preview.asImageBitmap(),
                contentDescription = "Print Preview",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color.White)
            )
        }
    }
}

@Composable
private fun AdvancedSettingsCard(
    settings: com.velox.jewelvault.data.bluetooth.PrinterSettings,
    onUploadLogo: () -> Unit,
    onTestVendorApp: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Advanced Settings",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onUploadLogo,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Upload, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Upload Logo")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = onTestVendorApp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Apps, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Test with Vendor App")
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val formatter = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault())
    return formatter.format(java.util.Date(timestamp))
}
