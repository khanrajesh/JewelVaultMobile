package com.velox.jewelvault.ui.screen.inventory

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velox.jewelvault.data.roomdb.AppDatabase
import com.velox.jewelvault.data.roomdb.dto.CatSubCatDto
import com.velox.jewelvault.data.roomdb.entity.CategoryEntity
import com.velox.jewelvault.data.roomdb.entity.ItemEntity
import com.velox.jewelvault.data.roomdb.entity.SubCategoryEntity
import com.velox.jewelvault.data.roomdb.entity.purchase.PurchaseOrderEntity
import com.velox.jewelvault.data.roomdb.entity.purchase.PurchaseOrderItemEntity
import com.velox.jewelvault.ui.components.InputFieldState
import com.velox.jewelvault.utils.DataStoreManager
import com.velox.jewelvault.utils.ioLaunch
import com.velox.jewelvault.utils.ioScope
import com.velox.jewelvault.utils.log
import com.velox.jewelvault.utils.mainScope
import com.velox.jewelvault.utils.roundTo3Decimal
import com.velox.jewelvault.utils.withIo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.sql.Timestamp
import javax.inject.Inject

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val appDatabase: AppDatabase, private val _dataStoreManager: DataStoreManager,
//    private val _loadingState: MutableState<Boolean>,
    private val _snackBarState: MutableState<String>, context: Context
) : ViewModel() {

    val dataStoreManager = _dataStoreManager

    //    val loadingState = _loadingState
    val snackBarState = _snackBarState
    val itemList: SnapshotStateList<ItemEntity> = SnapshotStateList()
    val catSubCatDto: SnapshotStateList<CatSubCatDto> = SnapshotStateList()
    val purchaseOrdersByDate: SnapshotStateList<PurchaseOrderEntity> = SnapshotStateList()

    val purchaseItems: SnapshotStateList<PurchaseOrderItemEntity> = SnapshotStateList()


    val addToName = InputFieldState()
    val entryType = InputFieldState()
    val qty = InputFieldState()
    val grWt = InputFieldState()
    val ntWt = InputFieldState()
    val purity = InputFieldState()
    val fnWt = InputFieldState()
    val chargeType = InputFieldState()
    val charge = InputFieldState()
    val otherChargeDes = InputFieldState()
    val othCharge = InputFieldState()
    val cgst = InputFieldState("1.5")
    val sgst = InputFieldState("1.5")
    val igst = InputFieldState()
    val desKey = InputFieldState()
    val desValue = InputFieldState()
    val huid = InputFieldState()
    val billDate = InputFieldState()
    val billNo = InputFieldState()
    val billItemDetails = mutableStateOf("")


    val isSelf = mutableStateOf(true)

    val itemHeaderList = listOf(
        "S.No",
        "Category",
        "Sub Category",
        "Item Id",
        "To Name",
        "Type",
        "Qty",
        "Gr.Wt",
        "Nt.Wt",
        "Unit",
        "Purity",
        "Fn.Wt",
        "MC.Type",
        "M.Chr",
        "Oth Chr",
        "Chr",
        "Tax",
        "H-UID",
        "DOA",
        "Des",
        "Value",
        "Extra"
    )




    fun getCategoryAndSubCategoryDetails() {
        ioScope {
            try {
                val userId = dataStoreManager.userId.first()
                val storeId = dataStoreManager.storeId.first()
                val result =
                    appDatabase.categoryDao().getCategoriesByUserIdAndStoreId(userId, storeId)

                val newList = result.map { cat ->
                    val subList = appDatabase.subCategoryDao().getSubCategoriesByCatId(cat.catId)
                    CatSubCatDto(catId = cat.catId,
                        catName = cat.catName,
                        gsWt = cat.gsWt,
                        fnWt = cat.fnWt,
                        userId = userId,
                        storeId = storeId,
                        subCategoryList = mutableStateListOf<SubCategoryEntity>().apply {
                            addAll(subList)
                        })
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
        ioLaunch {
            try {
                val userId = dataStoreManager.userId.first()
                val storeId = dataStoreManager.storeId.first()
                val s = appDatabase.categoryDao().insertCategory(
                    CategoryEntity(
                        catName = catName, userId = userId, storeId = storeId
                    )
                )
                if (s != -1L) {
                    _snackBarState.value = "Added new category id: $s"
                    this@InventoryViewModel.log("Added new category id: $s")
                    getCategoryAndSubCategoryDetails()
                } else {
                    _snackBarState.value = "failed to add category"
                    this@InventoryViewModel.log("failed to add category")
                }
            } catch (e: Exception) {
                _snackBarState.value = "unable to add category error: ${e.message}"
                this@InventoryViewModel.log("unable to add category error: ${e.message}")
            }
        }
    }

    fun addSubCategory(subCatName: String, catName: String, catId: Int) {
        ioLaunch {
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
                    _snackBarState.value = "Added new sub category id: $s"
                    this@InventoryViewModel.log("Added new sub category id: $s")
                    getCategoryAndSubCategoryDetails()
                } else {
                    _snackBarState.value = "failed to add sub category"
                    this@InventoryViewModel.log("failed to add sub category")
                }
            } catch (e: Exception) {
                _snackBarState.value = "unable to add sub category"
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
        ioLaunch {
                try {
//                    _loadingState.value = true
                    appDatabase.itemDao()
                        .filterItems(catId, subCatId, type, purity, crgType, startDate, endDate)
                        .collectLatest { items ->
                            itemList.clear()
                            itemList.addAll(items)
                        }
//                    _snackBarState.value = "Item Filtered"
                } catch (e: Exception) {
//                    _snackBarState.value="failed to filler item list error: ${e.localizedMessage}"
                    this@InventoryViewModel.log("failed to filler item list")
                }
//                _loadingState.value = false

        }
    }

    fun safeInsertItem(
        item: ItemEntity, onSuccess: (ItemEntity, Long) -> Unit, onFailure: (Throwable) -> Unit
    ) {
        viewModelScope.launch {
//            _loadingState.value = true
            try {
                withIo {
                    val userId = dataStoreManager.userId.first()
                    val storeId = dataStoreManager.storeId.first()
                    val it = item.copy(
                        userId = userId, storeId = storeId
                    )

                    val newItemId = appDatabase.itemDao().insert(it)

                    if (newItemId != -1L) {
                        val insertedItem = it.copy(itemId = newItemId.toInt())

                        try {
                            val subCategory = appDatabase.subCategoryDao()
                                .getSubCategoryById(subCatId = it.subCatId)
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
                                    val cat = appDatabase.categoryDao()
                                        .getCategoryById(catId = insertedItem.catId)
                                    cat?.let { catEntity ->
                                        val catGsWt =
                                            (catEntity.gsWt + insertedItem.gsWt).roundTo3Decimal()
                                        val catFnWt =
                                            (catEntity.fnWt + insertedItem.fnWt).roundTo3Decimal()
                                        val upCat = appDatabase.categoryDao().updateWeights(
                                            catId = insertedItem.catId,
                                            gsWt = catGsWt,
                                            fnWt = catFnWt
                                        )
                                        if (upCat != -1) {
                                            this@InventoryViewModel.log("Cat id: ${insertedItem.catId} update with weight")
                                            _snackBarState.value =
                                                "Item Added and categories updated."
//                                            _loadingState.value = false
                                            onSuccess(insertedItem, newItemId)
                                        }
                                    }
                                } else {
                                    _snackBarState.value = "Failed to update Sub Category Weight"
//                                    _loadingState.value = false
                                }
                            }
                        } catch (e: Exception) {
//                            _loadingState.value = false
                            _snackBarState.value =
                                ("Error updating cat and sub cat weight: ${e.message}")
                            this@InventoryViewModel.log("Error updating cat and sub cat weight: ${e.message}")
                        }
//                        _loadingState.value = true
                    } else {
//                        _loadingState.value = false
                        _snackBarState.value = ("Failed to insert item")
                        this@InventoryViewModel.log("Failed to insert item")
                    }
                }
            } catch (e: Exception) {
//                _loadingState.value = false
                _snackBarState.value = "Error inserting item error: ${e.message}"
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
                withIo {
                    val item = appDatabase.itemDao().getItemById(itemId)
                    if (item != null) {
                        val rowsDeleted = appDatabase.itemDao().deleteById(itemId, catId, subCatId)
                        if (rowsDeleted > 0) {
                            try {
                                val subCategory =
                                    appDatabase.subCategoryDao().getSubCategoryById(subCatId)
                                subCategory?.let {
                                    val updatedSubCatGsWt =
                                        (it.gsWt - item.gsWt).coerceAtLeast(0.0).roundTo3Decimal()
                                    val updatedSubCatFnWt =
                                        (it.fnWt - item.fnWt).coerceAtLeast(0.0).roundTo3Decimal()
                                    val updatedSubCatQty =
                                        (it.quantity - item.quantity).coerceAtLeast(0)

                                    appDatabase.subCategoryDao().updateWeightsAndQuantity(
                                        subCatId = subCatId,
                                        gsWt = updatedSubCatGsWt,
                                        fnWt = updatedSubCatFnWt,
                                        quantity = updatedSubCatQty
                                    )
                                }

                                val category = appDatabase.categoryDao().getCategoryById(catId)
                                category?.let {
                                    val updatedCatGsWt =
                                        (it.gsWt - item.gsWt).coerceAtLeast(0.0).roundTo3Decimal()
                                    val updatedCatFnWt =
                                        (it.fnWt - item.fnWt).coerceAtLeast(0.0).roundTo3Decimal()

                                    appDatabase.categoryDao().updateWeights(
                                        catId = catId, gsWt = updatedCatGsWt, fnWt = updatedCatFnWt
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


    fun getBillsFromDate() {
        ioLaunch {
            billItemDetails.value = ""
            purchaseOrdersByDate.clear()
            billNo.clear()
            val data = appDatabase.purchaseDao().getOrdersByBillDate(billDate.text)
            purchaseOrdersByDate.addAll(data)

            if (purchaseOrdersByDate.isEmpty()){
                _snackBarState.value = "No purchase bill found"
            }
        }
    }

    fun getPurchaseOrderItemDetails(item: PurchaseOrderEntity, subCatName: String) {
        ioLaunch {
            val purchaseItemList = appDatabase.purchaseDao()
                .getItemsByOrderIdAndSubCatName(item.purchaseOrderId, subCatName)
            purchaseItems.clear()
            billItemDetails.value = ""
            if (purchaseItemList.isNotEmpty()){
                val sellerInfo = appDatabase.purchaseDao().getSellerById(item.sellerId)

                if (sellerInfo!=null){
                    //todo
//                    sellerFirmId = 0,
//                    purchaseOrderId = 0,
//                    purchaseItemId = 0,
                    val firInfo = appDatabase.purchaseDao().getFirmById(sellerInfo.firmId)
                    val t = "${firInfo?.firmName} by ${sellerInfo.name} (${sellerInfo.mobileNumber})"
                    val u = purchaseItemList.groupBy { it.purity }.map { (purity, items) ->
                        val total = items.sumOf { it.gsWt }
                        "Purity: $purity: Total Gs Wt: $total gm "
                    }.joinToString(", ")
                    billItemDetails.value  = "$t \n$u"

                    purchaseItems.addAll(purchaseItemList)
                }
            }
        }
    }

}