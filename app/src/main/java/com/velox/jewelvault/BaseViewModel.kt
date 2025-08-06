package com.velox.jewelvault

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import com.velox.jewelvault.data.MetalRate
import com.velox.jewelvault.data.UpdateInfo
import com.velox.jewelvault.data.fetchAllMetalRates
import com.velox.jewelvault.data.roomdb.AppDatabase
import com.velox.jewelvault.data.DataStoreManager
import com.velox.jewelvault.utils.AppUpdateManager
import com.velox.jewelvault.utils.RemoteConfigManager
import com.velox.jewelvault.utils.backup.BackupManager
import com.velox.jewelvault.utils.ioLaunch
import com.velox.jewelvault.utils.log
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@HiltViewModel
class BaseViewModel @Inject constructor(
    private val _dataStoreManager: DataStoreManager,
    private val _loadingState : MutableState<Boolean>,
    private val _snackBarState: MutableState<String>,
    private val _metalRates: SnapshotStateList<MetalRate>,
    private val appDatabase: AppDatabase,
    private val _remoteConfigManager: RemoteConfigManager,
    private val _appUpdateManager: AppUpdateManager,
    private val _backupManager: BackupManager
) : ViewModel() {

    var loading by _loadingState
    var snackMessage by _snackBarState
    val dataStoreManager = _dataStoreManager
    val metalRates = _metalRates
    val metalRatesLoading = mutableStateOf(false)
    val isConnectedState = mutableStateOf(false)
    val storeImage = mutableStateOf<String?>(null)
    val storeName = mutableStateOf<String?>(null)
    
    // Update management
    val remoteConfigManager = _remoteConfigManager
    val appUpdateManager = _appUpdateManager
    val backupManager = _backupManager
    val updateInfo = mutableStateOf<UpdateInfo?>(null)
    val showUpdateDialog = mutableStateOf(false)
    val showForceUpdateDialog = mutableStateOf(false)
    val updateCheckLoading = mutableStateOf(false)

    suspend fun refreshMetalRates(state: String = "visakhapatnam", context: Context) {
        metalRates.clear()
        metalRates.addAll(fetchAllMetalRates(state, context,metalRatesLoading))
    }

    fun loadStoreImage() {
        ioLaunch {
            try {
                val storeId = _dataStoreManager.getSelectedStoreInfo().first.first()
                val store = appDatabase.storeDao().getStoreById(storeId)
                log("Loading store image: ${store?.image}")
                storeImage.value = store?.image
            } catch (e: Exception) {
                log("Error loading store image: ${e.message}")
            }
        }
    }

    fun loadStoreName() {
        ioLaunch {
            try {
                val storeId = _dataStoreManager.getSelectedStoreInfo().first.first()
                val store = appDatabase.storeDao().getStoreById(storeId)
                log("Loading store image: ${store?.name}")
                storeName.value = store?.name
            } catch (e: Exception) {
                log("Error loading store image: ${e.message}")
            }
        }
    }
    
    // Update management functions
    fun checkForUpdates(context: Context, forceCheck: Boolean = false) {
        log("🚀 Starting update check process...")
        log("🚀 Force check: $forceCheck")
        
        ioLaunch {
            try {
                updateCheckLoading.value = true
                log("🚀 Update check loading set to true")
                
                // Check if we should check for updates
                if (!forceCheck && !remoteConfigManager.shouldCheckForUpdate()) {
                    log("⏰ Skipping update check - using cached data")
                    // Use cached update info if available
                    val cachedInfo = remoteConfigManager.getCachedUpdateInfo()
                    if (cachedInfo != null) {
                        log("📋 Using cached update info: $cachedInfo")
                        handleUpdateInfo(cachedInfo)
                    } else {
                        log("📋 No cached update info available")
                    }
                    updateCheckLoading.value = false
                    return@ioLaunch
                }
                
                log("🔄 Fetching fresh update info from Remote Config...")
                // Fetch fresh update info
                val fetchResult = remoteConfigManager.fetchAndActivate()
                if (fetchResult.isSuccess) {
                    log("✅ Remote Config fetch successful")
                    val info = remoteConfigManager.getUpdateInfo()
                    log("📋 Got update info: $info")
                    remoteConfigManager.cacheUpdateInfo(info)
                    log("💾 Cached update info")
                    remoteConfigManager.updateLastCheckTime()
                    log("⏰ Updated last check time")
                    handleUpdateInfo(info)
                } else {
                    log("❌ Remote Config fetch failed: ${fetchResult.exceptionOrNull()?.message}")
                    // Use cached info if fetch fails
                    val cachedInfo = remoteConfigManager.getCachedUpdateInfo()
                    if (cachedInfo != null) {
                        log("📋 Using cached update info after fetch failure: $cachedInfo")
                        handleUpdateInfo(cachedInfo)
                    } else {
                        log("📋 No cached update info available after fetch failure")
                    }
                }
            } catch (e: Exception) {
                log("❌ Error checking for updates: ${e.message}")
                log("❌ Exception details: ${e.javaClass.simpleName}")
                e.printStackTrace()
            } finally {
                updateCheckLoading.value = false
                log("🚀 Update check loading set to false")
            }
        }
    }
    
    private suspend fun handleUpdateInfo(info: UpdateInfo) {
        log("🎯 Handling update info: $info")
        updateInfo.value = info
        
        // Check if force update is required
        val isForceRequired = remoteConfigManager.isForceUpdateRequired()
        log("🔍 Force update required: $isForceRequired")
        if (isForceRequired) {
            log("🚨 Showing force update dialog")
            showForceUpdateDialog.value = true
            return
        }
        
        // Check if update is available
        val isUpdateAvailable = remoteConfigManager.isUpdateAvailable()
        log("🔍 Update available: $isUpdateAvailable")
        if (isUpdateAvailable) {
            log("📱 Showing update dialog")
            showUpdateDialog.value = true
        } else {
            log("📱 No update available - not showing dialog")
        }
    }
    
    fun onUpdateClick(context: Context) {
        log("🔄 Update button clicked")
        val info = updateInfo.value
        if (info != null) {
            log("📱 Opening Play Store with update info: $info")
            appUpdateManager.openPlayStore(context, info)
        } else {
            log("📱 Opening Play Store without update info")
            appUpdateManager.openPlayStore(context)
        }
    }
    
    fun dismissUpdateDialog() {
        showUpdateDialog.value = false
    }
    
    fun dismissForceUpdateDialog() {
        showForceUpdateDialog.value = false
    }

}