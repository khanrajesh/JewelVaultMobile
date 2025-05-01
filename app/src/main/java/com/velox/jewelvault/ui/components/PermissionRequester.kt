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
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.velox.jewelvault.utils.permissions.PermissionRequestDialog

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

    if (showInitialPermissionDialog) {
        PermissionRequestDialog(
            showDialog = true,
            title = "Permissions Required",
            message = getMultiplePermissionMessage(permissionQueue),
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
        PermissionRequestDialog(
            showDialog = true,
            title = "Permission Denied",
            message = "Some permissions were permanently denied. Please open settings and grant them manually.",
            onDismiss = { showSettingsDialog = false },
            onConfirm = {
                showSettingsDialog = false
                openAppSettings(context)
            }
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
        if (permission != null && !ActivityCompat.shouldShowRequestPermissionRationale(context as Activity, permission)) {
            onShowSettings()
        } else {
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
