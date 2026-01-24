package com.velox.jewelvault.utils.permissions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun IconPermissionDialog(
    showDialog: Boolean,
    title: String,
    message: String,
    permissionType: PermissionType,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmButtonText: String = "Grant Permission",
    dismissButtonText: String = "Cancel",
    showCredibilityIndicator: Boolean = true
) {
    if (showDialog) {
        val backgroundColor = getPermissionTypeColor(permissionType)
        
        AlertDialog(
            onDismissRequest = onDismiss,
            modifier = Modifier.fillMaxWidth(0.95f),
            shape = RoundedCornerShape(16.dp),
            title = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Icon with background
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(backgroundColor.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = permissionType.icon,
                            contentDescription = null,
                            tint = backgroundColor,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Start
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Permission details card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = permissionType.detailIcon,
                                    contentDescription = null,
                                    tint = backgroundColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "What this permission allows:",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = permissionType.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    if (showCredibilityIndicator) {
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Credibility indicator
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.TwoTone.Verified,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "This permission is required for core app functionality",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = backgroundColor
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.TwoTone.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(confirmButtonText)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(dismissButtonText)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface
        )
    }
}

enum class PermissionType(
    val icon: ImageVector,
    val detailIcon: ImageVector,
    val description: String
) {
    STORAGE(
        icon = Icons.TwoTone.Folder,
        detailIcon = Icons.TwoTone.Storage,
        description = "- Save and read sync files\n- Import data from Excel files\n- Export data to local storage"
    ),
    NOTIFICATION(
        icon = Icons.TwoTone.Notifications,
        detailIcon = Icons.TwoTone.NotificationsActive,
        description = "- Show sync/restore progress\n- Display completion notifications\n- Alert you about important events"
    ),
    CAMERA(
        icon = Icons.TwoTone.CameraAlt,
        detailIcon = Icons.TwoTone.QrCodeScanner,
        description = "- Scan QR codes and barcodes\n- Capture product images\n- Read product information"
    ),
    LOCATION(
        icon = Icons.TwoTone.LocationOn,
        detailIcon = Icons.TwoTone.MyLocation,
        description = "- Find nearby shops\n- Get location-based recommendations\n- Improve service accuracy"
    ),
    CONTACTS(
        icon = Icons.TwoTone.Contacts,
        detailIcon = Icons.TwoTone.People,
        description = "- Import customer contacts\n- Sync with phone contacts\n- Invite friends to the app"
    ),
    MICROPHONE(
        icon = Icons.TwoTone.Mic,
        detailIcon = Icons.TwoTone.RecordVoiceOver,
        description = "- Voice search functionality\n- Audio notes for items\n- Voice commands"
    )
}

@Composable
fun getPermissionTypeColor(permissionType: PermissionType): Color {
    return when (permissionType) {
        PermissionType.STORAGE -> MaterialTheme.colorScheme.primary
        PermissionType.NOTIFICATION -> MaterialTheme.colorScheme.secondary
        PermissionType.CAMERA -> MaterialTheme.colorScheme.tertiary
        PermissionType.LOCATION -> MaterialTheme.colorScheme.error
        PermissionType.CONTACTS -> MaterialTheme.colorScheme.primary
        PermissionType.MICROPHONE -> MaterialTheme.colorScheme.secondary
    }
}

// Legacy function for backward compatibility
@Composable
fun PermissionRequestDialog(
    showDialog: Boolean,
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    isWarning: Boolean = false
) {
    val permissionType = if (isWarning) PermissionType.STORAGE else PermissionType.STORAGE
    
    IconPermissionDialog(
        showDialog = showDialog,
        title = title,
        message = message,
        permissionType = permissionType,
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}

@Preview(showBackground = true)
@Composable
fun IconPermissionDialogPreview() {
    MaterialTheme {
        var showDialog by remember { mutableStateOf(true) }
        
        IconPermissionDialog(
            showDialog = showDialog,
            title = "Storage Permission Required",
            message = "This app needs access to your device's storage to save sync files and import data. This ensures your data can be synced and restored when needed.",
            permissionType = PermissionType.STORAGE,
            onDismiss = { showDialog = false },
            onConfirm = { showDialog = false }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NotificationPermissionDialogPreview() {
    MaterialTheme {
        var showDialog by remember { mutableStateOf(true) }
        
        IconPermissionDialog(
            showDialog = showDialog,
            title = "Notification Permission",
            message = "Enable notifications to stay updated on sync progress, completion status, and important app events. This helps you track the status of your data operations.",
            permissionType = PermissionType.NOTIFICATION,
            onDismiss = { showDialog = false },
            onConfirm = { showDialog = false }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CameraPermissionDialogPreview() {
    MaterialTheme {
        var showDialog by remember { mutableStateOf(true) }
        
        IconPermissionDialog(
            showDialog = showDialog,
            title = "Camera Access Required",
            message = "Camera access is needed to scan QR codes and barcodes for quick product identification and inventory management.",
            permissionType = PermissionType.CAMERA,
            onDismiss = { showDialog = false },
            onConfirm = { showDialog = false }
        )
    }
}

