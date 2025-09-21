package com.velox.jewelvault.ui.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.velox.jewelvault.utils.permissions.IconPermissionDialog
import com.velox.jewelvault.utils.permissions.PermissionType
import com.velox.jewelvault.utils.permissions.getBackupRestorePermissions

@Composable
fun PermissionRequester(
    permissions: List<String>,
    onAllPermissionsGranted: () -> Unit
) {
    val context = LocalContext.current
    var permissionQueue by remember { mutableStateOf<List<String>>(emptyList()) }
    var currentPermissionIndex by remember { mutableStateOf(0) }
    var showInitialPermissionDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var hasReturnedFromSettings by remember { mutableStateOf(false) }

    val contextState = rememberUpdatedState(context)
    val permissionQueueState = rememberUpdatedState(permissionQueue)
    val currentPermissionIndexState = rememberUpdatedState(currentPermissionIndex)
    val onAllPermissionsGrantedState = rememberUpdatedState(onAllPermissionsGranted)
    val launchers = remember {
        mutableStateOf<Pair<
                ManagedActivityResultLauncher<String, Boolean>?,
                ManagedActivityResultLauncher<Intent, ActivityResult>?
                >>(null to null)
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        handlePermissionResult(
            context = contextState.value,
            permissionQueue = permissionQueueState.value,
            currentPermissionIndex = currentPermissionIndexState.value,
            isGranted = isGranted,
            onAllPermissionsGranted = onAllPermissionsGrantedState.value,
            requestPermissionLauncher = launchers.value.first!!,
            requestManageStorageLauncher = launchers.value.second!!,
            onShowSettings = { showSettingsDialog = true }
        )
    }

    val requestManageStorageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        handlePermissionResult(
            context = contextState.value,
            permissionQueue = permissionQueueState.value,
            currentPermissionIndex = currentPermissionIndexState.value,
            isGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Environment.isExternalStorageManager() else it.resultCode == Activity.RESULT_OK,
            onAllPermissionsGranted = onAllPermissionsGrantedState.value,
            requestPermissionLauncher = launchers.value.first!!,
            requestManageStorageLauncher = launchers.value.second!!,
            onShowSettings = { showSettingsDialog = true }
        )
    }

// Save both launchers together safely
    LaunchedEffect(Unit) {
        launchers.value = requestPermissionLauncher to requestManageStorageLauncher
    }


    LaunchedEffect(Unit) {
        permissionQueue = filterUngrantedPermissions(context, permissions)
        if (permissionQueue.isNotEmpty()) {
            currentPermissionIndex = 0
            showInitialPermissionDialog = true
        } else {
            onAllPermissionsGranted()
        }
    }
    
    // Re-check permissions when returning from settings
    LaunchedEffect(hasReturnedFromSettings) {
        if (hasReturnedFromSettings) {
            val remainingPermissions = filterUngrantedPermissions(context, permissions)
            if (remainingPermissions.isEmpty()) {
                onAllPermissionsGranted()
            } else {
                // Still have permissions to request
                permissionQueue = remainingPermissions
                currentPermissionIndex = 0
                showInitialPermissionDialog = true
            }
            hasReturnedFromSettings = false
        }
    }

    if (showInitialPermissionDialog) {
        IconPermissionDialog(
            showDialog = true,
            title = "Permissions Required",
            message = getMultiplePermissionMessage(permissionQueue),
            permissionType = getPermissionTypeForQueue(permissionQueue),
            onDismiss = { showInitialPermissionDialog = false },
            onConfirm = {
                showInitialPermissionDialog = false
                requestNextPermission(
                    permissionQueue = permissionQueue,
                    currentPermissionIndex = currentPermissionIndex,
                    requestPermissionLauncher = requestPermissionLauncher,
                    requestManageStorageLauncher = requestManageStorageLauncher
                )
            }
        )
    }

    if (showSettingsDialog) {
        IconPermissionDialog(
            showDialog = true,
            title = "Permission Required",
            message = "Some permissions are required for backup and restore functionality. They were denied and can only be granted through device settings. Please open settings and enable the required permissions.",
            permissionType = getPermissionTypeForQueue(permissionQueue),
            onDismiss = { 
                showSettingsDialog = false
                // Check permissions again after dismissing
                val remainingPermissions = filterUngrantedPermissions(context, permissions)
                if (remainingPermissions.isEmpty()) {
                    onAllPermissionsGranted()
                }
            },
            onConfirm = {
                showSettingsDialog = false
                hasReturnedFromSettings = true
                openAppSettings(context)
            },
            confirmButtonText = "Open Settings",
            dismissButtonText = "Skip for Now",
            showCredibilityIndicator = true
        )
    }
}

