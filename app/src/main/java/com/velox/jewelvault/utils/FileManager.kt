package com.velox.jewelvault.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object FileManager {
    
    private const val JEWEL_VAULT_FOLDER = "JewelVault"
    private const val LOGO_FILENAME = "store_logo.jpg"

    fun getJewelVaultFolderPath(context: Context): File {
        return File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), JEWEL_VAULT_FOLDER)
    }

    
    /**
     * Get file size in human readable format
     */
    fun formatFileSize(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB")
        var size = bytes.toDouble()
        var unitIndex = 0
        
        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }
        
        return "%.1f %s".format(size, units[unitIndex])
    }
    
    /**
     * Get file icon based on file type
     */
    fun getFileIcon(fileName: String): String {
        return when {
            fileName.endsWith(".pdf", ignoreCase = true) -> "PDF"
            fileName.endsWith(".xlsx", ignoreCase = true) || fileName.endsWith(".xls", ignoreCase = true) -> "EXCEL"
            fileName.endsWith(".csv", ignoreCase = true) -> "CSV"
            fileName.endsWith(".txt", ignoreCase = true) -> "TEXT"
            fileName.endsWith(".jpg", ignoreCase = true) || fileName.endsWith(".jpeg", ignoreCase = true) || fileName.endsWith(".png", ignoreCase = true) -> "IMAGE"
            else -> "FILE"
        }
    }
    
    // ==================== LOGO MANAGEMENT ====================

    /**
     * Get the JewelVault folder path in Downloads directory
     */



    
    /**
     * Get the local logo file path
     */
    fun getLogoFilePath(context: Context): File {
        val logoFolder = getJewelVaultFolderPath(context)
        return File(logoFolder, LOGO_FILENAME)
    }
    
    /**
     * Get the local logo file URI
     */
    fun getLogoFileUri(context: Context): Uri? {
        val logoFile = getLogoFilePath(context)
        return if (logoFile.exists()) {
            Uri.fromFile(logoFile)
        } else {
            null
        }
    }
    
    /**
     * Check if local logo file exists
     */
    fun isLogoFileExists(context: Context): Boolean {
        val logoFile = getLogoFilePath(context)
        return logoFile.exists()
    }
    
    /**
     * Download and save logo from URL to local storage
     */
    suspend fun downloadAndSaveLogo(context: Context, imageUrl: String): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            log("FileManager: Starting logo download from: $imageUrl")
            
            // Create logo folder if it doesn't exist
            val logoFolder = getJewelVaultFolderPath(context)
            if (!logoFolder.exists()) {
                logoFolder.mkdirs()
                log("FileManager: Created logo folder: ${logoFolder.absolutePath}")
            }
            
            // Get the logo file path
            val logoFile = getLogoFilePath(context)
            
            // Download the image
            val url = java.net.URL(imageUrl)
            val connection = url.openConnection()
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            
            val inputStream = connection.getInputStream()
            val outputStream = logoFile.outputStream()
            
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            
            log("FileManager: Logo downloaded and saved to: ${logoFile.absolutePath}")
            Result.success(Uri.fromFile(logoFile))
            
        } catch (e: Exception) {
            log("FileManager: Error downloading logo: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Delete local logo file
     */
    suspend fun deleteLogoFile(context: Context): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val logoFile = getLogoFilePath(context)
            if (logoFile.exists()) {
                val deleted = logoFile.delete()
                if (deleted) {
                    log("FileManager: Logo file deleted successfully")
                    Result.success(Unit)
                } else {
                    log("FileManager: Failed to delete logo file")
                    Result.failure(Exception("Failed to delete logo file"))
                }
            } else {
                log("FileManager: Logo file does not exist")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            log("FileManager: Error deleting logo file: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Get logo file info
     */
    suspend fun getLogoFileInfo(context: Context): FileItem? = withContext(Dispatchers.IO) {
        val logoFile = getLogoFilePath(context)
        if (logoFile.exists()) {
            FileItem(
                name = logoFile.name,
                path = logoFile.absolutePath,
                size = logoFile.length(),
                lastModified = logoFile.lastModified(),
                isDirectory = false,
                mimeType = "image/jpeg",
                uri = Uri.fromFile(logoFile)
            )
        } else {
            null
        }
    }
    
}

data class FileItem(
    val name: String,
    val path: String,
    val size: Long,
    val lastModified: Long,
    val isDirectory: Boolean,
    val mimeType: String? = null,
    val uri: Uri? = null
) {
    val formattedSize: String
        get() = if (isDirectory) "" else FileManager.formatFileSize(size)
    
    val formattedDate: String
        get() = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(lastModified))
    
    val fileType: String
        get() = FileManager.getFileIcon(name)
}
