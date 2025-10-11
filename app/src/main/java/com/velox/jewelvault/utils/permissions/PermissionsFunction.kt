package com.velox.jewelvault.utils.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat

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
        false // Notification permission not required for Android < 13
    }
}

fun getBackupRestorePermissions(): List<String> {
    val permissions = mutableListOf<String>()
    
    // Add storage permissions
    permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
    
    // Add notification permission for Android 13+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissions.add(Manifest.permission.POST_NOTIFICATIONS)
    }
    
    return permissions
}

fun hasConnectPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.BLUETOOTH_CONNECT
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    } else true
}

fun hasScanPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.BLUETOOTH_SCAN
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    } else true
}

fun hasFineLocation(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    } else true
}