package com.velox.jewelvault.ui.screen.order_and_purchase.order_item

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velox.jewelvault.data.roomdb.AppDatabase
import com.velox.jewelvault.data.roomdb.entity.customer.CustomerEntity
import com.velox.jewelvault.data.roomdb.entity.order.OrderEntity
import com.velox.jewelvault.data.roomdb.entity.order.OrderItemEntity
import com.velox.jewelvault.data.roomdb.entity.order.OrderWithItems
import com.velox.jewelvault.data.DataStoreManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderItemViewModel @Inject constructor(
    private val appDatabase: AppDatabase,
    private val _dataStoreManager: DataStoreManager,
    private val _loading : MutableState<Boolean>
) : ViewModel() {

    var orderWithItems by mutableStateOf<OrderWithItems?>(null)
        private set
    
    var customer by mutableStateOf<CustomerEntity?>(null)
        private set
    
    var isLoading by _loading
    
    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun loadOrderDetails(orderId: String) {
        viewModelScope.launch {
            try {
                isLoading = true
                errorMessage = null

                
                // Get order with items
                val orderData = appDatabase.orderDao().getOrderWithItems(orderId)
                orderWithItems = orderData
                
                // Get customer details
                val customerData = appDatabase.customerDao().getCustomerByMobile(orderData.order.customerMobile)
                customer = customerData
                
            } catch (e: Exception) {
                errorMessage = "Failed to load order details: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
    
    fun clearError() {
        errorMessage = null
    }
}