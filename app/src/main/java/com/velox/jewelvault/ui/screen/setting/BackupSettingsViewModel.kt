package com.velox.jewelvault.ui.screen.setting

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.storage.FirebaseStorage
import com.velox.jewelvault.data.DataStoreManager
import com.velox.jewelvault.data.roomdb.AppDatabase
import com.velox.jewelvault.utils.backup.*
import com.velox.jewelvault.utils.backup.BackupFrequency
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

/**
 * ViewModel for backup settings screen
 */
@HiltViewModel
class BackupSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val storage: FirebaseStorage,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    /**
     * return Triple of Flow<String> for userId, userName, mobileNo
     * */
    val admin: Triple<Flow<String>, Flow<String>, Flow<String>> = dataStoreManager.getAdminInfo()

    
    private val backupManager = BackupManager(context, database, storage, dataStoreManager)
    private val backupScheduler = BackupScheduler(context)
    
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
        log("BackupSettingsViewModel: Starting to collect backup and restore progress from SharedFlow")
        viewModelScope.launch {
            try {
                BackupService.progressFlow.collect { progress ->
                    log("BackupSettingsViewModel: Received progress update - message: '${progress.message}', progress: ${progress.progress}, isComplete: ${progress.isComplete}, isSuccess: ${progress.isSuccess}")
                    
                    if (progress.isComplete) {
                        // Handle completion
                        onBackupCompleted(progress.isSuccess, progress.message)
                    } else {
                        // Handle progress update (for both backup and restore)
                        updateBackupProgress(progress.message, progress.progress)
                    }
                }
            } catch (e: Exception) {
                log("BackupSettingsViewModel: Error collecting backup/restore progress: ${e.message}")
            }
        }
    }
    

    
    private fun updateBackupProgress(message: String, progress: Int) {
        log("BackupSettingsViewModel: Updating backup progress - message: '$message', progress: $progress")
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
                // Load backup frequency from DataStore
                dataStoreManager.backupFrequency.collect { frequencyString ->
                    val frequency = try {
                        BackupFrequency.valueOf(frequencyString)
                    } catch (e: Exception) {
                        BackupFrequency.WEEKLY // Default
                    }
                    
                    _uiState.update { it.copy(backupFrequency = frequency) }
                    
                    // Schedule automatic backup based on saved frequency
                    backupScheduler.scheduleAutomaticBackup(frequency)
                }
                
            } catch (e: Exception) {
                log("Error loading backup settings: ${e.message}")
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
                    val result = backupManager.getAvailableBackups(userMobile)
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
                log("Error loading available backups: ${e.message}")
            }
        }
    }
    
    fun setBackupFrequency(frequency: BackupFrequency) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(backupFrequency = frequency) }
                
                // Save to DataStore
                dataStoreManager.setBackupFrequency(frequency.name)
                
                // Update scheduled backup
                backupScheduler.scheduleAutomaticBackup(frequency)
                
                _uiState.update { 
                    it.copy(
                        statusMessage = "Automatic backup ${frequency.name.lowercase()} scheduled",
                        showResultMessage = true
                    )
                }
                
            } catch (e: Exception) {
                log("Error setting backup frequency: ${e.message}")
                _uiState.update { 
                    it.copy(
                        statusMessage = "Failed to schedule backup: ${e.message}",
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
                        progressMessage = "Starting backup...",
                        progressPercent = 0
                    )
                }
                
                // Start backup service
                BackupService.startBackup(context)
                
            } catch (e: Exception) {
                log("Error starting backup: ${e.message}")
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        statusMessage = "Failed to start backup: ${e.message}",
                        showResultMessage = true
                    )
                }
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
                    BackupService.startRestore(context, userMobile, restoreMode)
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
        loadAvailableBackups() // Refresh backup list
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
            loadAvailableBackups() // Refresh backup list
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
            loadAvailableBackups() // Refresh backup list
            // Note: The UI should automatically refresh when navigating to other screens
            // since the database has been updated with new data
        }
    }
    
    fun clearResultMessage() {
        _uiState.update { it.copy(showResultMessage = false) }
    }

    /**
     * Check if Firebase backup exists for current user
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
                
                val result = backupManager.checkFirebaseBackupExists(userMobile)
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
                val result = backupManager.validateExcelFile(fileUri)
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
                BackupService.startRestoreWithSource(context, userMobile, restoreSource, localFileUri, restoreMode)
                
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
     * Get default backup folder path
     */
    fun getDefaultBackupFolder(): String {
        return backupManager.getDefaultBackupFolder()
    }
}

/**
 * UI state for backup settings screen
 */
data class BackupSettingsUiState(
    val backupFrequency: BackupFrequency = BackupFrequency.WEEKLY,
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