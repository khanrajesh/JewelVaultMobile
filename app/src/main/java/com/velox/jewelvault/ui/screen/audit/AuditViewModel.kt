package com.velox.jewelvault.ui.screen.audit

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velox.jewelvault.data.roomdb.AppDatabase
import com.velox.jewelvault.data.roomdb.dto.CatSubCatDto
import com.velox.jewelvault.data.roomdb.dto.ItemSelectedModel
import com.velox.jewelvault.data.roomdb.entity.ItemEntity
import com.velox.jewelvault.data.roomdb.entity.category.SubCategoryEntity
import com.velox.jewelvault.data.DataStoreManager
import com.velox.jewelvault.ui.components.InputFieldState
import com.velox.jewelvault.utils.withIo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class AuditViewModel @Inject constructor(
    private val context: Context,
    private val appDatabase: AppDatabase,
    private val _dataStoreManager: DataStoreManager,
    private val _loadingState: MutableState<Boolean>,
    @Named("snackMessage") private val _snackBarState: MutableState<String>,
) : ViewModel() {

    val snackBarState = _snackBarState

    /**
     * return Triple of Flow<String> for userId, userName, mobileNo
     * */
    val admin: Triple<Flow<String>, Flow<String>, Flow<String>> = _dataStoreManager.getAdminInfo()

    /**
     * return Triple of Flow<String> for storeId, upiId, storeName
     * */
    val store: Triple<Flow<String>, Flow<String>, Flow<String>> = _dataStoreManager.getSelectedStoreInfo()
    
    // Category and Subcategory selection using InputFieldState
    val selectedCategory = InputFieldState()
    val selectedSubCategory = InputFieldState()
    val selectedCategoryId = mutableStateOf("")
    val selectedSubCategoryId = mutableStateOf("")
    
    // Available categories and subcategories
    val catSubCatDto: SnapshotStateList<CatSubCatDto> = SnapshotStateList()
    val availableSubCategories: SnapshotStateList<SubCategoryEntity> = SnapshotStateList()
    
    // Items in the selected category/subcategory
    val allItems: SnapshotStateList<ItemEntity> = SnapshotStateList()
    val scannedItemIds = mutableStateListOf<String>()
    val scannedItems: SnapshotStateList<ItemSelectedModel> = SnapshotStateList()

    init {
        loadCategoriesAndSubCategories()
    }

    fun loadCategoriesAndSubCategories() {
        viewModelScope.launch {
            try {
                _loadingState.value = true
                val userId = admin.first.first()
                val storeId = store.first.first()
                
                val categories = withIo { 
                    appDatabase.categoryDao().getCategoriesByUserIdAndStoreId(userId, storeId) 
                }
                
                val newList = categories.map { cat ->
                    val subList = withIo { 
                        appDatabase.subCategoryDao().getSubCategoriesByCatId(cat.catId) 
                    }
                    CatSubCatDto(
                        catId = cat.catId,
                        catName = cat.catName,
                        gsWt = cat.gsWt,
                        fnWt = cat.fnWt,
                        userId = userId,
                        storeId = storeId,
                        subCategoryList = SnapshotStateList<SubCategoryEntity>().apply {
                            addAll(subList)
                        }
                    )
                }
                
                catSubCatDto.clear()
                catSubCatDto.addAll(newList)
                
            } catch (e: Exception) {
                _snackBarState.value = "Error loading categories: ${e.message}"
            } finally {
                _loadingState.value = false
            }
        }
    }

    fun onCategorySelected(categoryName: String) {
        selectedCategory.text = categoryName
        val selectedCat = catSubCatDto.find { it.catName == categoryName }
        selectedCategoryId.value = selectedCat?.catId ?: ""
        
        // Update available subcategories
        availableSubCategories.clear()
        selectedCat?.subCategoryList?.let { 
            availableSubCategories.addAll(it) 
        }
        
        // Reset subcategory selection
        selectedSubCategory.text = ""
        selectedSubCategoryId.value = ""
        
        // Load items for the selected category
        loadItemsForCategory()
    }

    fun onSubCategorySelected(subCategoryName: String) {
        selectedSubCategory.text = subCategoryName
        val selectedSubCat = availableSubCategories.find { it.subCatName == subCategoryName }
        selectedSubCategoryId.value = selectedSubCat?.subCatId ?: ""
        
        // Load items for the selected subcategory
        loadItemsForSubCategory()
    }

    private fun loadItemsForCategory() {
        if (selectedCategoryId.value.isEmpty()) return
        
        viewModelScope.launch {
            try {
                _loadingState.value = true
                val userId = admin.first.first()
                val storeId = store.first.first()
                val items = withIo { 
                    appDatabase.itemDao().getAllItemsByUserIdAndStoreId(userId, storeId)
                        .filter { it.catId == selectedCategoryId.value }
                }
                
                allItems.clear()
                allItems.addAll(items)
                
            } catch (e: Exception) {
                _snackBarState.value = "Error loading items: ${e.message}"
            } finally {
                _loadingState.value = false
            }
        }
    }

    private fun loadItemsForSubCategory() {
        if (selectedSubCategoryId.value.isEmpty()) return
        
        viewModelScope.launch {
            try {
                _loadingState.value = true
                val userId = admin.first.first()
                val storeId = store.first.first()
                val items = withIo { 
                    appDatabase.itemDao().getAllItemsByUserIdAndStoreId(userId, storeId)
                        .filter { 
                            it.catId == selectedCategoryId.value && 
                            it.subCatId == selectedSubCategoryId.value 
                        }
                }
                
                allItems.clear()
                allItems.addAll(items)
                
            } catch (e: Exception) {
                _snackBarState.value = "Error loading items: ${e.message}"
            } finally {
                _loadingState.value = false
            }
        }
    }

    fun processScannedItem(itemId: String) {
        viewModelScope.launch {
            try {
                // Check if item exists in our filtered list
                val item = allItems.find { it.itemId == itemId }
                if (item == null) {
                    _snackBarState.value = "Item not found in selected category/subcategory"
                    return@launch
                }
                
                // Check if already scanned
                if (scannedItemIds.contains(itemId)) {
                    _snackBarState.value = "Item already scanned"
                    return@launch
                }
                
                // Convert to ItemSelectedModel
                val itemSelected = ItemSelectedModel(
                    itemId = item.itemId,
                    itemAddName = item.itemAddName,
                    catId = item.catId,
                    userId = item.userId,
                    storeId = item.storeId,
                    catName = item.catName,
                    subCatId = item.subCatId,
                    subCatName = item.subCatName,
                    entryType = item.entryType,
                    quantity = item.quantity,
                    gsWt = item.gsWt,
                    ntWt = item.ntWt,
                    fnWt = item.fnWt,
                    fnMetalPrice = 0.0,
                    purity = item.purity,
                    crgType = item.crgType,
                    crg = item.crg,
                    othCrgDes = item.othCrgDes,
                    othCrg = item.othCrg,
                    cgst = item.cgst,
                    sgst = item.sgst,
                    igst = item.igst,
                    huid = item.huid,
                    addDate = item.addDate,
                    addDesKey = item.addDesKey,
                    addDesValue = item.addDesValue,
                    sellerFirmId = item.sellerFirmId,
                    purchaseOrderId = item.purchaseOrderId,
                    purchaseItemId = item.purchaseItemId,
                )
                
                // Add to scanned items
                scannedItemIds.add(itemId)
                scannedItems.add(itemSelected)
                
            } catch (e: Exception) {
                _snackBarState.value = "Error processing scanned item: ${e.message}"
            }
        }
    }

    fun clearScannedItems() {
        scannedItemIds.clear()
        scannedItems.clear()
    }

    fun isItemScanned(itemId: String): Boolean {
        return scannedItemIds.contains(itemId)
    }

    fun getScannedCount(): Int {
        return scannedItemIds.size
    }

    fun getTotalCount(): Int {
        return allItems.size
    }
}
