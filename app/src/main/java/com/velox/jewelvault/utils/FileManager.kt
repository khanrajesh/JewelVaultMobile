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
    
    /**
     * Get the JewelVault folder path in Downloads directory
     */
    fun getJewelVaultFolderPath(context: Context): File {
        return File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), JEWEL_VAULT_FOLDER)
    }
    
    /**
     * Get list of folders in JewelVault directory
     * This function scans both file system and MediaStore for JewelVault files
     */
    suspend fun getFoldersInJewelVault(context: Context): List<FileItem> = withContext(Dispatchers.IO) {
        // Get folders from MediaStore (where most files are actually stored)
        val mediaStoreFolders = getFoldersFromMediaStore(context)
        
        // Get folders from file system (fallback)
        val fileSystemFolders = getFoldersFromFileSystem(context)
        
        // Combine and deduplicate folders
        val allFolders = (mediaStoreFolders + fileSystemFolders)
            .distinctBy { it.name }
            .sortedBy { it.name }
        
        allFolders
    }
    
    /**
     * Get folders from MediaStore (where files are actually stored)
     */
    private suspend fun getFoldersFromMediaStore(context: Context): List<FileItem> = withContext(Dispatchers.IO) {
        val folders = mutableSetOf<String>()
        
        try {
            val projection = arrayOf(
                MediaStore.Downloads.RELATIVE_PATH
            )
            
            val selection = "${MediaStore.Downloads.RELATIVE_PATH} LIKE ?"
            val selectionArgs = arrayOf("%JewelVault%")
            
            val cursor: Cursor? = context.contentResolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )
            
            cursor?.use { c ->
                val relativePathIndex = c.getColumnIndexOrThrow(MediaStore.Downloads.RELATIVE_PATH)
                
                while (c.moveToNext()) {
                    val relativePath = c.getString(relativePathIndex)
                    // Extract folder name from path like "Download/JewelVault/Invoice"
                    val pathParts = relativePath.split("/")
                    if (pathParts.size >= 3 && pathParts[1] == "JewelVault") {
                        folders.add(pathParts[2]) // Get the folder name (Invoice, DraftInvoice, etc.)
                    }
                }
            }
        } catch (e: Exception) {
            log("FileManager: Error reading from MediaStore: ${e.message}")
        }
        
        // Convert folder names to FileItem objects
        folders.map { folderName ->
            FileItem(
                name = folderName,
                path = "MediaStore: $folderName", // Indicate this is from MediaStore
                size = 0L, // We don't have folder size from MediaStore
                lastModified = System.currentTimeMillis(),
                isDirectory = true,
                mimeType = null,
                uri = null
            )
        }
    }
    
    /**
     * Get folders from file system (fallback method)
     */
    private suspend fun getFoldersFromFileSystem(context: Context): List<FileItem> = withContext(Dispatchers.IO) {
        val jewelVaultFolder = getJewelVaultFolderPath(context)
        
        if (!jewelVaultFolder.exists()) {
            return@withContext emptyList()
        }
        
        jewelVaultFolder.listFiles()?.filter { it.isDirectory }?.map { folder ->
            FileItem(
                name = folder.name,
                path = folder.absolutePath,
                size = folder.length(),
                lastModified = folder.lastModified(),
                isDirectory = true,
                mimeType = null,
                uri = null
            )
        } ?: emptyList()
    }
    
    /**
     * Get list of files in a specific folder within JewelVault directory
     * This function scans both MediaStore and file system for files
     */
    suspend fun getFilesInFolder(context: Context, folderName: String): List<FileItem> = withContext(Dispatchers.IO) {
        // Get files from MediaStore (where most files are actually stored)
        val mediaStoreFiles = getFilesFromMediaStore(context, folderName)
        
        // Get files from file system (fallback)
        val fileSystemFiles = getFilesFromFileSystem(context, folderName)
        
        // Combine and deduplicate files
        val allFiles = (mediaStoreFiles + fileSystemFiles)
            .distinctBy { it.name }
            .sortedBy { it.name }
        
        allFiles
    }
    
    /**
     * Get files from MediaStore for a specific folder
     */
    private suspend fun getFilesFromMediaStore(context: Context, folderName: String): List<FileItem> = withContext(Dispatchers.IO) {
        val files = mutableListOf<FileItem>()
        
        try {
            val projection = arrayOf(
                MediaStore.Downloads._ID,
                MediaStore.Downloads.DISPLAY_NAME,
                MediaStore.Downloads.SIZE,
                MediaStore.Downloads.DATE_MODIFIED,
                MediaStore.Downloads.MIME_TYPE,
                MediaStore.Downloads.RELATIVE_PATH
            )
            
            val selection = "${MediaStore.Downloads.RELATIVE_PATH} = ?"
            val selectionArgs = arrayOf("${Environment.DIRECTORY_DOWNLOADS}/JewelVault/$folderName")
            
            val cursor: Cursor? = context.contentResolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${MediaStore.Downloads.DATE_MODIFIED} DESC"
            )
            
            cursor?.use { c ->
                val idIndex = c.getColumnIndexOrThrow(MediaStore.Downloads._ID)
                val nameIndex = c.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)
                val sizeIndex = c.getColumnIndexOrThrow(MediaStore.Downloads.SIZE)
                val dateIndex = c.getColumnIndexOrThrow(MediaStore.Downloads.DATE_MODIFIED)
                val mimeTypeIndex = c.getColumnIndexOrThrow(MediaStore.Downloads.MIME_TYPE)
                val pathIndex = c.getColumnIndexOrThrow(MediaStore.Downloads.RELATIVE_PATH)
                
                while (c.moveToNext()) {
                    val id = c.getLong(idIndex)
                    val name = c.getString(nameIndex)
                    val size = c.getLong(sizeIndex)
                    val dateModified = c.getLong(dateIndex) * 1000 // Convert to milliseconds
                    val mimeType = c.getString(mimeTypeIndex)
                    val relativePath = c.getString(pathIndex)
                    
                    val uri = Uri.withAppendedPath(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id.toString())
                    
                    files.add(
                        FileItem(
                            name = name,
                            path = relativePath ?: "MediaStore: $name",
                            size = size,
                            lastModified = dateModified,
                            isDirectory = false,
                            mimeType = mimeType,
                            uri = uri
                        )
                    )
                }
            }
        } catch (e: Exception) {
            log("FileManager: Error reading files from MediaStore for folder $folderName: ${e.message}")
        }
        
        files
    }
    
    /**
     * Get files from file system for a specific folder (fallback method)
     */
    private suspend fun getFilesFromFileSystem(context: Context, folderName: String): List<FileItem> = withContext(Dispatchers.IO) {
        val jewelVaultFolder = getJewelVaultFolderPath(context)
        val targetFolder = File(jewelVaultFolder, folderName)
        
        if (!targetFolder.exists() || !targetFolder.isDirectory) {
            return@withContext emptyList()
        }
        
        targetFolder.listFiles()?.map { file ->
            FileItem(
                name = file.name,
                path = file.absolutePath,
                size = file.length(),
                lastModified = file.lastModified(),
                isDirectory = file.isDirectory,
                mimeType = getMimeType(file.name),
                uri = null
            )
        } ?: emptyList()
    }
    
    /**
     * Get MIME type based on file extension
     */
    private fun getMimeType(fileName: String): String? {
        return when {
            fileName.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
            fileName.endsWith(".xlsx", ignoreCase = true) -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            fileName.endsWith(".xls", ignoreCase = true) -> "application/vnd.ms-excel"
            fileName.endsWith(".csv", ignoreCase = true) -> "text/csv"
            fileName.endsWith(".txt", ignoreCase = true) -> "text/plain"
            fileName.endsWith(".jpg", ignoreCase = true) || fileName.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
            fileName.endsWith(".png", ignoreCase = true) -> "image/png"
            else -> null
        }
    }
    
    /**
     * Debug function to log all JewelVault files from MediaStore
     */
    suspend fun debugMediaStoreFiles(context: Context) = withContext(Dispatchers.IO) {
        try {
            val projection = arrayOf(
                MediaStore.Downloads._ID,
                MediaStore.Downloads.DISPLAY_NAME,
                MediaStore.Downloads.SIZE,
                MediaStore.Downloads.DATE_MODIFIED,
                MediaStore.Downloads.MIME_TYPE,
                MediaStore.Downloads.RELATIVE_PATH
            )
            
            val selection = "${MediaStore.Downloads.RELATIVE_PATH} LIKE ?"
            val selectionArgs = arrayOf("%JewelVault%")
            
            val cursor: Cursor? = context.contentResolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${MediaStore.Downloads.DATE_MODIFIED} DESC"
            )
            
            log("FileManager: DEBUG - Scanning MediaStore for JewelVault files...")
            var fileCount = 0
            
            cursor?.use { c ->
                val idIndex = c.getColumnIndexOrThrow(MediaStore.Downloads._ID)
                val nameIndex = c.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)
                val sizeIndex = c.getColumnIndexOrThrow(MediaStore.Downloads.SIZE)
                val dateIndex = c.getColumnIndexOrThrow(MediaStore.Downloads.DATE_MODIFIED)
                val mimeTypeIndex = c.getColumnIndexOrThrow(MediaStore.Downloads.MIME_TYPE)
                val pathIndex = c.getColumnIndexOrThrow(MediaStore.Downloads.RELATIVE_PATH)
                
                while (c.moveToNext()) {
                    val id = c.getLong(idIndex)
                    val name = c.getString(nameIndex)
                    val size = c.getLong(sizeIndex)
                    val dateModified = c.getLong(dateIndex)
                    val mimeType = c.getString(mimeTypeIndex)
                    val relativePath = c.getString(pathIndex)
                    
                    log("FileManager: DEBUG - Found file: $name, Path: $relativePath, Size: $size, MimeType: $mimeType")
                    fileCount++
                }
            }
            
            log("FileManager: DEBUG - Total files found in MediaStore: $fileCount")
            
        } catch (e: Exception) {
            log("FileManager: DEBUG - Error scanning MediaStore: ${e.message}")
        }
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