private fun handlePermissionResult(
    context: Context,
    permissionQueue: List<String>,
    currentPermissionIndex: Int,
    isGranted: Boolean,
    onAllPermissionsGranted: () -> Unit,
    requestPermissionLauncher: ManagedActivityResultLauncher<String, Boolean>,
    requestManageStorageLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>,
    onShowSettings: () -> Unit
) {
    val permission = permissionQueue.getOrNull(currentPermissionIndex)
    if (isGranted) {
        moveToNextPermission(
            context = context,
            permissionQueue = permissionQueue,
            currentPermissionIndex = currentPermissionIndex,
            onAllPermissionsGranted = onAllPermissionsGranted,
            requestPermissionLauncher = requestPermissionLauncher,
            requestManageStorageLauncher = requestManageStorageLauncher
        )
    } else {
        // Permission was denied
        if (permission != null) {
            // Check if permission is permanently denied (user selected "Don't ask again")
            val isPermanentlyDenied = !ActivityCompat.shouldShowRequestPermissionRationale(context as Activity, permission)
            
            if (isPermanentlyDenied) {
                // Show settings dialog for permanently denied permissions
                onShowSettings()
            } else {
                // Permission denied but not permanently, move to next permission
                moveToNextPermission(
                    context = context,
                    permissionQueue = permissionQueue,
                    currentPermissionIndex = currentPermissionIndex,
                    onAllPermissionsGranted = onAllPermissionsGranted,
                    requestPermissionLauncher = requestPermissionLauncher,
                    requestManageStorageLauncher = requestManageStorageLauncher
                )
            }
        } else {
            // No permission to check, move to next
            moveToNextPermission(
                context = context,
                permissionQueue = permissionQueue,
                currentPermissionIndex = currentPermissionIndex,
                onAllPermissionsGranted = onAllPermissionsGranted,
                requestPermissionLauncher = requestPermissionLauncher,
                requestManageStorageLauncher = requestManageStorageLauncher
            )
        }
    }
}

private fun moveToNextPermission(
    context: Context,
    permissionQueue: List<String>,
    currentPermissionIndex: Int,
    onAllPermissionsGranted: () -> Unit,
    requestPermissionLauncher: ManagedActivityResultLauncher<String, Boolean>,
    requestManageStorageLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>
) {
    if (currentPermissionIndex + 1 < permissionQueue.size) {
        val nextIndex = currentPermissionIndex + 1
        requestNextPermission(
            permissionQueue = permissionQueue,
            currentPermissionIndex = nextIndex,
            requestPermissionLauncher = requestPermissionLauncher,
            requestManageStorageLauncher = requestManageStorageLauncher
        )
    } else {
        onAllPermissionsGranted()
    }
}

private fun requestNextPermission(
    permissionQueue: List<String>,
    currentPermissionIndex: Int,
    requestPermissionLauncher: ManagedActivityResultLauncher<String, Boolean>,
    requestManageStorageLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>
) {
    val permission = permissionQueue.getOrNull(currentPermissionIndex)
    permission?.let {
        if (it == Manifest.permission.MANAGE_EXTERNAL_STORAGE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${permissionQueue[0]}")
                }
                requestManageStorageLauncher.launch(intent)
            }
        } else {
            requestPermissionLauncher.launch(it)
        }
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
    }
    context.startActivity(intent)
}

private fun getMultiplePermissionMessage(permissions: List<String>): String {
    if (permissions.isEmpty()) return "We need some permissions to continue."

    val messages = permissions.mapNotNull { permission ->
        when (permission) {
            Manifest.permission.CAMERA -> "• Camera access to scan QR/barcodes."
            Manifest.permission.WRITE_EXTERNAL_STORAGE -> "• Storage access to save gold price data."
            Manifest.permission.MANAGE_EXTERNAL_STORAGE -> "• Manage storage access for smooth file operations."
            Manifest.permission.READ_CONTACTS -> "• Contacts access to invite friends."
            Manifest.permission.ACCESS_FINE_LOCATION -> "• Location access for nearby shop recommendations."
            Manifest.permission.RECORD_AUDIO -> "• Microphone access for voice search."
            Manifest.permission.POST_NOTIFICATIONS -> "• Notification access to show backup/restore progress."
            Manifest.permission.BLUETOOTH -> "• Bluetooth access to connect to printers."
            Manifest.permission.BLUETOOTH_ADMIN -> "• Bluetooth admin access to manage printer connections."
            Manifest.permission.BLUETOOTH_CONNECT -> "• Bluetooth connect access to pair with printers."
            Manifest.permission.BLUETOOTH_SCAN -> "• Bluetooth scan access to discover nearby printers."
            Manifest.permission.BLUETOOTH_ADVERTISE -> "• Bluetooth advertise access for printer discovery."
            else -> null
        }
    }

    return buildString {
        appendLine("The app needs the following permissions:")
        messages.forEach { appendLine(it) }
    }
}

private fun filterUngrantedPermissions(context: Context, permissions: List<String>): List<String> {
    return permissions.filter { permission ->
        when (permission) {
            Manifest.permission.MANAGE_EXTERNAL_STORAGE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    !Environment.isExternalStorageManager()
                } else false
            }
            else -> {
                ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
            }
        }
    }
}

private fun getPermissionTypeForQueue(permissions: List<String>): PermissionType {
    return when {
        permissions.any { it == Manifest.permission.POST_NOTIFICATIONS } -> PermissionType.NOTIFICATION
        permissions.any { it == Manifest.permission.CAMERA } -> PermissionType.CAMERA
        permissions.any { it == Manifest.permission.ACCESS_FINE_LOCATION } -> PermissionType.LOCATION
        permissions.any { it == Manifest.permission.READ_CONTACTS } -> PermissionType.CONTACTS
        permissions.any { it == Manifest.permission.RECORD_AUDIO } -> PermissionType.MICROPHONE
        permissions.any { it in listOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE
        ) } -> PermissionType.BLUETOOTH
        permissions.any { it in listOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.MANAGE_EXTERNAL_STORAGE) } -> PermissionType.STORAGE
        else -> PermissionType.STORAGE
    }
}


