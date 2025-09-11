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
import com.velox.jewelvault.data.roomdb.entity.customer.CustomerEntity
import com.velox.jewelvault.data.roomdb.entity.order.OrderEntity
import com.velox.jewelvault.data.roomdb.entity.order.OrderItemEntity
import com.velox.jewelvault.ui.components.InputFieldState
import com.velox.jewelvault.ui.components.PaymentInfo
import com.velox.jewelvault.data.DataStoreManager
import com.velox.jewelvault.data.roomdb.entity.customer.CustomerTransactionEntity
import com.velox.jewelvault.data.MetalRate
import com.velox.jewelvault.data.roomdb.entity.StoreEntity
import com.velox.jewelvault.utils.EntryType
import com.velox.jewelvault.utils.InputValidator
import com.velox.jewelvault.utils.generateInvoicePdf
import com.velox.jewelvault.utils.generateId
import com.velox.jewelvault.utils.ioLaunch
import com.velox.jewelvault.utils.ioScope
import com.velox.jewelvault.utils.log
import com.velox.jewelvault.utils.roundTo3Decimal
import com.velox.jewelvault.utils.withIo
import com.velox.jewelvault.utils.CalculationUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.sql.Timestamp
import javax.inject.Inject
import javax.inject.Named
import com.velox.jewelvault.utils.createDraftInvoiceData
import com.velox.jewelvault.data.roomdb.dto.ExchangeItemDto
import com.velox.jewelvault.utils.to3FString

