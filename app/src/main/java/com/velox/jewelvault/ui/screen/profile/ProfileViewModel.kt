package com.velox.jewelvault.ui.screen.profile

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.velox.jewelvault.data.roomdb.AppDatabase
import com.velox.jewelvault.data.roomdb.entity.CategoryEntity
import com.velox.jewelvault.data.roomdb.entity.StoreEntity
import com.velox.jewelvault.data.roomdb.entity.SubCategoryEntity
import com.velox.jewelvault.ui.components.InputFieldState
import com.velox.jewelvault.utils.DataStoreManager
import com.velox.jewelvault.utils.ioLaunch
import com.velox.jewelvault.utils.log
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val _dataStoreManager: DataStoreManager,
    private val appDatabase: AppDatabase,
    private val _snackBarState: MutableState<String>
) : ViewModel() {

    val snackBarState = _snackBarState

    val shopName = InputFieldState()
    val propName = InputFieldState()
    val shopImage = InputFieldState()
    val address = InputFieldState()
    val registrationNo = InputFieldState()
    val gstinNo = InputFieldState()
    val panNumber = InputFieldState()

    val userEmail = InputFieldState()
    val userMobile = InputFieldState()

    val storeEntity = mutableStateOf<StoreEntity?>(null)


    fun initializeDefaultCategories() {
        ioLaunch {
            try {
                val userId = _dataStoreManager.userId.first()
                val storeId = _dataStoreManager.storeId.first()

                val existingCategories = appDatabase.categoryDao().getCategoriesByUserIdAndStoreId(userId,storeId)
                log("Existing categories: $existingCategories")

                if (existingCategories.isNotEmpty()) {
                    log("Categories already exist.")
                    return@ioLaunch
                }

                log("No categories found. Inserting default 'Gold' and 'Silver'...")

                val defaultCategories = listOf(
                    Triple("Gold", 1, "Fine"),
                    Triple("Silver", 2, "Fine")
                )

                for ((catName, catId, subCatName) in defaultCategories) {
                    val categoryExists = appDatabase.categoryDao().getCategoryByName(catName)

                    if (categoryExists == null) {
                        val insertResult = appDatabase.categoryDao().insertCategory(
                            CategoryEntity(catId = catId, catName = catName, userId = userId, storeId = storeId)
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

    fun getStoreData() {
        ioLaunch {
            val userId = _dataStoreManager.userId.first()
            val userData = appDatabase.userDao().getUserById(userId)
            storeEntity.value = appDatabase.storeDao().getStoreById(userId)

            userData?.let {
                userEmail.text = it.email ?: ""
                userMobile.text = it.mobileNo?:""
            }

            storeEntity.value?.let {
                propName.text = it.proprietor
                userMobile.text = it.phone
                shopName.text = it.name
                shopImage.text = it.image
                address.text = it.address
                registrationNo.text = it.registrationNo
                gstinNo.text = it.gstinNo
                panNumber.text = it.panNo
            }
        }
    }

    fun saveStoreData(onSuccess: () -> Unit, onFailure: () -> Unit) {
        ioLaunch {
            try {
                val storeId = _dataStoreManager.storeId.first()
                val userId = _dataStoreManager.userId.first()

                if (storeId != -1) {
                    val storeEntity = StoreEntity(
                        storeId = storeId,
                        userId = userId,
                        proprietor = propName.text,
                        name = shopName.text,
                        phone = userMobile.text,
                        address = address.text,
                        registrationNo = registrationNo.text,
                        gstinNo = gstinNo.text,
                        panNo = panNumber.text,
                        image = "",
                    )
                    if (appDatabase.storeDao().updateStore(storeEntity) != -1) onSuccess() else onFailure()
                } else {
                    val storeEntity = StoreEntity(
                        userId = userId,
                        proprietor = propName.text,
                        name = shopName.text,
                        phone = userMobile.text,
                        address = address.text,
                        registrationNo = registrationNo.text,
                        gstinNo = gstinNo.text,
                        panNo = panNumber.text,
                        image = "",
                    )

                    val result = appDatabase.storeDao().insertStore(storeEntity)
                    if (result != -1L) {
                        _dataStoreManager.setValue(DataStoreManager.STORE_ID_KEY, result.toInt())
                        onSuccess()
                    } else onFailure()
                }
            } catch (e: Exception) {

            }
        }
    }

}