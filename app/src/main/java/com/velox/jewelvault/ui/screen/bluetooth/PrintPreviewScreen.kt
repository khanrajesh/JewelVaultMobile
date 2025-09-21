package com.velox.jewelvault.ui.screen.bluetooth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.velox.jewelvault.data.bluetooth.LabelSizes
import com.velox.jewelvault.utils.LocalBaseViewModel
import com.velox.jewelvault.utils.LocalSubNavController
import com.velox.jewelvault.ui.nav.SubScreens
import com.velox.jewelvault.utils.createTestHtml
import com.velox.jewelvault.utils.renderHtmlToBitmap

/**
 * Screen for displaying print preview
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintPreviewScreen(
    htmlContent: String,
    onNavigateBack: () -> Unit,
    onPrint: () -> Unit,
    viewModel: PrinterViewModel = hiltViewModel()
) {
    val printerSettings by viewModel.printerSettings.collectAsStateWithLifecycle()
    val printPreview by viewModel.printPreview.collectAsStateWithLifecycle()
    val isGeneratingPreview by viewModel.isGeneratingPreview.collectAsStateWithLifecycle()
    val isPrinting by viewModel.isPrinting.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val baseViewModel = LocalBaseViewModel.current
    val subNavController = LocalSubNavController.current
    val context = LocalContext.current

    // Set screen heading
    LaunchedEffect(Unit) {
        baseViewModel.currentScreenHeading = "Print Preview"
    }

    // Handle errors with snackbar
    LaunchedEffect(errorMessage) {
        errorMessage?.let { error ->
            baseViewModel.snackBarState = error
            viewModel.clearError()
        }
    }

    // Generate preview when screen loads
    LaunchedEffect(htmlContent) {
        viewModel.generatePreview(htmlContent, context)
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
                title = { Text("Print Preview") },
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
                    if (isPrinting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Button(
                            onClick = onPrint,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Print,
                                contentDescription = "Print",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Print", fontSize = 12.sp)
                        }
                    }
                }
            )

            // Preview content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                if (isGeneratingPreview) {
                    // Loading state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                strokeWidth = 4.dp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Generating Preview...",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (printPreview != null) {
                    // Preview content
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Label size info
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Label Size: ${printerSettings.labelSize.name} (${printerSettings.labelSize.widthMm}x${printerSettings.labelSize.heightMm}mm)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Preview container
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                // Label outline
                                Box(
                                    modifier = Modifier
                                        .size(
                                            width = (printerSettings.labelSize.widthMm * 2).dp, // Scale for visibility
                                            height = (printerSettings.labelSize.heightMm * 2).dp
                                        )
                                        .border(
                                            width = 2.dp,
                                            color = MaterialTheme.colorScheme.outline,
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .clip(RoundedCornerShape(4.dp))
                                ) {
                                    // Preview image
                                    androidx.compose.foundation.Image(
                                        bitmap = printPreview!!.asImageBitmap(),
                                        contentDescription = "Print Preview",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.White)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onNavigateBack,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Back")
                            }

                            Button(
                                onClick = onPrint,
                                modifier = Modifier.weight(1f),
                                enabled = !isPrinting
                            ) {
                                if (isPrinting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Icon(Icons.Default.Print, contentDescription = null)
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isPrinting) "Printing..." else "Print")
                            }
                        }
                    }
                } else {
                    // No preview available
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.ImageNotSupported,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No Preview Available",
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Unable to generate preview for this content",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Preview screen for jewelry label
     */
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun JewelryLabelPreviewScreen(
        itemId: String,
        itemName: String,
        grossWeight: String,
        fineWeight: String,
        purity: String,
        storeName: String,
        onNavigateBack: () -> Unit,
        onPrint: () -> Unit,
        viewModel: PrinterViewModel = hiltViewModel()
    ) {
        val printerSettings by viewModel.printerSettings.collectAsStateWithLifecycle()
        val printPreview by viewModel.printPreview.collectAsStateWithLifecycle()
        val isGeneratingPreview by viewModel.isGeneratingPreview.collectAsStateWithLifecycle()
        val isPrinting by viewModel.isPrinting.collectAsStateWithLifecycle()
        val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
        val baseViewModel = LocalBaseViewModel.current
        val subNavController = LocalSubNavController.current
        val context = LocalContext.current

        // Set screen heading
        LaunchedEffect(Unit) {
            baseViewModel.currentScreenHeading = "Jewelry Label Preview"
        }

        // Handle errors with snackbar
        LaunchedEffect(errorMessage) {
            errorMessage?.let { error ->
                baseViewModel.snackBarState = error
                viewModel.clearError()
            }
        }

        // Generate preview when screen loads
        LaunchedEffect(itemId, itemName, grossWeight, fineWeight, purity, storeName) {
            val html = com.velox.jewelvault.utils.createJewelryLabelHtml(
                labelSize = printerSettings.labelSize,
                itemId = itemId,
                itemName = itemName,
                grossWeight = grossWeight,
                fineWeight = fineWeight,
                purity = purity,
                storeName = storeName
            )
            viewModel.generatePreview(html, context)
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
                    title = { Text("Jewelry Label Preview") },
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
                        if (isPrinting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Button(
                                onClick = onPrint,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Print,
                                    contentDescription = "Print",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Print", fontSize = 12.sp)
                            }
                        }
                    }
                )

                // Preview content
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
                {
                    if (isGeneratingPreview) {
                        // Loading state
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(48.dp),
                                    strokeWidth = 4.dp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Generating Label Preview...",
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else if (printPreview != null) {
                        // Preview content
                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Item info
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = "Item Information",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("ID: $itemId", fontSize = 14.sp)
                                    Text("Name: $itemName", fontSize = 14.sp)
                                    Text("Gross Weight: $grossWeight gm", fontSize = 14.sp)
                                    Text("Fine Weight: $fineWeight gm", fontSize = 14.sp)
                                    Text("Purity: $purity", fontSize = 14.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Preview container
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Label outline
                                    Box(
                                        modifier = Modifier
                                            .size(
                                                width = (printerSettings.labelSize.widthMm * 2).dp, // Scale for visibility
                                                height = (printerSettings.labelSize.heightMm * 2).dp
                                            )
                                            .border(
                                                width = 2.dp,
                                                color = MaterialTheme.colorScheme.outline,
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .clip(RoundedCornerShape(4.dp))
                                    ) {
                                        // Preview image
                                        androidx.compose.foundation.Image(
                                            bitmap = printPreview!!.asImageBitmap(),
                                            contentDescription = "Jewelry Label Preview",
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.White)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Action buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onNavigateBack,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Back")
                                }

                                Button(
                                    onClick = onPrint,
                                    modifier = Modifier.weight(1f),
                                    enabled = !isPrinting
                                ) {
                                    if (isPrinting) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    } else {
                                        Icon(Icons.Default.Print, contentDescription = null)
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isPrinting) "Printing..." else "Print Label")
                                }
                            }
                        }
                    } else {
                        // No preview available
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.ImageNotSupported,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No Preview Available",
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Unable to generate preview for this label",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

