package com.velox.jewelvault.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
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
     * Get all JewelVault files from MediaStore
     */
    suspend fun getAllJewelVaultFiles(context: Context): Result<List<FileItem>> {
        return withContext(Dispatchers.IO) {
            try {
                val files = mutableListOf<FileItem>()
                
                val projection = arrayOf(
                    MediaStore.Downloads._ID,
                    MediaStore.Downloads.DISPLAY_NAME,
                    MediaStore.Downloads.SIZE,
                    MediaStore.Downloads.DATE_MODIFIED,
                    MediaStore.Downloads.MIME_TYPE,
                    MediaStore.Downloads.RELATIVE_PATH
                )
                
                // Try multiple search strategies
                val searchStrategies = listOf(
                    // Strategy 1: Look for JewelVault in path
                    Pair("${MediaStore.Downloads.RELATIVE_PATH} LIKE ?", arrayOf("%$JEWEL_VAULT_FOLDER%")),
                    // Strategy 2: Look for JewelVault in filename
                    Pair("${MediaStore.Downloads.DISPLAY_NAME} LIKE ?", arrayOf("%jewelvault%")),
                    // Strategy 3: Look for common JewelVault file patterns
                    Pair("${MediaStore.Downloads.DISPLAY_NAME} LIKE ? OR ${MediaStore.Downloads.DISPLAY_NAME} LIKE ? OR ${MediaStore.Downloads.DISPLAY_NAME} LIKE ?", 
                         arrayOf("%invoice%", "%backup%", "%estimate%")),
                    // Strategy 4: Look for PDF and Excel files that might be from JewelVault
                    Pair("${MediaStore.Downloads.MIME_TYPE} IN (?, ?)", arrayOf("application/pdf", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                )
                
                for ((selection, selectionArgs) in searchStrategies) {
                    log("Trying search strategy: $selection")
                    
                    val cursor = context.contentResolver.query(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        projection,
                        selection,
                        selectionArgs,
                        "${MediaStore.Downloads.DATE_MODIFIED} DESC"
                    )
                    
                    cursor?.use { c ->
                        val idColumn = c.getColumnIndexOrThrow(MediaStore.Downloads._ID)
                        val nameColumn = c.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)
                        val sizeColumn = c.getColumnIndexOrThrow(MediaStore.Downloads.SIZE)
                        val dateColumn = c.getColumnIndexOrThrow(MediaStore.Downloads.DATE_MODIFIED)
                        val mimeColumn = c.getColumnIndexOrThrow(MediaStore.Downloads.MIME_TYPE)
                        val pathColumn = c.getColumnIndexOrThrow(MediaStore.Downloads.RELATIVE_PATH)
                        
                        while (c.moveToNext()) {
                            val id = c.getLong(idColumn)
                            val name = c.getString(nameColumn)
                            val size = c.getLong(sizeColumn)
                            val dateModified = c.getLong(dateColumn) * 1000 // Convert to milliseconds
                            val mimeType = c.getString(mimeColumn)
                            val relativePath = c.getString(pathColumn)
                            
                            // Check if this file is actually from JewelVault
                            val isJewelVaultFile = relativePath?.contains("JewelVault", ignoreCase = true) == true ||
                                                  name.contains("jewelvault", ignoreCase = true) ||
                                                  name.contains("invoice", ignoreCase = true) ||
                                                  name.contains("backup", ignoreCase = true) ||
                                                  name.contains("estimate", ignoreCase = true) ||
                                                  name.contains("draft", ignoreCase = true)
                            
                            if (isJewelVaultFile) {
                                val uri = Uri.withAppendedPath(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id.toString())
                                
                                val fileItem = FileItem(
                                    name = name,
                                    path = relativePath ?: "",
                                    size = size,
                                    lastModified = dateModified,
                                    isDirectory = false,
                                    mimeType = mimeType,
                                    uri = uri
                                )
                                
                                // Avoid duplicates
                                if (files.none { it.uri == fileItem.uri }) {
                                    files.add(fileItem)
                                    log("Added file: ${fileItem.name}, Path: ${fileItem.path}")
                                }
                            }
                        }
                    }
                }
                
                log("Total JewelVault files found: ${files.size}")
                Result.success(files)
                
            } catch (e: Exception) {
                log("Error loading JewelVault files: ${e.message}")
                Result.failure(e)
            }
        }
    }
    
    /**
     * Search files by name
     */
    suspend fun searchFiles(context: Context, query: String): Result<List<FileItem>> {
        return withContext(Dispatchers.IO) {
            try {
                val files = mutableListOf<FileItem>()
                
                val projection = arrayOf(
                    MediaStore.Downloads._ID,
                    MediaStore.Downloads.DISPLAY_NAME,
                    MediaStore.Downloads.SIZE,
                    MediaStore.Downloads.DATE_MODIFIED,
                    MediaStore.Downloads.MIME_TYPE,
                    MediaStore.Downloads.RELATIVE_PATH
                )
                
                val selection = "${MediaStore.Downloads.RELATIVE_PATH} LIKE ? AND ${MediaStore.Downloads.DISPLAY_NAME} LIKE ?"
                val selectionArgs = arrayOf("%$JEWEL_VAULT_FOLDER%", "%$query%")
                
                val cursor = context.contentResolver.query(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    "${MediaStore.Downloads.DATE_MODIFIED} DESC"
                )
                
                cursor?.use { c ->
                    val idColumn = c.getColumnIndexOrThrow(MediaStore.Downloads._ID)
                    val nameColumn = c.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)
                    val sizeColumn = c.getColumnIndexOrThrow(MediaStore.Downloads.SIZE)
                    val dateColumn = c.getColumnIndexOrThrow(MediaStore.Downloads.DATE_MODIFIED)
                    val mimeColumn = c.getColumnIndexOrThrow(MediaStore.Downloads.MIME_TYPE)
                    val pathColumn = c.getColumnIndexOrThrow(MediaStore.Downloads.RELATIVE_PATH)
                    
                    while (c.moveToNext()) {
                        val id = c.getLong(idColumn)
                        val name = c.getString(nameColumn)
                        val size = c.getLong(sizeColumn)
                        val dateModified = c.getLong(dateColumn) * 1000
                        val mimeType = c.getString(mimeColumn)
                        val relativePath = c.getString(pathColumn)
                        
                        val uri = Uri.withAppendedPath(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id.toString())
                        
                        files.add(
                            FileItem(
                                name = name,
                                path = relativePath ?: "",
                                size = size,
                                lastModified = dateModified,
                                isDirectory = false,
                                mimeType = mimeType,
                                uri = uri
                            )
                        )
                    }
                }
                
                Result.success(files)
                
            } catch (e: Exception) {
                log("Error searching files: ${e.message}")
                Result.failure(e)
            }
        }
    }
    
    /**
     * Delete a file
     */
    suspend fun deleteFile(context: Context, file: FileItem): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                file.uri?.let { uri ->
                    val deleted = context.contentResolver.delete(uri, null, null)
                    Result.success(deleted > 0)
                } ?: Result.success(false)
            } catch (e: Exception) {
                log("Error deleting file: ${e.message}")
                Result.failure(e)
            }
        }
    }
    
    /**
     * Open file with external app
     */
    fun openFile(context: Context, file: FileItem): Result<Boolean> {
        return try {
            file.uri?.let { uri ->
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, file.mimeType ?: "*/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    Result.success(true)
                } else {
                    // Fallback: try to open with any app
                    val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                        setData(uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(fallbackIntent)
                    Result.success(true)
                }
            } ?: Result.success(false)
        } catch (e: Exception) {
            log("Error opening file: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Share file with other apps
     */
    fun shareFile(context: Context, file: FileItem): Result<Boolean> {
        return try {
            file.uri?.let { uri ->
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = file.mimeType ?: "*/*"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                val chooser = Intent.createChooser(intent, "Share ${file.name}")
                context.startActivity(chooser)
                Result.success(true)
            } ?: Result.success(false)
        } catch (e: Exception) {
            log("Error sharing file: ${e.message}")
            Result.failure(e)
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
    
    /**
     * Check if file management permissions are granted
     */
    fun hasFileManagementPermission(context: Context): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            android.os.Environment.isExternalStorageManager()
        } else {
            true // For older versions, assume permission is granted
        }
    }
    
    /**
     * Request file management permission
     */
    fun requestFileManagementPermission(context: Context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        }
    }
    
    /**
     * Get folder structure for JewelVault files
     */
    suspend fun getFolderStructure(context: Context): Result<Map<String, List<FileItem>>> {
        return withContext(Dispatchers.IO) {
            try {
                val allFilesResult = getAllJewelVaultFiles(context)
                if (allFilesResult.isFailure) {
                    return@withContext Result.failure(allFilesResult.exceptionOrNull() ?: Exception("Failed to load files"))
                }
                
                val allFiles = allFilesResult.getOrNull() ?: emptyList()
                val folderMap = mutableMapOf<String, MutableList<FileItem>>()
                
                log("Total files found: ${allFiles.size}")
                
                allFiles.forEach { file ->
                    log("File: ${file.name}, Path: ${file.path}")
                    
                    val folderPath = if (file.path.contains("/")) {
                        val pathParts = file.path.split("/")
                        log("Path parts: $pathParts")
                        
                        // Find the JewelVault part and get the subfolder
                        val jewelVaultIndex = pathParts.indexOfFirst { it.contains("JewelVault", ignoreCase = true) }
                        log("JewelVault index: $jewelVaultIndex")
                        
                        if (jewelVaultIndex >= 0 && jewelVaultIndex < pathParts.size - 1) {
                            // Get the subfolder after JewelVault
                            val subPath = pathParts.subList(jewelVaultIndex + 1, pathParts.size - 1)
                            log("Sub path: $subPath")
                            
                            if (subPath.isNotEmpty()) {
                                val folderName = subPath.joinToString("/")
                                log("Final folder name: $folderName")
                                folderName
                            } else {
                                "Root"
                            }
                        } else {
                            "Root"
                        }
                    } else {
                        "Root"
                    }
                    
                    log("Assigned to folder: $folderPath")
                    folderMap.getOrPut(folderPath) { mutableListOf() }.add(file)
                }
                
                log("All folders found: ${folderMap.keys}")
                log("Folder structure: ${folderMap.mapValues { it.value.size }}")
                
                // If we found very few folders, try alternative approach
                if (folderMap.size <= 1) {
                    log("Few folders found, trying alternative approach...")
                    return@withContext getFolderStructureAlternative(context)
                }
                
                Result.success(folderMap)
                
            } catch (e: Exception) {
                log("Error getting folder structure: ${e.message}")
                Result.failure(e)
            }
        }
    }
    
    /**
     * Alternative method to get folder structure by scanning file system
     */
    private suspend fun getFolderStructureAlternative(context: Context): Result<Map<String, List<FileItem>>> {
        return withContext(Dispatchers.IO) {
            try {
                val folderMap = mutableMapOf<String, MutableList<FileItem>>()
                
                // Try to get all files with broader search
                val projection = arrayOf(
                    MediaStore.Downloads._ID,
                    MediaStore.Downloads.DISPLAY_NAME,
                    MediaStore.Downloads.SIZE,
                    MediaStore.Downloads.DATE_MODIFIED,
                    MediaStore.Downloads.MIME_TYPE,
                    MediaStore.Downloads.RELATIVE_PATH
                )
                
                // Search for any file that might be related to JewelVault
                val selection = "${MediaStore.Downloads.DISPLAY_NAME} LIKE ? OR ${MediaStore.Downloads.RELATIVE_PATH} LIKE ?"
                val selectionArgs = arrayOf("%jewelvault%", "%JewelVault%")
                
                val cursor = context.contentResolver.query(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    "${MediaStore.Downloads.DATE_MODIFIED} DESC"
                )
                
                cursor?.use { c ->
                    val idColumn = c.getColumnIndexOrThrow(MediaStore.Downloads._ID)
                    val nameColumn = c.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)
                    val sizeColumn = c.getColumnIndexOrThrow(MediaStore.Downloads.SIZE)
                    val dateColumn = c.getColumnIndexOrThrow(MediaStore.Downloads.DATE_MODIFIED)
                    val mimeColumn = c.getColumnIndexOrThrow(MediaStore.Downloads.MIME_TYPE)
                    val pathColumn = c.getColumnIndexOrThrow(MediaStore.Downloads.RELATIVE_PATH)
                    
                    while (c.moveToNext()) {
                        val id = c.getLong(idColumn)
                        val name = c.getString(nameColumn)
                        val size = c.getLong(sizeColumn)
                        val dateModified = c.getLong(dateColumn) * 1000
                        val mimeType = c.getString(mimeColumn)
                        val relativePath = c.getString(pathColumn)
                        
                        val uri = Uri.withAppendedPath(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id.toString())
                        
                        val file = FileItem(
                            name = name,
                            path = relativePath ?: "",
                            size = size,
                            lastModified = dateModified,
                            isDirectory = false,
                            mimeType = mimeType,
                            uri = uri
                        )
                        
                        log("Alternative file: ${file.name}, Path: ${file.path}")
                        
                        val folderPath = if (file.path.contains("/")) {
                            val pathParts = file.path.split("/")
                            val jewelVaultIndex = pathParts.indexOfFirst { it.contains("JewelVault", ignoreCase = true) }
                            if (jewelVaultIndex >= 0 && jewelVaultIndex < pathParts.size - 1) {
                                val subPath = pathParts.subList(jewelVaultIndex + 1, pathParts.size - 1)
                                if (subPath.isNotEmpty()) {
                                    subPath.joinToString("/")
                                } else {
                                    "Root"
                                }
                            } else {
                                "Root"
                            }
                        } else {
                            "Root"
                        }
                        
                        folderMap.getOrPut(folderPath) { mutableListOf() }.add(file)
                    }
                }
                
                log("Alternative approach - All folders found: ${folderMap.keys}")
                log("Alternative approach - Folder structure: ${folderMap.mapValues { it.value.size }}")
                Result.success(folderMap)
                
            } catch (e: Exception) {
                log("Error in alternative folder structure: ${e.message}")
                Result.failure(e)
            }
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
