package com.velox.jewelvault.ui.screen.dashboard

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velox.jewelvault.data.roomdb.AppDatabase
import com.velox.jewelvault.data.roomdb.dao.IndividualSellItem
import com.velox.jewelvault.data.roomdb.dao.SalesSummary
import com.velox.jewelvault.data.roomdb.dao.TimeRange
import com.velox.jewelvault.data.roomdb.dao.TopItemByCategory
import com.velox.jewelvault.data.roomdb.dao.TopSubCategory
import com.velox.jewelvault.data.roomdb.dao.range
import com.velox.jewelvault.utils.DataStoreManager
import com.velox.jewelvault.utils.withIo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val appDatabase: AppDatabase,
    private val dataStoreManager: DataStoreManager,
    private val _loadingState: MutableState<Boolean>,
    private val _snackBarState: MutableState<String>
) : ViewModel() {

    var selectedRange: TimeRange = TimeRange.WEEKLY

    val recentSellsItem = SnapshotStateList<IndividualSellItem>()
    val topSellingItemsMap = mutableStateMapOf<String, List<TopItemByCategory>>()
    val topSubCategories = SnapshotStateList<TopSubCategory>()
    val salesSummary: MutableState<SalesSummary?> = mutableStateOf(null)

    fun getRecentSellItem() {
        viewModelScope.launch {
            withIo {
                val (start, end) = selectedRange.range()
                val list = appDatabase.orderDao().getIndividualSellItems(start, end)
                recentSellsItem.clear()
                recentSellsItem.addAll(list)
            }
        }
    }

    fun getTopSellingItems() {
        viewModelScope.launch {
            withIo {
                val (start, end) = selectedRange.range()
                val allItems = appDatabase.orderDao().getGroupedItemWeights(start, end)
                val topItemsPerCategory = allItems
                    .groupBy { it.category }
                    .mapValues { (_, items) -> items.sortedByDescending { it.totalFnWt }.take(5) }

                // Step 3: Store for UI or other processing
                topSellingItemsMap.clear()
                topSellingItemsMap.putAll(topItemsPerCategory)
            }
        }
    }


    fun getTopSellingSubCategories() {
        viewModelScope.launch {
            withIo {
                val (start, end) = selectedRange.range()
                val list = appDatabase.orderDao().getTopSellingSubcategories(start, end)
                topSubCategories.clear()
                topSubCategories.addAll(list)
            }
        }
    }

    fun getSalesSummary() {
        viewModelScope.launch {
            withIo {
                val (start, end) = selectedRange.range()
                val summary = appDatabase.orderDao().getTotalSalesSummary(start, end)
                salesSummary.value = summary
            }
        }
    }
}
