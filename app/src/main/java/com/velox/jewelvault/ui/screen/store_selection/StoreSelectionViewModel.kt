package com.velox.jewelvault.ui.screen.store_selection

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.velox.jewelvault.data.DataStoreManager
import com.velox.jewelvault.data.firebase.FirebaseUtils
import com.velox.jewelvault.data.roomdb.AppDatabase
import com.velox.jewelvault.data.roomdb.entity.StoreEntity
import com.velox.jewelvault.utils.StoreDeviceSessionManager
import com.velox.jewelvault.utils.log
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StoreSelectionViewModel @Inject constructor(
    private val appDatabase: AppDatabase,
    private val dataStoreManager: DataStoreManager,
    private val firestore: FirebaseFirestore,
    private val storeDeviceSessionManager: StoreDeviceSessionManager
) : ViewModel() {

    val stores = mutableStateListOf<StoreEntity>()
    val isLoading = mutableStateOf(false)
    val isSelecting = mutableStateOf(false)
    val hasNoStores = mutableStateOf(false)
    val requiresManualSelection = mutableStateOf(false)
    val navigateToStartLoading = mutableStateOf(false)
    val blockedDeviceInfo = mutableStateOf<StoreDeviceSessionManager.ActiveDeviceInfo?>(null)
    val errorMessage = mutableStateOf<String?>(null)

    private val hasLoaded = mutableStateOf(false)

    fun loadStores() {
        if (hasLoaded.value) return
        hasLoaded.value = true
        viewModelScope.launch {
            isLoading.value = true
            hasNoStores.value = false
            requiresManualSelection.value = false
            errorMessage.value = null

            try {
                val adminMobile = resolveAdminMobile()
                if (adminMobile.isBlank()) {
                    hasNoStores.value = true
                    return@launch
                }
                val savedStoreId = dataStoreManager.getSelectedStoreInfo().first.first().trim()

                val mergedStores = loadMergedStores(adminMobile)
                    .filter { it.storeId.isNotBlank() }
                    .sortedBy { it.name.lowercase() }

                stores.clear()
                stores.addAll(mergedStores)

                when {
                    mergedStores.isEmpty() -> {
                        hasNoStores.value = true
                    }

                    savedStoreId.isNotBlank() -> {
                        val savedStore = mergedStores.firstOrNull { it.storeId == savedStoreId }
                        when {
                            savedStore != null -> {
                                selectStoreInternal(savedStore, adminMobile)
                            }

                            mergedStores.size == 1 -> {
                                selectStoreInternal(mergedStores.first(), adminMobile)
                            }

                            else -> {
                                requiresManualSelection.value = true
                            }
                        }
                    }

                    mergedStores.size == 1 -> {
                        selectStoreInternal(mergedStores.first(), adminMobile)
                    }

                    else -> {
                        requiresManualSelection.value = true
                    }
                }
            } catch (e: Exception) {
                errorMessage.value = "Failed to load stores"
                log("StoreSelection: loadStores failed -> ${e.message}")
            } finally {
                isLoading.value = false
            }
        }
    }

    fun selectStore(store: StoreEntity) {
        if (isSelecting.value) return
        viewModelScope.launch {
            val adminMobile = resolveAdminMobile()
            if (adminMobile.isBlank()) {
                errorMessage.value = "Unable to resolve admin user"
                return@launch
            }
            selectStoreInternal(store, adminMobile)
        }
    }

    fun dismissBlockedDeviceDialog() {
        blockedDeviceInfo.value = null
    }

    fun consumeNavigation() {
        navigateToStartLoading.value = false
    }

    fun consumeError() {
        errorMessage.value = null
    }

    private suspend fun selectStoreInternal(store: StoreEntity, adminMobile: String) {
        isSelecting.value = true
        blockedDeviceInfo.value = null
        errorMessage.value = null

        try {
            when (val result = storeDeviceSessionManager.checkAndActivate(adminMobile, store.storeId)) {
                StoreDeviceSessionManager.CheckResult.Allowed -> {
                    saveSelectedStore(store)
                    navigateToStartLoading.value = true
                }

                is StoreDeviceSessionManager.CheckResult.Blocked -> {
                    blockedDeviceInfo.value = result.info
                    requiresManualSelection.value = true
                }

                is StoreDeviceSessionManager.CheckResult.Error -> {
                    log("StoreSelection: device check failed, allowing login -> ${result.message}")
                    saveSelectedStore(store)
                    navigateToStartLoading.value = true
                }
            }
        } catch (e: Exception) {
            log("StoreSelection: selectStoreInternal failed -> ${e.message}")
            errorMessage.value = "Unable to select store right now"
        } finally {
            isSelecting.value = false
        }
    }

    private suspend fun saveSelectedStore(store: StoreEntity) {
        dataStoreManager.saveSelectedStoreInfo(store.storeId, store.upiId, store.name)
        dataStoreManager.setUpiId(store.upiId)
        dataStoreManager.setMerchantName(store.name)
    }

    private suspend fun resolveAdminMobile(): String {
        val adminUser = appDatabase.userDao().getAdminUser()
        if (!adminUser?.mobileNo.isNullOrBlank()) {
            return adminUser?.mobileNo ?: ""
        }

        val adminFromPrefs = dataStoreManager.getAdminInfo().third.first()
        if (adminFromPrefs.isNotBlank()) {
            return adminFromPrefs
        }

        return dataStoreManager.getCurrentLoginUser().mobileNo
    }

    private suspend fun loadMergedStores(adminMobile: String): List<StoreEntity> {
        val merged = linkedMapOf<String, StoreEntity>()

        appDatabase.storeDao().getAllStores().forEach { localStore ->
            merged[localStore.storeId] = localStore
        }

        val cloudStoresResult = FirebaseUtils.getAllStores(firestore, adminMobile)
        if (cloudStoresResult.isSuccess) {
            cloudStoresResult.getOrNull().orEmpty().forEach { (storeId, storeMap) ->
                val remoteStore = FirebaseUtils.mapToStoreEntity(storeMap).copy(storeId = storeId)
                val localStore = merged[storeId]
                val shouldUpdateLocal = localStore == null ||
                    remoteStore.lastUpdated > localStore.lastUpdated ||
                    localStore.lastUpdated == 0L

                if (shouldUpdateLocal) {
                    upsertStore(remoteStore)
                    merged[storeId] = remoteStore
                }
            }
        }

        return merged.values.toList()
    }

    private suspend fun upsertStore(store: StoreEntity) {
        val existingStore = appDatabase.storeDao().getStoreById(store.storeId)
        if (existingStore != null) {
            appDatabase.storeDao().updateStore(store)
        } else {
            appDatabase.storeDao().insertStore(store)
        }
    }
}
