package com.velox.jewelvault.ui.screen.sell_invoice

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velox.jewelvault.data.roomdb.AppDatabase
import com.velox.jewelvault.data.roomdb.dto.ItemSelectedModel
import com.velox.jewelvault.data.roomdb.entity.CustomerEntity
import com.velox.jewelvault.data.roomdb.entity.order.OrderEntity
import com.velox.jewelvault.data.roomdb.entity.order.OrderItemEntity
import com.velox.jewelvault.ui.components.InputFieldState
import com.velox.jewelvault.utils.DataStoreManager
import com.velox.jewelvault.utils.EntryType
import com.velox.jewelvault.utils.generateInvoicePdf
import com.velox.jewelvault.utils.ioScope
import com.velox.jewelvault.utils.log
import com.velox.jewelvault.utils.roundTo3Decimal
import com.velox.jewelvault.utils.withIo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.sql.Timestamp
import javax.inject.Inject

@HiltViewModel
class SellInvoiceViewModel @Inject constructor(
    private val context: Context,
    private val appDatabase: AppDatabase,
    private val _dataStoreManager: DataStoreManager,
    private val _loadingState: MutableState<Boolean>,
    private val _snackBarState: MutableState<String>
) : ViewModel() {

    val snackBarState = _snackBarState
    val showAddItemDialog = mutableStateOf(false)
    val showSeparateCharges = mutableStateOf(true)
    val selectedItem = mutableStateOf<ItemSelectedModel?>(null)
    val selectedItemList = SnapshotStateList<ItemSelectedModel>()

    val customerName = InputFieldState()
    val customerMobile = InputFieldState()
    val customerAddress = InputFieldState()
    val customerGstin = InputFieldState()

    private val customerExists = mutableStateOf(false)
    val ownerSign = mutableStateOf<ImageBitmap?>(null)
    val customerSign = mutableStateOf<ImageBitmap?>(null)

    var generatedPdfFile by mutableStateOf<Uri?>(null)
        private set

    init {
        viewModelScope.launch {
            withIo {
                showSeparateCharges.value =
                    _dataStoreManager.getValue(DataStoreManager.SHOW_SEPARATE_CHARGE).first()
                        ?: false
            }
        }
    }

    fun updateChargeView(state: Boolean) {
        viewModelScope.launch {
            withIo {
                _dataStoreManager.setValue(DataStoreManager.SHOW_SEPARATE_CHARGE, state)
                delay(50)
                showSeparateCharges.value =
                    _dataStoreManager.getValue(DataStoreManager.SHOW_SEPARATE_CHARGE).first()
                        ?: false
            }
        }
    }

    fun getItemById(itemId: Int, onSuccess: () -> Unit, onFailure: (String) -> Unit = {}) {
        viewModelScope.launch {
            return@launch withIo {
                val item = appDatabase.itemDao().getItemById(itemId)
                if (item != null) {
                    val addItem = ItemSelectedModel(
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
                    )

                    selectedItem.value = addItem
                    onSuccess()
                } else {
                    _snackBarState.value = "No Item Found"
                    onFailure("No Item Found")
                }
            }
        }
    }


    //region customer
    fun getCustomerByMobile(onFound: (CustomerEntity?) -> Unit = {}) {
        viewModelScope.launch {
            ioScope {
                try {
                    _loadingState.value = true
                    val mobile = customerMobile.text.trim()
                    if (mobile.isNotEmpty()) {
                        val customer = appDatabase.customerDao().getCustomerByMobile(mobile)
                        customer?.let {
                            customerExists.value = true
                            customerName.text = it.name
                            customerAddress.text = it.address ?: ""
                            customerGstin.text = it.gstin_pan ?: ""
                            _loadingState.value = false
                            _snackBarState.value = "Customer Found"
                            onFound(customer)
                        } ?: run {
                            customerExists.value = false
                            _loadingState.value = false
                            _snackBarState.value = "No Customer Found"
                            onFound(null)
                        }

                    } else {
                        customerExists.value = false
                        _loadingState.value = false
                        _snackBarState.value = "No Customer Found"
                        onFound(null)
                    }

                } catch (e: Exception) {
                    _loadingState.value = false
                    _snackBarState.value = "Error fetching customer details"
                }
            }
        }
    }

    //endregion


    fun completeOrder(
        onSuccess: () -> Unit, onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            withIo {
                _loadingState.value = true
                try {
                    //1. check if customer exist
                    val mobile = customerMobile.text.trim()

                    val existingCustomer = appDatabase.customerDao().getCustomerByMobile(mobile)

                    //2. add customer if not exist
                    if (existingCustomer == null) {
                        val newCustomer = CustomerEntity(
                            mobileNo = mobile,
                            name = customerName.text.trim(),
                            address = customerAddress.text.trim(),
                            gstin_pan = customerGstin.text.trim(),
                            addDate = Timestamp(System.currentTimeMillis()),
                            lastModifiedDate = Timestamp(System.currentTimeMillis()),
                        )
                        appDatabase.customerDao().insertCustomer(newCustomer)
                    }

                    // Calculate totals
                    val totalAmount = selectedItemList.sumOf { it.price }
                    val totalTax = selectedItemList.sumOf { it.tax }
                    val totalCharge = selectedItemList.sumOf { it.chargeAmount }
                    val userId = _dataStoreManager.userId.first()
                    val storeId = _dataStoreManager.storeId.first()
                    //3. add order and its item details
                    addOrderWithItems(
                        userId = userId,
                        storeId = storeId,
                        mobile = mobile,
                        totalAmount = totalAmount,
                        totalTax = totalTax,
                        totalCharge = totalCharge,
                        onSuccess = {
                            ioScope {
                                val cus = appDatabase.customerDao().getCustomerByMobile(mobile)

                                if (cus != null) {
                                    val ta =
                                        cus.totalAmount + (totalAmount + totalTax + totalCharge)
                                    val qty =
                                        cus.totalItemBought + selectedItemList.sumOf { it.quantity }

                                    //4. update the customer details
                                    val a = appDatabase.customerDao().updateCustomer(
                                        cus.copy(
                                            lastModifiedDate = Timestamp(System.currentTimeMillis()),
                                            totalAmount = ta,
                                            totalItemBought = qty,
                                        )
                                    )

                                    if (a != -1) {
                                        //successfully update the customer details
                                        //5. remove item from items, subcategory, category
                                        removeItemSafely(onSuccess = {
                                            //6. generate the pdf
                                            generateInvoice(
                                                storeId,
                                                cus,
                                                onSuccess = onSuccess,
                                                onFailure = onFailure
                                            )
                                        }, onFailure)
                                    } else {
                                        //failed to update the customer details
                                        _loadingState.value = false
                                        onFailure("Failed to update customer details")
                                    }
                                }
                            }
                        },
                        onFailure = onFailure
                    )
                } catch (e: Exception) {
                    _loadingState.value = false
                    onFailure("")
                }
            }
        }
    }

    private fun addOrderWithItems(
        userId: Int,
        storeId: Int,
        mobile: String,
        totalAmount: Double,
        totalTax: Double,
        totalCharge: Double,
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _loadingState.value = true
            try {
                withIo {
                    val order = OrderEntity(
                        customerMobile = mobile,
                        storeId = storeId,
                        userId = userId,
                        orderDate = Timestamp(System.currentTimeMillis()),
                        totalAmount = totalAmount,
                        totalTax = totalTax,
                        totalCharge = totalCharge,
                        note = "note"
                    )

                    val orderId = appDatabase.orderDao().insertOrder(order)

                    if (orderId != -1L) {
                        val orderItems = selectedItemList.map {
                            OrderItemEntity(
                                orderId = orderId.toInt(),
                                itemId = it.itemId,
                                itemAddName = it.itemAddName,
                                catId = it.catId,
                                catName = it.catName,
                                subCatId = it.subCatId,
                                subCatName = it.subCatName,
                                entryType = it.entryType,
                                quantity = it.quantity,
                                gsWt = it.gsWt,
                                ntWt = it.ntWt,
                                fnWt = it.fnWt,
                                fnMetalPrice = it.fnMetalPrice,
                                purity = it.purity,
                                crgType = it.crgType,
                                crg = it.crg,
                                othCrgDes = it.othCrgDes,
                                othCrg = it.othCrg,
                                cgst = it.cgst,
                                sgst = it.sgst,
                                igst = it.igst,
                                huid = it.huid,
                                price = it.price,
                                charge = it.chargeAmount,
                                tax = it.tax
                            )
                        }
                        val res = appDatabase.orderDao().insertItems(orderItems)

                        if (res.isNotEmpty()) {
                            onSuccess()
                        } else {
                            _loadingState.value = false
                            onFailure("Failed to insert all the items to DB")
                        }
                    } else {
                        _loadingState.value = false
                        onFailure("Failed to insert order to DB")
                    }
                }
            } catch (e: Exception) {
                _loadingState.value = false
                onFailure("Error inserting order and items error: ${e.message}")
            }
        }
    }


    private fun removeItemSafely(
        onSuccess: () -> Unit = {}, onFailure: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            withIo {
                _loadingState.value = true
                try {
                    selectedItemList.forEach { item ->
                        try {
                            val itemId = item.itemId
                            val catId = item.catId
                            val subCatId = item.subCatId
                            val itemGsWt = item.gsWt
                            val itemNtWt = item.ntWt
                            val itemFnWt = item.fnWt
                            val itemQty = item.quantity
                            val result = if (item.entryType == EntryType.Lot.type) {
                                val itemEntity = appDatabase.itemDao().getItemById(itemId)
                                if (itemEntity != null) {
                                    if ( (itemEntity.quantity - itemQty) <= 0){
                                        appDatabase.itemDao().deleteById(itemId, catId, subCatId)
                                    }else{
                                        appDatabase.itemDao().updateItem(
                                            itemEntity.copy(
                                                gsWt = (itemEntity.gsWt - itemGsWt),
                                                ntWt = (itemEntity.ntWt - itemNtWt),
                                                fnWt = (itemEntity.fnWt - itemFnWt),
                                                quantity = (itemEntity.quantity - itemQty)
                                            )
                                        )
                                    }
                                } else {
                                    onFailure("Item with lot failed to delete from DB")
                                    -1
                                }
                            } else {
                                appDatabase.itemDao().deleteById(itemId, catId, subCatId)
                            }
                            if (result > 0) {
                                try {
                                    val subCategory =
                                        appDatabase.subCategoryDao().getSubCategoryById(subCatId)
                                    subCategory?.let {
                                        val updatedSubCatGsWt =
                                            (it.gsWt - itemGsWt).coerceAtLeast(0.0)
                                                .roundTo3Decimal()
                                        val updatedSubCatFnWt =
                                            (it.fnWt - itemFnWt).coerceAtLeast(0.0)
                                                .roundTo3Decimal()
                                        val updatedSubCatQty =
                                            (it.quantity - itemQty).coerceAtLeast(0)

                                        appDatabase.subCategoryDao().updateWeightsAndQuantity(
                                            subCatId = subCatId,
                                            gsWt = updatedSubCatGsWt,
                                            fnWt = updatedSubCatFnWt,
                                            quantity = updatedSubCatQty
                                        )
                                    }

                                    val category = appDatabase.categoryDao().getCategoryById(catId)
                                    category?.let {
                                        val updatedCatGsWt = (it.gsWt - itemGsWt).coerceAtLeast(0.0)
                                            .roundTo3Decimal()
                                        val updatedCatFnWt = (it.fnWt - itemFnWt).coerceAtLeast(0.0)
                                            .roundTo3Decimal()

                                        appDatabase.categoryDao().updateWeights(
                                            catId = catId,
                                            gsWt = updatedCatGsWt,
                                            fnWt = updatedCatFnWt
                                        )
                                    }

                                    onSuccess()
                                } catch (e: Exception) {
                                    _loadingState.value = false
                                    onFailure("Error updating Cat and SubCat Weight error: ${e.message}")
                                    this@SellInvoiceViewModel.log("Error updating Cat and SubCat Weight error: ${e.message}")

                                }
                            } else {
                                _loadingState.value = false
                                onFailure("No rows deleted. Check itemId, catId, and subCatId.")
                                this@SellInvoiceViewModel.log("No rows deleted. Check itemId, catId, and subCatId.")
                            }

                        } catch (e: Exception) {
                            _loadingState.value = false
                            onFailure("Error removing the item from DB, error: ${e.message}")
                            this@SellInvoiceViewModel.log("Error removing the item from DB, error: ${e.message}")

                        }
                    }
                } catch (e: Exception) {
                    _loadingState.value = false
                    onFailure("Error removing the item safely DB, error: ${e.message}")
                    this@SellInvoiceViewModel.log("Error removing the item safely DB, error: ${e.message}")
                }
            }
        }
    }


    private fun generateInvoice(
        storeId: Int, cus: CustomerEntity, onSuccess: () -> Unit, onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            withIo {
                _loadingState.value = true
                try {
                    val store = appDatabase.storeDao().getStoreById(storeId)
                    if (store != null && customerSign.value != null && ownerSign.value != null) {
                        generateInvoicePdf(
                            context = context,
                            store = store,
                            customer = cus,
                            items = selectedItemList,
                            customerSign = customerSign.value!!,
                            ownerSign = ownerSign.value!!
                        ) { file ->
                            generatedPdfFile = file
                            _loadingState.value = false
                            onSuccess()
                        }
                    } else {
                        _loadingState.value = false
                        onFailure("Store Details Not Found")
                    }
                } catch (e: Exception) {
                    _loadingState.value = false
                    onFailure("Unable to Generate Invoice PDF")
                }
            }
        }
    }

    fun clearData() {
        selectedItem.value = null
        selectedItemList.clear()
        customerName.text = ""
        customerMobile.text = ""
        customerAddress.text = ""
        customerGstin.text = ""
        ownerSign.value =null
        customerSign.value = null
        generatedPdfFile =null
    }

}