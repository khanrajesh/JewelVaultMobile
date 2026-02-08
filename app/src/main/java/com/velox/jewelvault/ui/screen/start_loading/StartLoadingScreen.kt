package com.velox.jewelvault.ui.screen.start_loading

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Cancel
import androidx.compose.material.icons.twotone.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.velox.jewelvault.R
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.velox.jewelvault.ui.nav.Screens
import com.velox.jewelvault.ui.nav.SubScreens
import com.velox.jewelvault.utils.LocalBaseViewModel
import com.velox.jewelvault.utils.LocalNavController
import com.velox.jewelvault.utils.sync.SyncService
import kotlinx.coroutines.delay

@Composable
fun StartLoadingScreen() {
    val navHost = LocalNavController.current
    val baseViewModel = LocalBaseViewModel.current
    val viewModel = hiltViewModel<StartLoadingViewModel>()

    val hasNavigated = remember { mutableStateOf(false) }
    val minDelayDone = remember { mutableStateOf(false) }
    val minDelayStarted = remember { mutableStateOf(false) }
    val needsStoreSetup = viewModel.needsStoreSetup.value
    val overallProgress = viewModel.progress.value
    val progressMessage = viewModel.progressMessage.value
    val showBackupDialog = viewModel.showBackupDialog.value
    val backupDecisionPending = viewModel.backupDecisionPending.value
    val backupRestoreInProgress = viewModel.backupRestoreInProgress.value
    val permissionsGranted = viewModel.permissionsGranted.value
    val monitorStarted = remember { mutableStateOf(false) }

    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.loader_cat))
    val lottieProgress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    val context = LocalContext.current
    val permissionItems = remember { buildPermissionInfoList() }
    val permissionStatuses = remember { mutableStateMapOf<String, Boolean>() }
    val permissionRefreshKey = remember { mutableStateOf(0) }
    val showPermissionDialog = remember { mutableStateOf(false) }
    val permissionRequestInProgress = remember { mutableStateOf(false) }

    val permissionList = remember { permissionItems.map { it.permission }.distinct() }

    val manageStorageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        permissionRequestInProgress.value = false
        permissionRefreshKey.value += 1
    }

    val permissionRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        permissionRequestInProgress.value = false
        permissionRefreshKey.value += 1
        val needsManageStorage = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            permissionList.contains(Manifest.permission.MANAGE_EXTERNAL_STORAGE) &&
            !Environment.isExternalStorageManager()
        if (needsManageStorage) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            manageStorageLauncher.launch(intent)
        }
    }

    fun requestAllPermissions() {
        if (permissionRequestInProgress.value) return
        val missingStandardPermissions = permissionList
            .filterNot { it == Manifest.permission.MANAGE_EXTERNAL_STORAGE }
            .filter { !isPermissionGranted(context, it) }

        if (missingStandardPermissions.isNotEmpty()) {
            permissionRequestInProgress.value = true
            permissionRequestLauncher.launch(missingStandardPermissions.toTypedArray())
            return
        }

        val needsManageStorage = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            permissionList.contains(Manifest.permission.MANAGE_EXTERNAL_STORAGE) &&
            !Environment.isExternalStorageManager()
        if (needsManageStorage) {
            permissionRequestInProgress.value = true
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            manageStorageLauncher.launch(intent)
        }
    }

    LaunchedEffect(permissionRefreshKey.value) {
        permissionStatuses.clear()
        permissionItems.forEach { item ->
            permissionStatuses[item.permission] = isPermissionGranted(context, item.permission)
        }
        if (permissionItems.any { permissionStatuses[it.permission] != true }) {
            showPermissionDialog.value = true
        }
    }

    val allPermissionsGranted = permissionItems.all { permissionStatuses[it.permission] == true }

    LaunchedEffect(allPermissionsGranted) {
        if (!allPermissionsGranted) return@LaunchedEffect
        viewModel.markPermissionsGranted()
        try {
            baseViewModel.refreshMetalRates(context)
        } catch (_: Exception) { }
        viewModel.startLoadingWork()
        showPermissionDialog.value = false
        if (!monitorStarted.value) {
            SyncService.startMonitoring(context)
            monitorStarted.value = true
        }
        if (!minDelayStarted.value) {
            minDelayStarted.value = true
            delay(2500)
            minDelayDone.value = true
        }
    }

    LaunchedEffect(needsStoreSetup, minDelayDone.value, overallProgress) {
        if (needsStoreSetup == null ||
            hasNavigated.value ||
            !minDelayDone.value ||
            overallProgress < 1f ||
            backupDecisionPending
        ) {
            return@LaunchedEffect
        }

        hasNavigated.value = true
        if (needsStoreSetup) {
            baseViewModel.snackBarState = "Please Set Up Your Store First."
            baseViewModel.setPendingNotificationNavigation(SubScreens.Profile.route, "true")
        } else {
            baseViewModel.setPendingNotificationNavigation(SubScreens.Dashboard.route, null)
        }
        navHost.navigate(Screens.Main.route) {
            popUpTo(Screens.StartLoading.route) { inclusive = true }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LottieAnimation(
                composition = composition,
                progress = { lottieProgress },
                modifier = Modifier.size(400.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { overallProgress },
                modifier = Modifier
                    .height(6.dp)
                    .fillMaxWidth(0.6f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Progress: ${(overallProgress * 100).toInt()}%",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp
            )
            if (progressMessage.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = progressMessage,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
    }

    if (showPermissionDialog.value) {
        AlertDialog(
            onDismissRequest = { if (allPermissionsGranted) showPermissionDialog.value = false },
            title = { Text("Permissions Required") },
            text = {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .verticalScroll(scrollState)
                ) {
                    permissionItems.forEach { item ->
                        val granted = permissionStatuses[item.permission] == true
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = if (granted) Icons.TwoTone.CheckCircle else Icons.TwoTone.Cancel,
                                contentDescription = null,
                                tint = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            Column(modifier = Modifier.padding(start = 12.dp)) {
                                Text(text = item.title, fontSize = 14.sp)
                                Text(
                                    text = item.description,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { requestAllPermissions() },
                    enabled = !permissionRequestInProgress.value
                ) {
                    Text("Grant")
                }
            },
            properties = DialogProperties(
                dismissOnBackPress = allPermissionsGranted,
                dismissOnClickOutside = allPermissionsGranted
            )
        )
    }

    if (showBackupDialog && permissionsGranted) {
        AlertDialog(
            onDismissRequest = { if (!backupRestoreInProgress) viewModel.onBackupRestoreCancelled() },
            title = { Text("Sync data found") },
            text = {
                Text(
                    if (backupRestoreInProgress) {
                        "Restoring data from cloud sync..."
                    } else {
                        "Your data is already available on the server. Would you like to load it now?"
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.onBackupRestoreConfirmed() },
                    enabled = !backupRestoreInProgress
                ) {
                    Text("Load")
                }
            },
            dismissButton = {
                Button(
                    onClick = { viewModel.onBackupRestoreCancelled() },
                    enabled = !backupRestoreInProgress
                ) {
                    Text("Cancel")
                }
            },
            properties = DialogProperties(
                dismissOnBackPress = !backupRestoreInProgress,
                dismissOnClickOutside = !backupRestoreInProgress
            )
        )
    }
}

private data class PermissionInfo(
    val permission: String,
    val title: String,
    val description: String
)

private fun buildPermissionInfoList(): List<PermissionInfo> {
    val items = mutableListOf(
        PermissionInfo(
            permission = Manifest.permission.CAMERA,
            title = "Camera",
            description = "Scan QR/barcodes and capture images."
        ),
        PermissionInfo(
            permission = Manifest.permission.ACCESS_FINE_LOCATION,
            title = "Location",
            description = "Find nearby devices and improve scan accuracy."
        )
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        items += PermissionInfo(
            permission = Manifest.permission.BLUETOOTH_CONNECT,
            title = "Bluetooth Connect",
            description = "Connect to printers and Bluetooth devices."
        )
        items += PermissionInfo(
            permission = Manifest.permission.BLUETOOTH_SCAN,
            title = "Bluetooth Scan",
            description = "Scan for nearby Bluetooth devices."
        )
    } else {
        items += PermissionInfo(
            permission = Manifest.permission.BLUETOOTH,
            title = "Bluetooth",
            description = "Access Bluetooth printers and devices."
        )
        items += PermissionInfo(
            permission = Manifest.permission.BLUETOOTH_ADMIN,
            title = "Bluetooth Admin",
            description = "Manage Bluetooth device discovery."
        )
    }

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        items += PermissionInfo(
            permission = Manifest.permission.READ_EXTERNAL_STORAGE,
            title = "Read Storage",
            description = "Import sync files and read exported files."
        )
        items += PermissionInfo(
            permission = Manifest.permission.WRITE_EXTERNAL_STORAGE,
            title = "Write Storage",
            description = "Export and save sync files."
        )
    } else {
        items += PermissionInfo(
            permission = Manifest.permission.READ_MEDIA_IMAGES,
            title = "Read Media",
            description = "Read sync files from device storage."
        )
        items += PermissionInfo(
            permission = Manifest.permission.POST_NOTIFICATIONS,
            title = "Notifications",
            description = "Show sync, restore, and update alerts."
        )
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        items += PermissionInfo(
            permission = Manifest.permission.MANAGE_EXTERNAL_STORAGE,
            title = "All Files Access",
            description = "Allow full access for sync files and exports."
        )
    }

    return items
}

private fun isPermissionGranted(context: Context, permission: String): Boolean {
    return when (permission) {
        Manifest.permission.MANAGE_EXTERNAL_STORAGE -> {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager()
        }
        Manifest.permission.READ_MEDIA_IMAGES -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        }
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE -> {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        }
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        }
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN -> {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        }
        Manifest.permission.POST_NOTIFICATIONS -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        }
        else -> {
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
}
