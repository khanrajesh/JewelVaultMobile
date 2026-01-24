package com.velox.jewelvault.ui.components

import android.Manifest
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
import androidx.compose.runtime.saveable.rememberSaveable
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
    onAllPermissionsGranted: () -> Unit,
    autoRequest: Boolean = true,
    requestKey: Int = 0,
    showDialogs: Boolean = true,
    onPermissionsChanged: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var permissionQueue by remember { mutableStateOf<List<String>>(emptyList()) }
    var currentPermissionIndex by remember { mutableStateOf(0) }
    var showInitialPermissionDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var hasReturnedFromSettings by remember { mutableStateOf(false) }
    var manageStorageRequested by rememberSaveable { mutableStateOf(false) }

    val contextState = rememberUpdatedState(context)
    val permissionQueueState = rememberUpdatedState(permissionQueue)
    val currentPermissionIndexState = rememberUpdatedState(currentPermissionIndex)
    val onAllPermissionsGrantedState = rememberUpdatedState(onAllPermissionsGranted)
    val onPermissionsChangedState = rememberUpdatedState(onPermissionsChanged)
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
        onPermissionsChangedState.value?.invoke()
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
        onPermissionsChangedState.value?.invoke()
    }

    // Save both launchers together safely
    LaunchedEffect(Unit) {
        launchers.value = requestPermissionLauncher to requestManageStorageLauncher
    }

    fun startRequestFlow() {
        val queue = filterUngrantedPermissions(context, permissions).let { ungranted ->
            if (manageStorageRequested) {
                ungranted.filterNot { it == Manifest.permission.MANAGE_EXTERNAL_STORAGE }
            } else {
                ungranted
            }
        }
        permissionQueue = queue
        if (permissionQueue.isNotEmpty()) {
            currentPermissionIndex = 0
            if (showDialogs) {
                showInitialPermissionDialog = true
            } else {
                requestNextPermission(
                    context = context,
                    permissionQueue = permissionQueue,
                    currentPermissionIndex = currentPermissionIndex,
                    requestPermissionLauncher = requestPermissionLauncher,
                    requestManageStorageLauncher = requestManageStorageLauncher
                )
            }
        } else {
            onAllPermissionsGranted()
        }
        onPermissionsChangedState.value?.invoke()
    }

    LaunchedEffect(Unit) {
        if (autoRequest) {
            startRequestFlow()
        }
    }

    LaunchedEffect(requestKey) {
        if (!autoRequest && requestKey > 0) {
            startRequestFlow()
        }
    }

    // Re-check permissions when returning from settings
    LaunchedEffect(hasReturnedFromSettings) {
        if (hasReturnedFromSettings) {
            val remainingPermissions = filterUngrantedPermissions(context, permissions).let { ungranted ->
                if (manageStorageRequested) {
                    ungranted.filterNot { it == Manifest.permission.MANAGE_EXTERNAL_STORAGE }
                } else {
                    ungranted
                }
            }
            if (remainingPermissions.isEmpty()) {
                onAllPermissionsGranted()
            } else {
                permissionQueue = remainingPermissions
                currentPermissionIndex = 0
                showInitialPermissionDialog = true
            }
            hasReturnedFromSettings = false
            onPermissionsChangedState.value?.invoke()
        }
    }

    if (showDialogs && showInitialPermissionDialog) {
        IconPermissionDialog(
            showDialog = true,
            title = "Permissions Required",
            message = getMultiplePermissionMessage(permissionQueue),
            permissionType = getPermissionTypeForQueue(permissionQueue),
            onDismiss = { showInitialPermissionDialog = false },
            onConfirm = {
                showInitialPermissionDialog = false
                if (permissionQueue.getOrNull(currentPermissionIndex) == Manifest.permission.MANAGE_EXTERNAL_STORAGE) {
                    manageStorageRequested = true
                }
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

    if (showDialogs && showSettingsDialog) {
        IconPermissionDialog(
            showDialog = true,
            title = "Permission Required",
            message = "Some permissions are required for sync and restore functionality. They were denied and can only be granted through device settings. Please open settings and enable the required permissions.",
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
