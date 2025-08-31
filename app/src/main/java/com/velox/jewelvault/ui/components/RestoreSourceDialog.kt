package com.velox.jewelvault.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.velox.jewelvault.utils.backup.FileValidationResult
import com.velox.jewelvault.utils.backup.RestoreMode
import com.velox.jewelvault.utils.backup.RestoreSource

/**
 * Dialog for selecting restore source (Firebase or Local file)
 */
@Composable
fun RestoreSourceDialog(
    onDismiss: () -> Unit,
    onRestore: (RestoreSource, Uri?, RestoreMode) -> Unit,
    checkFirebaseBackup: (RestoreSource, (Boolean, String?) -> Unit) -> Unit,
    validateLocalFile: (Uri, (FileValidationResult) -> Unit) -> Unit,
    defaultBackupFolder: String
) {
    var selectedSource by remember { mutableStateOf<RestoreSource?>(null) }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedMode by remember { mutableStateOf(RestoreMode.MERGE) }
    var isCheckingFirebase by remember { mutableStateOf(false) }
    var isSelectingFile by remember { mutableStateOf(false) }
    var firebaseCheckResult by remember { mutableStateOf<Pair<Boolean, String?>?>(null) }
    var fileValidationResult by remember { mutableStateOf<FileValidationResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        isSelectingFile = false
        if (uri != null) {
            selectedFileUri = uri
            // Validate the selected file
            validateLocalFile(uri) { result ->
                fileValidationResult = result
                if (!result.isValid) {
                    errorMessage = result.message
                } else {
                    errorMessage = null
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Text(
                    text = "Restore Data",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Source Selection
                Text(
                    text = "Choose Restore Source:",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Firebase Option
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedSource == RestoreSource.FIREBASE) 
                            MaterialTheme.colorScheme.primaryContainer 
                        else 
                            MaterialTheme.colorScheme.surface
                    ),
                    onClick = {
                        selectedSource = RestoreSource.FIREBASE
                        selectedFileUri = null
                        fileValidationResult = null
                        errorMessage = null
                        // Check Firebase backup availability
                        isCheckingFirebase = true
                        checkFirebaseBackup(RestoreSource.FIREBASE) { exists, error ->
                            isCheckingFirebase = false
                            firebaseCheckResult = Pair(exists, error)
                            if (!exists) {
                                errorMessage = error ?: "No backup files found in Firebase"
                            }
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedSource == RestoreSource.FIREBASE,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Download from Firebase",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Restore from your cloud backup",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (isCheckingFirebase) {
                                Text(
                                    text = "Checking availability...",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else if (firebaseCheckResult != null) {
                                val (exists, _) = firebaseCheckResult!!
                                Text(
                                    text = if (exists) "✓ Backup available" else "✗ No backup found",
                                    fontSize = 12.sp,
                                    color = if (exists) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                // Local File Option
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedSource == RestoreSource.LOCAL) 
                            MaterialTheme.colorScheme.primaryContainer 
                        else 
                            MaterialTheme.colorScheme.surface
                    ),
                    onClick = {
                        selectedSource = RestoreSource.LOCAL
                        firebaseCheckResult = null
                        errorMessage = null
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedSource == RestoreSource.LOCAL,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Select Local File",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Choose an Excel backup file from your device",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Default location: $defaultBackupFolder",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // File Selection for Local Option
                if (selectedSource == RestoreSource.LOCAL) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = {
                            isSelectingFile = true
                            filePickerLauncher.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Select Excel File (.xlsx)")
                    }

                    // Show selected file info
                    if (selectedFileUri != null) {
                        val uri = selectedFileUri!!
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = "Selected File:",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = uri.lastPathSegment ?: "Unknown file",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                // Show validation result
                                if (fileValidationResult != null) {
                                    val result = fileValidationResult!!
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (result.isValid) "✓ File validated" else "✗ ${result.message}",
                                        fontSize = 12.sp,
                                        color = if (result.isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }

                // Restore Mode Selection
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Restore Mode:",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // MERGE Mode
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        RadioButton(
                            selected = selectedMode == RestoreMode.MERGE,
                            onClick = { selectedMode = RestoreMode.MERGE }
                        )
                        Text(
                            text = "Merge",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Add new data,\nskip existing",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // REPLACE Mode
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        RadioButton(
                            selected = selectedMode == RestoreMode.REPLACE,
                            onClick = { selectedMode = RestoreMode.REPLACE }
                        )
                        Text(
                            text = "Replace",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Replace all data\nexcept current user",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // Error Message
                if (errorMessage != null) {
                    val error = errorMessage!!
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = error,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // Action Buttons
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Button(
                        onClick = {
                            val currentSource = selectedSource
                            if (currentSource != null) {
                                onRestore(currentSource, selectedFileUri, selectedMode)
                            }
                        },
                        enabled = selectedSource != null && 
                                (selectedSource == RestoreSource.FIREBASE && 
                                 (firebaseCheckResult?.first == true || firebaseCheckResult == null)) ||
                                (selectedSource == RestoreSource.LOCAL && 
                                 selectedFileUri != null && 
                                 fileValidationResult?.isValid == true),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Start Restore")
                    }
                }
            }
        }
    }
}
