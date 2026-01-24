package com.velox.jewelvault.utils.permissions

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
import androidx.activity.result.ActivityResult
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Bluetooth
import androidx.compose.material.icons.twotone.CameraAlt
import androidx.compose.material.icons.twotone.Folder
import androidx.compose.material.icons.twotone.LocationOn
import androidx.compose.material.icons.twotone.Notifications
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

data class PermissionItem(
    val id: String,
    val title: String,
    val description: String,
    val permissions: List<String>,
    val icon: ImageVector,
    val minApiLevel: Int? = null
)

data class StorageState(val anyGranted: Boolean, val fullGranted: Boolean)

fun buildPermissionItems(): List<PermissionItem> {
    val storagePermissions = mutableListOf<String>()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        storagePermissions += listOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO
        )
    } else {
        storagePermissions += listOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    return listOf(
        PermissionItem(
            id = "storage",
            title = "Storage & Media",
            description = "Enable sync, restore, and exports to your device storage.",
            permissions = storagePermissions,
            icon = Icons.TwoTone.Folder
        ),
        PermissionItem(
            id = "camera",
            title = "Camera",
            description = "Scan QR/barcodes and capture images.",
            permissions = listOf(Manifest.permission.CAMERA),
            icon = Icons.TwoTone.CameraAlt
        ),
        PermissionItem(
            id = "location",
            title = "Location",
            description = "Find nearby devices and improve scan accuracy.",
            permissions = listOf(Manifest.permission.ACCESS_FINE_LOCATION),
            icon = Icons.TwoTone.LocationOn
        ),
        PermissionItem(
            id = "notifications",
            title = "Notifications",
            description = "Show updates, alerts, and sync status notifications.",
            permissions = listOf(Manifest.permission.POST_NOTIFICATIONS),
            icon = Icons.TwoTone.Notifications,
            minApiLevel = Build.VERSION_CODES.TIRAMISU
        ),
        PermissionItem(
            id = "bluetooth",
            title = "Bluetooth Access",
            description = "Connect to printers and scan nearby Bluetooth devices.",
            permissions = listOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            ),
            icon = Icons.TwoTone.Bluetooth,
            minApiLevel = Build.VERSION_CODES.S
        ),
        PermissionItem(
            id = "manage_storage",
            title = "All Files Access",
            description = "Allow full file access for sync files and exports on some devices.",
            permissions = listOf(Manifest.permission.MANAGE_EXTERNAL_STORAGE),
            icon = Icons.TwoTone.Folder,
            minApiLevel = Build.VERSION_CODES.R
        )
    )
}

fun refreshAllPermissions(
    context: Context,
    items: List<PermissionItem>,
    state: MutableMap<String, Boolean>
) {
    items.forEach { item ->
        state[item.id] = isPermissionGranted(context, item)
    }
}

fun isPermissionSupported(item: PermissionItem): Boolean {
    return item.minApiLevel?.let { Build.VERSION.SDK_INT >= it } ?: true
}

fun isPermissionGranted(context: Context, item: PermissionItem): Boolean {
    if (!isPermissionSupported(item)) return true
    if (item.id == "storage") {
        return storageState(context).anyGranted
    }
    if (item.id == "manage_storage") {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager()
    }
    val standardGranted = standardPermissions(item).all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
    val needsManage = requiresManageStorage(item)
    val manageGranted = if (needsManage && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else true

    return standardGranted && manageGranted
}

fun requestPermissionForItem(
    context: Context,
    item: PermissionItem,
    requestPermissionsLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>
): Boolean {
    if (!isPermissionSupported(item)) return false
    if (item.id == "storage" && storageState(context).anyGranted) {
        return false
    }
    val missingStandard = missingStandardPermissions(context, item)
    if (missingStandard.isNotEmpty()) {
        requestPermissionsLauncher.launch(missingStandard.toTypedArray())
        return true
    }

    val needsManage = requiresManageStorage(item)
    if (needsManage && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
        launchManageStorageSettings(context)
        return true
    }
    return false
}

fun missingStandardPermissions(context: Context, item: PermissionItem): List<String> {
    return standardPermissions(item).filter {
        when (it) {
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO -> !hasVisualMediaAccess(context)
            else -> ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
    }
}

fun standardPermissions(item: PermissionItem): List<String> {
    return item.permissions.filterNot { it == Manifest.permission.MANAGE_EXTERNAL_STORAGE }
}

fun requiresManageStorage(item: PermissionItem): Boolean {
    return item.permissions.contains(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
}

fun storageState(context: Context): StorageState {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val visualGranted = hasVisualMediaAccess(context)
        val audioGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
        val anyGranted = visualGranted || audioGranted
        val fullGranted = visualGranted && audioGranted
        StorageState(anyGranted = anyGranted, fullGranted = fullGranted)
    } else {
        listOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ).let { perms ->
            val grantedCount = perms.count { perm ->
                ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
            }
            StorageState(anyGranted = grantedCount > 0, fullGranted = grantedCount == perms.size)
        }
    }
}

private fun hasVisualMediaAccess(context: Context): Boolean {
    val imagesGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
    val videosGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
    if (imagesGranted || videosGranted) return true
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        ContextCompat.checkSelfPermission(context, "android.permission.READ_MEDIA_VISUAL_USER_SELECTED") == PackageManager.PERMISSION_GRANTED
    } else {
        false
    }
}

