package com.velox.jewelvault.ui.screen.setting

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.firestore.FirebaseFirestore
import com.velox.jewelvault.data.DataStoreManager
import com.velox.jewelvault.data.roomdb.AppDatabase
import com.velox.jewelvault.utils.sync.*
import com.velox.jewelvault.utils.sync.SyncFrequency
import com.velox.jewelvault.utils.log
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Named

/**
 * ViewModel for sync settings screen
 */
@HiltViewModel
class BackupSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val storage: FirebaseStorage,
    private val firestore: FirebaseFirestore,
    private val dataStoreManager: DataStoreManager,
    @Named("snackMessage") private val _snackBarState: MutableState<String>,
    @Named("currentScreenHeading") private val _currentScreenHeadingState: MutableState<String>,
) : ViewModel() {

    val currentScreenHeadingState = _currentScreenHeadingState

    /**
     * return Triple of Flow<String> for userId, userName, mobileNo
     * */
    val admin: Triple<Flow<String>, Flow<String>, Flow<String>> = dataStoreManager.getAdminInfo()

    
    private val syncManager = SyncManager(context, database, storage, dataStoreManager, firestore)
    private val syncScheduler = SyncScheduler(context)
    
    private val _uiState = MutableStateFlow(BackupSettingsUiState())
    val uiState: StateFlow<BackupSettingsUiState> = _uiState.asStateFlow()
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    
    init {
        log("BackupSettingsViewModel: ViewModel initialized")
        loadBackupSettings()
        loadAvailableBackups()
        collectBackupProgress()
        log("BackupSettingsViewModel: Init completed")
    }
    
    private fun collectBackupProgress() {
        log("BackupSettingsViewModel: Starting to collect sync and restore progress from SharedFlow")
        viewModelScope.launch {
            try {
                SyncService.progressFlow.collect { progress ->
                    log("BackupSettingsViewModel: Received progress update - message: '${progress.message}', progress: ${progress.progress}, isComplete: ${progress.isComplete}, isSuccess: ${progress.isSuccess}")
                    
                    if (progress.isComplete) {
                        // Handle completion
                        onBackupCompleted(progress.isSuccess, progress.message)
                    } else {
                        // Handle progress update (for both sync and restore)
                        updateBackupProgress(progress.message, progress.progress)
                    }
                }
            } catch (e: Exception) {
                log("BackupSettingsViewModel: Error collecting sync/restore progress: ${e.message}")
            }
        }
    }
    

    
    private fun updateBackupProgress(message: String, progress: Int) {
        log("BackupSettingsViewModel: Updating sync progress - message: '$message', progress: $progress")
        _uiState.update { 
            it.copy(
                progressMessage = message,
                progressPercent = progress
            )
        }
        log("BackupSettingsViewModel: Backup progress updated in UI state")
    }
    
    private fun updateRestoreProgress(message: String, progress: Int) {
        log("BackupSettingsViewModel: Updating restore progress - message: '$message', progress: $progress")
        _uiState.update { 
            it.copy(
                progressMessage = message,
                progressPercent = progress
            )
        }
        log("BackupSettingsViewModel: Restore progress updated in UI state")
    }
    
    private fun loadBackupSettings() {
        viewModelScope.launch {
            try {
                // Load sync frequency from DataStore
                dataStoreManager.syncFrequency.collect { frequencyString ->
                    val frequency = try {
                        SyncFrequency.valueOf(frequencyString)
                    } catch (e: Exception) {
                        SyncFrequency.WEEKLY // Default
                    }
                    
                    dataStoreManager.setValue(DataStoreManager.SYNC_INTERVAL_MINUTES, frequency.intervalMinutes)
                    _uiState.update { it.copy(syncFrequency = frequency) }
                    
                    // Schedule automatic sync based on saved frequency
                    syncScheduler.scheduleAutomaticBackup(frequency)
                }
                
            } catch (e: Exception) {
                log("Error loading sync settings: ${e.message}")
            }
        }
    }
    
    private fun loadAvailableBackups() {
        viewModelScope.launch {
            try {
                val userId = admin.first.first()
                val userData = database.userDao().getUserById(userId)
                val userMobile = userData?.mobileNo ?: ""
                
                if (userMobile.isNotEmpty()) {
                    val result = syncManager.getAvailableBackups(userMobile)
                    if (result.isSuccess) {
                        val backups = result.getOrNull() ?: emptyList()
                        _uiState.update { 
                            it.copy(
                                availableBackups = backups,
                                lastBackupDate = if (backups.isNotEmpty()) 
                                    dateFormat.format(backups.first().uploadDate) 
                                else "",
                                lastBackupSuccess = backups.isNotEmpty()
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                log("Error loading available syncs: ${e.message}")
            }
        }
    }
    
    fun setBackupFrequency(frequency: SyncFrequency) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(syncFrequency = frequency) }
                
                // Save to DataStore
                dataStoreManager.setBackupFrequency(frequency.name)
                dataStoreManager.setValue(DataStoreManager.SYNC_INTERVAL_MINUTES, frequency.intervalMinutes)
                
                // Update scheduled sync
                syncScheduler.scheduleAutomaticBackup(frequency)
                
                _uiState.update { 
                    it.copy(
                        statusMessage = "Automatic sync ${frequency.name.lowercase()} scheduled",
                        showResultMessage = true
                    )
                }
                
            } catch (e: Exception) {
                log("Error setting sync frequency: ${e.message}")
                _uiState.update { 
                    it.copy(
                        statusMessage = "Failed to schedule sync: ${e.message}",
                        showResultMessage = true
                    )
                }
            }
        }
    }
    
    fun startBackup() {
        viewModelScope.launch {
            try {
                log("BackupSettingsViewModel: startBackup called")
                _uiState.update { 
                    it.copy(
                        isLoading = true,
                        progressMessage = "Starting sync...",
                        progressPercent = 0
                    )
                }
                
                // Start sync service
                SyncService.startBackup(context)
                
            } catch (e: Exception) {
                log("Error starting sync: ${e.message}")
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        statusMessage = "Failed to start sync: ${e.message}",
                        showResultMessage = true
                    )
                }
            }
        }
    }

    fun startLocalExport() {
        viewModelScope.launch {
            try {
                _uiState.update {
                    it.copy(
                        isLoading = true,
                        progressMessage = "Starting local export...",
                        progressPercent = 0
                    )
                }

                val result = syncManager.performLocalExport { message, progress ->
                    updateBackupProgress(message, progress)
                }

                if (result.isSuccess) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            statusMessage = "Export saved to Downloads/JewelVault/Sync",
                            showResultMessage = true
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            statusMessage = "Local export failed: ${result.exceptionOrNull()?.message ?: "Unknown error"}",
                            showResultMessage = true
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        statusMessage = "Local export failed: ${e.message}",
                        showResultMessage = true
                    )
                }
            }
        }
    }

    fun startLocalImport(localFileUri: Uri, restoreMode: RestoreMode = RestoreMode.MERGE) {
        viewModelScope.launch {
            try {
                _uiState.update {
                    it.copy(
                        isLoading = true,
                        progressMessage = "Starting local import...",
                        progressPercent = 0
                    )
                }

                val result = syncManager.performLocalImport(localFileUri, restoreMode) { message, progress ->
                    updateBackupProgress(message, progress)
                }

                if (result.isSuccess) {
                    onRestoreCompleted(true, result.getOrNull()?.message ?: "Import completed")
                } else {
                    onRestoreCompleted(false, result.exceptionOrNull()?.message ?: "Local import failed")
                }
            } catch (e: Exception) {
                onRestoreCompleted(false, "Local import failed: ${e.message}")
            }
        }
    }
    
    fun startRestore(fileName: String, restoreMode: RestoreMode = RestoreMode.MERGE) {
        viewModelScope.launch {
            try {
                log("BackupSettingsViewModel: startRestore called for file: $fileName with mode: $restoreMode")
                _uiState.update { 
                    it.copy(
                        isLoading = true,
                        progressMessage = "Starting restore...",
                        progressPercent = 0,
                        showBackupDialog = false
                    )
                }
                
                val userId = admin.first.first()
                val userData = database.userDao().getUserById(userId)
                val userMobile = userData?.mobileNo ?: ""
                
                if (userMobile.isNotEmpty()) {
                    // Start restore service with specified mode
                    SyncService.startRestore(context, userMobile, restoreMode)
                } else {
                    throw Exception("User mobile number not found")
                }
                
            } catch (e: Exception) {
                log("Error starting restore: ${e.message}")
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        statusMessage = "Failed to start restore: ${e.message}",
                        showResultMessage = true
                    )
                }
            }
        }
    }
    
    fun showBackupDialog() {
        _uiState.update { it.copy(showBackupDialog = true) }
        loadAvailableBackups() // Refresh sync list
    }
    
    fun hideBackupDialog() {
        _uiState.update { it.copy(showBackupDialog = false) }
    }
    
    fun onBackupCompleted(success: Boolean, message: String) {
        _uiState.update { 
            it.copy(
                isLoading = false,
                lastBackupSuccess = success,
                statusMessage = message,
                showResultMessage = true,
                lastBackupDate = if (success) dateFormat.format(Date()) else it.lastBackupDate
            )
        }
        
        if (success) {
            loadAvailableBackups() // Refresh sync list
        }
    }
    
    fun onRestoreCompleted(success: Boolean, message: String) {
        _uiState.update { 
            it.copy(
                isLoading = false,
                statusMessage = message,
                showResultMessage = true
            )
        }
        
        if (success) {
            // Refresh data after successful restore to update UI
            loadAvailableBackups() // Refresh sync list
            // Note: The UI should automatically refresh when navigating to other screens
            // since the database has been updated with new data
        }
    }
    
    fun clearResultMessage() {
        _uiState.update { it.copy(showResultMessage = false) }
    }

    /**
     * Check if Firebase sync exists for current user
     */
    fun checkFirebaseBackupExists(onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val userId = admin.first.first()
                val userData = database.userDao().getUserById(userId)
                val userMobile = userData?.mobileNo ?: ""
                
                if (userMobile.isEmpty()) {
                    onResult(false, "User mobile number not found")
                    return@launch
                }
                
                val result = syncManager.checkFirebaseBackupExists(userMobile)
                if (result.isSuccess) {
                    val exists = result.getOrNull() ?: false
                    onResult(exists, null)
                } else {
                    onResult(false, result.exceptionOrNull()?.message ?: "Unknown error")
                }
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }

    /**
     * Validate local Excel file for import
     */
    fun validateLocalFile(fileUri: Uri, onResult: (FileValidationResult) -> Unit) {
        viewModelScope.launch {
            try {
                val result = syncManager.validateExcelFile(fileUri)
                onResult(result)
            } catch (e: Exception) {
                onResult(FileValidationResult(false, "Validation failed: ${e.message}"))
            }
        }
    }

    /**
     * Start restore with source selection
     */
    fun startRestoreWithSource(
        restoreSource: RestoreSource,
        localFileUri: Uri? = null,
        restoreMode: RestoreMode = RestoreMode.MERGE
    ) {
        viewModelScope.launch {
            try {
                log("BackupSettingsViewModel: startRestoreWithSource called with source: $restoreSource, mode: $restoreMode")
                _uiState.update { 
                    it.copy(
                        isLoading = true,
                        progressMessage = "Starting restore...",
                        progressPercent = 0,
                        showRestoreSourceDialog = false,
                        showBackupDialog = false
                    )
                }
                
                val userId = admin.first.first()
                val userData = database.userDao().getUserById(userId)
                val userMobile = userData?.mobileNo ?: ""
                
                if (userMobile.isEmpty()) {
                    throw Exception("User mobile number not found")
                }
                
                // Start restore service with source selection
                SyncService.startRestoreWithSource(context, userMobile, restoreSource, localFileUri, restoreMode)
                
            } catch (e: Exception) {
                log("Error starting restore with source: ${e.message}")
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        statusMessage = "Failed to start restore: ${e.message}",
                        showResultMessage = true
                    )
                }
            }
        }
    }

    fun showRestoreSourceDialog() {
        _uiState.update { it.copy(showRestoreSourceDialog = true) }
    }

    fun hideRestoreSourceDialog() {
        _uiState.update { it.copy(showRestoreSourceDialog = false) }
    }
    
    /**
     * Get default sync folder path
     */
    fun getDefaultBackupFolder(): String {
        return syncManager.getDefaultBackupFolder()
    }
}

/**
 * UI state for sync settings screen
 */
data class BackupSettingsUiState(
    val syncFrequency: SyncFrequency = SyncFrequency.WEEKLY,
    val availableBackups: List<BackupInfo> = emptyList(),
    val isLoading: Boolean = false,
    val progressMessage: String = "",
    val progressPercent: Int = 0,
    val lastBackupDate: String = "",
    val lastBackupSuccess: Boolean = true,
    val statusMessage: String = "",
    val showBackupDialog: Boolean = false,
    val showRestoreSourceDialog: Boolean = false,
    val showResultMessage: Boolean = false
)
