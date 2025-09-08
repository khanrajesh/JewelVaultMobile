package com.velox.jewelvault.ui.screen.order_and_purchase

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import com.velox.jewelvault.data.DataStoreManager
import com.velox.jewelvault.data.roomdb.AppDatabase
import com.velox.jewelvault.data.roomdb.dto.PurchaseOrderWithDetails
import com.velox.jewelvault.data.roomdb.entity.order.OrderEntity
import com.velox.jewelvault.utils.SortOrder
import com.velox.jewelvault.utils.ioLaunch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class OrderAndReportViewModel @Inject constructor(
    private val appDatabase: AppDatabase,
    private val _dataStoreManager: DataStoreManager,
    @Named("currentScreenHeading") private val _currentScreenHeadingState: MutableState<String>,

    ) : ViewModel() {


    /*
    *
    val customerMobile: String, // FK to CustomerEntity
    val storeId: Int,
    val userId: Int,
    val orderDate: Timestamp,
    val totalAmount: Double = 0.0,
    val totalTax: Double = 0.0,
    val totalCharge: Double = 0.0,
    val note: String? = null
    *
    * */

    val currentScreenHeadingState = _currentScreenHeadingState

    val orderHeaderList = listOf(
        "S.No",
        "Order Id",
        "Order Date",
        "Customer Name",
        "Customer No",
        "Amount",
        "M.Charge",
        "Tax",
        "Total"
    )
    val purchaseHeaderList = listOf(
        "S.No",
        "Order Id",
        "Bill Date",
        "Firm Name/No",
        "Seller Name/No",
        "Bill No",
        "Item Details",
        "Exchange Details"
    )
    val orderList: SnapshotStateList<OrderEntity> = SnapshotStateList()

    val purchase: SnapshotStateList<PurchaseOrderWithDetails> = SnapshotStateList()

    var selectedTabIndex by  mutableIntStateOf(0)


    fun getAllOrdersSorted(sort: SortOrder) {
        ioLaunch {
            when (sort) {
                SortOrder.ASCENDING -> appDatabase.orderDao().getAllOrdersAsc()
                SortOrder.DESCENDING -> appDatabase.orderDao().getAllOrdersDesc()
            }.collectLatest {
                orderList.clear()
                orderList.addAll(it)
            }

        }
    }

    fun getAllPurchaseSorted(sort: SortOrder) {
        ioLaunch {
            when (sort) {
                SortOrder.ASCENDING -> appDatabase.purchaseDao()
                    .getAllOrdersWithDetailsByBillDateAsc()

                SortOrder.DESCENDING -> appDatabase.purchaseDao()
                    .getAllOrdersWithDetailsByBillDateDesc()
            }.collectLatest {
                purchase.clear()
                purchase.addAll(it)
            }

        }
    }

    fun deleteOrderWithItems(orderId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        ioLaunch {
            try {
                // Delete exchange items first
                appDatabase.orderDao().deleteExchangeItemsByOrderId(orderId)
                
                // Delete order items
                appDatabase.orderDao().deleteOrderItemsByOrderId(orderId)
                
                // Delete the order itself
                appDatabase.orderDao().deleteOrderById(orderId)
                
                // Refresh the order list
                getAllOrdersSorted(SortOrder.DESCENDING)
                onSuccess()
            } catch (e: Exception) {
                onFailure("Failed to delete order: ${e.message}")
            }
        }
    }

    fun deletePurchaseWithItems(purchaseOrderId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        ioLaunch {
            try {
                // Delete purchase order items
                val purchaseItems = appDatabase.purchaseDao().getItemsByOrderId(purchaseOrderId)
                purchaseItems.forEach { item ->
                    appDatabase.purchaseDao().deleteItem(item)
                }
                
                // Delete metal exchange items for this purchase order
                val metalExchanges = appDatabase.purchaseDao().getExchangeByOrderId(purchaseOrderId.toLong())
                metalExchanges.forEach { exchange ->
                    appDatabase.purchaseDao().deleteExchange(exchange)
                }
                
                // Delete the purchase order itself
                val purchaseOrder = purchase.find { it.order.purchaseOrderId == purchaseOrderId }
                purchaseOrder?.let {
                    appDatabase.purchaseDao().deleteOrder(it.order)
                }
                
                // Refresh the purchase list
                getAllPurchaseSorted(SortOrder.DESCENDING)
                onSuccess()
            } catch (e: Exception) {
                onFailure("Failed to delete purchase: ${e.message}")
            }
        }
    }

}