fun launchManageStorageSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val perAppIntent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
            data = Uri.parse("package:${context.packageName}")
            addCategory(Intent.CATEGORY_DEFAULT)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val globalIntent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(perAppIntent)
            return
        } catch (_: Exception) {
        }
        try {
            context.startActivity(globalIntent)
            return
        } catch (_: Exception) {
        }
    }
    openAppSettings(context)
}

fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

fun filterUngrantedPermissions(context: Context, permissions: List<String>): List<String> {
    return permissions.filter { permission ->
        when (permission) {
            Manifest.permission.MANAGE_EXTERNAL_STORAGE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    !Environment.isExternalStorageManager()
                } else false
            }
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    when (permission) {
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.READ_MEDIA_VIDEO -> !hasVisualMediaAccess(context)
                        else -> ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
                    }
                } else false
            }
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    false
                } else {
                    ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
                }
            }
            else -> {
                ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
            }
        }
    }
}

fun getMultiplePermissionMessage(permissions: List<String>): String {
    if (permissions.isEmpty()) return "We need some permissions to continue."

    val messages = permissions.mapNotNull { permission ->
        when (permission) {
            Manifest.permission.CAMERA -> "- Camera access to scan QR/barcodes."
            Manifest.permission.WRITE_EXTERNAL_STORAGE -> "- Storage access to save gold price data."
            Manifest.permission.MANAGE_EXTERNAL_STORAGE -> "- Manage storage access for smooth file operations."
            Manifest.permission.READ_CONTACTS -> "- Contacts access to invite friends."
            Manifest.permission.ACCESS_FINE_LOCATION -> "- Location access for nearby shop recommendations."
            Manifest.permission.RECORD_AUDIO -> "- Microphone access for voice search."
            Manifest.permission.POST_NOTIFICATIONS -> "- Notification access to show sync/restore progress."
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO -> "- Media access to read sync files."
            else -> null
        }
    }

    return buildString {
        appendLine("The app needs the following permissions:")
        messages.forEach { appendLine(it) }
    }
}

fun getPermissionTypeForQueue(permissions: List<String>): PermissionType {
    return when {
        permissions.any { it == Manifest.permission.POST_NOTIFICATIONS } -> PermissionType.NOTIFICATION
        permissions.any { it == Manifest.permission.CAMERA } -> PermissionType.CAMERA
        permissions.any { it == Manifest.permission.ACCESS_FINE_LOCATION } -> PermissionType.LOCATION
        permissions.any { it == Manifest.permission.READ_CONTACTS } -> PermissionType.CONTACTS
        permissions.any { it == Manifest.permission.RECORD_AUDIO } -> PermissionType.MICROPHONE
        permissions.any { it in listOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.MANAGE_EXTERNAL_STORAGE,
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) } -> PermissionType.STORAGE
        else -> PermissionType.STORAGE
    }
}

fun requestNextPermission(
    context: Context,
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
                    data = Uri.parse("package:${context.packageName}")
                }
                requestManageStorageLauncher.launch(intent)
            }
        } else {
            requestPermissionLauncher.launch(it)
        }
    }
}

fun moveToNextPermission(
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
            context = context,
            permissionQueue = permissionQueue,
            currentPermissionIndex = nextIndex,
            requestPermissionLauncher = requestPermissionLauncher,
            requestManageStorageLauncher = requestManageStorageLauncher
        )
    } else {
        onAllPermissionsGranted()
    }
}

fun handlePermissionResult(
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
        if (permission != null) {
            val isPermanentlyDenied = !ActivityCompat.shouldShowRequestPermissionRationale(context as Activity, permission)
            if (isPermanentlyDenied) {
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

fun needsStoragePermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        !Environment.isExternalStorageManager()
    } else {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) != PackageManager.PERMISSION_GRANTED
    }
}

fun needsNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED
    } else {
        false
    }
}

fun getBackupRestorePermissions(): List<String> {
    val permissions = mutableListOf<String>()

    // Storage permissions are only needed on API 28 and below; scoped storage/MediaStore
    // handles writes for API 29+ without explicit storage grants.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        permissions += listOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissions += Manifest.permission.POST_NOTIFICATIONS
    }

    return permissions
}

fun hasConnectPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    } else true
}

fun hasScanPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.BLUETOOTH_SCAN
        ) == PackageManager.PERMISSION_GRANTED
    } else true
}

fun hasFineLocation(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    } else true
}
