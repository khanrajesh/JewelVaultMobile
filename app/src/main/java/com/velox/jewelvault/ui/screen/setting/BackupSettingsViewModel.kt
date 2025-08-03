package com.velox.jewelvault.ui.screen.setting

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.storage.FirebaseStorage
import com.velox.jewelvault.data.DataStoreManager
import com.velox.jewelvault.data.roomdb.AppDatabase
import com.velox.jewelvault.utils.backup.*
import com.velox.jewelvault.utils.log
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
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
    
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    
    init {
        loadBackupSettings()
        loadAvailableBackups()
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
    
    fun startRestore(fileName: String) {
        viewModelScope.launch {
            try {
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
                    // Start restore service
                    BackupService.startRestore(context, userMobile)
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
    }
    
    fun clearResultMessage() {
        _uiState.update { it.copy(showResultMessage = false) }
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
    val showResultMessage: Boolean = false
)