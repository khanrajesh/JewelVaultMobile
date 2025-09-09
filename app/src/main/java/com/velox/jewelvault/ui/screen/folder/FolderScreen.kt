package com.velox.jewelvault.ui.screen.folder

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velox.jewelvault.ui.components.CusOutlinedTextField
import com.velox.jewelvault.ui.components.InputFieldState
import com.velox.jewelvault.utils.FileManager
import com.velox.jewelvault.utils.FileItem
import com.velox.jewelvault.utils.log
import kotlinx.coroutines.launch

enum class ViewType {
    LIST, GRID
}

@Composable
fun FolderScreen() {
    val context = LocalContext.current
    var folderStructure by remember { mutableStateOf<Map<String, List<FileItem>>>(emptyMap()) }
    var expandedFolders by remember { mutableStateOf<Set<String>>(setOf("Root")) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var fileToDelete by remember { mutableStateOf<FileItem?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }
    var sortType by remember { mutableStateOf(SortType.NAME) }
    var viewType by remember { mutableStateOf(ViewType.LIST) }
    var showViewMenu by remember { mutableStateOf(false) }
    
    val searchFieldState = remember { InputFieldState() }
    
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        scope.launch {
            FileManager.getFolderStructure(context).fold(
                onSuccess = { structure ->
                    folderStructure = structure
                    isLoading = false
                },
                onFailure = { error ->
                    log("Error loading folder structure: ${error.message}")
                    isLoading = false
                }
            )
        }
    }
    
    LaunchedEffect(searchQuery) {
        scope.launch {
            if (searchQuery.isNotEmpty()) {
                FileManager.searchFiles(context, searchQuery).fold(
                    onSuccess = { searchResults ->
                        // Group search results by folder
                        val groupedResults = searchResults.groupBy { file ->
                            val pathParts = file.path.split("/")
                            if (pathParts.size > 1) {
                                pathParts.dropLast(1).joinToString("/")
                            } else {
                                "Root"
                            }
                        }
                        folderStructure = groupedResults
                    },
                    onFailure = { error ->
                        log("Error searching files: ${error.message}")
                    }
                )
            } else {
                FileManager.getFolderStructure(context).fold(
                    onSuccess = { structure ->
                        folderStructure = structure
                    },
                    onFailure = { error ->
                        log("Error loading folder structure: ${error.message}")
                    }
                )
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "File Manager",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row {
                // Debug button
                IconButton(onClick = {
                    scope.launch {
                        FileManager.getAllJewelVaultFiles(context).fold(
                            onSuccess = { files ->
                                log("DEBUG: Found ${files.size} files")
                                files.forEach { file ->
                                    log("DEBUG: File: ${file.name}, Path: ${file.path}")
                                }
                            },
                            onFailure = { error ->
                                log("DEBUG: Error getting files: ${error.message}")
                            }
                        )
                    }
                }) {
                    Icon(Icons.Default.BugReport, contentDescription = "Debug")
                }
                
                // View type button
                IconButton(onClick = { showViewMenu = true }) {
                    Icon(
                        imageVector = if (viewType == ViewType.LIST) Icons.Default.GridView else Icons.Default.List,
                        contentDescription = "View Type"
                    )
                }
                
                // Sort button
                IconButton(onClick = { showSortMenu = true }) {
                    Icon(Icons.Default.Sort, contentDescription = "Sort")
                }
                
                // Refresh button
                IconButton(onClick = {
                    isLoading = true
                    scope.launch {
                        FileManager.getFolderStructure(context).fold(
                            onSuccess = { structure ->
                                folderStructure = structure
                                isLoading = false
                            },
                            onFailure = { error ->
                                log("Error refreshing folder structure: ${error.message}")
                                isLoading = false
                            }
                        )
                    }
                }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Search bar
        CusOutlinedTextField(
            state = searchFieldState,
            placeholderText = "Search files...",
            leadingIcon = Icons.Default.Search,
            onTextChange = { searchQuery = it }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Folder structure info
        if (folderStructure.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Folders: ${folderStructure.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Total Files: ${folderStructure.values.sumOf { it.size }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            // Show all folders found
            Text(
                text = "Folders found: ${folderStructure.keys.joinToString(", ")}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            // Expand/Collapse all buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = { 
                        expandedFolders = folderStructure.keys.toSet()
                    }
                ) {
                    Text("Expand All")
                }
                
                TextButton(
                    onClick = { 
                        expandedFolders = emptySet()
                    }
                ) {
                    Text("Collapse All")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // Folder structure list
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (folderStructure.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "No files",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No files found" else "No files in JewelVault folder",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            if (viewType == ViewType.LIST) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Sort folders by name
                    val sortedFolders = folderStructure.toList().sortedBy { it.first }
                    
                    items(sortedFolders) { (folderPath, files) ->
                        FolderCard(
                            folderPath = folderPath,
                            files = files,
                            isExpanded = expandedFolders.contains(folderPath),
                            onToggleExpanded = { 
                                expandedFolders = if (expandedFolders.contains(folderPath)) {
                                    expandedFolders - folderPath
                                } else {
                                    expandedFolders + folderPath
                                }
                            },
                            onFileClick = { file ->
                                FileManager.openFile(context, file)
                            },
                            onFileLongClick = { file ->
                                fileToDelete = file
                                showDeleteDialog = true
                            },
                            sortType = sortType,
                            viewType = viewType
                        )
                    }
                }
            } else {
                // Grid view for folders
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val sortedFolders = folderStructure.toList().sortedBy { it.first }
                    
                    items(sortedFolders.chunked(2)) { folderRow ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            folderRow.forEach { (folderPath, files) ->
                                Box(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    FolderCard(
                                        folderPath = folderPath,
                                        files = files,
                                        isExpanded = expandedFolders.contains(folderPath),
                                        onToggleExpanded = { 
                                            expandedFolders = if (expandedFolders.contains(folderPath)) {
                                                expandedFolders - folderPath
                                            } else {
                                                expandedFolders + folderPath
                                            }
                                        },
                                        onFileClick = { file ->
                                            FileManager.openFile(context, file)
                                        },
                                        onFileLongClick = { file ->
                                            fileToDelete = file
                                            showDeleteDialog = true
                                        },
                                        sortType = sortType,
                                        viewType = viewType
                                    )
                                }
                            }
                            // Fill empty space if odd number of folders
                            if (folderRow.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Sort menu
    DropdownMenu(
        expanded = showSortMenu,
        onDismissRequest = { showSortMenu = false }
    ) {
        SortType.values().forEach { sort ->
            DropdownMenuItem(
                text = { Text(sort.displayName) },
                onClick = {
                    sortType = sort
                    showSortMenu = false
                }
            )
        }
    }
    
    // View type menu
    DropdownMenu(
        expanded = showViewMenu,
        onDismissRequest = { showViewMenu = false }
    ) {
        ViewType.values().forEach { view ->
            DropdownMenuItem(
                text = { Text(view.name) },
                onClick = {
                    viewType = view
                    showViewMenu = false
                }
            )
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog && fileToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete File") },
            text = { Text("Are you sure you want to delete '${fileToDelete!!.name}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            FileManager.deleteFile(context, fileToDelete!!).fold(
                                onSuccess = { success ->
                                    if (success) {
                                        FileManager.getFolderStructure(context).fold(
                                            onSuccess = { structure ->
                                                folderStructure = structure
                                            },
                                            onFailure = { error ->
                                                log("Error refreshing after delete: ${error.message}")
                                            }
                                        )
                                    }
                                },
                                onFailure = { error ->
                                    log("Error deleting file: ${error.message}")
                                }
                            )
                        }
                        showDeleteDialog = false
                        fileToDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun FileItemCard(
    file: FileItem,
    onFileClick: (FileItem) -> Unit,
    onFileLongClick: (FileItem) -> Unit,
    showFolderIcon: Boolean = true,
    isCompact: Boolean = false
) {

    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onFileClick(file) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        if (isCompact) {
            // Compact grid view
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = getFileIcon(file),
                    contentDescription = file.name,
                    modifier = Modifier.size(32.dp),
                    tint = if (file.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                
                if (!file.isDirectory) {
                    Text(
                        text = file.formattedSize,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            // Regular list view
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // File icon
                if (showFolderIcon || !file.isDirectory) {
                    Icon(
                        imageVector = getFileIcon(file),
                        contentDescription = file.name,
                        modifier = Modifier.size(if (showFolderIcon) 32.dp else 24.dp),
                        tint = if (file.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    Spacer(modifier = Modifier.size(24.dp))
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // File info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = file.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (!file.isDirectory) {
                        Text(
                            text = file.formattedSize,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    
                    Text(
                        text = file.formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                
                // Action buttons
                Row {
                    if (!file.isDirectory) {
                        IconButton(
                            onClick = { FileManager.openFile(context, file) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = "Open file",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        IconButton(
                            onClick = { FileManager.shareFile(context, file) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share file",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    
                    IconButton(
                        onClick = { onFileLongClick(file) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete file",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

private fun getFileIcon(file: FileItem): ImageVector {
    return when {
        file.isDirectory -> Icons.Default.Folder
        file.name.endsWith(".pdf", ignoreCase = true) -> Icons.Default.PictureAsPdf
        file.name.endsWith(".xlsx", ignoreCase = true) || file.name.endsWith(".xls", ignoreCase = true) -> Icons.Default.TableChart
        file.name.endsWith(".csv", ignoreCase = true) -> Icons.Default.TableChart
        file.name.endsWith(".txt", ignoreCase = true) -> Icons.Default.Description
        file.name.endsWith(".jpg", ignoreCase = true) || file.name.endsWith(".jpeg", ignoreCase = true) || file.name.endsWith(".png", ignoreCase = true) -> Icons.Default.Image
        else -> Icons.Default.InsertDriveFile
    }
}

@Composable
private fun FolderCard(
    folderPath: String,
    files: List<FileItem>,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onFileClick: (FileItem) -> Unit,
    onFileLongClick: (FileItem) -> Unit,
    sortType: SortType,
    viewType: ViewType = ViewType.LIST
) {
    val sortedFiles = remember(files, sortType) {
        when (sortType) {
            SortType.NAME -> files.sortedWith(compareBy { it.name.lowercase() })
            SortType.SIZE -> files.sortedWith(compareBy { it.size })
            SortType.DATE -> files.sortedWith(compareByDescending { it.lastModified })
            SortType.TYPE -> files.sortedWith(compareBy { it.mimeType ?: "" })
        }
    }
    
    val folderName = if (folderPath == "Root") "JewelVault" else folderPath.split("/").lastOrNull() ?: folderPath
    val fileCount = files.size
    val totalSize = files.sumOf { it.size }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column {
            // Folder header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpanded() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = folderName,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = folderName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$fileCount files • ${FileManager.formatFileSize(totalSize)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            
            // Files list (when expanded)
            if (isExpanded && sortedFiles.isNotEmpty()) {
                Divider()
                if (viewType == ViewType.LIST) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                    ) {
                        items(sortedFiles) { file ->
                            FileItemCard(
                                file = file,
                                onFileClick = onFileClick,
                                onFileLongClick = onFileLongClick,
                                showFolderIcon = false
                            )
                        }
                    }
                } else {
                    // Grid view for files within folders
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sortedFiles) { file ->
                            FileItemCard(
                                file = file,
                                onFileClick = onFileClick,
                                onFileLongClick = onFileLongClick,
                                showFolderIcon = false,
                                isCompact = true
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun sortFiles(files: List<FileItem>, sortType: SortType): List<FileItem> {
    return when (sortType) {
        SortType.NAME -> files.sortedWith(compareBy<FileItem> { !it.isDirectory }.thenBy { it.name.lowercase() })
        SortType.SIZE -> files.sortedWith(compareBy<FileItem> { !it.isDirectory }.thenBy { it.size })
        SortType.DATE -> files.sortedWith(compareBy<FileItem> { !it.isDirectory }.thenByDescending { it.lastModified })
        SortType.TYPE -> files.sortedWith(compareBy<FileItem> { !it.isDirectory }.thenBy { it.mimeType ?: "" })
    }
}


enum class SortType(val displayName: String) {
    NAME("Name"),
    SIZE("Size"),
    DATE("Date"),
    TYPE("Type")
}
