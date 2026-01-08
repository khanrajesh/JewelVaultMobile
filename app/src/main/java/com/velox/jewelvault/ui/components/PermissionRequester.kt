package com.velox.jewelvault.ui.components

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Environment
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.velox.jewelvault.utils.permissions.IconPermissionDialog
import com.velox.jewelvault.utils.permissions.filterUngrantedPermissions
import com.velox.jewelvault.utils.permissions.getMultiplePermissionMessage
import com.velox.jewelvault.utils.permissions.getPermissionTypeForQueue
import com.velox.jewelvault.utils.permissions.handlePermissionResult
import com.velox.jewelvault.utils.permissions.openAppSettings
import com.velox.jewelvault.utils.permissions.requestNextPermission

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
                    context = context,
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
