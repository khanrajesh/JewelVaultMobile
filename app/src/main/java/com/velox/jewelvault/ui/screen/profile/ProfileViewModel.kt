package com.velox.jewelvault.ui.screen.profile

import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.velox.jewelvault.data.roomdb.AppDatabase
import com.velox.jewelvault.data.roomdb.entity.category.CategoryEntity
import com.velox.jewelvault.data.roomdb.entity.StoreEntity
import com.velox.jewelvault.data.roomdb.entity.category.SubCategoryEntity
import com.velox.jewelvault.ui.components.InputFieldState
import com.velox.jewelvault.data.DataStoreManager
import com.velox.jewelvault.data.FeatureDefaults
import com.velox.jewelvault.data.FeatureListState
import com.velox.jewelvault.data.SubscriptionState
import com.velox.jewelvault.utils.FileManager
import com.velox.jewelvault.data.firebase.FirebaseUtils
import com.velox.jewelvault.utils.ioLaunch
import com.velox.jewelvault.utils.log
import com.velox.jewelvault.utils.InputValidator
import com.velox.jewelvault.utils.generateId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val _dataStoreManager: DataStoreManager,
    private val appDatabase: AppDatabase,
    @Named("snackMessage") private val _snackBarState: MutableState<String>,
    @Named("currentScreenHeading") private val _currentScreenHeadingState: MutableState<String>,
    private val _firestore: FirebaseFirestore,
    private val _firebaseStorage: FirebaseStorage
) : ViewModel() {

    val currentScreenHeadingState = _currentScreenHeadingState
    val snackBarState = _snackBarState
    val dataStoreManager = _dataStoreManager

    /**
     * return Triple of Flow<String> for userId, userName, mobileNo
     * */
    val admin: Triple<Flow<String>, Flow<String>, Flow<String>> = _dataStoreManager.getAdminInfo()

    /**
     * return Triple of Flow<String> for storeId, upiId, storeName
     * */
    val store: Triple<Flow<String>, Flow<String>, Flow<String>> =
        _dataStoreManager.getSelectedStoreInfo()

    // Store details fields - no dummy data
    val shopName = InputFieldState()
    val propName = InputFieldState()
    val shopImage = InputFieldState()
    val address = InputFieldState()
    val registrationNo = InputFieldState()
    val gstinNo = InputFieldState()
    val panNumber = InputFieldState()

    // User details fields (these get data from UserEntity, no dummy text)
    val userEmail = InputFieldState()
    val userMobile = InputFieldState()

    // Payment details fields
    val upiId = InputFieldState()

    val storeEntity = mutableStateOf<StoreEntity?>(null)
    val selectedImageUri = mutableStateOf<String?>(null)
    val selectedImageFileUri = mutableStateOf<Uri?>(null)
    val isLoading = mutableStateOf(false)
    val isUploadingImage = mutableStateOf(false)

    private fun resetFieldErrors() {
        listOf(
            shopName,
            propName,
            userEmail,
            userMobile,
            address,
            registrationNo,
            gstinNo,
            panNumber,
            upiId
        ).forEach { it.error = "" }
    }

    fun clearFieldErrors() {
        resetFieldErrors()
    }

    private fun setError(state: InputFieldState, message: String): Boolean {
        state.error = message
        _snackBarState.value = message
        return false
    }

    private fun validateStoreFields(): Boolean {
        resetFieldErrors()

        shopName.text = InputValidator.sanitizeText(shopName.text)
        propName.text = InputValidator.sanitizeText(propName.text)
        address.text = InputValidator.sanitizeText(address.text)
        registrationNo.text = InputValidator.sanitizeText(registrationNo.text).uppercase()
        gstinNo.text = gstinNo.text.trim().uppercase()
        panNumber.text = panNumber.text.trim().uppercase()
        userEmail.text = InputValidator.sanitizeText(userEmail.text)
        upiId.text = upiId.text.trim()

        if (shopName.text.isBlank()) return setError(shopName, "Store name is required")
        if (shopName.text.length < 3) return setError(shopName, "Store name must be at least 3 characters")

        if (propName.text.isBlank()) return setError(propName, "Proprietor name is required")
        if (propName.text.length < 2) return setError(propName, "Proprietor name must be at least 2 characters")

        if (userEmail.text.isBlank()) return setError(userEmail, "Email address is required")
        if (!InputValidator.isValidEmail(userEmail.text)) return setError(userEmail, "Enter a valid email address")

        val mobile = userMobile.text.trim()
        if (mobile.isBlank()) return setError(userMobile, "Mobile number is required")
        if (mobile.length != 10 || !InputValidator.isValidPhoneNumber(mobile)) {
            return setError(userMobile, "Enter a valid 10-digit mobile number")
        }

        if (address.text.isBlank()) return setError(address, "Store address is required")
        if (address.text.length < 10) return setError(address, "Please enter a complete address (min 10 characters)")

        if (registrationNo.text.isBlank()) return setError(registrationNo, "Registration number is required")

        if (gstinNo.text.isBlank()) return setError(gstinNo, "GSTIN number is required")
        if (!InputValidator.isValidGSTIN(gstinNo.text)) {
            return setError(gstinNo, "Enter a valid GSTIN (15 characters, e.g. 22AAAAA0000A1Z5)")
        }

        if (panNumber.text.isBlank()) return setError(panNumber, "PAN number is required")
        if (!InputValidator.isValidPAN(panNumber.text)) {
            return setError(panNumber, "Enter a valid PAN (format ABCDE1234F)")
        }

        if (upiId.text.isBlank()) return setError(upiId, "UPI ID is required")
        if (!InputValidator.isValidUpiId(upiId.text)) {
            return setError(upiId, "Enter a valid UPI ID (name@bank)")
        }

        return true
    }

    fun initializeDefaultCategories() {
        ioLaunch {
            try {
                val userId = admin.first.first()
                val storeId = store.first.first()

                val existingCategories =
                    appDatabase.categoryDao().getCategoriesByUserIdAndStoreId(userId, storeId)
                log("Existing categories: $existingCategories")

                if (existingCategories.isNotEmpty()) {
                    log("Categories already exist.")
                    return@ioLaunch
                }

                log("No categories found. Inserting default 'Gold' and 'Silver'...")

                val defaultCategories = listOf(
                    Triple("Gold", "1", "Fine"),
                    Triple("Silver", "2", "Fine")
                )

                for ((catName, catId, subCatName) in defaultCategories) {
                    val categoryExists = appDatabase.categoryDao().getCategoryByName(catName)

                    if (categoryExists == null) {
                        val insertResult = appDatabase.categoryDao().insertCategory(
                            CategoryEntity(
                                catId = catId,
                                catName = catName,
                                userId = userId,
                                storeId = storeId
                            )
                        )

                        if (insertResult != -1L) {
                            log("Inserted category: $catName with ID: $catId")
                        } else {
                            log("Insert failed for category: $catName")
                            continue
                        }
                    } else {
                        log("Category $catName already exists, skipping insert.")
                    }

                    val category = appDatabase.categoryDao().getCategoryByName(catName)
                    if (category != null) {
                        val subExists = appDatabase.subCategoryDao()
                            .getSubCategoryByName(catId = category.catId, subCatName = subCatName)

                        if (subExists == null) {
                            val subInsert = appDatabase.subCategoryDao().insertSubCategory(
                                SubCategoryEntity(
                                    subCatId = generateId(),
                                    subCatName = subCatName,
                                    catId = category.catId,
                                    catName = category.catName,
                                    userId = userId,
                                    storeId = storeId
                                )
                            )

                            if (subInsert != -1L) {
                                log("Added subcategory '$subCatName' under '$catName'")
                                _snackBarState.value = "Added $catName with subcategory $subCatName"
                            } else {
                                log("Failed to insert subcategory '$subCatName' for '$catName'")
                            }
                        } else {
                            log("Subcategory '$subCatName' already exists under '$catName'")
                        }
                    } else {
                        log("Could not retrieve category $catName after insert.")
                    }
                }

            } catch (e: Exception) {
                log("Error initializing categories: ${e.message}")
                _snackBarState.value = "Error initializing categories"
            }
        }
    }

    fun getStoreData(storeId: String, isFirstLaunch: Boolean = false) {
        ioLaunch {
            try {
                isLoading.value = true
                val userId = admin.first.first()
                val userData = appDatabase.userDao().getUserById(userId)
                val mobileNumber = userData?.mobileNo ?: ""

                if (storeId.isNotBlank()) {
                    val result =
                        FirebaseUtils.getStoreDataFromFirestore(_firestore, mobileNumber, storeId)
                    if (result.isSuccess) {
                        val storeFromFirestore =
                            FirebaseUtils.mapToStoreEntity(result.getOrNull()!!)
                        storeEntity.value = storeFromFirestore
                    } else {
                        log("Error getting store data from Firestore: ${result.exceptionOrNull()?.message}")
                        _snackBarState.value = "Failed to load store data"
                    }
                } else {
                    val result = FirebaseUtils.getAllStores(_firestore, mobileNumber)
                    if (result.isSuccess) {
                        val storeList = result.getOrNull()
                        when {
                            storeList.isNullOrEmpty() -> {
                                log("No stores found for this mobile number in Firestore")
                                _snackBarState.value = "No stores found in cloud"
                            }

                            storeList.size == 1 -> {
                                log("Found 1 store in Firestore")
                                val storeId = storeList[0].first
                                val storeFromFirestore =
                                    FirebaseUtils.mapToStoreEntity(storeList[0].second)
                                        .copy(storeId)

                                _dataStoreManager.saveSelectedStoreInfo(
                                    storeId,
                                    storeFromFirestore.upiId,
                                    storeFromFirestore.name
                                )

                                // Update local database with Firestore data
                                val existingStore = appDatabase.storeDao().getStoreById(storeId)
                                if (existingStore != null) {
                                    // Update existing store
                                    appDatabase.storeDao().updateStore(storeFromFirestore)
                                } else {
                                    // Insert new store

                                    appDatabase.storeDao().insertStore(storeFromFirestore)
                                }

                                storeEntity.value = storeFromFirestore
                                log("Store data loaded from Firestore")
                                if (isFirstLaunch) {
                                    initializeDefaultCategories()
                                }
                            }

                            storeList.size > 1 -> {
                                log("Found ${storeList.size} stores in Firestore")
                                // Handle the multiple stores case here if needed
                            }
                        }
                    }
                }


                // Load user data (no dummy text for these fields)
                userData?.let {
                    userMobile.text = it.mobileNo ?: ""
                    userEmail.text = it.email ?: ""
                }

                // Load store data (will override dummy text if exists)
                storeEntity.value?.let {
                    propName.text = it.proprietor
                    userMobile.text = it.phone
                    shopName.text = it.name
                    shopImage.text = it.image
                    address.text = it.address
                    registrationNo.text = it.registrationNo
                    gstinNo.text = it.gstinNo
                    panNumber.text = it.panNo
                    selectedImageUri.value = it.image
                    userEmail.text = it.email
                    upiId.text = it.upiId

                    // Save store name to DataStore for UPI QR generation
                    _dataStoreManager.setMerchantName(it.name)
                    
                    // Note: Logo download and caching is handled by BaseViewModel.loadStoreImage()
                    // which automatically downloads if no local logo exists but URL is available
                }

                isLoading.value = false
            } catch (e: Exception) {
                log("Error loading store data: ${e.message}")
                _snackBarState.value = "Failed to load store data"
                isLoading.value = false
            }
        }
    }

    fun saveStoreData(onSuccess: () -> Unit, onFailure: () -> Unit, onImageUpdated: (() -> Unit)? = null) {
        if (!validateStoreFields()) {
            onFailure()
            return
        }

        ioLaunch {
            try {
                isLoading.value = true
                val userId = admin.first.first()
                val storeId = store.first.first()
                val isFirstStoreSetup = storeId.isBlank()
                val userData = appDatabase.userDao().getUserById(userId)
                val mobileNumber = userData?.mobileNo ?: ""

                // Handle image upload if there's a new image file
                var finalImageUrl = selectedImageUri.value ?: ""
                if (selectedImageFileUri.value != null) {
                    isUploadingImage.value = true
                    val uploadResult = FirebaseUtils.uploadImageToStorage(
                        _firebaseStorage,
                        selectedImageFileUri.value!!,
                        mobileNumber
                    )

                    if (uploadResult.isSuccess) {
                        finalImageUrl = uploadResult.getOrNull() ?: ""
                        selectedImageUri.value = finalImageUrl
                        log("Image uploaded successfully: $finalImageUrl")
                        
                        // Download and cache the logo locally
                        downloadAndCacheLogo(finalImageUrl)
                        
                        // Notify that image was updated
                        onImageUpdated?.invoke()
                    } else {
                        log("Failed to upload image: ${uploadResult.exceptionOrNull()?.message}")
                        _snackBarState.value = "Failed to upload image. Please try again."
                        onFailure()
                        isLoading.value = false
                        isUploadingImage.value = false
                        return@ioLaunch
                    }
                    isUploadingImage.value = false
                }

                var storeEntity = StoreEntity(
                    storeId = storeId,
                    userId = userId,
                    proprietor = InputValidator.sanitizeText(propName.text),
                    name = InputValidator.sanitizeText(shopName.text),
                    phone = userMobile.text.trim(),
                    address = InputValidator.sanitizeText(address.text),
                    registrationNo = InputValidator.sanitizeText(registrationNo.text),
                    gstinNo = gstinNo.text.trim().uppercase(),
                    panNo = panNumber.text.trim().uppercase(),
                    image = finalImageUrl,
                    email = InputValidator.sanitizeText(userEmail.text),
                    upiId = upiId.text.trim(),
                    lastUpdated = System.currentTimeMillis()
                )

                // Save to Firestore
                val firestoreData = FirebaseUtils.storeEntityToMap(storeEntity)
                val firestoreResult = FirebaseUtils.saveOrUpdateStoreData(
                    _firestore,
                    mobileNumber,
                    firestoreData,
                    storeId = storeId

                )

                if (firestoreResult.isSuccess) {

                    val generatedId = firestoreResult.getOrNull()

                    // If this is a new store, assign generatedId
                    if (generatedId != null) {
                        storeEntity = storeEntity.copy(storeId = generatedId)
                    }

                    log("Store data saved to both local database and Firestore")
                    _snackBarState.value = "Store details saved successfully"

                    // Save to local database
                    val localResult = if (storeId != "") {
                        appDatabase.storeDao().updateStore(storeEntity)
                    } else {
                        appDatabase.storeDao().insertStore(storeEntity)
                    }

                    if (localResult != -1L) {
                        // Save the final store ID in DataStore
                        _dataStoreManager.saveSelectedStoreInfo(
                            storeEntity.storeId,
                            storeEntity.upiId,
                            storeEntity.name
                        )
                        if (isFirstStoreSetup) {
                            initializeDefaultCategories()
                        }
                    } else {
                        log("Failed to save store to local database")
                        _snackBarState.value = "Failed to save store details. Please try again."
                        onFailure()
                    }

                    if (isFirstStoreSetup) {
                        initializeFeatureAndSubscriptionDefaults(mobileNumber)
                    }
                    onSuccess()
                } else {
                    log("Failed to save to Firestore: ${firestoreResult.exceptionOrNull()?.message}")
                    _snackBarState.value = "Saved locally but failed to sync with cloud"
                    if (isFirstStoreSetup) {
                        initializeFeatureAndSubscriptionDefaults(mobileNumber)
                    }
                    onSuccess() // Still consider it a success since local save worked
                }



                isLoading.value = false
            } catch (e: Exception) {
                log("Error saving store data: ${e.message}")
                _snackBarState.value = "An error occurred while saving. Please try again."
                onFailure()
                isLoading.value = false
                isUploadingImage.value = false
            }
        }
    }

    private suspend fun initializeFeatureAndSubscriptionDefaults(mobileNumber: String) {
        if (mobileNumber.isBlank()) return
        val now = System.currentTimeMillis()

        val featureState = FeatureListState(
            features = FeatureDefaults.defaultFeatureMap(),
            lastUpdated = now
        )
        _dataStoreManager.saveFeatureList(featureState)
        val featureMap = featureState.features.mapValues { it.value as Any }.toMutableMap()
        featureMap["lastUpdated"] = now
        FirebaseUtils.saveFeatureList(
            _firestore,
            mobileNumber,
            featureMap
        )

        val subscriptionState = SubscriptionState(
            plan = "trial-pro",
            isActive = true,
            startAt = now,
            endAt = now + java.util.concurrent.TimeUnit.DAYS.toMillis(30),
            lastUpdated = now
        )
        _dataStoreManager.saveSubscription(subscriptionState)
        FirebaseUtils.saveSubscription(
            _firestore,
            mobileNumber,
            mapOf(
                "plan" to subscriptionState.plan,
                "isActive" to subscriptionState.isActive,
                "startAt" to subscriptionState.startAt,
                "endAt" to subscriptionState.endAt,
                "lastUpdated" to subscriptionState.lastUpdated
            )
        )
    }


    fun setSelectedImageFile(imageUri: Uri?) {
        selectedImageFileUri.value = imageUri
        // Don't set selectedImageUri here - it should only be set after successful upload
        // This ensures we show the local file URI immediately for preview
        shopImage.text = imageUri?.toString() ?: ""
    }
    
    /**
     * Download and cache logo from URL to local storage
     */
    private fun downloadAndCacheLogo(imageUrl: String) {
        ioLaunch {
            try {
                log("ProfileViewModel: Starting logo download and cache from: $imageUrl")
                val result = FileManager.downloadAndSaveLogo(android.app.Application(), imageUrl)
                
                if (result.isSuccess) {
                    log("ProfileViewModel: Logo downloaded and cached successfully: ${result.getOrNull()}")
                } else {
                    log("ProfileViewModel: Failed to download logo: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                log("ProfileViewModel: Error downloading logo: ${e.message}")
            }
        }
    }

    /**
     * Sync store data from Firestore to local database
     */
    fun syncFromFirestore(onSuccess: () -> Unit, onFailure: () -> Unit) {
        ioLaunch {
            try {
                isLoading.value = true
                val userId = admin.first.first()
                val storeId = store.first.first()
                val userData = appDatabase.userDao().getUserById(userId)
                val mobileNumber = userData?.mobileNo ?: ""

                val firestoreResult =
                    FirebaseUtils.getStoreDataFromFirestore(_firestore, mobileNumber, storeId)

                if (firestoreResult.isSuccess && firestoreResult.getOrNull() != null) {
                    val firestoreData = firestoreResult.getOrNull()!!
                    val storeFromFirestore = FirebaseUtils.mapToStoreEntity(firestoreData)

                    // Update or insert in local database
                    val existingStore = appDatabase.storeDao().getStoreById(userId)
                    val result = if (existingStore != null) {
                        appDatabase.storeDao().updateStore(storeFromFirestore)
                    } else {
                        appDatabase.storeDao().insertStore(storeFromFirestore)
                    }

                    if (result != -1L) {

                        _dataStoreManager.saveSelectedStoreInfo(
                            storeFromFirestore.storeId,
                            storeFromFirestore.upiId,
                            storeFromFirestore.name
                        )


                        // Update UPI settings in DataStore
                        _dataStoreManager.setUpiId(storeFromFirestore.upiId)
                        _dataStoreManager.setMerchantName(storeFromFirestore.name)

                        // Update UI with synced data
                        storeEntity.value = storeFromFirestore
                        propName.text = storeFromFirestore.proprietor
                        userMobile.text = storeFromFirestore.phone
                        shopName.text = storeFromFirestore.name
                        shopImage.text = storeFromFirestore.image
                        address.text = storeFromFirestore.address
                        registrationNo.text = storeFromFirestore.registrationNo
                        gstinNo.text = storeFromFirestore.gstinNo
                        panNumber.text = storeFromFirestore.panNo
                        selectedImageUri.value = storeFromFirestore.image
                        userEmail.text = storeFromFirestore.email
                        upiId.text = storeFromFirestore.upiId

                        log("Store data synced from Firestore successfully")
                        _snackBarState.value = "Store data synced from cloud"
                        onSuccess()
                    } else {
                        log("Failed to save synced data to local database")
                        _snackBarState.value = "Failed to sync data locally"
                        onFailure()
                    }
                } else {
                    log("No store data found in Firestore")
                    _snackBarState.value = "No store data found in cloud"
                    onFailure()
                }

                isLoading.value = false
            } catch (e: Exception) {
                log("Error syncing from Firestore: ${e.message}")
                _snackBarState.value = "Failed to sync from cloud"
                onFailure()
                isLoading.value = false
            }
        }
    }

}
