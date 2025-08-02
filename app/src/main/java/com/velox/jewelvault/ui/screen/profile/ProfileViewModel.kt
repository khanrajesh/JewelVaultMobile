package com.velox.jewelvault.ui.screen.profile

import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.velox.jewelvault.data.roomdb.AppDatabase
import com.velox.jewelvault.data.roomdb.entity.CategoryEntity
import com.velox.jewelvault.data.roomdb.entity.StoreEntity
import com.velox.jewelvault.data.roomdb.entity.SubCategoryEntity
import com.velox.jewelvault.ui.components.InputFieldState
import com.velox.jewelvault.data.DataStoreManager
import com.velox.jewelvault.utils.FirebaseUtils
import com.velox.jewelvault.utils.ioLaunch
import com.velox.jewelvault.utils.log
import com.velox.jewelvault.utils.InputValidator
import com.velox.jewelvault.utils.generateId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val _dataStoreManager: DataStoreManager,
    private val appDatabase: AppDatabase,
    private val _snackBarState: MutableState<String>,
    private val _firestore: FirebaseFirestore,
    private val _firebaseStorage: FirebaseStorage
) : ViewModel() {

    val snackBarState = _snackBarState
    val dataStoreManager = _dataStoreManager

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

    fun initializeDefaultCategories() {
        ioLaunch {
            try {
                val userId = _dataStoreManager.userId.first()
                val storeId = _dataStoreManager.storeId.first()

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
                val userId = _dataStoreManager.userId.first()
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
                                val storeId =storeList[0].first
                                val storeFromFirestore =
                                    FirebaseUtils.mapToStoreEntity(storeList[0].second).copy(storeId)

                                _dataStoreManager.setValue(DataStoreManager.STORE_ID_KEY, storeId)
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
                }

                isLoading.value = false
            } catch (e: Exception) {
                log("Error loading store data: ${e.message}")
                _snackBarState.value = "Failed to load store data"
                isLoading.value = false
            }
        }
    }

    fun saveStoreData(onSuccess: () -> Unit, onFailure: () -> Unit) {
        // Comprehensive validation with specific error messages
        when {
            /*    shopName.text.isBlank() -> {
                    _snackBarState.value = "Shop name cannot be empty"
                    onFailure()
                    return
                }
                shopName.text.length < 3 -> {
                    _snackBarState.value = "Shop name must be at least 3 characters"
                    onFailure()
                    return
                }
                propName.text.isBlank() -> {
                    _snackBarState.value = "Proprietor name cannot be empty"
                    onFailure()
                    return
                }
                propName.text.length < 2 -> {
                    _snackBarState.value = "Proprietor name must be at least 2 characters"
                    onFailure()
                    return
                }
                userEmail.text.isBlank() -> {
                    _snackBarState.value = "Email address is required"
                    onFailure()
                    return
                }
                !InputValidator.isValidEmail(userEmail.text) -> {
                    _snackBarState.value = "Please enter a valid email address"
                    onFailure()
                    return
                }
                userMobile.text.isBlank() -> {
                    _snackBarState.value = "Mobile number is required"
                    onFailure()
                    return
                }
                !InputValidator.isValidPhoneNumber(userMobile.text) -> {
                    _snackBarState.value = "Please enter a valid 10-digit mobile number"
                    onFailure()
                    return
                }
                address.text.isBlank() -> {
                    _snackBarState.value = "Store address is required"
                    onFailure()
                    return
                }
                address.text.length < 10 -> {
                    _snackBarState.value = "Please enter a complete address (minimum 10 characters)"
                    onFailure()
                    return
                }
                registrationNo.text.isBlank() -> {
                    _snackBarState.value = "Registration number is required"
                    onFailure()
                    return
                }
                gstinNo.text.isBlank() -> {
                    _snackBarState.value = "GSTIN number is required"
                    onFailure()
                    return
                }
                !InputValidator.isValidGSTIN(gstinNo.text) -> {
                    _snackBarState.value = "Please enter a valid GSTIN number (15 characters, format: 22AAAAA0000A1Z5)"
                    onFailure()
                    return
                }
                panNumber.text.isBlank() -> {
                    _snackBarState.value = "PAN number is required"
                    onFailure()
                    return
                }
                !InputValidator.isValidPAN(panNumber.text) -> {
                    _snackBarState.value = "Please enter a valid PAN number (10 characters, format: ABCDE1234F)"
                    onFailure()
                    return
                }*/
        }

        ioLaunch {
            try {
                isLoading.value = true
                val storeId = _dataStoreManager.storeId.first()
                val userId = _dataStoreManager.userId.first()
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
                    upiId = upiId.text.trim()
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
                        _dataStoreManager.setValue(DataStoreManager.STORE_ID_KEY, generatedId)
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
                        _dataStoreManager.setValue(
                            DataStoreManager.STORE_ID_KEY,
                            storeEntity.storeId
                        )

                        _dataStoreManager.setUpiId(upiId.text.trim())
                        _dataStoreManager.setMerchantName(shopName.text.trim())
                    } else {
                        log("Failed to save store to local database")
                        _snackBarState.value = "Failed to save store details. Please try again."
                        onFailure()
                    }
                    onSuccess()
                } else {
                    log("Failed to save to Firestore: ${firestoreResult.exceptionOrNull()?.message}")
                    _snackBarState.value = "Saved locally but failed to sync with cloud"
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


    fun setSelectedImageFile(imageUri: Uri?) {
        selectedImageFileUri.value = imageUri
        selectedImageUri.value = imageUri?.toString() // Set the URI string for immediate display
        shopImage.text = imageUri?.toString() ?: ""
    }

    /**
     * Sync store data from Firestore to local database
     */
    fun syncFromFirestore(onSuccess: () -> Unit, onFailure: () -> Unit) {
        ioLaunch {
            try {
                isLoading.value = true
                val storeId = _dataStoreManager.storeId.first()
                val userId = _dataStoreManager.userId.first()
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
                        if (existingStore == null) {
                            _dataStoreManager.setValue(
                                DataStoreManager.STORE_ID_KEY,
                                storeFromFirestore.storeId
                            )
                        }

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