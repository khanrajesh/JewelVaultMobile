package com.velox.jewelvault.ui.screen.order_and_purchase

import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import com.velox.jewelvault.data.roomdb.AppDatabase
import com.velox.jewelvault.data.roomdb.dto.PurchaseOrderWithDetails
import com.velox.jewelvault.data.roomdb.entity.order.OrderEntity
import com.velox.jewelvault.data.DataStoreManager
import com.velox.jewelvault.utils.SortOrder
import com.velox.jewelvault.utils.ioLaunch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

@HiltViewModel
class OrderAndReportViewModel @Inject constructor(
    private val appDatabase: AppDatabase,
    private val _dataStoreManager: DataStoreManager,
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
    val orderHeaderList =  listOf(
        "S.No", "Order Id","Order Date","Customer Name","Customer No", "Amount", "M.Charge", "Tax", "Total"
    )
    val purchaseHeaderList =  listOf(
        "S.No", "Order Id","Bill Date","Firm Name/No","Seller Name/No", "Bill No", "Item Details","Exchange Details"
    )
    val orderList: SnapshotStateList<OrderEntity> = SnapshotStateList()

    val purchase: SnapshotStateList<PurchaseOrderWithDetails> = SnapshotStateList()

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
                    SortOrder.ASCENDING -> appDatabase.purchaseDao().getAllOrdersWithDetailsByBillDateAsc()
                    SortOrder.DESCENDING -> appDatabase.purchaseDao().getAllOrdersWithDetailsByBillDateDesc()
                }.collectLatest {
                    purchase.clear()
                    purchase.addAll(it)
                }

        }
    }

}