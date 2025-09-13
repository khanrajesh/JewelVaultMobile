package com.velox.jewelvault.ui.screen.setting

import android.Manifest
import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.velox.jewelvault.ui.nav.SubScreens
import com.velox.jewelvault.utils.LocalSubNavController
import com.velox.jewelvault.utils.backup.*
import com.velox.jewelvault.utils.to1FString
import com.velox.jewelvault.ui.components.RestoreSourceDialog
import com.velox.jewelvault.ui.components.PermissionRequester

/**
 * Screen for backup and restore settings
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSettingsScreen(
    viewModel: BackupSettingsViewModel
) {
    val subNavController = LocalSubNavController.current
    val uiState by viewModel.uiState.collectAsState()
    viewModel.currentScreenHeadingState.value= "Backup & Restore"
    BackHandler {
        subNavController.navigate(SubScreens.Setting.route) {
            popUpTo(SubScreens.Setting.route) {
                inclusive = true
            }
        }
    }
    
    // Permission requester for backup/restore operations
    PermissionRequester(
        permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.POST_NOTIFICATIONS
        )
    ) {}
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(topStart = 18.dp))
            .padding(5.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 0.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Quick Actions Section
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Quick Actions",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.startBackup() },
                                    modifier = Modifier.weight(1f),
                                    enabled = !uiState.isLoading
                                ) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Backup Now")
                                }
                                
                                OutlinedButton(
                                    onClick = { viewModel.showRestoreSourceDialog() },
                                    modifier = Modifier.weight(1f),
                                    enabled = !uiState.isLoading
                                ) {
                                    Icon(Icons.Default.CloudDownload, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Restore")
                                }
                            }
                        }
                    }
                }
                
                // Automatic Backup Section
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Automatic Backup",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "Schedule automatic backups to keep your data safe",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            var expanded by remember { mutableStateOf(false) }
                            
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = !expanded }
                            ) {
                                OutlinedTextField(
                                    value = uiState.backupFrequency.name.lowercase().replaceFirstChar { it.uppercase() },
                                    onValueChange = { },
                                    readOnly = true,
                                    label = { Text("Backup Frequency") },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
                                )
                                
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    BackupFrequency.entries.forEach { frequency ->
                                        DropdownMenuItem(
                                            text = { 
                                                Text(frequency.name.lowercase().replaceFirstChar { it.uppercase() })
                                            },
                                            onClick = {
                                                viewModel.setBackupFrequency(frequency)
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Backup Status Section
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Backup Status",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            if (uiState.isLoading) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = uiState.progressMessage,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "${uiState.progressPercent}%",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        if (uiState.lastBackupSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                        contentDescription = null,
                                        tint = if (uiState.lastBackupSuccess) 
                                            MaterialTheme.colorScheme.primary 
                                        else 
                                            MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = if (uiState.lastBackupDate.isNotEmpty()) 
                                                "Last backup: ${uiState.lastBackupDate}"
                                            else 
                                                "No backup yet",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        if (uiState.statusMessage.isNotEmpty()) {
                                            Text(
                                                text = uiState.statusMessage,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Information Section
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "About Backup",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "• Backup includes all your data: items, customers, orders, transactions\n" +
                                        "• Data is stored securely in cloud storage\n" +
                                        "• Only you can access your backup data\n" +
                                        "• Smart restore with merge/replace options\n" +
                                        "• Merge mode: Add new data safely\n" +
                                        "• Replace mode: Complete data replacement\n" +
                                        "• Keep your app updated for best compatibility",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Show backup dialog
    if (uiState.showBackupDialog) {
        EnhancedBackupDialog(
            onDismiss = { viewModel.hideBackupDialog() },
            onBackupRequested = { viewModel.startBackup() },
            onRestoreRequested = { fileName, restoreMode -> viewModel.startRestore(fileName, restoreMode) },
            availableBackups = uiState.availableBackups,
            isLoading = uiState.isLoading,
            progressMessage = uiState.progressMessage,
            progressPercent = uiState.progressPercent
        )
    }
    
    // Show restore source dialog
    if (uiState.showRestoreSourceDialog) {
        RestoreSourceDialog(
            onDismiss = { viewModel.hideRestoreSourceDialog() },
            onRestore = { restoreSource, localFileUri, restoreMode ->
                viewModel.startRestoreWithSource(restoreSource, localFileUri, restoreMode)
            },
            checkFirebaseBackup = { restoreSource, onResult ->
                viewModel.checkFirebaseBackupExists(onResult)
            },
            validateLocalFile = { fileUri, onResult ->
                viewModel.validateLocalFile(fileUri, onResult)
            },
            defaultBackupFolder = viewModel.getDefaultBackupFolder()
        )
    }
    
    // Show result snackbar
    if (uiState.showResultMessage) {
        LaunchedEffect(uiState.statusMessage) {
            // You can show a snackbar here if needed
            viewModel.clearResultMessage()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnhancedBackupDialog(
    onDismiss: () -> Unit,
    onBackupRequested: () -> Unit,
    onRestoreRequested: (String, RestoreMode) -> Unit,
    availableBackups: List<BackupInfo>,
    isLoading: Boolean,
    progressMessage: String,
    progressPercent: Int
) {
    val dateFormat = remember { java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault()) }
    androidx.compose.ui.window.Dialog(onDismissRequest = { if (!isLoading) onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Backup & Restore",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (!isLoading) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }
                if (isLoading) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            strokeWidth = 4.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = progressMessage,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                        progress = { progressPercent / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        color = ProgressIndicatorDefaults.linearColor,
                        trackColor = ProgressIndicatorDefaults.linearTrackColor,
                        strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                        )
                        Text(
                            text = "$progressPercent%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onBackupRequested,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Create Backup")
                        }
                        OutlinedButton(
                            onClick = { /* Refresh backup list */ },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Refresh")
                        }
                    }
                    HorizontalDivider()
                    Text(
                        text = "Available Backups",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (availableBackups.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.CloudOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No backups found",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Create your first backup to get started",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(availableBackups) { backup ->
                                BackupItem(
                                    backup = backup,
                                    dateFormat = dateFormat,
                                    onRestoreClick = { restoreMode -> onRestoreRequested(backup.fileName, restoreMode) }
                                )
                            }
                        }
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Choose restore mode carefully. Merge mode is recommended for admin users with existing data.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BackupItem(
    backup: BackupInfo,
    dateFormat: java.text.SimpleDateFormat,
    onRestoreClick: (RestoreMode) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = dateFormat.format(backup.uploadDate),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Size: ${formatFileSize(backup.fileSize)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (backup.description.isNotEmpty()) {
                    Text(
                        text = backup.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            var showRestoreOptions by remember { mutableStateOf(false) }
            
            Button(
                onClick = { showRestoreOptions = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(Icons.Default.CloudDownload, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Restore")
            }
            
            if (showRestoreOptions) {
                RestoreModeDialog(
                    onDismiss = { showRestoreOptions = false },
                    onRestoreSelected = { restoreMode ->
                        onRestoreClick(restoreMode)
                        showRestoreOptions = false
                    }
                )
            }
        }
    }
}

@Composable
private fun RestoreModeDialog(
    onDismiss: () -> Unit,
    onRestoreSelected: (RestoreMode) -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Choose Restore Mode",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "Select how you want to handle existing data during restore:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Merge Mode Option
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Merge (Recommended)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• Add new data from backup\n" +
                                    "• Keep existing data unchanged\n" +
                                    "• Safe for admin users with existing data\n" +
                                    "• No data loss",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { onRestoreSelected(RestoreMode.MERGE) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Use Merge Mode")
                        }
                    }
                }
                
                // Replace Mode Option
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Replace (Advanced)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• Replace all data with backup\n" +
                                    "• Current data will be overwritten\n" +
                                    "• Only current user/store preserved\n" +
                                    "• Use with caution",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { onRestoreSelected(RestoreMode.REPLACE) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Use Replace Mode")
                        }
                    }
                }
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

@SuppressLint("DefaultLocale")
private fun formatFileSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return when {
        mb >= 1 -> "${mb.to1FString()} MB"
        kb >= 1 -> "${kb.to1FString()} KB"
        else -> "$bytes B"
    }
}