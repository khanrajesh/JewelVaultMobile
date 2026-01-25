package com.velox.jewelvault.ui.screen.start_loading

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velox.jewelvault.data.DataStoreManager
import com.velox.jewelvault.data.FeatureDefaults
import com.velox.jewelvault.data.FeatureListState
import com.velox.jewelvault.data.SubscriptionState
import com.velox.jewelvault.data.firebase.FirebaseUtils
import com.velox.jewelvault.data.roomdb.AppDatabase
import com.velox.jewelvault.data.roomdb.entity.StoreEntity
import com.velox.jewelvault.data.roomdb.entity.category.CategoryEntity
import com.velox.jewelvault.data.roomdb.entity.category.SubCategoryEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.velox.jewelvault.utils.sync.SyncManager
import com.velox.jewelvault.utils.sync.RestoreMode
import com.velox.jewelvault.utils.generateId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoadingTask(
    val id: String,
    val title: String,
    val isComplete: Boolean = false
)

@HiltViewModel
class StartLoadingViewModel @Inject constructor(
    private val dataStoreManager: DataStoreManager,
    private val appDatabase: AppDatabase,
    private val firestore: FirebaseFirestore,
    private val syncManager: SyncManager
) : ViewModel() {

    val needsStoreSetup = mutableStateOf<Boolean?>(null)
    val progress = mutableStateOf(0f)
    val progressMessage = mutableStateOf("")
    val permissionsGranted = mutableStateOf(false)
    val showBackupDialog = mutableStateOf(false)
    val backupRestoreInProgress = mutableStateOf(false)
    val backupDecisionPending = mutableStateOf(false)
    val importProgress = mutableStateOf<Int?>(null)

    private val tasks = listOf(
        LoadingTask(id = "permissions", title = "Grant permissions"),
        LoadingTask(id = "feature_list", title = "Load feature list"),
        LoadingTask(id = "store_setup", title = "Check store setup"),
        LoadingTask(id = "backup_check", title = "Check cloud sync")
    ).toMutableStateList()

    val workItems: SnapshotStateList<LoadingTask> = tasks

    private val loadingStarted = mutableStateOf(false)

    fun markPermissionsGranted() {
        if (permissionsGranted.value) return
        permissionsGranted.value = true
        markComplete("permissions")
    }

    suspend fun getFeature(key: String): Boolean {
        return dataStoreManager.getFeature(key)
    }

    fun startLoadingWork() {
        if (!permissionsGranted.value) return
        if (loadingStarted.value) return
        loadingStarted.value = true
        viewModelScope.launch {
            val needsSetup = syncStoreAndUsers()
            needsStoreSetup.value = needsSetup
            markComplete("store_setup")

            if (needsSetup) {
                clearImportProgress()
                markComplete("feature_list")
                markComplete("backup_check")
                return@launch
            }

            // 1. Sync features and subscription first
            syncFeaturesAndSubscription()
            markComplete("feature_list")

            // 2. Then check for backup only if back_up_online is true
            val isBackupOnline = dataStoreManager.getFeature("back_up_online")
            if (isBackupOnline) {
                handleBackupDecisionFlow()
            } else {
                markComplete("backup_check")
            }
        }
    }

    private suspend fun syncStoreAndUsers(): Boolean {
        val storeInfo = dataStoreManager.getSelectedStoreInfo()
        val selectedStoreId = storeInfo.first.first()
        val localStore = resolveLocalStore(selectedStoreId)
        val adminMobile = getAdminMobile()
        if (adminMobile.isBlank()) {
            return localStore == null
        }

        val resolvedStore = if (localStore == null) {
            val storeFromCloud = fetchStoreFromCloud(adminMobile, selectedStoreId)
            if (storeFromCloud != null) {
                upsertStoreAndPersistSelection(storeFromCloud)
                storeFromCloud
            } else {
                return true
            }
        } else {
            if (selectedStoreId.isBlank()) {
                dataStoreManager.saveSelectedStoreInfo(
                    localStore.storeId,
                    localStore.upiId,
                    localStore.name
                )
            }
            val updatedStore = syncStoreIfRemoteNewer(adminMobile, localStore)
            updatedStore ?: localStore
        }

        syncUsersFromFirestore(adminMobile, resolvedStore.storeId)
        return false
    }

    private suspend fun getAdminMobile(): String {
        val adminUser = appDatabase.userDao().getAdminUser()
        if (!adminUser?.mobileNo.isNullOrBlank()) {
            return adminUser?.mobileNo ?: ""
        }
        return dataStoreManager.getAdminInfo().third.first()
    }

    private suspend fun resolveLocalStore(selectedStoreId: String): StoreEntity? {
        if (selectedStoreId.isNotBlank()) {
            return appDatabase.storeDao().getStoreById(selectedStoreId)
        }

        val stores = appDatabase.storeDao().getAllStores()
        return stores.firstOrNull()
    }

    private suspend fun fetchStoreFromCloud(
        adminMobile: String,
        selectedStoreId: String
    ): StoreEntity? {
        if (selectedStoreId.isNotBlank()) {
            val storeResult = FirebaseUtils.getStoreDataFromFirestore(
                firestore,
                adminMobile,
                selectedStoreId
            )
            val data = storeResult.getOrNull()
            return if (storeResult.isSuccess && data != null) {
                FirebaseUtils.mapToStoreEntity(data).copy(storeId = selectedStoreId)
            } else {
                null
            }
        }

        val storeListResult = FirebaseUtils.getAllStores(firestore, adminMobile)
        if (!storeListResult.isSuccess) return null

        val storeList = storeListResult.getOrNull().orEmpty()
        if (storeList.isEmpty()) return null

        val (storeId, storeData) = storeList.first()
        return FirebaseUtils.mapToStoreEntity(storeData).copy(storeId = storeId)
    }

    private suspend fun upsertStoreAndPersistSelection(store: StoreEntity) {
        val existingStore = appDatabase.storeDao().getStoreById(store.storeId)
        if (existingStore != null) {
            appDatabase.storeDao().updateStore(store)
        } else {
            appDatabase.storeDao().insertStore(store)
        }

        dataStoreManager.saveSelectedStoreInfo(store.storeId, store.upiId, store.name)
        dataStoreManager.setUpiId(store.upiId)
        dataStoreManager.setMerchantName(store.name)
    }

    private suspend fun syncStoreIfRemoteNewer(
        adminMobile: String,
        localStore: StoreEntity
    ): StoreEntity? {
        val storeResult = FirebaseUtils.getStoreDataFromFirestore(
            firestore,
            adminMobile,
            localStore.storeId
        )
        if (!storeResult.isSuccess) return null
        val remoteData = storeResult.getOrNull() ?: return null
        val remoteStore = FirebaseUtils.mapToStoreEntity(remoteData).copy(storeId = localStore.storeId)

        val localUpdated = localStore.lastUpdated
        val remoteUpdated = remoteStore.lastUpdated
        val shouldUpdate = remoteUpdated > localUpdated || localUpdated == 0L
        if (shouldUpdate) {
            upsertStoreAndPersistSelection(remoteStore)
            return remoteStore
        }

        return null
    }

    private suspend fun syncUsersFromFirestore(adminMobile: String, storeId: String) {
        if (storeId.isBlank()) return

        val firestoreResult = FirebaseUtils.getAllUsersFromFirestore(firestore, adminMobile, storeId)
        if (!firestoreResult.isSuccess) return

        val userDocuments = firestoreResult.getOrNull().orEmpty()
        for ((appUserMobileNumber, userData) in userDocuments) {
            val mappedUser = FirebaseUtils.mapToUserEntity(userData).copy(
                userId = appUserMobileNumber,
                mobileNo = userData["mobileNo"] as? String ?: appUserMobileNumber
            )
            val existingUser = appDatabase.userDao().getUserById(appUserMobileNumber)
            val localUpdated = existingUser?.lastUpdated ?: 0L
            val remoteUpdated = mappedUser.lastUpdated

            if (existingUser == null || remoteUpdated > localUpdated || localUpdated == 0L) {
                if (existingUser != null) {
                    appDatabase.userDao().updateUser(mappedUser)
                } else {
                    appDatabase.userDao().insertUser(mappedUser)
                }
            }

            val additionalInfoResult = FirebaseUtils.getUserAdditionalInfoFromFirestore(
                firestore,
                adminMobile,
                storeId,
                appUserMobileNumber
            )

            if (additionalInfoResult.isSuccess && additionalInfoResult.getOrNull() != null) {
                val remoteAdditionalInfo = FirebaseUtils.mapToUserAdditionalInfoEntity(
                    additionalInfoResult.getOrNull()!!
                ).copy(userId = appUserMobileNumber)

                val existingAdditionalInfo =
                    appDatabase.userAdditionalInfoDao().getUserAdditionalInfoById(appUserMobileNumber)
                val localInfoUpdated = existingAdditionalInfo?.updatedAt ?: 0L
                val remoteInfoUpdated = remoteAdditionalInfo.updatedAt
                val shouldUpdate = existingAdditionalInfo == null ||
                    remoteInfoUpdated > localInfoUpdated ||
                    localInfoUpdated == 0L

                if (shouldUpdate) {
                    if (existingAdditionalInfo != null) {
                        appDatabase.userAdditionalInfoDao().updateUserAdditionalInfo(remoteAdditionalInfo)
                    } else {
                        appDatabase.userAdditionalInfoDao().insertUserAdditionalInfo(remoteAdditionalInfo)
                    }
                }
            }
        }
    }

    private suspend fun handleBackupDecisionFlow() {
        val storeId = dataStoreManager.getSelectedStoreInfo().first.first()
        val userId = dataStoreManager.getAdminInfo().first.first()
        val hasCategories = if (userId.isNotBlank() && storeId.isNotBlank()) {
            appDatabase.categoryDao().getCategoriesByUserIdAndStoreId(userId, storeId).isNotEmpty()
        } else {
            false
        }
        val isFreshInstall = appDatabase.storeDao().getAllStores().size == 1 && !hasCategories
        val adminMobile = getAdminMobile()

        if (!isFreshInstall || adminMobile.isBlank() || storeId.isBlank()) {
            clearImportProgress()
            markComplete("backup_check")
            return
        }

        val backupExistsResult = syncManager.checkFirebaseBackupExists(adminMobile)
        val backupExists = backupExistsResult.getOrNull() == true
        if (backupExists) {
            backupDecisionPending.value = true
            showBackupDialog.value = true
            return
        }

        clearImportProgress()
        initializeDefaultCategoriesIfMissing()
        markComplete("backup_check")
    }

    fun onBackupRestoreConfirmed() {
        if (backupRestoreInProgress.value) return
        showBackupDialog.value = false
        backupRestoreInProgress.value = true
        setImportProgress("Starting restore...", 0)
        viewModelScope.launch {
            try {
                val adminMobile = getAdminMobile()
                if (adminMobile.isBlank()) {
                    initializeDefaultCategoriesIfMissing()
                } else {
                    val result = syncManager.performRestoreWithSource(
                        userMobile = adminMobile,
                        restoreSource = com.velox.jewelvault.utils.sync.RestoreSource.FIREBASE,
                        restoreMode = RestoreMode.MERGE
                    ) { message, progress ->
                        setImportProgress(message, progress)
                    }
                    if (result.isFailure) {
                        initializeDefaultCategoriesIfMissing()
                    }
                }
            } finally {
                backupRestoreInProgress.value = false
                backupDecisionPending.value = false
                markComplete("backup_check")
                clearImportProgress()
            }
        }
    }

    fun onBackupRestoreCancelled() {
        showBackupDialog.value = false
        viewModelScope.launch {
            clearImportProgress()
            initializeDefaultCategoriesIfMissing()
            backupDecisionPending.value = false
            markComplete("backup_check")
        }
    }

    private suspend fun initializeDefaultCategoriesIfMissing() {
        val userId = dataStoreManager.getAdminInfo().first.first()
        val storeId = dataStoreManager.getSelectedStoreInfo().first.first()
        if (userId.isBlank() || storeId.isBlank()) return

        val existingCategories =
            appDatabase.categoryDao().getCategoriesByUserIdAndStoreId(userId, storeId)
        if (existingCategories.isNotEmpty()) {
            return
        }

        val defaultCategories = listOf(
            Triple("Gold", "1", "Fine"),
            Triple("Silver", "2", "Fine")
        )

        for ((catName, catId, subCatName) in defaultCategories) {
            val categoryExists = appDatabase.categoryDao().getCategoryByName(catName)
            if (categoryExists == null) {
                appDatabase.categoryDao().insertCategory(
                    CategoryEntity(
                        catId = catId,
                        catName = catName,
                        userId = userId,
                        storeId = storeId
                    )
                )
            }

            val category = appDatabase.categoryDao().getCategoryByName(catName)
            if (category != null) {
                val subExists = appDatabase.subCategoryDao()
                    .getSubCategoryByName(catId = category.catId, subCatName = subCatName)
                if (subExists == null) {
                    appDatabase.subCategoryDao().insertSubCategory(
                        SubCategoryEntity(
                            subCatId = generateId(),
                            subCatName = subCatName,
                            catId = category.catId,
                            catName = category.catName,
                            userId = userId,
                            storeId = storeId
                        )
                    )
                }
            }
        }
    }

    private suspend fun syncFeaturesAndSubscription() {
        val adminMobile = getAdminMobile()
        if (adminMobile.isBlank()) {
            return
        }

        syncFeatureList(adminMobile)
        syncSubscription(adminMobile)
    }

    private suspend fun syncFeatureList(adminMobile: String) {
        val localState = dataStoreManager.getFeatureList()
        val result = FirebaseUtils.getFeatureList(firestore, adminMobile)
        val remoteData = result.getOrNull()

        if (result.isSuccess && remoteData != null) {
            val remoteState = parseFeatureList(remoteData)
            val shouldUpdate = remoteState.lastUpdated > localState.lastUpdated ||
                localState.lastUpdated == 0L ||
                localState.features.isEmpty()
            if (shouldUpdate) {
                dataStoreManager.saveFeatureList(remoteState)
            }
            return
        }

        val defaults = FeatureListState(
            features = FeatureDefaults.defaultFeatureMap(),
            lastUpdated = System.currentTimeMillis()
        )
        dataStoreManager.saveFeatureList(defaults)
        FirebaseUtils.saveFeatureList(firestore, adminMobile, featureStateToMap(defaults))
    }

    private suspend fun syncSubscription(adminMobile: String) {
        val localState = dataStoreManager.getSubscription()
        val result = FirebaseUtils.getSubscription(firestore, adminMobile)
        val remoteData = result.getOrNull()

        if (result.isSuccess && remoteData != null) {
            val remoteState = parseSubscription(remoteData)
            val shouldUpdate = remoteState.lastUpdated > localState.lastUpdated ||
                localState.lastUpdated == 0L ||
                localState.plan.isBlank()
            if (shouldUpdate) {
                dataStoreManager.saveSubscription(remoteState)
            }
            return
        }

        val defaults = SubscriptionState(
            plan = "trial-pro",
            isActive = true,
            startAt = System.currentTimeMillis(),
            endAt = System.currentTimeMillis() + java.util.concurrent.TimeUnit.DAYS.toMillis(30),
            lastUpdated = System.currentTimeMillis()
        )
        dataStoreManager.saveSubscription(defaults)
        FirebaseUtils.saveSubscription(firestore, adminMobile, subscriptionStateToMap(defaults))
    }

    private fun parseFeatureList(data: Map<String, Any>): FeatureListState {
        val lastUpdated = (data["lastUpdated"] as? Number)?.toLong()
            ?: (data["last_update"] as? Number)?.toLong()
            ?: 0L
        val features = data
            .filterKeys { it != "lastUpdated" && it != "last_update" }
            .mapNotNull { (key, value) ->
                when (value) {
                    is Boolean -> key to value
                    else -> null
                }
            }
            .toMap()
        return FeatureListState(features = features, lastUpdated = lastUpdated)
    }

    private fun featureStateToMap(state: FeatureListState): Map<String, Any> {
        val map = state.features.mapValues { it.value as Any }.toMutableMap()
        map["lastUpdated"] = state.lastUpdated
        return map
    }

    private fun parseSubscription(data: Map<String, Any>): SubscriptionState {
        val lastUpdated = (data["lastUpdated"] as? Number)?.toLong()
            ?: (data["last_update"] as? Number)?.toLong()
            ?: 0L
        return SubscriptionState(
            plan = data["plan"] as? String ?: "",
            isActive = data["isActive"] as? Boolean ?: false,
            startAt = (data["startAt"] as? Number)?.toLong() ?: 0L,
            endAt = (data["endAt"] as? Number)?.toLong() ?: 0L,
            lastUpdated = lastUpdated
        )
    }

    private fun subscriptionStateToMap(state: SubscriptionState): Map<String, Any> {
        return mapOf(
            "plan" to state.plan,
            "isActive" to state.isActive,
            "startAt" to state.startAt,
            "endAt" to state.endAt,
            "lastUpdated" to state.lastUpdated
        )
    }

    private fun markComplete(id: String) {
        val index = tasks.indexOfFirst { it.id == id }
        if (index == -1) return
        if (tasks[index].isComplete) return
        tasks[index] = tasks[index].copy(isComplete = true)
        updateProgress()
    }

    private fun setImportProgress(message: String, progress: Int) {
        progressMessage.value = message
        importProgress.value = progress.coerceIn(0, 100)
        updateProgress()
    }

    private fun clearImportProgress() {
        progressMessage.value = ""
        importProgress.value = null
        updateProgress()
    }

    private fun updateProgress() {
        val total = tasks.size
        if (total == 0) {
            progress.value = 0f
            return
        }
        val completed = tasks.count { it.isComplete }
        val backupTask = tasks.firstOrNull { it.id == "backup_check" }
        val backupIsComplete = backupTask?.isComplete == true
        val partial = if (!backupIsComplete && importProgress.value != null) {
            (importProgress.value!!.coerceIn(0, 100) / 100f)
        } else {
            0f
        }
        progress.value = (completed + partial).toFloat() / total.toFloat()
    }
}
