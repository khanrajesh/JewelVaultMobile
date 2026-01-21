package com.velox.jewelvault.ui.screen.setting

import android.annotation.SuppressLint
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.*
import androidx.compose.material3.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.velox.jewelvault.ui.nav.SubScreens
import com.velox.jewelvault.utils.LocalSubNavController
import com.velox.jewelvault.utils.backup.*
import com.velox.jewelvault.utils.to1FString
import com.velox.jewelvault.ui.components.RestoreSourceDialog
import com.velox.jewelvault.ui.components.PermissionRequester
import com.velox.jewelvault.utils.permissions.getBackupRestorePermissions

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
    val backupPermissions = remember { getBackupRestorePermissions() }
    var showLocalImportDialog by remember { mutableStateOf(false) }
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
        permissions = backupPermissions
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
                                    Icon(Icons.TwoTone.CloudUpload, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Backup Now")
                                }
                                
                                OutlinedButton(
                                    onClick = { viewModel.showRestoreSourceDialog() },
                                    modifier = Modifier.weight(1f),
                                    enabled = !uiState.isLoading
                                ) {
                                    Icon(Icons.TwoTone.CloudDownload, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Restore")
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.startLocalExport() },
                                    modifier = Modifier.weight(1f),
                                    enabled = !uiState.isLoading
                                ) {
                                    Icon(Icons.TwoTone.Download, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Export File")
                                }

                                OutlinedButton(
                                    onClick = { showLocalImportDialog = true },
                                    modifier = Modifier.weight(1f),
                                    enabled = !uiState.isLoading
                                ) {
                                    Icon(Icons.TwoTone.UploadFile, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Import File")
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
                                        if (uiState.lastBackupSuccess) Icons.TwoTone.CheckCircle else Icons.TwoTone.Error,
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
                                    Icons.TwoTone.Info,
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

    if (showLocalImportDialog) {
        LocalImportDialog(
            isLoading = uiState.isLoading,
            defaultBackupFolder = viewModel.getDefaultBackupFolder(),
            validateLocalFile = { fileUri, onResult ->
                viewModel.validateLocalFile(fileUri, onResult)
            },
            onImport = { fileUri, restoreMode ->
                viewModel.startLocalImport(fileUri, restoreMode)
                showLocalImportDialog = false
            },
            onDismiss = { showLocalImportDialog = false }
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
                            Icon(Icons.TwoTone.Close, contentDescription = "Close")
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
                            Icon(Icons.TwoTone.CloudUpload, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Create Backup")
                        }
                        OutlinedButton(
                            onClick = { /* Refresh backup list */ },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.TwoTone.Refresh, contentDescription = null)
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
                                    Icons.TwoTone.CloudOff,
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
                                Icons.TwoTone.Warning,
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
                Icon(Icons.TwoTone.CloudDownload, contentDescription = null)
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
                                Icons.TwoTone.Add,
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
                                Icons.TwoTone.Warning,
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

@Composable
private fun LocalImportDialog(
    isLoading: Boolean,
    defaultBackupFolder: String,
    validateLocalFile: (Uri, (FileValidationResult) -> Unit) -> Unit,
    onImport: (Uri, RestoreMode) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedMode by remember { mutableStateOf(RestoreMode.MERGE) }
    var fileValidationResult by remember { mutableStateOf<FileValidationResult?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedFileUri = uri
            validateLocalFile(uri) { result ->
                fileValidationResult = result
            }
        }
    }

    Dialog(onDismissRequest = { if (!isLoading) onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Import From File",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Default location: $defaultBackupFolder",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        filePickerLauncher.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    Text("Select Excel File (.xlsx)")
                }

                selectedFileUri?.let { uri ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Selected File:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = uri.lastPathSegment ?: "Unknown file",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                fileValidationResult?.let { result ->
                    Spacer(modifier = Modifier.height(8.dp))
                    val isValid = result.isValid
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isValid)
                                MaterialTheme.colorScheme.surfaceVariant
                            else
                                MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(12.dp)
                                .heightIn(max = 180.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = if (isValid) "File validated successfully." else "File validation failed:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = if (isValid)
                                    MaterialTheme.colorScheme.onSurface
                                else
                                    MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = result.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isValid)
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                else
                                    MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Import Mode:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        RadioButton(
                            selected = selectedMode == RestoreMode.MERGE,
                            onClick = { selectedMode = RestoreMode.MERGE }
                        )
                        Text(
                            text = "Merge",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        RadioButton(
                            selected = selectedMode == RestoreMode.REPLACE,
                            onClick = { selectedMode = RestoreMode.REPLACE }
                        )
                        Text(
                            text = "Replace",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val fileUri = selectedFileUri
                            if (fileUri != null) {
                                onImport(fileUri, selectedMode)
                            }
                        },
                        enabled = !isLoading && selectedFileUri != null && fileValidationResult?.isValid == true,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Start Import")
                    }
                }
            }
        }
    }
}
