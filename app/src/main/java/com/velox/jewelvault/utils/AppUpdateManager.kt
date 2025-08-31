package com.velox.jewelvault.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.velox.jewelvault.data.UpdateInfo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppUpdateManager @Inject constructor() {
    
    fun openPlayStore(context: Context, updateInfo: UpdateInfo) {
        log("🛒 Opening Play Store with UpdateInfo: $updateInfo")
        log("🛒 Play Store URL: ${updateInfo.playStoreUrl}")
        
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(updateInfo.playStoreUrl)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            // Check if Play Store is available
            if (intent.resolveActivity(context.packageManager) != null) {
                log("🛒 Play Store app found, launching...")
                context.startActivity(intent)
            } else {
                log("🛒 Play Store app not found, falling back to browser")
                // Fallback to browser
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(updateInfo.playStoreUrl))
                browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(browserIntent)
            }
        } catch (e: Exception) {
            log("❌ Failed to open Play Store: ${e.message}")
            Toast.makeText(
                context,
                "Unable to open Play Store. Please update manually.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    fun openPlayStore(context: Context, packageName: String = "com.velox.jewelvault") {
        log("🛒 Opening Play Store with package name: $packageName")
        try {
            val playStoreUrl = "https://play.google.com/store/apps/details?id=$packageName"
            log("🛒 Generated Play Store URL: $playStoreUrl")
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(playStoreUrl)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            if (intent.resolveActivity(context.packageManager) != null) {
                log("🛒 Play Store app found, launching...")
                context.startActivity(intent)
            } else {
                log("🛒 Play Store app not found, falling back to browser")
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(playStoreUrl))
                browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(browserIntent)
            }
        } catch (e: Exception) {
            log("❌ Failed to open Play Store: ${e.message}")
            Toast.makeText(
                context,
                "Unable to open Play Store. Please update manually.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    fun validatePlayStoreUrl(url: String): Boolean {
        return url.startsWith("https://play.google.com/store/apps/details") ||
               url.startsWith("market://details")
    }
    
    fun getDefaultPlayStoreUrl(packageName: String = "com.velox.jewelvault"): String {
        return "https://play.google.com/store/apps/details?id=$packageName"
    }
} 