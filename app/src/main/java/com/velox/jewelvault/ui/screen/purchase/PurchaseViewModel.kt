package com.velox.jewelvault.ui.screen.purchase

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velox.jewelvault.data.MetalRate
import com.velox.jewelvault.data.roomdb.AppDatabase
import com.velox.jewelvault.data.roomdb.dto.CatSubCatDto
import com.velox.jewelvault.data.roomdb.dto.PurchaseItemInputDto
import com.velox.jewelvault.data.roomdb.dto.PurchaseMetalRateDto
import com.velox.jewelvault.data.roomdb.entity.ItemEntity
import com.velox.jewelvault.data.roomdb.entity.SubCategoryEntity
import com.velox.jewelvault.data.roomdb.entity.purchase.FirmEntity
import com.velox.jewelvault.data.roomdb.entity.purchase.MetalExchangeEntity
import com.velox.jewelvault.data.roomdb.entity.purchase.PurchaseOrderEntity
import com.velox.jewelvault.data.roomdb.entity.purchase.PurchaseOrderItemEntity
import com.velox.jewelvault.data.roomdb.entity.purchase.SellerEntity
import com.velox.jewelvault.ui.components.InputFieldState
import com.velox.jewelvault.data.DataStoreManager
import com.velox.jewelvault.utils.generateId
import com.velox.jewelvault.utils.getCurrentDate
import com.velox.jewelvault.utils.getCurrentTimestamp
import com.velox.jewelvault.utils.ioLaunch
import com.velox.jewelvault.utils.log
import com.velox.jewelvault.utils.mainScope
import com.velox.jewelvault.utils.roundTo3Decimal
import com.velox.jewelvault.utils.withIo
import com.velox.jewelvault.utils.withMain
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject


@HiltViewModel
class PurchaseViewModel @Inject constructor(
    private val appDatabase: AppDatabase,
    private val _dataStoreManager: DataStoreManager,
    private val _loadingState: MutableState<Boolean>,
    private val _snackBarState: MutableState<String>,
    private val _metalRates: SnapshotStateList<MetalRate>,
) : ViewModel() {

    val snackBarState = _snackBarState
    val metalRates = _metalRates

    val firmName = InputFieldState()
    val firmMobile = InputFieldState()
    val firmAddress = InputFieldState()
    val firmGstin = InputFieldState()


    val sellerName = InputFieldState()
    val sellerMobile = InputFieldState()
    val addBillNo = InputFieldState()


    val addBillDate = InputFieldState()
    val addCGst = InputFieldState("1.5")
    val addIGst = InputFieldState("1.5")
    val addSGst = InputFieldState()


    val addItemCat = InputFieldState()
    val addItemSubCat = InputFieldState()
    val addItemGsWt = InputFieldState()
    val addItemPurity = InputFieldState()
    val addItemFnWt = InputFieldState()
    val addItemNtWt = InputFieldState()
    val addItemFineRatePerGm = InputFieldState()
    val addItemWastage = InputFieldState()
    val addItemExtraChargeDes = InputFieldState()
    val addItemExtraCharge = InputFieldState()



    val addExchangeCategory = InputFieldState()
    val addExchangeFnWeight = InputFieldState()


    val catSubCatDto: SnapshotStateList<CatSubCatDto> = SnapshotStateList()
    val purchaseItemList = mutableStateListOf<PurchaseItemInputDto>()


    val exchangeMetalRateDto = mutableStateListOf<PurchaseMetalRateDto>()



    fun getCategoryAndSubCategoryDetails() {
        ioLaunch {
            try {
                val userId = _dataStoreManager.userId.first()
                val storeId = _dataStoreManager.storeId.first()
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
                this@PurchaseViewModel.log("unable to fetch categorical data")
            }
        }
    }

    fun addPurchaseItem() {

        val category = catSubCatDto.find { it.catName == addItemCat.text }
        val catId = category?.catId?:""
        val subCatId = category?.subCategoryList?.find { it.subCatName == addItemSubCat.text }?.subCatId?:""

        val newItem = PurchaseItemInputDto(

            billNo = addBillNo.text,
            catName = addItemCat.text,
            catId = catId,
            subCatId = subCatId,
            subCatName = addItemSubCat.text,
            name = "${addItemCat.text} - ${addItemSubCat.text}",
            gsWt = addItemGsWt.text.toDoubleOrNull() ?: 0.0,
            ntWt = addItemNtWt.text.toDoubleOrNull() ?: 0.0,
            purity = addItemPurity.text,
            fnWt = addItemFnWt.text.toDoubleOrNull() ?: 0.0,
            fnRatePerGm = addItemFineRatePerGm.text.toDoubleOrNull() ?: 0.0,
            wastage = addItemWastage.text.toDoubleOrNull() ?: 0.0,
            extraChargeDes = addItemExtraChargeDes.text,
            extraCharge = addItemExtraCharge.text.toDoubleOrNull() ?: 0.0,
            toAdd = true
        )

        purchaseItemList.add(newItem)

        clearPurchaseInputs()
    }

    fun clearPurchaseInputs() {
        addItemCat.clear()
        addItemSubCat.clear()
        addItemGsWt.clear()
        addItemNtWt.clear()
        addItemPurity.clear()
        addItemFnWt.clear()
        addItemFineRatePerGm.clear()
        addItemWastage.clear()
        addItemExtraChargeDes.clear()
        addItemExtraCharge.clear()
    }


    fun setMetalRate() {

        val catName = addExchangeCategory.text
        val category = catSubCatDto.firstOrNull { it.catName == catName }
        val categoryId = category?.catId
        val subCatId = category?.subCategoryList?.find { it.subCatName == "Fine" }?.subCatId ?: ""
        val rate = addExchangeFnWeight.text.toDoubleOrNull() ?: 0.0

        if (categoryId == null) {
            _snackBarState.value = "Invalid Category Name"
            return
        }

        val index =
            exchangeMetalRateDto.indexOfFirst { it.catId == categoryId && it.catName == catName }
        if (index >= 0) {
            exchangeMetalRateDto[index] =
                PurchaseMetalRateDto(categoryId, catName, subCatId, "Fine", rate)
        } else {
            exchangeMetalRateDto.add(
                PurchaseMetalRateDto(
                    categoryId, catName, subCatId, "Fine", rate
                )
            )
        }
        addExchangeCategory.clear()
        addExchangeFnWeight.clear()
    }

    fun getFirmByFirmMobile() {
        ioLaunch{
                try {
                    if (firmMobile.text.length != 10) {
                        _snackBarState.value = "Invalid Firm Mobile Number"
                        return@ioLaunch
                    }
                    val firmDetails = appDatabase.purchaseDao().getFirmByMobile(firmMobile.text)
                    firmDetails?.let {
                        firmName.text = it.firmName
                        firmAddress.text = it.address
                        firmGstin.text = it.gstNumber
                    } ?: run {
                        _snackBarState.value = "No Firm Found"
                    }

                } catch (e: Exception) {
                    _snackBarState.value = "Fail to search firm by mobile"
                    this@PurchaseViewModel.log("Fail to search firm by mobile")
                }

        }
    }

    fun getFirmBySellerMobile() {
        ioLaunch {
                try {

                    if (sellerMobile.text.length != 10) {
                        _snackBarState.value = "Invalid Seller Mobile Number"
                        return@ioLaunch
                    }
                    val sellerInfo = appDatabase.purchaseDao().getSellerByMobile(sellerMobile.text)

                    sellerInfo?.let {
                        sellerMobile.text = it.mobileNumber
                        sellerName.text = it.name

                        val firmDetails = appDatabase.purchaseDao().getFirmById(sellerInfo.firmId)
                        firmDetails?.let { it2 ->
                            if (firmMobile.text.isEmpty()) {
                                firmName.text = it2.firmName
                                firmAddress.text = it2.address
                                firmGstin.text = it2.gstNumber
                                firmMobile.text = it2.firmMobileNumber
                            }
                        } ?: run {
                            _snackBarState.value = "No Firm Found"
                            return@ioLaunch
                        }
                    } ?: run {
                        _snackBarState.value = "No Seller Found"
                        return@ioLaunch
                    }
                } catch (e: Exception) {
                    _snackBarState.value = "Fail to search firm by seller mobile"
                    this@PurchaseViewModel.log("Fail to search firm by seller mobile")
                }
        }
    }


    @OptIn(ExperimentalCoroutinesApi::class)
    fun addToReg(onComplete: () -> Unit) {
        _loadingState.value = true
        verifyAddFirmAndSeller(onSuccess =   { sellerId ->
            if (purchaseItemList.isEmpty()) {
                _snackBarState.value = "Please add items"
                _loadingState.value = false
                return@verifyAddFirmAndSeller
            }

            savePurchaseOrderAndDetails(sellerId) {

                val mergeList = purchaseItemList
                    .filter { it.subCatName.equals("Fine", ignoreCase = true) }
                    .groupBy { it.catId to it.catName }
                    .map { (catPair, items) ->
                        val totalFnWt = items.sumOf { it.fnWt }
                        val firstItem = items.firstOrNull()
                        PurchaseItemInputDto(
                            catId = catPair.first,
                            catName = catPair.second,
                            subCatId = firstItem?.subCatId ?: "",
                            subCatName = firstItem?.subCatName ?: "Fine",
                            fnWt = totalFnWt,
                            toAdd = true
                        )
                    }

                val exchangeList = exchangeMetalRateDto.map {
                    PurchaseItemInputDto(
                        catId = it.catId,
                        catName = it.catName,
                        subCatId = it.subCatId,
                        subCatName = it.subCatName,
                        fnWt = it.fnWt,
                        toAdd = false
                    )
                }

                val allUpdates = mergeList + exchangeList

                if (allUpdates.isEmpty()) {
                    _loadingState.value = false
                    onComplete()
                    return@savePurchaseOrderAndDetails
                }

                ioLaunch {
                        try {
                            allUpdates.forEach { item ->
                                suspendCancellableCoroutine { continuation ->
                                    safeUpdateFineItem(
                                        catId = item.catId,
                                        catName = item.catName,
                                        subCatId = item.subCatId,
                                        subCatName = item.subCatName,
                                        fnWt = item.fnWt,
                                        add = item.toAdd,
                                        onSuccess = { updatedItem ->
                                            updateCatAndSubCat(item.subCatId,
                                                updatedItem,
                                                item.toAdd
                                            ) {
                                                continuation.resume(Unit) {}
                                            }
                                        },
                                        onFailure = {
                                            continuation.resume(Unit) {}
                                        }
                                    )
                                }
                            }

                            _loadingState.value = false
                            _snackBarState.value = "Completed"
                            onComplete()
                        } catch (e: Exception) {
                            _loadingState.value = false
                            _snackBarState.value = "Error finalizing registration: ${e.message}"
                        }

                }
            }
        },onFailure = {
            _loadingState.value = false
        })

    }


    private fun updateCatAndSubCat(
        subCatId: String, insertedItem: ItemEntity, toAdd:Boolean,onSuccess: suspend (ItemEntity) -> Unit
    ) {
        try {

           ioLaunch {
                    val subCategory =
                        appDatabase.subCategoryDao().getSubCategoryById(subCatId = subCatId)
                    subCategory?.let { subCat ->
//                        val subCatGsWt =if(toAdd){(subCat.gsWt + insertedItem.gsWt)}else{(subCat.gsWt - insertedItem.gsWt)}.roundTo3Decimal()
//                        val subCatFnWt = if(toAdd){(subCat.fnWt + insertedItem.fnWt)}else{(subCat.fnWt - insertedItem.fnWt)}.roundTo3Decimal()

                        val subCatGsWt = insertedItem.gsWt
                        val subCatFnWt = insertedItem.fnWt

                        val upSub = appDatabase.subCategoryDao().updateWeightsAndQuantity(
                            subCatId = insertedItem.subCatId,
                            gsWt = subCatGsWt.roundTo3Decimal(),
                            fnWt = subCatFnWt.roundTo3Decimal(),
                            quantity = 1
                        )

                        if (upSub != -1) {
                            this@PurchaseViewModel.log("Sub Cat id: ${insertedItem.subCatId} update with weight")
                            val cat = appDatabase.categoryDao()
                                .getCategoryById(catId = insertedItem.catId)
                            cat?.let { catEntity ->

                                val subCatItem = appDatabase.subCategoryDao().getSubCategoriesByCatId(catId = insertedItem.catId)

                                val catGsWt = subCatItem.sumOf { it.fnWt }.roundTo3Decimal()
                                val catFnWt = subCatItem.sumOf { it.gsWt }.roundTo3Decimal()


                                val upCat = appDatabase.categoryDao().updateWeights(
                                    catId = insertedItem.catId, gsWt = catGsWt, fnWt = catFnWt
                                )

                                if (upCat != -1) {
                                    this@PurchaseViewModel.log("Cat id: ${insertedItem.catId} update with weight")
                                    _snackBarState.value = "Item Added and categories updated."
                                            _loadingState.value = false
                                    onSuccess(insertedItem)
                                }
                            }
                        } else {

                            _snackBarState.value = "Failed to update Sub Category Weight"
                                    _loadingState.value = false
                        }
                    }
            }


        } catch (e: Exception) {
                            _loadingState.value = false
            _snackBarState.value = ("Error updating cat and sub cat weight: ${e.message}")
            this@PurchaseViewModel.log("Error updating cat and sub cat weight: ${e.message}")
        }
    }

    private fun safeUpdateFineItem(
        catId: String,
        catName: String,
        subCatId: String,
        subCatName: String,
        fnWt: Double,
        add: Boolean,
        onSuccess: suspend (ItemEntity) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        viewModelScope.launch {
            try {
                withIo {
                    val userId = _dataStoreManager.userId.first()
                    val storeId = _dataStoreManager.storeId.first()

                    val existingItem = appDatabase.itemDao().getFineItemByCat(catId)

                    val modifiedFnWt = if (add) {
                        (existingItem?.fnWt ?: 0.0) + fnWt
                    } else {
                        (existingItem?.fnWt ?: 0.0) - fnWt
                    }

                    val finalItem = if (existingItem != null) {
                        val updated = existingItem.copy(
                            quantity = 1,
                            gsWt = modifiedFnWt,
                            fnWt = modifiedFnWt,
                            ntWt = modifiedFnWt
                        )
                        val rowsUpdated = appDatabase.itemDao().updateItem(updated)
                        if (rowsUpdated > 0) {
                            _snackBarState.value = "Fine item updated successfully"
                            updated
                        } else {
                            _loadingState.value = false
                            throw Exception("Failed to update Fine item")
                        }
                    } else {
                        val newItem = ItemEntity(
                            itemId = "0",
                            itemAddName = "Fine",
                            userId = userId,
                            storeId = storeId,
                            catId = catId,
                            catName = catName,
                            subCatId = subCatId,
                            subCatName = subCatName,
                            quantity = 0,
                            gsWt = fnWt,
                            fnWt = fnWt,
                            ntWt = fnWt,
                            entryType = "",
                            purity = "1000",
                            crgType = "",
                            crg = 0.0,
                            othCrgDes = "",
                            othCrg = 0.0,
                            cgst = 1.5,
                            sgst = 0.0,
                            igst = 1.5,
                            huid = "",
                            unit = "gm",
                            addDesKey = "",
                            addDesValue = "",
                            addDate = getCurrentTimestamp(),
                            modifiedDate = getCurrentTimestamp(),
                            sellerFirmId = "0",
                            purchaseOrderId = "0",
                            purchaseItemId = "0",
                        )
                        val insertId = appDatabase.itemDao().insert(newItem)
                        if (insertId != -1L) {
                            _snackBarState.value = "Fine item added successfully"
                            newItem
                        } else {
                            throw Exception("Failed to insert Fine item")
                        }
                    }

                    onSuccess(finalItem)
                }
            } catch (e: Exception) {
                _snackBarState.value = "Error: ${e.message}"
                onFailure(e)
            }
        }
    }


    private fun verifyAddFirmAndSeller(
        onSuccess: (sellerId: String) -> Unit,
        onFailure: () -> Unit
    ) {
        if (firmName.text.isNotBlank() && firmMobile.text.isNotBlank() && firmAddress.text.isNotBlank() && firmGstin.text.isNotBlank() && sellerName.text.isNotBlank() && sellerMobile.text.isNotBlank() && addBillNo.text.isNotBlank() && addBillDate.text.isNotBlank()) {
            ioLaunch {
                val dao = appDatabase.purchaseDao()
                val tempFirmId = generateId()
                // Check or insert firm
                val firm = dao.getFirmByMobile(firmMobile.text)
                val firmId = firm?.firmId ?: run {
                    dao.insertFirm(
                        FirmEntity(
                            firmId = tempFirmId,
                            firmName = firmName.text,
                            firmMobileNumber = firmMobile.text,
                            gstNumber = firmGstin.text,
                            address = firmAddress.text
                        )
                    )
                    tempFirmId
                }

                // Check or insert seller
                val existingSeller = dao.getSellerByMobile(sellerMobile.text)
                val sellerId = if (existingSeller == null || existingSeller.firmId != firmId) {
                    val newSellerId = generateId()
                    dao.insertSeller(
                        SellerEntity(
                            sellerId = newSellerId,
                            firmId = firmId,
                            name = sellerName.text,
                            mobileNumber = sellerMobile.text
                        )
                    )
                    newSellerId
                } else {
                    existingSeller.sellerId
                }

                withMain {
                    onSuccess(sellerId)
                }
            }
        } else {
            _snackBarState.value = "Please fill all firm and seller fields"
            onFailure()
        }
    }


    private fun savePurchaseOrderAndDetails(sellerId: String, onComplete: () -> Unit) {
        ioLaunch {
            val dao = appDatabase.purchaseDao()

            // Insert PurchaseOrder
            val orderId = generateId()
            dao.insertPurchaseOrder(
                PurchaseOrderEntity(
                    purchaseOrderId = orderId,
                    sellerId = sellerId,
                    billNo = addBillNo.text,
                    billDate = addBillDate.text,
                    entryDate = getCurrentDate(),
                    extraChargeDescription = "",
                    extraCharge = purchaseItemList.sumOf { it.extraCharge },
                    totalFinalWeight = purchaseItemList.sumOf { it.ntWt },
                    totalFinalAmount = purchaseItemList.sumOf { it.fnWt * 0.0 + it.extraCharge },
                    notes = "purchaseNote.text",
                    cgstPercent = addCGst.text.toDoubleOrNull() ?: 1.5,
                    sgstPercent = addSGst.text.toDoubleOrNull() ?: 0.0,
                    igstPercent = addIGst.text.toDoubleOrNull() ?: 1.5,
                )
            )

            // Insert Purchase Items
            purchaseItemList.forEach { dto ->
                dao.insertOrderItem(
                    PurchaseOrderItemEntity(
                        purchaseItemId = generateId(),
                        purchaseOrderId = orderId,
                        catId = dto.catId,
                        catName = dto.catName,
                        subCatId = dto.subCatId,
                        subCatName = dto.subCatName,
                        gsWt = dto.gsWt,
                        purity = dto.purity,
                        ntWt = dto.ntWt,
                        fnWt = dto.fnWt,
                        fnRate = dto.fnRatePerGm,
                        wastagePercent = dto.wastage,
                    )
                )
            }

            // Insert Metal Exchanges
            exchangeMetalRateDto.forEach { dto ->
                dao.insertExchange(
                    MetalExchangeEntity(
                        generateId(),
                        purchaseOrderId = orderId,
                        catId = dto.catId,
                        catName = dto.catName,
                        subCatId = dto.subCatId,
                        subCatName = dto.subCatName,
                        fnWeight = dto.fnWt
                    )
                )
            }

            withMain {
                onComplete()
            }
        }
    }


}