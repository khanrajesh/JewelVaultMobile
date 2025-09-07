package com.velox.jewelvault.ui.screen.dashboard

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import com.velox.jewelvault.data.DataStoreManager
import com.velox.jewelvault.data.roomdb.AppDatabase
import com.velox.jewelvault.data.roomdb.dao.IndividualSellItem
import com.velox.jewelvault.data.roomdb.dao.SalesSummary
import com.velox.jewelvault.data.roomdb.dao.TimeRange
import com.velox.jewelvault.data.roomdb.dao.TopItemByCategory
import com.velox.jewelvault.data.roomdb.dao.TopSubCategory
import com.velox.jewelvault.data.roomdb.dao.range
import com.velox.jewelvault.data.roomdb.dto.CustomerBalanceSummary
import com.velox.jewelvault.utils.ioLaunch
import com.velox.jewelvault.utils.log
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Named

// Data class for customer summary statistics
data class CustomerSummary(
    val totalCustomers: Int = 0,
    val activeCustomers: Int = 0,
    val activeKhataBooks: Int = 0,
    val totalOutstandingBalance: Double = 0.0,
    val totalKhataBookAmount: Double = 0.0,
    val totalKhataBookPaidAmount: Double = 0.0,
    val totalKhataBookRemainingAmount: Double = 0.0,
    val customersWithOutstandingBalance: Int = 0
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val appDatabase: AppDatabase,
    private val dataStoreManager: DataStoreManager,
    private val _loadingState: MutableState<Boolean>,
    @Named("snackMessage") private val _snackBarState: MutableState<String>,
    @Named("currentScreenHeading") private val _currentScreenHeadingState: MutableState<String>,

    ) : ViewModel() {

    val loadingState = _loadingState
    val snackBarState = _snackBarState
    val currentScreenHeadingState = _currentScreenHeadingState

    /**
     * return Triple of Flow<String> for userId, userName, mobileNo
     * */
    val admin: Triple<Flow<String>, Flow<String>, Flow<String>> = dataStoreManager.getAdminInfo()

    /**
     * return Triple of Flow<String> for storeId, upiId, storeName
     * */
    val store: Triple<Flow<String>, Flow<String>, Flow<String>> =
        dataStoreManager.getSelectedStoreInfo()
    val selectedRange: MutableState<TimeRange> = mutableStateOf(TimeRange.WEEKLY)

    val recentSellsItem = SnapshotStateList<IndividualSellItem>()
    val topSellingItemsMap = mutableStateMapOf<String, List<TopItemByCategory>>()
    val topSubCategories = SnapshotStateList<TopSubCategory>()
    val salesSummary: MutableState<SalesSummary?> = mutableStateOf(null)

    // Customer-related data
    val customerSummary: MutableState<CustomerSummary?> = mutableStateOf(null)
    val customersWithOutstandingBalance = SnapshotStateList<CustomerBalanceSummary>()

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

    fun getCustomerSummary() {
        ioLaunch {
            try {
                val userId = admin.first.first()
                val storeId = store.first.first()

                // Get total customers
                val allCustomers = appDatabase.customerDao().getAllCustomers().first()
                val totalCustomers = allCustomers.size
                val activeCustomers = allCustomers.count { it.isActive }

                // Get outstanding balance statistics
                val totalOutstandingBalance =
                    appDatabase.customerTransactionDao().getTotalOutstandingAmount(userId, storeId)
                val customersWithOutstanding = appDatabase.customerTransactionDao()
                    .getCustomersWithOutstandingBalance(userId, storeId)

                // Get khata book statistics
                val khataBookSummaries =
                    appDatabase.customerKhataBookDao().getKhataBookSummaries(userId, storeId)
                val activeKhataBooks = khataBookSummaries.count { it.status == "active" }
                val totalKhataBookMonthlyAmount = khataBookSummaries.sumOf { it.monthlyAmount }

                // Calculate paid amounts for active khata books
                val totalKhataBookPaidAmount = khataBookSummaries.sumOf { summary ->
                    val paidAmount = appDatabase.customerTransactionDao()
                        .getKhataBookTotalPaidAmount(summary.khataBookId)
                    paidAmount
                }

                val totalKhataBookRemainingAmount = totalKhataBookMonthlyAmount

                val summary = CustomerSummary(
                    totalCustomers = totalCustomers,
                    activeCustomers = activeCustomers,
                    activeKhataBooks = activeKhataBooks,
                    totalOutstandingBalance = totalOutstandingBalance,
                    totalKhataBookAmount = totalKhataBookMonthlyAmount,
                    totalKhataBookPaidAmount = totalKhataBookPaidAmount,
                    totalKhataBookRemainingAmount = totalKhataBookRemainingAmount,
                    customersWithOutstandingBalance = customersWithOutstanding.size
                )

                customerSummary.value = summary
                customersWithOutstandingBalance.clear()
                customersWithOutstandingBalance.addAll(customersWithOutstanding)

            } catch (e: Exception) {
                log("Failed to load customer summary: ${e.message}")
            }
        }
    }

    fun getSubCategoryCount(onComplete: (Int) -> Unit) {
        ioLaunch {
            val count = appDatabase.subCategoryDao().getSubCategoryCount()
            onComplete(count)
        }
    }

    fun refreshAllData() {
        getRecentSellItem()
        getSalesSummary()
        getTopSellingItems()
        getTopSellingSubCategories()
        getCustomerSummary()
    }

    fun updateTimeRange(newRange: TimeRange) {
        selectedRange.value = newRange
        refreshAllData()
    }
}
