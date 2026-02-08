package com.velox.jewelvault.ui.screen.order_and_purchase.order_item

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velox.jewelvault.data.roomdb.AppDatabase
import com.velox.jewelvault.data.roomdb.entity.customer.CustomerEntity
import com.velox.jewelvault.data.roomdb.entity.order.OrderDetailsEntity
import com.velox.jewelvault.data.roomdb.entity.StoreEntity
import com.velox.jewelvault.data.DataStoreManager
import com.velox.jewelvault.ui.components.InputFieldState
import com.velox.jewelvault.utils.generateInvoicePdf
import com.velox.jewelvault.utils.createDraftInvoiceData
import com.velox.jewelvault.utils.to3FString
import com.velox.jewelvault.utils.SecurityUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class OrderItemViewModel @Inject constructor(
    private val appDatabase: AppDatabase,
    private val _dataStoreManager: DataStoreManager,
    private val _loading : MutableState<Boolean>,
    @Named("snackMessage") private val _snackBar: MutableState<String>,
    @Named("currentScreenHeading") private val _currentScreenHeadingState: MutableState<String>,

) : ViewModel() {
    val snackBar = _snackBar

    val currentScreenHeadingState = _currentScreenHeadingState
    var orderDetailsEntity by mutableStateOf<OrderDetailsEntity?>(null)
        private set
    
    var customer by mutableStateOf<CustomerEntity?>(null)
        private set
    
    var store by mutableStateOf<StoreEntity?>(null)
        private set
    
    var isLoading by _loading

    var generatedPdfUri by mutableStateOf<Uri?>(null)
        private set
        
    var isPdfGenerating by mutableStateOf(false)
        private set

    val invoiceNo = InputFieldState(initValue = "${System.currentTimeMillis() % 10000}")


    fun loadOrderDetails(orderId: String) {
        viewModelScope.launch {
            try {
                isLoading = true

                
                // Get order with items
                val orderData = appDatabase.orderDao().getOrderWithItems(orderId)
                orderDetailsEntity = orderData
                
                // Get customer details
                val customerData = appDatabase.customerDao().getCustomerByMobile(orderData.order.customerMobile)
                customer = customerData
                
                // Get store details
                val storeData = appDatabase.storeDao().getStoreById(orderData.order.storeId)
                store = storeData
                
            } catch (e: Exception) {
                snackBar.value = "Failed to load order details: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
    
    fun generateOrderPdf(context: Context) {
        if (orderDetailsEntity == null || customer == null || store == null) {
            snackBar.value = "Cannot generate PDF: Missing order data"
            return
        }
        
        viewModelScope.launch {
            try {
                isPdfGenerating = true

                // Convert order items to ItemSelectedModel for PDF generation
                val items = orderDetailsEntity!!.items.map { orderItem ->
                    com.velox.jewelvault.data.roomdb.dto.ItemSelectedModel(
                        itemId = orderItem.itemId,
                        catName = orderItem.catName,
                        subCatName = orderItem.subCatName,
                        itemAddName = orderItem.itemAddName,
                        quantity = orderItem.quantity,
                        gsWt = orderItem.gsWt,
                        ntWt = orderItem.ntWt,
                        fnWt = orderItem.fnWt,
                        fnMetalPrice = orderItem.fnMetalPrice,
                        purity = orderItem.purity,
                        crgType = orderItem.crgType,
                        crg = orderItem.crg,
                        compCrg = orderItem.othCrg,
                        cgst = orderItem.cgst,
                        sgst = orderItem.sgst,
                        igst = orderItem.igst,
                        huid = orderItem.huid,
                        addDesKey = orderItem.addDesKey,
                        addDesValue = orderItem.addDesValue,
                        price = orderItem.price,
                        chargeAmount = orderItem.charge,
                        addDate = orderItem.orderDate,
                        sellerFirmId = orderItem.sellerFirmId,
                        purchaseOrderId = orderItem.purchaseOrderId,
                        purchaseItemId = orderItem.purchaseItemId,
                        compDes = orderItem.othCrgDes,
                        entryType = orderItem.entryType,
                        catId = orderItem.catId,
                        subCatId = orderItem.subCatId,
                        userId = orderItem.customerMobile,
                        storeId = orderItem.orderId
                    )
                }
                
                // Create DraftInvoiceData object
                val invoiceData = createDraftInvoiceData(
                    store = store!!,
                    customer = customer!!,
                    items = items,
                    metalRates = emptyList(), // We don't have metal rates in order context
                    paymentInfo = mutableStateOf(null), // No payment info for order details
                    appDatabase = appDatabase,
                    customerSign = mutableStateOf(null), // No signatures for order details
                    ownerSign = mutableStateOf(null),
                    gstLabel = "GST @3%",
                    discount = orderDetailsEntity!!.order.discount.to3FString(),
                    cardCharges = "0.00",
                    oldExchange = orderDetailsEntity!!.exchangeItems.sumOf { it.exchangeValue }.to3FString(),
                    roundOff = "0.00",
                    dataStoreManager = _dataStoreManager,
                    invoiceNo = invoiceNo.text
                )
                
                generateInvoicePdf(
                    context = context,
                    data = invoiceData,
                    scale = 2f
                ) { uri ->
                    generatedPdfUri = uri
                    isPdfGenerating = false
                }
                
            } catch (e: Exception) {
                snackBar.value = "Failed to generate PDF: ${e.message}"
                isPdfGenerating = false
            }
        }
    }
    
    fun clearPdf() {
        generatedPdfUri = null
    }
    
    
    fun deleteOrderWithItems(
        orderId: String,
        adminPin: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                isLoading = true
                
                // Verify admin pin first
                if (!verifyAdminPin(adminPin)) {
                    onFailure("Invalid Admin PIN")
                    return@launch
                }
                
                // Delete exchange items first
                appDatabase.orderDao().deleteExchangeItemsByOrderId(orderId)
                
                // Delete order items
                appDatabase.orderDao().deleteOrderItemsByOrderId(orderId)
                
                // Delete the order itself
                appDatabase.orderDao().deleteOrderById(orderId)
                
                onSuccess()
            } catch (e: Exception) {
                onFailure("Failed to delete order: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }
    
    private suspend fun verifyAdminPin(adminPin: String): Boolean {
        return try {
            val adminUser = appDatabase.userDao().getAdminUser()
            if (adminUser != null && adminUser.pin != null) {
                SecurityUtils.verifyPin(adminPin, adminUser.pin)
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}