@HiltViewModel
class InvoiceViewModel @Inject constructor(
    private val context: Context,
    private val appDatabase: AppDatabase,
    private val _dataStoreManager: DataStoreManager,
    private val _loadingState: MutableState<Boolean>,
    @Named("snackMessage") private val _snackBarState: MutableState<String>,
    private val _metalRates: SnapshotStateList<MetalRate>
) : ViewModel() {

    /**
     * return Triple of Flow<String> for userId, userName, mobileNo
     * */
    val admin: Triple<Flow<String>, Flow<String>, Flow<String>> = _dataStoreManager.getAdminInfo()

    /**
     * return Triple of Flow<String> for storeId, upiId, storeName
     * */
    val store: Triple<Flow<String>, Flow<String>, Flow<String>> =
        _dataStoreManager.getSelectedStoreInfo()

    val snackBarState = _snackBarState
    val metalRates = _metalRates
    val showAddItemDialog = mutableStateOf(false)
    val showSeparateCharges = mutableStateOf(true)
    val selectedItem = mutableStateOf<ItemSelectedModel?>(null)
    val selectedItemList = SnapshotStateList<ItemSelectedModel>()

    // Exchange Items related states
    val showExchangeItemDialog = mutableStateOf(false)
    val exchangeItemList = SnapshotStateList<ExchangeItemDto>()

    val customerName = InputFieldState()
    val customerMobile = InputFieldState()
    val customerAddress = InputFieldState()
    val customerGstin = InputFieldState()

    private val customerExists = mutableStateOf(false)
    val ownerSign = mutableStateOf<ImageBitmap?>(null)
    val customerSign = mutableStateOf<ImageBitmap?>(null)

    // Payment related states
    val showPaymentDialog = mutableStateOf(false)
    val paymentInfo = mutableStateOf<PaymentInfo?>(null)
    val discount = InputFieldState()
    val upiId = mutableStateOf("")
    val storeName = mutableStateOf("Merchant")

    var generatedPdfFile by mutableStateOf<Uri?>(null)
        private set

    init {
        ioLaunch {
            showSeparateCharges.value =
                _dataStoreManager.getValue(DataStoreManager.SHOW_SEPARATE_CHARGE).first() ?: false
        }
        loadUpiSettings()
    }

    private fun loadUpiSettings() {
        viewModelScope.launch {
            upiId.value = store.second.first()
        }
        viewModelScope.launch {
            storeName.value = store.third.first()
        }
    }

    fun updateChargeView(state: Boolean) {
        ioLaunch {
            _dataStoreManager.setValue(DataStoreManager.SHOW_SEPARATE_CHARGE, state)
            delay(50)
            showSeparateCharges.value =
                _dataStoreManager.getValue(DataStoreManager.SHOW_SEPARATE_CHARGE).first() ?: false
        }
    }

    fun getItemById(itemId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit = {}) {
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
                        addDesKey = item.addDesKey,
                        addDesValue = item.addDesValue,
                        sellerFirmId = item.sellerFirmId,
                        purchaseOrderId = item.purchaseOrderId,
                        purchaseItemId = item.purchaseItemId,
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
        ioLaunch {
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

    //endregion

    fun getTotalOrderAmount(): Double {
        val summary = CalculationUtils.summaryTotals(selectedItemList.toList())
        return summary.grandTotal
    }

    fun completeOrder(
        onSuccess: () -> Unit, onFailure: (String) -> Unit
    ) {
        ioLaunch {
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

                // Calculate totals using CalculationUtils
                val summary = CalculationUtils.summaryTotals(selectedItemList.toList())
                val totalAmount = summary.totalBasePrice
                val totalTax = summary.totalTax
                val totalCharge = summary.totalMakingCharges
                val userId = admin.first.first()
                val storeId = store.first.first()

                // Get payment info or use default (full cash payment)
                val grandTotal = totalAmount + totalTax + totalCharge
                val payment = paymentInfo.value ?: PaymentInfo(
                    paymentMethod = "Cash",
                    totalAmount = grandTotal,
                    paidAmount = grandTotal,
                    outstandingAmount = 0.0,
                    isPaidInFull = true
                )

                //3. add order and its item details
                addOrderWithItems(
                    userId = userId,
                    storeId = storeId,
                    mobile = mobile,
                    discount = discount.text.toDoubleOrNull() ?: 0.0,
                    totalAmount = totalAmount,
                    totalTax = totalTax,
                    totalCharge = totalCharge,
                    paymentInfo = payment,
                    onSuccess = {
                        ioScope {
                            val cus = appDatabase.customerDao().getCustomerByMobile(mobile)

                            if (cus != null) {
                                val ta = cus.totalAmount + grandTotal
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
                                    //5. handle outstanding balance if partial payment
                                    if ((payment.outstandingAmount ?: 0.0) > 0) {
                                        val outstandingTransaction = CustomerTransactionEntity(
                                            transactionId = generateId(),
                                            customerMobile = mobile,
                                            transactionDate = Timestamp(System.currentTimeMillis()),
                                            amount = payment.outstandingAmount ?: 0.0,
                                            transactionType = "debit",
                                            category = "outstanding",
                                            description = "Outstanding balance from order",
                                            paymentMethod = payment.paymentMethod,
                                            notes = "Order total: ₹${payment.totalAmount}, Paid: ₹${payment.paidAmount}",
                                            userId = userId,
                                            storeId = storeId
                                        )
                                        appDatabase.customerTransactionDao()
                                            .insertTransaction(outstandingTransaction)
                                    }

                                    //6. remove item from items, subcategory, category
                                    removeItemSafely(onSuccess = {
                                        //7. generate the pdf
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

    private fun addOrderWithItems(
        userId: String,
        storeId: String,
        mobile: String,
        discount: Double,
        totalAmount: Double,
        totalTax: Double,
        totalCharge: Double,
        paymentInfo: PaymentInfo,
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _loadingState.value = true
            try {
                withIo {

                    val id = generateId()
                    val justNowTime = Timestamp(System.currentTimeMillis())
                    val order = OrderEntity(
                        orderId = id,
                        customerMobile = mobile,
                        storeId = storeId,
                        userId = userId,
                        orderDate = justNowTime,
                        discount = discount,
                        totalAmount = totalAmount,
                        totalTax = totalTax,
                        totalCharge = totalCharge,
                        note = paymentInfo.toNoteString()
                    )

                    val orderId = appDatabase.orderDao().insertOrder(order)

                    if (orderId != -1L) {
                        val orderItems = selectedItemList.map {
                            OrderItemEntity(
                                orderItemId = generateId(),
                                orderId = id,
                                orderDate = justNowTime,
                                itemId = it.itemId,
                                customerMobile = mobile,
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
                                tax = it.tax,
                                addDesKey = it.addDesKey,
                                addDesValue = it.addDesValue,
                                sellerFirmId = it.sellerFirmId,
                                purchaseOrderId = it.purchaseOrderId,
                                purchaseItemId = it.purchaseItemId

                            )
                        }
                        val res = appDatabase.orderDao().insertItems(orderItems)

                        if (res.isNotEmpty()) {
                            // Also save exchange items if any
                            if (exchangeItemList.isNotEmpty()) {
                                val exchangeEntities = exchangeItemList.map { exchangeItem ->
                                    exchangeItem.toEntity().copy(
                                        orderId = id,
                                        orderDate = justNowTime,
                                        customerMobile = mobile
                                    )
                                }
                                appDatabase.orderDao().insertExchangeItems(exchangeEntities)
                            }
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
        ioLaunch {
            _loadingState.value = true
            try {
                var successCount = 0
                var failureCount = 0
                
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
                                if ((itemEntity.quantity - itemQty) <= 0) {
                                    appDatabase.itemDao().deleteById(itemId, catId, subCatId)
                                } else {
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
                                this@InvoiceViewModel.log("Item with lot failed to delete from DB")
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
                                        (it.gsWt - itemGsWt).coerceAtLeast(0.0).roundTo3Decimal()
                                    val updatedSubCatFnWt =
                                        (it.fnWt - itemFnWt).coerceAtLeast(0.0).roundTo3Decimal()
                                    val updatedSubCatQty = (it.quantity - itemQty).coerceAtLeast(0)

                                    appDatabase.subCategoryDao().updateWeightsAndQuantity(
                                        subCatId = subCatId,
                                        gsWt = updatedSubCatGsWt,
                                        fnWt = updatedSubCatFnWt,
                                        quantity = updatedSubCatQty
                                    )
                                } ?: run {
                                    this@InvoiceViewModel.log("Sub Category not found for item removal")
                                    failureCount++
                                    return@forEach
                                }

                                val category = appDatabase.categoryDao().getCategoryById(catId)
                                category?.let {
                                    val updatedCatGsWt =
                                        (it.gsWt - itemGsWt).coerceAtLeast(0.0).roundTo3Decimal()
                                    val updatedCatFnWt =
                                        (it.fnWt - itemFnWt).coerceAtLeast(0.0).roundTo3Decimal()

                                    appDatabase.categoryDao().updateWeights(
                                        catId = catId, gsWt = updatedCatGsWt, fnWt = updatedCatFnWt
                                    )
                                } ?: run {
                                    this@InvoiceViewModel.log("Category not found for item removal")
                                    failureCount++
                                    return@forEach
                                }

                                successCount++
                            } catch (e: Exception) {
                                this@InvoiceViewModel.log("Error updating Cat and SubCat Weight error: ${e.message}")
                                failureCount++
                            }
                        } else {
                            this@InvoiceViewModel.log("No rows deleted. Check itemId, catId, and subCatId.")
                            failureCount++
                        }

                    } catch (e: Exception) {
                        this@InvoiceViewModel.log("Error removing the item from DB, error: ${e.message}")
                        failureCount++
                    }
                }
                
                // Call callbacks based on results
                if (successCount > 0 && failureCount == 0) {
                    _loadingState.value = false
                    onSuccess()
                } else if (successCount > 0 && failureCount > 0) {
                    _loadingState.value = false
                    onFailure("Some items were removed successfully, but ${failureCount} items failed to remove")
                } else {
                    _loadingState.value = false
                    onFailure("Failed to remove all items")
                }
                
            } catch (e: Exception) {
                _loadingState.value = false
                onFailure("Error removing the item safely DB, error: ${e.message}")
                this@InvoiceViewModel.log("Error removing the item safely DB, error: ${e.message}")
            }
        }
    }


    private fun generateInvoice(
        storeId: String, cus: CustomerEntity, onSuccess: () -> Unit, onFailure: (String) -> Unit
    ) {
        ioLaunch {
            _loadingState.value = true
            try {
                val store = appDatabase.storeDao().getStoreById(storeId)
                if (store != null && customerSign.value != null && ownerSign.value != null) {

                   val oldExchange = exchangeItemList.sumOf { it.exchangeValue }

                    // Create DraftInvoiceData object
                    val invoiceData = createDraftInvoiceData(
                        store = store,
                        customer = cus,
                        items =selectedItemList,
                        metalRates = metalRates,
                        paymentInfo = paymentInfo,
                        appDatabase = appDatabase,
                        customerSign = customerSign,
                        ownerSign = ownerSign,
                        gstLabel = "GST @3 ",
                        discount = discount.text,
                        cardCharges= "0.00",
                        oldExchange = oldExchange.to3FString(),
                        roundOff = "0.00",
                        dataStoreManager = _dataStoreManager
                    )


                    generateInvoicePdf(
                        context = context,
                        data = invoiceData,
                        scale = 2f
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


    fun clearData() {
        selectedItem.value = null
        selectedItemList.clear()
        clearExchangeItems()
        customerName.text = ""
        customerMobile.text = ""
        customerAddress.text = ""
        customerGstin.text = ""
        ownerSign.value = null
        customerSign.value = null
        showPaymentDialog.value = false
        paymentInfo.value = null
        generatedPdfFile = null
    }

    //region Draft Invoice - Using same variables as regular invoice
    // Customer details - using existing customer fields
    // Item form fields
    val draftDialogItemName = InputFieldState("")
    val draftDialogCategoryName = InputFieldState("")
    val draftDialogSubCategoryName = InputFieldState("")
    val draftDialogEntryType = InputFieldState("")
    val draftDialogQuantity = InputFieldState("")
    val draftDialogGrossWeight = InputFieldState("")
    val draftDialogNetWeight = InputFieldState("")
    val draftDialogFineWeight = InputFieldState("")
    val draftDialogPurity = InputFieldState("")
    val draftDialogChargeType = InputFieldState("")
    val draftDialogCharge = InputFieldState("")
    val draftDialogOtherChargeDescription = InputFieldState("")
    val draftDialogOtherCharge = InputFieldState("")
    val draftDialogCgst = InputFieldState("")
    val draftDialogSgst = InputFieldState("")
    val draftDialogIgst = InputFieldState("")
    val draftDialogHuid = InputFieldState("")
    val draftDialogDescription = InputFieldState("")
    val draftDialogDescriptionValue = InputFieldState("")

    // UI states
    val draftShowAddItemDialog = mutableStateOf(false)


    fun draftAddSampleItem() {
        // Add multiple sample items for testing
        val sampleItems = listOf(
            ItemSelectedModel(
                itemId = generateId(),
                itemAddName = "Gold Ring",
                catId = "1",
                userId = "1",
                storeId = "1",
                catName = "Ring",
                subCatId = "1",
                subCatName = "Gold Ring",
                entryType = "Piece",
                quantity = 1,
                gsWt = 5.50,
                ntWt = 5.00,
                fnWt = 4.58,
                fnMetalPrice = 6000.0,
                purity = "916",
                crgType = "Percentage",
                crg = 500.0,
                othCrgDes = "Stone",
                othCrg = 100.0,
                cgst = 150.0,
                sgst = 150.0,
                igst = 0.0,
                addDesKey = "Design",
                addDesValue = "Floral Pattern",
                huid = "H123456789",
                price = 30000.0,
                chargeAmount = 600.0,
                tax = 300.0,
                addDate = java.sql.Timestamp(System.currentTimeMillis()),
                sellerFirmId = "0",
                purchaseOrderId = "0",
                purchaseItemId = "0"
            ),
            ItemSelectedModel(
                itemId = generateId(),
                itemAddName = "Silver Necklace",
                catId = "2",
                userId = "1",
                storeId = "1",
                catName = "Necklace",
                subCatId = "2",
                subCatName = "Silver Necklace",
                entryType = "Piece",
                quantity = 1,
                gsWt = 12.75,
                ntWt = 12.00,
                fnWt = 11.04,
                fnMetalPrice = 750.0,
                purity = "925",
                crgType = "Fixed",
                crg = 200.0,
                othCrgDes = "Pendant",
                othCrg = 150.0,
                cgst = 75.0,
                sgst = 75.0,
                igst = 0.0,
                addDesKey = "Style",
                addDesValue = "Modern Chain",
                huid = "H987654321",
                price = 9000.0,
                chargeAmount = 350.0,
                tax = 150.0,
                addDate = java.sql.Timestamp(System.currentTimeMillis()),
                sellerFirmId = "0",
                purchaseOrderId = "0",
                purchaseItemId = "0"
            ),
            ItemSelectedModel(
                itemId = generateId(),
                itemAddName = "Platinum Earrings",
                catId = "3",
                userId = "1",
                storeId = "1",
                catName = "Earrings",
                subCatId = "3",
                subCatName = "Platinum Earrings",
                entryType = "Pair",
                quantity = 1,
                gsWt = 3.25,
                ntWt = 3.00,
                fnWt = 2.85,
                fnMetalPrice = 3500.0,
                purity = "950",
                crgType = "Percentage",
                crg = 300.0,
                othCrgDes = "Diamond",
                othCrg = 500.0,
                cgst = 175.0,
                sgst = 175.0,
                igst = 0.0,
                addDesKey = "Cut",
                addDesValue = "Brilliant",
                huid = "H456789123",
                price = 12000.0,
                chargeAmount = 800.0,
                tax = 350.0,
                addDate = java.sql.Timestamp(System.currentTimeMillis()),
                sellerFirmId = "0",
                purchaseOrderId = "0",
                purchaseItemId = "0"
            ),
            ItemSelectedModel(
                itemId = generateId(),
                itemAddName = "Diamond Bracelet",
                catId = "4",
                userId = "1",
                storeId = "1",
                catName = "Bracelet",
                subCatId = "4",
                subCatName = "Diamond Bracelet",
                entryType = "Piece",
                quantity = 1,
                gsWt = 8.90,
                ntWt = 8.50,
                fnWt = 7.82,
                fnMetalPrice = 4500.0,
                purity = "750",
                crgType = "Fixed",
                crg = 400.0,
                othCrgDes = "Diamond",
                othCrg = 800.0,
                cgst = 225.0,
                sgst = 225.0,
                igst = 0.0,
                addDesKey = "Clarity",
                addDesValue = "VS1",
                huid = "H789123456",
                price = 18000.0,
                chargeAmount = 1200.0,
                tax = 450.0,
                addDate = java.sql.Timestamp(System.currentTimeMillis()),
                sellerFirmId = "0",
                purchaseOrderId = "0",
                purchaseItemId = "0"
            )
        )

        selectedItemList.addAll(sampleItems)
        calculateTotals()
        snackBarState.value = "${sampleItems.size} sample items added for testing"
    }


    fun draftUpdateChargeView(show: Boolean) {
        showSeparateCharges.value = show
    }

    fun draftAddItem() {
        // Validate inputs
        if (draftDialogItemName.text.isBlank()) {
            snackBarState.value = "Item name is required"
            return
        }

        if (draftDialogCategoryName.text.isBlank()) {
            snackBarState.value = "Category name is required"
            return
        }

        if (draftDialogSubCategoryName.text.isBlank()) {
            snackBarState.value = "Sub-category name is required"
            return
        }

        if (!InputValidator.isValidQuantity(draftDialogQuantity.text)) {
            snackBarState.value = "Invalid quantity"
            return
        }

        if (!InputValidator.isValidWeight(draftDialogGrossWeight.text)) {
            snackBarState.value = "Invalid gross weight"
            return
        }

        if (!InputValidator.isValidWeight(draftDialogNetWeight.text)) {
            snackBarState.value = "Invalid net weight"
            return
        }

        if (!InputValidator.isValidWeight(draftDialogFineWeight.text)) {
            snackBarState.value = "Invalid fine weight"
            return
        }

        if (draftDialogPurity.text.isBlank()) {
            snackBarState.value = "Purity is required"
            return
        }

        // Calculate price (basic calculation - can be enhanced)
        val netWt = draftDialogNetWeight.text.toDoubleOrNull() ?: 0.0
        val chargeAmt = draftDialogCharge.text.toDoubleOrNull() ?: 0.0
        val otherChargeAmt = draftDialogOtherCharge.text.toDoubleOrNull() ?: 0.0
        val cgstAmt = draftDialogCgst.text.toDoubleOrNull() ?: 0.0
        val sgstAmt = draftDialogSgst.text.toDoubleOrNull() ?: 0.0
        val igstAmt = draftDialogIgst.text.toDoubleOrNull() ?: 0.0

        // Basic price calculation using CalculationUtils
        val basePrice = netWt * 6000 // Assuming 6000 per gram as base rate
        val totalCharges = chargeAmt + otherChargeAmt
        val totalTaxes = cgstAmt + sgstAmt + igstAmt
        val finalPrice = CalculationUtils.totalPrice(basePrice, totalCharges, totalTaxes)

        val newItem = ItemSelectedModel(
            itemId = "DB_${selectedItemList.size + 1}",
            itemAddName = InputValidator.sanitizeText(draftDialogItemName.text),
            catId = "1", // Default category ID for draft
            userId = "1",
            storeId = "1",
            catName = InputValidator.sanitizeText(draftDialogCategoryName.text),
            subCatId = "1", // Default sub-category ID for draft
            subCatName = InputValidator.sanitizeText(draftDialogSubCategoryName.text),
            entryType = InputValidator.sanitizeText(draftDialogEntryType.text),
            quantity = draftDialogQuantity.text.toIntOrNull() ?: 1,
            gsWt = draftDialogGrossWeight.text.toDoubleOrNull() ?: 0.0,
            ntWt = draftDialogNetWeight.text.toDoubleOrNull() ?: 0.0,
            fnWt = draftDialogFineWeight.text.toDoubleOrNull() ?: 0.0,
            fnMetalPrice = 6000.0, // Default metal price
            purity = InputValidator.sanitizeText(draftDialogPurity.text),
            crgType = InputValidator.sanitizeText(draftDialogChargeType.text),
            crg = draftDialogCharge.text.toDoubleOrNull() ?: 0.0,
            othCrgDes = InputValidator.sanitizeText(draftDialogOtherChargeDescription.text),
            othCrg = draftDialogOtherCharge.text.toDoubleOrNull() ?: 0.0,
            cgst = draftDialogCgst.text.toDoubleOrNull() ?: 0.0,
            sgst = draftDialogSgst.text.toDoubleOrNull() ?: 0.0,
            igst = draftDialogIgst.text.toDoubleOrNull() ?: 0.0,
            addDesKey = InputValidator.sanitizeText(draftDialogDescription.text),
            addDesValue = InputValidator.sanitizeText(draftDialogDescriptionValue.text),
            huid = draftDialogHuid.text.trim().uppercase(),
            price = finalPrice,
            chargeAmount = totalCharges,
            tax = totalTaxes,
            addDate = java.sql.Timestamp(System.currentTimeMillis()),
            sellerFirmId = "0",
            purchaseOrderId = "0",
            purchaseItemId = "0"
        )

        selectedItemList.add(newItem)
        calculateTotals()
        draftClearItemForm()
        draftShowAddItemDialog.value = false
        snackBarState.value = "Item added successfully"
    }


    fun draftRemoveItem(item: ItemSelectedModel) {
        selectedItemList.remove(item)
        calculateTotals()
        snackBarState.value = "Item removed"
    }

    private fun calculateTotals() {
        // This method will be used by both regular and draft invoices
        // The totals are calculated in real-time as items are added/removed
    }


    private fun draftClearItemForm() {
        draftDialogItemName.clear()
        draftDialogCategoryName.clear()
        draftDialogSubCategoryName.clear()
        draftDialogEntryType.clear()
        draftDialogQuantity.clear()
        draftDialogGrossWeight.clear()
        draftDialogNetWeight.clear()
        draftDialogFineWeight.clear()
        draftDialogPurity.clear()
        draftDialogChargeType.clear()
        draftDialogCharge.clear()
        draftDialogOtherChargeDescription.clear()
        draftDialogOtherCharge.clear()
        draftDialogCgst.clear()
        draftDialogSgst.clear()
        draftDialogIgst.clear()
        draftDialogHuid.clear()
        draftDialogDescription.clear()
        draftDialogDescriptionValue.clear()
    }


    fun draftClearData() {
        customerGstin.clear()
        customerAddress.clear()
        customerName.clear()
        customerMobile.clear()
        draftClearItemForm()
        clearExchangeItems()
        customerSign.value = null
        ownerSign.value = null
        selectedItemList.clear()
        selectedItem.value = null
        draftShowAddItemDialog.value = false
        generatedPdfFile = null
        showPaymentDialog.value = false
        paymentInfo.value = null
    }


    fun draftCompleteOrder(context: Context, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        ioLaunch {
            try {
                if (selectedItemList.isNotEmpty()) {
                    // Create mock customer and store for draft invoice
                    val customer = CustomerEntity(
                        mobileNo = customerMobile.text,
                        name = customerName.text,
                        address = customerAddress.text,
                        gstin_pan = customerGstin.text,
                        addDate = Timestamp(System.currentTimeMillis()),
                        lastModifiedDate = Timestamp(System.currentTimeMillis())
                    )

                    val store = StoreEntity(
                        userId = generateId(),
                        proprietor = "Raj Kumar",
                        name = "RAJ JEWELLERS",
                        email = "rajjewellers@gmail.com",
                        phone = "9437206994",
                        address = "Old Medical Road, Malkangiri, Odisha - 764048",
                        registrationNo = "RAJ-REG-001",
                        gstinNo = "21APEPK7976C1ZZ",
                        panNo = "APEPK7976C",
                        image = "",
                        invoiceNo = 0,
                        storeId = generateId()
                    )


                    // Create DraftInvoiceData object

                    val invoiceData = createDraftInvoiceData(
                        store = store,
                        customer = customer,
                        items =selectedItemList,
                        metalRates = metalRates,
                        paymentInfo = paymentInfo,
                        appDatabase = appDatabase,
                        customerSign = customerSign,
                        ownerSign = ownerSign,
                        gstLabel = "GST @3 ",
                        discount = discount.text,
                        cardCharges= "0.00",
                        oldExchange = "0.00",
                        roundOff = "0.00",
                        dataStoreManager = _dataStoreManager
                    )

                    generateInvoicePdf(
                        context = context,
                        data = invoiceData,
                        scale = 2f
                    ) { file ->
                        generatedPdfFile = file
                        onSuccess()
                    }
                } else {
                    onFailure("Please add at least one item to generate the invoice")
                }
            } catch (e: Exception) {
                onFailure("Unable to Generate Draft Invoice PDF: ${e.message}")
            }
        }
    }
    //endregion


    fun editExchangeItem(exchangeItem: ExchangeItemDto) {
        showExchangeItemDialog.value = true
    }

    fun deleteExchangeItem(exchangeItem: ExchangeItemDto) {
        exchangeItemList.remove(exchangeItem)
    }

    fun getTotalExchangeValue(): Double {
        return exchangeItemList.sumOf { it.exchangeValue }
    }

    fun getNetPayableAmount(): Double {
        val orderTotal = getTotalOrderAmount()
        val manualDiscount = discount.text.toDoubleOrNull() ?: 0.0
        val exchangeValue = getTotalExchangeValue()
        return orderTotal - manualDiscount - exchangeValue
    }

    fun clearExchangeItems() {
        exchangeItemList.clear()
        showExchangeItemDialog.value = false
    }

    fun updateExchangeItemList(newList: List<ExchangeItemDto>) {
        exchangeItemList.clear()
        exchangeItemList.addAll(newList)
    }

    fun clearAllExchangeItems() {
        exchangeItemList.clear()
    }
    //endregion

}
