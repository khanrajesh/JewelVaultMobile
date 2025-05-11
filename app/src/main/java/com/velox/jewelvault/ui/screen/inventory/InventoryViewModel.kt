package com.velox.jewelvault.ui.screen.inventory

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velox.jewelvault.data.roomdb.AppDatabase
import com.velox.jewelvault.data.roomdb.dao.ItemDao
import com.velox.jewelvault.data.roomdb.dto.CatSubCatDto
import com.velox.jewelvault.data.roomdb.entity.CategoryEntity
import com.velox.jewelvault.data.roomdb.entity.ItemEntity
import com.velox.jewelvault.data.roomdb.entity.SubCategoryEntity
import com.velox.jewelvault.utils.DataStoreManager
import com.velox.jewelvault.utils.ioScope
import com.velox.jewelvault.utils.log
import com.velox.jewelvault.utils.mainScope
import com.velox.jewelvault.utils.roundTo3Decimal
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.sql.Timestamp
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val appDatabase: AppDatabase,
    private val _dataStoreManager: DataStoreManager,
    context: Context
) : ViewModel() {

    val dataStoreManager = _dataStoreManager

    val itemList: SnapshotStateList<ItemEntity> = SnapshotStateList()
    val catSubCatDto: SnapshotStateList<CatSubCatDto> = SnapshotStateList()


    fun init() {
        checkForCategory()
    }


    private fun checkForCategory() {
        ioScope {
            try {
                val userId = dataStoreManager.userId.first()
                val storeId = dataStoreManager.storeId.first()
                val result =
                    appDatabase.categoryDao().getCategoriesByUserIdAndStoreId(userId, storeId)

                if (result.isEmpty()) {
                    this@InventoryViewModel.log("No Category exists")
                    val g = appDatabase.categoryDao().insertCategory(
                        CategoryEntity(
                            catName = "Gold",
                            userId = userId,
                            storeId = storeId
                        )
                    )
                    val s = appDatabase.categoryDao().insertCategory(
                        CategoryEntity(
                            catName = "Silver",
                            userId = userId,
                            storeId = storeId
                        )
                    )

                    if (g != -1L && s != -1L) {
                        this@InventoryViewModel.log("Added new category gold and silver")
                    } else {
                        this@InventoryViewModel.log("Unable to added new category gold and silver")
                    }
                } else {
                    this@InventoryViewModel.log("Category exists")
                }

                getCategoryAndSubCategoryDetails()
            } catch (e: Exception) {
                this@InventoryViewModel.log("error: ${e.message}")
            }
        }
    }

    private fun getCategoryAndSubCategoryDetails() {
        ioScope {
            try {
                val userId = dataStoreManager.userId.first()
                val storeId = dataStoreManager.storeId.first()
                val result =
                    appDatabase.categoryDao().getCategoriesByUserIdAndStoreId(userId, storeId)

                val newList = result.map { cat ->
                    val subList = appDatabase.subCategoryDao().getSubCategoriesByCatId(cat.catId)
                    CatSubCatDto(
                        catId = cat.catId,
                        catName = cat.catName,
                        gsWt = cat.gsWt,
                        fnWt = cat.fnWt,
                        userId = userId,
                        storeId = storeId,
                        subCategoryList = mutableStateListOf<SubCategoryEntity>().apply {
                            addAll(subList)
                        }
                    )
                }

                mainScope {
                    // update only if different
                    if (catSubCatDto != newList) {
                        catSubCatDto.clear()
                        catSubCatDto.addAll(newList)
                    }
                }
            } catch (e: Exception) {
                this@InventoryViewModel.log("unable to fetch categorical data")
            }
        }
    }

    fun addCategory(catName: String) {
        ioScope {
            try {
                val userId = dataStoreManager.userId.first()
                val storeId = dataStoreManager.storeId.first()
                val s = appDatabase.categoryDao().insertCategory(
                    CategoryEntity(
                        catName = catName,
                        userId = userId,
                        storeId = storeId
                    )
                )
                if (s != -1L) {
                    this@InventoryViewModel.log("Added new category id: $s")
                    getCategoryAndSubCategoryDetails()
                } else {
                    this@InventoryViewModel.log("failed to add category")
                }
            } catch (e: Exception) {
                this@InventoryViewModel.log("unable to add category error: ${e.message}")
            }
        }
    }

    fun addSubCategory(subCatName: String, catName: String, catId: Int) {
        ioScope {
            try {
                val userId = dataStoreManager.userId.first()
                val storeId = dataStoreManager.storeId.first()
                val s = appDatabase.subCategoryDao().insertSubCategory(
                    SubCategoryEntity(
                        subCatName = subCatName,
                        catId = catId,
                        catName = catName,
                        userId = userId,
                        storeId = storeId
                    )
                )
                if (s != -1L) {
                    this@InventoryViewModel.log("Added new sub category id: $s")
                    getCategoryAndSubCategoryDetails()
                } else {
                    this@InventoryViewModel.log("failed to add sub category")
                }
            } catch (e: Exception) {
                this@InventoryViewModel.log("unable to add sub category")
            }
        }
    }

//    fun getAllItems() {
//        viewModelScope.launch {
//            appDatabase.itemDao().getAll().collect { items ->
//                // Update the SnapshotStateList with the fetched items
//                itemList.clear() // Clear the list before adding new data
//                itemList.addAll(items) // Add all items to the SnapshotStateList
//            }
//        }
//    }

    fun filterItems(
        catId: Int? = null,
        subCatId: Int? = null,
        type: String? = null,
        purity: String? = null,
        crgType: String? = null,
        startDate: Timestamp? = null,
        endDate: Timestamp? = null
    ) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    appDatabase.itemDao()
                        .filterItems(catId, subCatId, type, purity, crgType, startDate, endDate)
                        .collectLatest { items ->
                            itemList.clear() // Clear the list before adding new data
                            itemList.addAll(items) // Add all items to the SnapshotStateList
                        }
                } catch (e: Exception) {
                    this@InventoryViewModel.log("failed to filler item list")
                }
            }
        }
    }

    fun safeInsertItem(
        item: ItemEntity,
        onSuccess: (ItemEntity, Long) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val userId = dataStoreManager.userId.first()
                    val storeId = dataStoreManager.storeId.first()
                    val it = item.copy(
                        userId = userId,
                        storeId = storeId
                    )

                    val newItemId = appDatabase.itemDao().insert(it)

                    if (newItemId != -1L) {
                        val insertedItem = it.copy(itemId = newItemId.toInt())

                        try {
                            val subCategory = appDatabase.subCategoryDao().getSubCategoryById(subCatId = it.subCatId)
                            subCategory?.let { subCat ->
                                val subCatGsWt = (subCat.gsWt + insertedItem.gsWt).roundTo3Decimal()
                                val subCatFnWt = (subCat.fnWt + insertedItem.fnWt).roundTo3Decimal()
                                val subCatQty = subCat.quantity + insertedItem.quantity

                                val upSub = appDatabase.subCategoryDao().updateWeightsAndQuantity(
                                    subCatId = insertedItem.subCatId,
                                    gsWt = subCatGsWt,
                                    fnWt = subCatFnWt,
                                    quantity = subCatQty
                                )

                                if (upSub != -1) {
                                    this@InventoryViewModel.log("Sub Cat id: ${insertedItem.subCatId} update with weight")

                                    val cat = appDatabase.categoryDao().getCategoryById(catId = insertedItem.catId)
                                    cat?.let { catEntity ->
                                        val catGsWt = (catEntity.gsWt + insertedItem.gsWt).roundTo3Decimal()
                                        val catFnWt = (catEntity.fnWt + insertedItem.fnWt).roundTo3Decimal()
                                        val upCat = appDatabase.categoryDao().updateWeights(
                                            catId = insertedItem.catId,
                                            gsWt = catGsWt,
                                            fnWt = catFnWt
                                        )
                                        if (upCat != -1) {
                                            this@InventoryViewModel.log("Cat id: ${insertedItem.catId} update with weight")
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            this@InventoryViewModel.log("Error updating cat and sub cat weight: ${e.message}")
                        }
                        onSuccess(insertedItem, newItemId)
                    } else {
                        this@InventoryViewModel.log("Failed to insert item")
                    }
                }
            } catch (e: Exception) {
                onFailure(e)
            }
        }
    }

    fun safeDeleteItem(
        itemId: Int,
        catId: Int,
        subCatId: Int,
        onSuccess: (Int) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val item = appDatabase.itemDao().getItemById(itemId)
                    if (item != null) {
                        val rowsDeleted = appDatabase.itemDao().deleteById(itemId, catId, subCatId)
                        if (rowsDeleted > 0) {
                            try {
                                val subCategory = appDatabase.subCategoryDao().getSubCategoryById(subCatId)
                                subCategory?.let {
                                    val updatedSubCatGsWt = (it.gsWt - item.gsWt).coerceAtLeast(0.0).roundTo3Decimal()
                                    val updatedSubCatFnWt = (it.fnWt - item.fnWt).coerceAtLeast(0.0).roundTo3Decimal()
                                    val updatedSubCatQty = (it.quantity - item.quantity).coerceAtLeast(0)

                                    appDatabase.subCategoryDao().updateWeightsAndQuantity(
                                        subCatId = subCatId,
                                        gsWt = updatedSubCatGsWt,
                                        fnWt = updatedSubCatFnWt,
                                        quantity = updatedSubCatQty
                                    )
                                }

                                val category = appDatabase.categoryDao().getCategoryById(catId)
                                category?.let {
                                    val updatedCatGsWt = (it.gsWt - item.gsWt).coerceAtLeast(0.0).roundTo3Decimal()
                                    val updatedCatFnWt = (it.fnWt - item.fnWt).coerceAtLeast(0.0).roundTo3Decimal()

                                    appDatabase.categoryDao().updateWeights(
                                        catId = catId,
                                        gsWt = updatedCatGsWt,
                                        fnWt = updatedCatFnWt
                                    )
                                }

                                onSuccess(rowsDeleted)
                            } catch (e: Exception) {
                                onFailure(e)
                            }
                        } else {
                            this@InventoryViewModel.log("No rows deleted. Check itemId, catId, and subCatId.")
                        }
                    } else {
                        this@InventoryViewModel.log("No Item found with ID $itemId")
                    }
                }
            } catch (e: Exception) {
                onFailure(e)
            }
        }
    }






}