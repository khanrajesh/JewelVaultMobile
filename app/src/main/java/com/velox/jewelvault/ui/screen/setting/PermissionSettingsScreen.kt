package com.velox.jewelvault.ui.screen.setting

import android.app.Activity
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Bluetooth
import androidx.compose.material.icons.twotone.CameraAlt
import androidx.compose.material.icons.twotone.Folder
import androidx.compose.material.icons.twotone.LocationOn
import androidx.compose.material.icons.twotone.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.velox.jewelvault.ui.nav.SubScreens
import com.velox.jewelvault.utils.LocalBaseViewModel
import com.velox.jewelvault.utils.LocalSubNavController
import com.velox.jewelvault.utils.permissions.PermissionItem
import com.velox.jewelvault.utils.permissions.buildPermissionItems
import com.velox.jewelvault.utils.permissions.isPermissionGranted
import com.velox.jewelvault.utils.permissions.isPermissionSupported
import com.velox.jewelvault.utils.permissions.launchManageStorageSettings
import com.velox.jewelvault.utils.permissions.openAppSettings
import com.velox.jewelvault.utils.permissions.refreshAllPermissions
import com.velox.jewelvault.utils.permissions.requestPermissionForItem
import com.velox.jewelvault.utils.permissions.storageState
import com.velox.jewelvault.ui.components.baseBackground0

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionSettingsScreen() {
    val subNavController = LocalSubNavController.current
    val baseViewModel = LocalBaseViewModel.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    baseViewModel.currentScreenHeading = "Permissions"

    val permissionItems = remember { buildPermissionItems() }
    val permissionStates = remember { mutableStateMapOf<String, Boolean>() }
    var pendingPermission by remember { mutableStateOf<PermissionItem?>(null) }
    var disableTarget by remember { mutableStateOf<PermissionItem?>(null) }

    val requestPermissionsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            pendingPermission?.let { item ->
                val granted = if (item.id == "storage") storageState(context).anyGranted else isPermissionGranted(context, item)
                permissionStates[item.id] = granted
                if (!granted) {
                    val activity = context as? Activity
                    val deniedPermissions = result.filterValues { !it }.keys.ifEmpty { item.permissions }
                    val permanentlyDenied = activity != null && deniedPermissions.any { perm ->
                        !ActivityCompat.shouldShowRequestPermissionRationale(activity, perm)
                    }
                    if (item.id == "manage_storage" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        launchManageStorageSettings(context)
                    } else if (permanentlyDenied) {
                        openAppSettings(context)
                    }
                }
            } ?: refreshAllPermissions(context, permissionItems, permissionStates)
            pendingPermission = null
        }

    BackHandler {
        subNavController.navigate(SubScreens.Setting.route) {
            popUpTo(SubScreens.Setting.route) {
                inclusive = true
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshAllPermissions(context, permissionItems, permissionStates)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshAllPermissions(context, permissionItems, permissionStates)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .baseBackground0()
            .padding(5.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Manage Permissions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Enable or disable the app permissions below. Turning a switch on requests the permission, while turning it off takes you to system settings to revoke it.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(permissionItems.size) { index ->
                    val item = permissionItems[index]
                    val storageState = if (item.id == "storage") storageState(context) else null
                    val isGranted = permissionStates[item.id] ?: false
                    val isSupported = isPermissionSupported(item)
                    val statusText = when {
                        !isSupported -> "Not required on this device"
                        item.id == "storage" && storageState != null -> when {
                            !storageState.anyGranted -> "Disabled"
                            storageState.fullGranted -> "Enabled"
                            else -> "Partial access"
                        }
                        isGranted -> "Enabled"
                        else -> "Disabled"
                    }
                    val statusColor = when {
                        !isSupported -> MaterialTheme.colorScheme.onSurfaceVariant
                        isGranted -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    PermissionToggleCard(
                        permissionItem = item,
                        checked = isGranted,
                        enabled = isSupported,
                        statusText = statusText,
                        statusColor = statusColor,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                pendingPermission = item
                                val launched = requestPermissionForItem(
                                    context = context,
                                    item = item,
                                    requestPermissionsLauncher = requestPermissionsLauncher
                                )
                                if (item.id == "manage_storage") {
                                    // We launched a settings page, clear pending and refresh on resume
                                    pendingPermission = null
                                    permissionStates[item.id] = isPermissionGranted(context, item)
                                }
                                if (!launched) {
                                    pendingPermission = null
                                    val grantedAfterRequest = isPermissionGranted(context, item)
                                    permissionStates[item.id] = grantedAfterRequest
                                    if (!grantedAfterRequest) {
                                        openAppSettings(context)
                                    }
                                }
                            } else {
                                disableTarget = item
                            }
                        }
                    )
                }
                item { Spacer(modifier = Modifier.height(12.dp)) }
            }
        }
    }

    disableTarget?.let { item ->
        AlertDialog(
            onDismissRequest = { disableTarget = null },
            title = { Text("Disable ${item.title}") },
            text = {
                Text(
                    text = "Permissions are controlled by the system. To disable ${item.title.lowercase()}, open app settings and turn it off for JewelVault.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
            onClick = {
                disableTarget = null
                if (item.id == "manage_storage" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    launchManageStorageSettings(context)
                } else {
                    openAppSettings(context)
                }
            }
        ) {
            Text("Open Settings")
        }
            },
            dismissButton = {
                TextButton(onClick = { disableTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PermissionToggleCard(
    permissionItem: PermissionItem,
    checked: Boolean,
    enabled: Boolean,
    statusText: String,
    statusColor: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    val accentColor = MaterialTheme.colorScheme.primary
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = permissionItem.icon,
                        contentDescription = null,
                        tint = accentColor
                    )
                }
                Spacer(modifier = Modifier.size(8.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = permissionItem.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = permissionItem.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val statusText = when {
                        !enabled -> "Not required on this device"
                        checked -> "Enabled"
                        else -> "Disabled"
                    }
                    val statusColor = when {
                        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant
                        checked -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor
                    )
                }
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    enabled = enabled
                )
            }
            HorizontalDivider()
            Text(
                text = "Turn on to request this permission. Turn off to open system settings and revoke it if already granted.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
