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
import com.velox.jewelvault.data.roomdb.dto.PreOrderSummary
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
    val totalOutstandingBalance: Double = 0.0,
    val currentMonthKhataBookPayments: Double = 0.0,
    val totalKhataBookPaidAmount: Double = 0.0,
    val activeKhataBookCustomers: Int = 0
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

    // Pre-order related data
    val upcomingPreOrders = SnapshotStateList<PreOrderSummary>()

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

    fun getUpcomingPreOrders(limit: Int = 10) {
        ioLaunch {
            try {
                val list = appDatabase.preOrderDao().observeUpcomingPreOrders(limit).first()
                upcomingPreOrders.clear()
                upcomingPreOrders.addAll(list)
            } catch (e: Exception) {
                log("Failed to load upcoming pre-orders: ${e.message}")
            }
        }
    }

    fun getCustomerSummary() {
        ioLaunch {
            try {
                val userId = admin.first.first()
                val storeId = store.first.first()

                // 1. Total number of customers
                val allCustomers = appDatabase.customerDao().getAllCustomers().first()
                val totalCustomers = allCustomers.size
                val activeCustomers = allCustomers.count { it.mobileNo.isNotBlank() }

                // 2. Remaining outstanding balance of all customers
                val totalOutstandingBalance = appDatabase.customerTransactionDao()
                    .getTotalOutstandingAmount(userId, storeId)

                // 3. Current month total khata book incoming payments from customers
                val currentMonthKhataBookPayments = appDatabase.customerTransactionDao()
                    .getCurrentMonthKhataBookPayments(userId, storeId)

                // 4. Total paid amount of khata book plans by customers (all time)
                val totalKhataBookPaidAmount = appDatabase.customerTransactionDao()
                    .getTotalKhataBookPayments(userId, storeId)

                // 5. Total active khata book customers
                val activeKhataBooks = appDatabase.customerKhataBookDao().getActiveKhataBooks(userId, storeId)
                val activeKhataBookCustomers = activeKhataBooks.map { it.customerMobile }.distinct().size

                val summary = CustomerSummary(
                    totalCustomers = totalCustomers,
                    activeCustomers = activeCustomers,
                    totalOutstandingBalance = totalOutstandingBalance,
                    currentMonthKhataBookPayments = currentMonthKhataBookPayments,
                    totalKhataBookPaidAmount = totalKhataBookPaidAmount,
                    activeKhataBookCustomers = activeKhataBookCustomers
                )

                customerSummary.value = summary

                // Also update the customers with outstanding balance list
                val customersWithOutstanding = appDatabase.customerTransactionDao()
                    .getCustomersWithOutstandingBalance(userId, storeId)
                customersWithOutstandingBalance.clear()
                customersWithOutstandingBalance.addAll(customersWithOutstanding)

                log("Customer Summary Loaded:")
                log("  Total Customers: $totalCustomers")
                log("  Active Customers: $activeCustomers")
                log("  Total Outstanding Balance: $totalOutstandingBalance")
                log("  Current Month Khata Payments: $currentMonthKhataBookPayments")
                log("  Total Khata Book Paid: $totalKhataBookPaidAmount")
                log("  Active Khata Book Customers: $activeKhataBookCustomers")

            } catch (e: Exception) {
                log("Failed to load customer summary: ${e.message}")
            }
        }
    }

    fun getSubCategoryCount(onComplete: (Int) -> Unit) {
        ioLaunch {
            try {
                val count = appDatabase.subCategoryDao().getSubCategoryCount()
                onComplete(count)
            } catch (e: Exception) {
                log("Failed to get subcategory count: ${e.message}")
                onComplete(0) // Return 0 on error to prevent UI hanging
            }
        }
    }

    fun refreshAllData() {
        getRecentSellItem()
        getSalesSummary()
        getTopSellingItems()
        getTopSellingSubCategories()
        getCustomerSummary()
        getUpcomingPreOrders()
    }

    fun updateTimeRange(newRange: TimeRange) {
        selectedRange.value = newRange
        refreshAllData()
    }
}
