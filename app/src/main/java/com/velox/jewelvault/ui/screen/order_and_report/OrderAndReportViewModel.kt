package com.velox.jewelvault.ui.screen.order_and_report

import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velox.jewelvault.data.roomdb.AppDatabase
import com.velox.jewelvault.data.roomdb.entity.ItemEntity
import com.velox.jewelvault.data.roomdb.entity.order.OrderEntity
import com.velox.jewelvault.utils.DataStoreManager
import com.velox.jewelvault.utils.OrderSort
import com.velox.jewelvault.utils.withIo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderAndReportViewModel @Inject constructor(
    private val appDatabase: AppDatabase,
    private val _dataStoreManager: DataStoreManager,
) : ViewModel() {


    /*
    *
    *     val customerMobile: String, // FK to CustomerEntity
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
        "S.No", "Order Id","Order Date","Customer Name","Customer No", "Total Amount", "Total Charge"
    )
    val orderList: SnapshotStateList<OrderEntity> = SnapshotStateList()

    init {
        getAllOrdersSorted(OrderSort.ASCENDING)
    }

    private fun getAllOrdersSorted(sort: OrderSort) {
        viewModelScope.launch {
            withIo {
                when (sort) {
                    OrderSort.ASCENDING -> appDatabase.orderDao().getAllOrdersAsc()
                    OrderSort.DESCENDING -> appDatabase.orderDao().getAllOrdersDesc()
                }.collectLatest {
                    orderList.clear()
                    orderList.addAll(it)
                }
            }
        }
    }

}