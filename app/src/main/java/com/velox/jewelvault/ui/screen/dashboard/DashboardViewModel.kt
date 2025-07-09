package com.velox.jewelvault.ui.screen.dashboard

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import com.velox.jewelvault.data.roomdb.AppDatabase
import com.velox.jewelvault.data.roomdb.dao.IndividualSellItem
import com.velox.jewelvault.data.roomdb.dao.SalesSummary
import com.velox.jewelvault.data.roomdb.dao.TimeRange
import com.velox.jewelvault.data.roomdb.dao.TopItemByCategory
import com.velox.jewelvault.data.roomdb.dao.TopSubCategory
import com.velox.jewelvault.data.roomdb.dao.range
import com.velox.jewelvault.data.roomdb.entity.CategoryEntity
import com.velox.jewelvault.data.roomdb.entity.SubCategoryEntity
import com.velox.jewelvault.utils.DataStoreManager
import com.velox.jewelvault.utils.ioLaunch
import com.velox.jewelvault.utils.log
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val appDatabase: AppDatabase,
    private val dataStoreManager: DataStoreManager,
    private val _loadingState: MutableState<Boolean>,
    private val _snackBarState: MutableState<String>
) : ViewModel() {

    val selectedRange: MutableState<TimeRange> = mutableStateOf(TimeRange.WEEKLY)

    val recentSellsItem = SnapshotStateList<IndividualSellItem>()
    val topSellingItemsMap = mutableStateMapOf<String, List<TopItemByCategory>>()
    val topSubCategories = SnapshotStateList<TopSubCategory>()
    val salesSummary: MutableState<SalesSummary?> = mutableStateOf(null)



    fun getRecentSellItem() {
        ioLaunch {
            val (start, end) = selectedRange.value.range()
            val list = appDatabase.orderDao().getIndividualSellItems(start, end)
            recentSellsItem.clear()
            recentSellsItem.addAll(list)
        }
    }

    fun getTopSellingItems() {
        ioLaunch {
            val (start, end) = selectedRange.value.range()
            val allItems = appDatabase.orderDao().getGroupedItemWeights(start, end)
            val topItemsPerCategory = allItems.groupBy { it.category }
                .mapValues { (_, items) -> items.sortedByDescending { it.totalFnWt }.take(5) }
            topSellingItemsMap.clear()
            topSellingItemsMap.putAll(topItemsPerCategory)
        }
    }


    fun getTopSellingSubCategories() {
        ioLaunch {
            val (start, end) = selectedRange.value.range()
            val list = appDatabase.orderDao().getTopSellingSubcategories(start, end)
            topSubCategories.clear()
            topSubCategories.addAll(list)
        }
    }

    fun getSalesSummary() {
        ioLaunch {
            val (start, end) = selectedRange.value.range()
            val summary = appDatabase.orderDao().getTotalSalesSummary(start, end)
            salesSummary.value = summary
        }
    }

    fun getSubCategoryCount(onComplete: (Int) -> Unit) {
        ioLaunch {
            val count = appDatabase.subCategoryDao().getSubCategoryCount()
            onComplete(count)
        }
    }
}
