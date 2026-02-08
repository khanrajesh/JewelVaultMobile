package com.velox.jewelvault.ui.screen.customers

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velox.jewelvault.data.DataStoreManager
import com.velox.jewelvault.data.roomdb.AppDatabase
import com.velox.jewelvault.data.roomdb.dto.CustomerSummaryDto
import com.velox.jewelvault.data.roomdb.dto.TransactionItem
import com.velox.jewelvault.data.roomdb.entity.customer.CustomerEntity
import com.velox.jewelvault.data.roomdb.entity.customer.CustomerKhataBookEntity
import com.velox.jewelvault.data.roomdb.entity.customer.CustomerKhataBookPlanEntity
import com.velox.jewelvault.data.roomdb.entity.customer.CustomerTransactionEntity
import com.velox.jewelvault.ui.components.InputFieldState
import com.velox.jewelvault.utils.TransactionUtils
import com.velox.jewelvault.utils.InputValidator
import com.velox.jewelvault.utils.formatCurrency
import com.velox.jewelvault.utils.formatDate
import com.velox.jewelvault.utils.generateId
import com.velox.jewelvault.utils.ioLaunch
import com.velox.jewelvault.utils.ioScope
import com.velox.jewelvault.utils.log
import com.velox.jewelvault.utils.mainScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.sql.Timestamp
import javax.inject.Inject
import javax.inject.Named


@HiltViewModel
class CustomerViewModel @Inject constructor(
    private val appDatabase: AppDatabase,
    private val _dataStoreManager: DataStoreManager,
    private val _loadingState: MutableState<Boolean>,

    @Named("snackMessage") private val _snackBarState: MutableState<String>,
    @Named("currentScreenHeading") private val _currentScreenHeadingState: MutableState<String>,
) : ViewModel() {

    val currentScreenHeadingState = _currentScreenHeadingState

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
    val dataStoreManager = _dataStoreManager

    // Customer lists
    val customerList: SnapshotStateList<CustomerSummaryDto> = SnapshotStateList()

    // Add a backing list to store all loaded customers
    private val allCustomers: SnapshotStateList<CustomerSummaryDto> = SnapshotStateList()
    private var lastSearchQuery: String = ""

    private fun applyFilterAndSearch() {
        val filter = filterType.value
        val query = lastSearchQuery.trim().lowercase()
        val filtered = when (filter) {
            "all" -> allCustomers
            "outstanding" -> allCustomers.filter { it.outstandingBalance > 0 }
            "khata" -> allCustomers.filter { it.hasKhataBook }
            else -> allCustomers
        }
        val searched = if (query.isNotEmpty()) {
            filtered.filter {
                it.name.lowercase()
                    .contains(query) || it.mobileNo.contains(query) || (it.address?.lowercase()
                    ?.contains(query) == true)
            }
        } else filtered
        customerList.clear()
        customerList.addAll(searched)
    }

    // Form states for adding/editing customers
    val customerName = InputFieldState()
    val customerMobile = InputFieldState()
    val customerAddress = InputFieldState()
    val customerGstin = InputFieldState()
    val customerNotes = InputFieldState()

    // Outstanding balance form states
    val outstandingAmount = InputFieldState()
    val outstandingType = InputFieldState("debt") // "debt", "payment", "adjustment"
    val outstandingDescription = InputFieldState()
    val outstandingNotes = InputFieldState()

    // Khata book form states
    val khataBookMonthlyAmount = InputFieldState()
    val khataBookTotalMonths = InputFieldState("12")
    val khataBookPlanName = InputFieldState("Standard Plan")
    val khataBookStartDate = InputFieldState()
    val khataBookNotes = InputFieldState()

    // Khata payment form states
    val khataPaymentMonth = InputFieldState()
    val khataPaymentAmount = InputFieldState()
    val khataPaymentType = InputFieldState("full") // "full", "partial", "advance"
    val khataPaymentNotes = InputFieldState()

    // Regular payment form states
    val regularPaymentAmount = InputFieldState()
    val regularPaymentType = InputFieldState("regular") // "regular", "advance", "refund"
    val regularPaymentMethod = InputFieldState("cash")
    val regularPaymentReference = InputFieldState()
    val regularPaymentNotes = InputFieldState()

    // Search and filter states
    val searchQuery = InputFieldState()
    val filterType = mutableStateOf("all") // "all", "outstanding", "khata"

    // Statistics
    val totalCustomers = mutableStateOf(0)
    val totalOutstandingAmount = mutableStateOf(0.0)
    val totalKhataBookAmount = mutableStateOf(0.0)
    val totalKhataBookMonthlyPaid = mutableStateOf(0.0)
    val totalKhataBookMonthlyPending = mutableStateOf(0.0)
    val activeCustomersCount = mutableStateOf(0)

    // New statistics for updated summary
    val currentMonthKhataBookPayments = mutableStateOf(0.0)
    val totalKhataBookPaidAmount = mutableStateOf(0.0)
    val activeKhataBookCustomersCount = mutableStateOf(0)

    // Selected customer for detail view
    val selectedCustomer = mutableStateOf<CustomerEntity?>(null)
    val selectedCustomerTransactions = mutableStateListOf<CustomerTransactionEntity>()
    val selectedCustomerKhataBooks = mutableStateListOf<CustomerKhataBookEntity>()
    val selectedCustomerCompletedKhataBooks = mutableStateListOf<CustomerKhataBookEntity>()

    val customerEditPermissions = mutableStateOf<CustomerEditPermissions?>(null)

    // Force recomposition when transactions are updated
    val transactionsUpdated = mutableStateOf(0)

    // Khata book plans data
    val activeKhataBookCustomers = mutableStateListOf<CustomerKhataBookEntity>()
    val maturedKhataBookPlans = mutableStateListOf<CustomerKhataBookEntity>()
    val pendingKhataBookAmount = mutableStateOf(0.0)

    // Customer orders and transactions
    val customerOrders =
        mutableStateOf<List<com.velox.jewelvault.data.roomdb.entity.order.OrderEntity>>(emptyList())
    val transactionHistory = mutableStateOf<List<TransactionItem>>(emptyList())

    // Loading states
    val isLoadingCustomerDetails = mutableStateOf(false)
    val isLoadingKhataBook = mutableStateOf(false)
    val isLoadingPayment = _loadingState

    val planList = mutableStateListOf<KhataBookPlan>()

    init {
        // Load predefined plans and refresh custom plans from DB.
        planList.addAll(getPredefinedPlans())
        ioLaunch {
            try {
                val userId = admin.first.first()
                val storeId = store.first.first()
                refreshPlanList(userId, storeId)
            } catch (e: Exception) {
                log("Failed to load khata book plans: ${e.message}")
            }
        }
    }

    fun addPlan(name: String, payMonths: Int, benefitMonths: Int, description: String) {
        ioLaunch {
            try {
                val userId = admin.first.first()
                val storeId = store.first.first()
                val now = System.currentTimeMillis()
                val benefitPercentage = calculateBenefitPercentage(payMonths, benefitMonths)
                val planEntity = CustomerKhataBookPlanEntity(
                    planId = generateId(),
                    name = name.trim(),
                    payMonths = payMonths,
                    benefitMonths = benefitMonths,
                    description = description.trim(),
                    benefitPercentage = benefitPercentage,
                    userId = userId,
                    storeId = storeId,
                    createdAt = now,
                    updatedAt = now
                )
                appDatabase.customerKhataBookPlanDao().insertPlan(planEntity)
                refreshPlanList(userId, storeId)
                mainScope {
                    _snackBarState.value = "Khata book plan added successfully"
                }
            } catch (e: Exception) {
                log("Failed to add khata book plan: ${e.message}")
                mainScope {
                    _snackBarState.value = "Failed to add khata book plan: ${e.message}"
                }
            }
        }
    }

    fun editPlan(
        plan: KhataBookPlan,
        name: String,
        payMonths: Int,
        benefitMonths: Int,
        description: String
    ) {
        val benefitPercentage = calculateBenefitPercentage(payMonths, benefitMonths)
        if (!plan.isCustom || plan.planId.isBlank()) {
            val idx = planList.indexOfFirst { it.planId == plan.planId && plan.planId.isNotBlank() }
                .takeIf { it >= 0 } ?: planList.indexOf(plan)
            if (idx >= 0) {
                planList[idx] = plan.copy(
                    name = name,
                    payMonths = payMonths,
                    benefitMonths = benefitMonths,
                    description = description,
                    benefitPercentage = benefitPercentage,
                    updatedAt = System.currentTimeMillis()
                )
            }
            return
        }

        ioLaunch {
            try {
                val userId = admin.first.first()
                val storeId = store.first.first()
                val now = System.currentTimeMillis()
                val updatedEntity = CustomerKhataBookPlanEntity(
                    planId = plan.planId,
                    name = name.trim(),
                    payMonths = payMonths,
                    benefitMonths = benefitMonths,
                    description = description.trim(),
                    benefitPercentage = benefitPercentage,
                    userId = userId,
                    storeId = storeId,
                    createdAt = if (plan.createdAt > 0) plan.createdAt else now,
                    updatedAt = now
                )
                appDatabase.customerKhataBookPlanDao().insertPlan(updatedEntity)
                refreshPlanList(userId, storeId)
                mainScope {
                    _snackBarState.value = "Khata book plan updated successfully"
                }
            } catch (e: Exception) {
                log("Failed to update khata book plan: ${e.message}")
                mainScope {
                    _snackBarState.value = "Failed to update khata book plan: ${e.message}"
                }
            }
        }
    }

    fun deletePlan(plan: KhataBookPlan) {
        if (!plan.isCustom || plan.planId.isBlank()) {
            _snackBarState.value = "Predefined plans can't be deleted"
            return
        }

        ioLaunch {
            try {
                val userId = admin.first.first()
                val storeId = store.first.first()
                appDatabase.customerKhataBookPlanDao().deletePlanById(plan.planId)
                refreshPlanList(userId, storeId)
                mainScope {
                    _snackBarState.value = "Khata book plan deleted successfully"
                }
            } catch (e: Exception) {
                log("Failed to delete khata book plan: ${e.message}")
                mainScope {
                    _snackBarState.value = "Failed to delete khata book plan: ${e.message}"
                }
            }
        }
    }

    private fun calculateBenefitPercentage(payMonths: Int, benefitMonths: Int): Double {
        return if (payMonths + benefitMonths > 0) {
            benefitMonths * 100.0 / (payMonths + benefitMonths)
        } else 0.0
    }

    private fun findPlanByName(planName: String): KhataBookPlan? {
        return planList.firstOrNull { it.name == planName }
            ?: getPredefinedPlans().firstOrNull { it.name == planName }
    }

    private fun mapPlanEntity(entity: CustomerKhataBookPlanEntity): KhataBookPlan {
        return KhataBookPlan(
            name = entity.name,
            payMonths = entity.payMonths,
            benefitMonths = entity.benefitMonths,
            description = entity.description,
            benefitPercentage = entity.benefitPercentage,
            planId = entity.planId,
            isCustom = true,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    private suspend fun refreshPlanList(userId: String, storeId: String) {
        val customPlans =
            appDatabase.customerKhataBookPlanDao().getPlansByUserAndStore(userId, storeId)
        mainScope {
            planList.clear()
            planList.addAll(customPlans.map { mapPlanEntity(it) })
            planList.addAll(getPredefinedPlans())
        }
    }



    fun loadCustomerData() {
        viewModelScope.launch {
            try {
                val userId = admin.first.first()
                val storeId = store.first.first()

                log("Starting customer data load for userId: $userId, storeId: $storeId")

                // Load customers and statistics in a single coroutine
                ioLaunch {
                    try {
                        // Load customers first
                        val customers = appDatabase.customerDao().getAllCustomers().first()
                        log("Loaded ${customers.size} customers from database")

                        // Convert to CustomerSummaryDto with outstanding and khata book data
                        val customerSummaries = customers.map { customer ->
                            createCustomerSummary(customer)
                        }
                        
                        allCustomers.clear()
                        allCustomers.addAll(customerSummaries)
                        applyFilterAndSearch()
                        
                        // Load outstanding balance statistics using new unified system
                        val totalOutstanding =
                            appDatabase.customerTransactionDao().getTotalOutstandingAmount(userId, storeId)

                        // Load khata book statistics - calculate monthly payments and pending
                        val activeKhataBooks =
                            appDatabase.customerKhataBookDao().getActiveKhataBooks(userId, storeId)

                        // Calculate current month's total payments and pending amounts
                        val currentMonth =
                            java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1
                        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)

                        var totalMonthlyPaid = 0.0
                        var totalMonthlyPending = 0.0
                        var totalKhataAmount = 0.0

                        for (khataBook in activeKhataBooks) {
                            // Get payments for current month
                            val currentMonthPayments = appDatabase.customerTransactionDao()
                                .getKhataBookPayments(khataBook.khataBookId).first().filter {
                                    val cal = java.util.Calendar.getInstance()
                                    cal.timeInMillis = it.transactionDate.time
                                    cal.get(java.util.Calendar.MONTH) + 1 == currentMonth && cal.get(java.util.Calendar.YEAR) == currentYear
                                }

                            val monthlyPaid = currentMonthPayments.sumOf { it.amount }
                            val monthlyPending = khataBook.monthlyAmount - monthlyPaid

                            totalMonthlyPaid += monthlyPaid
                            totalMonthlyPending += maxOf(0.0, monthlyPending)
                            totalKhataAmount += khataBook.totalAmount
                        }

                        // Get new metrics using DAO queries
                        val currentMonthKhataPayments = appDatabase.customerTransactionDao()
                            .getCurrentMonthKhataBookExpected(userId, storeId)
                        val totalKhataPaidAmount =
                            appDatabase.customerTransactionDao().getTotalKhataBookPayments(userId, storeId)
                        val activeKhataBookCustomers =
                            activeKhataBooks.map { it.customerMobile }.distinct().size

                        log("=== CUSTOMER DATA LOADING DEBUG ===")
                        log("Total customers loaded: ${customers.size}")
                        log("Total outstanding amount: $totalOutstanding")
                        log("Active khata books count: ${activeKhataBooks.size}")
                        log("Current month paid: $totalMonthlyPaid")
                        log("Current month pending: $totalMonthlyPending")
                        log("Total khata amount: $totalKhataAmount")
                        log("Current month khata payments (DAO): $currentMonthKhataPayments")
                        log("Total khata paid amount (DAO): $totalKhataPaidAmount")
                        log("Active khata book customers: $activeKhataBookCustomers")
                        log("=== END DEBUG ===")

                        mainScope {
                            // Update customer statistics
                            totalCustomers.value = customers.size
                            activeCustomersCount.value = customerSummaries.count { it.isActive }
                            
                            // Update outstanding balance
                            totalOutstandingAmount.value = totalOutstanding
                            
                            // Update khata book statistics
                            totalKhataBookAmount.value = totalKhataAmount
                            totalKhataBookMonthlyPaid.value = totalMonthlyPaid
                            totalKhataBookMonthlyPending.value = totalMonthlyPending

                            // Update new metrics
                            currentMonthKhataBookPayments.value = currentMonthKhataPayments
                            totalKhataBookPaidAmount.value = totalKhataPaidAmount
                            activeKhataBookCustomersCount.value = activeKhataBookCustomers
                            
                            log("=== VALUES UPDATED IN UI ===")
                            log("totalCustomers.value: ${totalCustomers.value}")
                            log("totalOutstandingAmount.value: ${totalOutstandingAmount.value}")
                            log("currentMonthKhataBookPayments.value: ${currentMonthKhataBookPayments.value}")
                            log("totalKhataBookPaidAmount.value: ${totalKhataBookPaidAmount.value}")
                            log("activeKhataBookCustomersCount.value: ${activeKhataBookCustomersCount.value}")
                            log("=== END VALUES UPDATE ===")
                        }
                    } catch (e: Exception) {
                        log("Error in ioLaunch: ${e.message}")
                        mainScope {
                            _snackBarState.value = "Failed to load customer data: ${e.message}"
                        }
                    }
                }

            } catch (e: Exception) {
                log("Failed to load customer data: ${e.message}")
                _snackBarState.value = "Failed to load customer data: ${e.message}"
            }
        }
    }

    // Add a retry mechanism for data loading
    fun retryLoadCustomerData() {
        log("Retrying customer data load...")
        loadCustomerData()
    }

    fun loadKhataBookPlans() {
        ioLaunch {
            try {
                val userId = admin.first.first()
                val storeId = store.first.first()
                refreshPlanList(userId, storeId)

                // Load active khata book customers
                val activeKhataBooks =
                    appDatabase.customerKhataBookDao().getActiveKhataBooks(userId, storeId)
                mainScope {
                    activeKhataBookCustomers.clear()
                    activeKhataBookCustomers.addAll(activeKhataBooks)
                }

                // Load matured khata book plans
                val maturedKhataBooks =
                    appDatabase.customerKhataBookDao().getMaturedKhataBooks(userId, storeId)
                mainScope {
                    maturedKhataBookPlans.clear()
                    maturedKhataBookPlans.addAll(maturedKhataBooks)
                }

                // Calculate pending amount using new transaction system
                val pendingAmount = activeKhataBooks.sumOf { khataBook ->
                    val paidAmount = appDatabase.customerTransactionDao()
                        .getKhataBookTotalPaidAmount(khataBook.khataBookId)
                    val totalAmount = khataBook.totalAmount
                    val remaining = totalAmount - paidAmount
                    log("Khata book ${khataBook.khataBookId}: total=$totalAmount, paid=$paidAmount, remaining=$remaining")
                    remaining
                }
                log("Total pending amount: $pendingAmount")
                mainScope {
                    pendingKhataBookAmount.value = pendingAmount
                }

            } catch (e: Exception) {
                log("Failed to load khata book plans: ${e.message}")
                _snackBarState.value = "Failed to load khata book plans: ${e.message}"
            }
        }
    }


    fun applyKhataBookPlan(
        customerMobile: String,
        plan: KhataBookPlan,
        monthlyAmount: Double = 1000.0
    ) {
        ioLaunch {
            try {
                val userId = admin.first.first()
                val storeId = store.first.first()
                val currentTime = Timestamp(System.currentTimeMillis())

                // Use the provided monthly amount
                val totalMonths = plan.payMonths + plan.benefitMonths

                val khataBook = CustomerKhataBookEntity(
                    khataBookId = generateId(),
                    customerMobile = customerMobile,
                    planName = plan.name,
                    startDate = currentTime,
                    endDate = Timestamp(currentTime.time + (totalMonths * 30L * 24 * 60 * 60 * 1000)),
                    monthlyAmount = monthlyAmount,
                    totalMonths = totalMonths,
                    totalAmount = monthlyAmount * totalMonths,
                    status = "active",
                    notes = "Applied plan: ${plan.name}",
                    userId = userId,
                    storeId = storeId
                )

                val result = appDatabase.customerKhataBookDao().insertKhataBook(khataBook)
                if (result != -1L) {
                    _snackBarState.value = "Khata book plan applied successfully"
                    loadCustomerData()
                    loadKhataBookPlans()
                } else {
                    _snackBarState.value = "Failed to apply khata book plan"
                }
            } catch (e: Exception) {
                log("Failed to apply khata book plan: ${e.message}")
                _snackBarState.value = "Failed to apply khata book plan: ${e.message}"
            }
        }
    }

    private suspend fun createCustomerSummary(
        customer: CustomerEntity,
    ): CustomerSummaryDto {
        // Get outstanding balance using new unified system
        val outstandingBalance =
            appDatabase.customerTransactionDao().getCustomerOutstandingBalance(customer.mobileNo)

        // Get active khata books (now supports multiple)
        val activeKhataBooks =
            appDatabase.customerKhataBookDao().getActiveKhataBooks(customer.mobileNo)

        // Get last order date and total orders
        val orders = appDatabase.orderDao().getAllOrdersDesc().first()
        val customerOrders = orders.filter { it.customerMobile == customer.mobileNo }
        val lastOrderDate = customerOrders.maxOfOrNull { it.orderDate }
        val totalOrders = customerOrders.size

        // Calculate khata book totals
        val totalKhataAmount = activeKhataBooks.sumOf { it.totalAmount }
        val totalKhataPaidAmount = activeKhataBooks.sumOf { khataBook ->
            appDatabase.customerTransactionDao().getKhataBookTotalPaidAmount(khataBook.khataBookId)
        }
        val totalKhataRemainingAmount = totalKhataAmount - totalKhataPaidAmount

        // Check if customer has any completed plans
        val completedKhataBooks =
            appDatabase.customerKhataBookDao().getCompletedKhataBooks(customer.mobileNo)

        return CustomerSummaryDto(
            mobileNo = customer.mobileNo,
            name = customer.name,
            address = customer.address,
            gstin_pan = customer.gstin_pan,
            addDate = customer.addDate,
            totalItemBought = customer.totalItemBought,
            totalAmount = customer.totalAmount,
            lastOrderDate = lastOrderDate,
            totalOrders = totalOrders,
            isActive = activeKhataBooks.isNotEmpty(), // Customer is active only if they have active khata books
            outstandingBalance = outstandingBalance,
            activeKhataBookCount = activeKhataBooks.size,
            totalKhataBookAmount = totalKhataAmount,
            totalKhataBookPaidAmount = totalKhataPaidAmount,
            totalKhataBookRemainingAmount = totalKhataRemainingAmount,
            khataBookStatus = when {
                activeKhataBooks.isNotEmpty() -> "active"
                completedKhataBooks.isNotEmpty() -> "completed"
                else -> "none"
            }
        )
    }

    fun loadCustomerDetails(customerMobile: String) {
        viewModelScope.launch {
            try {
                isLoadingCustomerDetails.value = true

                // Load customer
                val customer = appDatabase.customerDao().getCustomerByMobile(customerMobile)
                selectedCustomer.value = customer

                // Load all transactions using new unified system
                val transactions =
                    appDatabase.customerTransactionDao().getCustomerTransactions(customerMobile)
                        .first()
                selectedCustomerTransactions.clear()
                selectedCustomerTransactions.addAll(transactions)

                // Debug logging for transactions
                log("DEBUG: Loaded ${transactions.size} transactions for customer $customerMobile")
                transactions.forEach { transaction ->
                    log("DEBUG: Transaction: ${transaction.transactionId} - ${transaction.transactionType} - ${transaction.category} - ${transaction.amount}")
                }

                // Force recomposition
                transactionsUpdated.value++

                // Load active khata books (now supports multiple)
                val khataBooks =
                    appDatabase.customerKhataBookDao().getActiveKhataBooks(customerMobile)
                selectedCustomerKhataBooks.clear()
                selectedCustomerKhataBooks.addAll(khataBooks)

                // Load completed khata books
                val completedKhataBooks =
                    appDatabase.customerKhataBookDao().getCompletedKhataBooks(customerMobile)
                selectedCustomerCompletedKhataBooks.clear()
                selectedCustomerCompletedKhataBooks.addAll(completedKhataBooks)

                // Load customer orders
                loadCustomerOrders(customerMobile)

                // Load transaction history
                loadTransactionHistory(customerMobile)
                refreshCustomerEditPermissions(customerMobile)

            } catch (e: Exception) {
                log("Failed to load customer details: ${e.message}")
                _snackBarState.value = "Failed to load customer details: ${e.message}"
            } finally {
                isLoadingCustomerDetails.value = false
            }
        }
    }

    fun refreshCustomerEditPermissions(customerMobile: String) {
        viewModelScope.launch {
            try {
                val outstandingBalance =
                    appDatabase.customerTransactionDao().getCustomerOutstandingBalance(customerMobile)
                val orderCount = appDatabase.orderDao().getOrderCountForCustomer(customerMobile)
                val khataBooks =
                    appDatabase.customerKhataBookDao().getCustomerKhataBooks(customerMobile).first()
                val khataBookCount = khataBooks.size
                val canDelete = outstandingBalance <= 0.0 && orderCount == 0 && khataBookCount == 0
                customerEditPermissions.value = CustomerEditPermissions(
                    canDelete = canDelete,
                    canEditMobile = canDelete,
                    outstandingBalance = outstandingBalance,
                    orderCount = orderCount,
                    khataBookCount = khataBookCount
                )
            } catch (e: Exception) {
                log("Failed to refresh customer edit permissions: ${e.message}")
                customerEditPermissions.value = null
            }
        }
    }

    fun deleteCustomer(customerMobile: String) {
        ioLaunch {
            try {
                val permissions = customerEditPermissions.value
                if (permissions?.canDelete != true) {
                    _snackBarState.value = "Customer cannot be deleted while orders, khata books, or outstanding balance exist"
                    return@ioLaunch
                }

                val customer = appDatabase.customerDao().getCustomerByMobile(customerMobile)
                if (customer == null) {
                    _snackBarState.value = "Customer not found"
                    return@ioLaunch
                }

                appDatabase.customerDao().deleteCustomer(customer)
                _snackBarState.value = "Customer deleted successfully"
                customerEditPermissions.value = null
                clearSelectedCustomerData()
                loadCustomerData()
            } catch (e: Exception) {
                log("Failed to delete customer: ${e.message}")
                _snackBarState.value = "Failed to delete customer: ${e.message}"
            }
        }
    }

    fun updateCustomerProfile(
        customerMobile: String,
        newName: String,
        newAddress: String,
        requestedMobile: String,
        gstinPan: String
    ) {
        ioLaunch {
            try {
                val sanitizedName = InputValidator.sanitizeText(newName)
                if (sanitizedName.isBlank() || sanitizedName.length < 2) {
                    _snackBarState.value = "Customer name must be at least 2 characters"
                    return@ioLaunch
                }

                val sanitizedAddress = InputValidator.sanitizeText(newAddress)
                if (sanitizedAddress.isBlank() || sanitizedAddress.length < 6) {
                    _snackBarState.value = "Address must be at least 6 characters"
                    return@ioLaunch
                }
                val sanitizedGstinPan = gstinPan.trim().uppercase()
                if (sanitizedGstinPan.isNotBlank() &&
                    !InputValidator.isValidGSTIN(sanitizedGstinPan) &&
                    !InputValidator.isValidPAN(sanitizedGstinPan)
                ) {
                    _snackBarState.value = "Enter a valid GSTIN or PAN"
                    return@ioLaunch
                }

                val customer = appDatabase.customerDao().getCustomerByMobile(customerMobile)
                if (customer == null) {
                    _snackBarState.value = "Customer not found"
                    return@ioLaunch
                }

                var finalMobile = customer.mobileNo
                val mobileDigits = requestedMobile.filter(Char::isDigit)
                val permissions = customerEditPermissions.value
                if (permissions?.canEditMobile == true && mobileDigits.length == 10 && mobileDigits.matches(Regex("^[6-9][0-9]{9}$"))) {
                    finalMobile = mobileDigits
                }

                val finalGstinPan = sanitizedGstinPan.takeIf { it.isNotBlank() }
                val updatedCustomer = customer.copy(
                    name = sanitizedName,
                    address = sanitizedAddress.takeIf { it.isNotBlank() },
                    mobileNo = finalMobile,
                    gstin_pan = finalGstinPan,
                    lastModifiedDate = Timestamp(System.currentTimeMillis())
                )

                val result = appDatabase.customerDao().updateCustomer(updatedCustomer)
                if (result > 0) {
                    _snackBarState.value = "Customer details updated"
                    loadCustomerDetails(finalMobile)
                    loadCustomerData()
                    refreshCustomerEditPermissions(finalMobile)
                } else {
                    _snackBarState.value = "Failed to update customer"
                }
            } catch (e: Exception) {
                log("Failed to update customer: ${e.message}")
                _snackBarState.value = "Failed to update customer: ${e.message}"
            }
        }
    }

    private fun loadCustomerOrders(customerMobile: String) {
        viewModelScope.launch {
            try {
                val orders = appDatabase.orderDao().getAllOrdersDesc().first()
                val customerOrdersList = orders.filter { it.customerMobile == customerMobile }
                    .sortedByDescending { it.orderDate }

                customerOrders.value = customerOrdersList
            } catch (e: Exception) {
                log("Failed to load customer orders: ${e.message}")
            }
        }
    }

    private fun loadTransactionHistory(customerMobile: String) {
        viewModelScope.launch {
            try {
                val transactions =
                    appDatabase.customerTransactionDao().getCustomerTransactions(customerMobile)
                        .first()

                val transactionItems = transactions.map { transaction ->
                    TransactionItem.fromTransaction(transaction)
                }.sortedByDescending { it.date }

                transactionHistory.value = transactionItems
            } catch (e: Exception) {
                log("Failed to load transaction history: ${e.message}")
            }
        }
    }

    fun addCustomer() {
        ioLaunch {
            try {
                val userId = admin.first.first()
                val storeId = store.first.first()
                val currentTime = Timestamp(System.currentTimeMillis())

                val name = InputValidator.sanitizeText(customerName.text)
                val mobile = customerMobile.text.filter(Char::isDigit)
                val address = InputValidator.sanitizeText(customerAddress.text)
                val gstinPan = customerGstin.text.trim().uppercase()
                val notes = InputValidator.sanitizeText(customerNotes.text)

                if (name.isBlank() || name.length < 2) {
                    _snackBarState.value = "Customer name is required"
                    return@ioLaunch
                }
                if (mobile.isBlank() || !mobile.matches(Regex("^[6-9][0-9]{9}$"))) {
                    _snackBarState.value = "Enter a valid 10-digit mobile number"
                    return@ioLaunch
                }
                if (address.isBlank() || address.length < 10) {
                    _snackBarState.value = "Please enter a complete address (min 10 characters)"
                    return@ioLaunch
                }
                if (gstinPan.isNotBlank() && !InputValidator.isValidGSTIN(gstinPan) && !InputValidator.isValidPAN(gstinPan)) {
                    _snackBarState.value = "Enter a valid GSTIN or PAN"
                    return@ioLaunch
                }

                val existingCustomer = appDatabase.customerDao().getCustomerByMobile(mobile)
                if (existingCustomer != null) {
                    _snackBarState.value = "Customer with this mobile already exists."
                    return@ioLaunch
                }

                val customer = CustomerEntity(
                    mobileNo = mobile,
                    name = name,
                    address = address.takeIf { it.isNotEmpty() },
                    gstin_pan = gstinPan.takeIf { it.isNotEmpty() },
                    addDate = currentTime,
                    lastModifiedDate = currentTime,
                    notes = notes.takeIf { it.isNotEmpty() },
                    userId = userId,
                    storeId = storeId
                )

                val result = appDatabase.customerDao().insertCustomer(customer)
                if (result != -1L) {
                    _snackBarState.value = "Customer added successfully"
                    clearCustomerForm()
                    loadCustomerData()
                } else {
                    _snackBarState.value = "Failed to add customer"
                }
            } catch (e: Exception) {
                log("Failed to add customer: ${e.message}")
                _snackBarState.value = "Failed to add customer: ${e.message}"
            }
        }
    }

    fun addOutstandingTransaction(customerMobile: String) {
        if (outstandingAmount.text.isEmpty()) {
            _snackBarState.value = "Amount is required"
            return
        }

        viewModelScope.launch {
            try {
                isLoadingPayment.value = true
                val userId = admin.first.first()
                val storeId = store.first.first()

                val amount = outstandingAmount.text.toDoubleOrNull() ?: 0.0
                val transactionType = outstandingType.text
                val description = outstandingDescription.text.trim().takeIf { it.isNotEmpty() }
                val notes = outstandingNotes.text.trim().takeIf { it.isNotEmpty() }

                val transaction = when (transactionType) {
                    "payment" -> TransactionUtils.createOutstandingPayment(
                        customerMobile = customerMobile,
                        amount = amount,
                        notes = notes,
                        userId = userId,
                        storeId = storeId
                    )

                    else -> TransactionUtils.createOutstandingDebt(
                        customerMobile = customerMobile,
                        amount = amount,
                        description = description,
                        notes = notes,
                        userId = userId,
                        storeId = storeId
                    )
                }

                val result = appDatabase.customerTransactionDao().insertTransaction(transaction)
                if (result != -1L) {
                    _snackBarState.value = "Transaction added successfully"
                    clearOutstandingForm()
                    // Reload customer details and data to update outstanding balance
                    loadCustomerDetails(customerMobile)
                    loadCustomerData()
                } else {
                    _snackBarState.value = "Failed to add transaction"
                }
            } catch (e: Exception) {
                log("Failed to add outstanding transaction: ${e.message}")
                _snackBarState.value = "Failed to add transaction: ${e.message}"
            } finally {
                isLoadingPayment.value = false
            }
        }
    }

    fun createKhataBook(customerMobile: String) {
        if (khataBookMonthlyAmount.text.isEmpty() || khataBookTotalMonths.text.isEmpty()) {
            _snackBarState.value = "Monthly amount and total months are required"
            return
        }

        viewModelScope.launch {
            try {
                isLoadingKhataBook.value = true
                val userId = admin.first.first()
                val storeId = store.first.first()
                val currentTime = Timestamp(System.currentTimeMillis())

                val monthlyAmount = khataBookMonthlyAmount.text.toDoubleOrNull() ?: 0.0
                val totalMonths = khataBookTotalMonths.text.toIntOrNull() ?: 12
                val totalAmount = monthlyAmount * totalMonths
                val planName =
                    khataBookPlanName.text.trim().takeIf { it.isNotEmpty() } ?: "Standard Plan"

                val khataBookId = generateId()
                val khataBook = CustomerKhataBookEntity(
                    khataBookId,
                    customerMobile = customerMobile,
                    planName = planName,
                    startDate = currentTime,
                    endDate = Timestamp(currentTime.time + (totalMonths * 30L * 24 * 60 * 60 * 1000)),
                    monthlyAmount = monthlyAmount,
                    totalMonths = totalMonths,
                    totalAmount = totalAmount,
                    status = "active",
                    notes = khataBookNotes.text.trim().takeIf { it.isNotEmpty() },
                    userId = userId,
                    storeId = storeId
                )

                val result = appDatabase.customerKhataBookDao().insertKhataBook(khataBook)
                if (result != -1L) {
                    // Create initial khata debit transaction
                    val khataDebit = TransactionUtils.createKhataDebit(
                        customerMobile = customerMobile,
                        khataBookId = khataBookId,
                        amount = totalAmount,
                        notes = "Initial khata book debit for $planName",
                        userId = userId,
                        storeId = storeId
                    )
                    appDatabase.customerTransactionDao().insertTransaction(khataDebit)

                    _snackBarState.value = "Khata book created successfully"
                    clearKhataBookForm()
                    loadCustomerDetails(customerMobile)
                    loadCustomerData()
                } else {
                    _snackBarState.value = "Failed to create khata book"
                }
            } catch (e: Exception) {
                log("Failed to create khata book: ${e.message}")
                _snackBarState.value = "Failed to create khata book: ${e.message}"
            } finally {
                isLoadingKhataBook.value = false
            }
        }
    }

    fun addKhataPayment(customerMobile: String) {
        if (khataPaymentAmount.text.isEmpty() || khataPaymentMonth.text.isEmpty()) {
            _snackBarState.value = "Amount and month are required"
            return
        }

        viewModelScope.launch {
            try {
                isLoadingPayment.value = true
                val userId = admin.first.first()
                val storeId = store.first.first()

                val amount = khataPaymentAmount.text.toDoubleOrNull() ?: 0.0
                val monthNumber = khataPaymentMonth.text.toIntOrNull() ?: 1
                khataPaymentType.text
                val notes = khataPaymentNotes.text.trim().takeIf { it.isNotEmpty() }

                // Get active khata books for this customer
                val khataBooks =
                    appDatabase.customerKhataBookDao().getActiveKhataBooks(customerMobile)
                if (khataBooks.isEmpty()) {
                    _snackBarState.value = "No active khata book found for this customer"
                    return@launch
                }

                // For now, use the first active khata book (you can add UI to select specific plan)
                val khataBook = khataBooks.first()

                val payment = TransactionUtils.createKhataPayment(
                    customerMobile = customerMobile,
                    khataBookId = khataBook.khataBookId,
                    monthNumber = monthNumber,
                    amount = amount,
                    notes = notes,
                    userId = userId,
                    storeId = storeId
                )

                val result = appDatabase.customerTransactionDao().insertTransaction(payment)
                if (result != -1L) {
                    _snackBarState.value = "Khata payment added successfully"
                    clearKhataPaymentForm()
                    // Reload customer details and data to update outstanding balance
                    loadCustomerDetails(customerMobile)
                    loadCustomerData()
                } else {
                    _snackBarState.value = "Failed to add khata payment"
                }
            } catch (e: Exception) {
                log("Failed to add khata payment: ${e.message}")
                _snackBarState.value = "Failed to add khata payment: ${e.message}"
            } finally {
                isLoadingPayment.value = false
            }
        }
    }

    fun addRegularPayment(customerMobile: String) {
        if (regularPaymentAmount.text.isEmpty()) {
            _snackBarState.value = "Payment amount is required"
            return
        }

        viewModelScope.launch {
            try {
                isLoadingPayment.value = true
                val userId = admin.first.first()
                val storeId = store.first.first()

                val amount = regularPaymentAmount.text.toDoubleOrNull() ?: 0.0
                regularPaymentType.text
                val paymentMethod = regularPaymentMethod.text
                val referenceNumber = regularPaymentReference.text.trim().takeIf { it.isNotEmpty() }
                val notes = regularPaymentNotes.text.trim().takeIf { it.isNotEmpty() }

                val payment = TransactionUtils.createRegularPayment(
                    customerMobile = customerMobile,
                    amount = amount,
                    paymentMethod = paymentMethod,
                    referenceNumber = referenceNumber,
                    notes = notes,
                    userId = userId,
                    storeId = storeId
                )

                val result = appDatabase.customerTransactionDao().insertTransaction(payment)
                if (result != -1L) {
                    _snackBarState.value = "Payment added successfully"
                    clearRegularPaymentForm()
                    // Reload customer details and data to update outstanding balance
                    loadCustomerDetails(customerMobile)
                    loadCustomerData()
                } else {
                    _snackBarState.value = "Failed to add payment"
                }
            } catch (e: Exception) {
                log("Failed to add regular payment: ${e.message}")
                _snackBarState.value = "Failed to add payment: ${e.message}"
            } finally {
                isLoadingPayment.value = false
            }
        }
    }

    fun updateKhataBookStatus(khataBookId: String, status: String) {
        viewModelScope.launch {
            try {
                val khataBook = selectedCustomerKhataBooks.find { it.khataBookId == khataBookId }
                khataBook?.let { kb ->
                    val updatedKhataBook = kb.copy(status = status)
                    val result =
                        appDatabase.customerKhataBookDao().updateKhataBook(updatedKhataBook)
                    if (result > 0) {
                        _snackBarState.value = "Khata book status updated successfully"
                        loadCustomerDetails(kb.customerMobile)
                        loadCustomerData()
                    } else {
                        _snackBarState.value = "Failed to update khata book status"
                    }
                }
            } catch (e: Exception) {
                log("Failed to update khata book status: ${e.message}")
                _snackBarState.value = "Failed to update khata book status: ${e.message}"
            }
        }
    }

    private fun clearCustomerForm() {
        customerName.text = ""
        customerMobile.text = ""
        customerAddress.text = ""
        customerGstin.text = ""
        customerNotes.text = ""
    }

    private fun clearOutstandingForm() {
        outstandingAmount.text = ""
        outstandingType.text = "debt"
        outstandingDescription.text = ""
        outstandingNotes.text = ""
    }

    private fun clearKhataBookForm() {
        khataBookMonthlyAmount.text = ""
        khataBookTotalMonths.text = "12"
        khataBookPlanName.text = "Standard Plan"
        khataBookStartDate.text = ""
        khataBookNotes.text = ""
    }

    private fun clearKhataPaymentForm() {
        khataPaymentMonth.text = ""
        khataPaymentAmount.text = ""
        khataPaymentType.text = "full"
        khataPaymentNotes.text = ""
    }

    private fun clearRegularPaymentForm() {
        regularPaymentAmount.text = ""
        regularPaymentType.text = "regular"
        regularPaymentMethod.text = "cash"
        regularPaymentReference.text = ""
        regularPaymentNotes.text = ""
    }

    fun clearSelectedCustomerData() {
        selectedCustomer.value = null
        selectedCustomerTransactions.clear()
        selectedCustomerKhataBooks.clear()
        selectedCustomerCompletedKhataBooks.clear()
        customerOrders.value = emptyList()
        transactionHistory.value = emptyList()
    }


    fun searchCustomers(query: String) {
        lastSearchQuery = query
        applyFilterAndSearch()
    }

    fun filterCustomers(filterType: String) {
        this.filterType.value = filterType
        applyFilterAndSearch()
    }

    // Clear monthly payment for a specific month
    fun clearMonthlyPayment(khataBookId: String, monthNumber: Int) {
        ioLaunch {
            try {

                // Find and delete the payment transaction for this month
                val paymentTransactions =
                    appDatabase.customerTransactionDao().getKhataBookPayments(khataBookId).first()
                val monthPayment = paymentTransactions.find { it.monthNumber == monthNumber }

                if (monthPayment != null) {
                    appDatabase.customerTransactionDao().deleteTransaction(monthPayment)

                    mainScope {
                        _snackBarState.value = "Payment cleared for month $monthNumber"
                    }

                    // Reload customer details
                    loadCustomerDetails(selectedCustomer.value?.mobileNo ?: "")
                } else {
                    mainScope {
                        _snackBarState.value = "No payment found for month $monthNumber"
                    }
                }

            } catch (e: Exception) {
                log("Failed to clear monthly payment: ${e.message}")
                mainScope {
                    _snackBarState.value = "Failed to clear monthly payment: ${e.message}"
                }
            }
        }
    }

    // Complete a khata book plan
    fun completeKhataBookPlan(khataBookId: String) {
        ioLaunch {
            try {
                // Get the khata book
                val khataBook = appDatabase.customerKhataBookDao().getKhataBookById(khataBookId)
                if (khataBook == null) {
                    mainScope {
                        _snackBarState.value = "Khata book plan not found"
                    }
                    return@ioLaunch
                }

                // Check if all pay months are completed
                val paidMonths = appDatabase.customerKhataBookDao().getPaidMonths(khataBookId)
                val paidMonthNumbers = paidMonths.mapNotNull { it.monthNumber }.toSet()

                // Get plan info to determine pay months
                val planInfo = findPlanByName(khataBook.planName)
                val requiredPayMonths = planInfo?.payMonths ?: khataBook.totalMonths

                val completedPayMonths = paidMonthNumbers.count { it <= requiredPayMonths }

                if (completedPayMonths < requiredPayMonths) {
                    mainScope {
                        _snackBarState.value =
                            "Cannot complete plan. Only $completedPayMonths out of $requiredPayMonths pay months completed"
                    }
                    return@ioLaunch
                }

                // Update khata book status to completed
                val updatedKhataBook = khataBook.copy(status = "completed")
                val result = appDatabase.customerKhataBookDao().updateKhataBook(updatedKhataBook)

                if (result > 0) {
                    mainScope {
                        _snackBarState.value =
                            "Khata book plan completed successfully! Customer is now eligible for reward months."
                    }

                    // Reload data
                    loadCustomerData()
                    loadKhataBookPlans()
                    loadCustomerDetails(khataBook.customerMobile)
                } else {
                    mainScope {
                        _snackBarState.value = "Failed to complete khata book plan"
                    }
                }

            } catch (e: Exception) {
                log("Failed to complete khata book plan: ${e.message}")
                mainScope {
                    _snackBarState.value = "Failed to complete khata book plan: ${e.message}"
                }
            }
        }
    }

    // Delete a transaction
    fun deleteTransaction(transaction: CustomerTransactionEntity) {
        viewModelScope.launch {
            try {
                appDatabase.customerTransactionDao().deleteTransaction(transaction)
                _snackBarState.value = "Transaction deleted successfully"

                // Reload customer data to update the UI
                loadCustomerDetails(transaction.customerMobile)
                loadCustomerData()

                // Force recomposition
                transactionsUpdated.value++
            } catch (e: Exception) {
                log("Failed to delete transaction: ${e.message}")
                _snackBarState.value = "Failed to delete transaction: ${e.message}"
            }
        }
    }

    fun deleteTransactionHistoryItem(transactionItem: TransactionItem) {
        viewModelScope.launch {
            try {
                // Find the corresponding CustomerTransactionEntity from current customer's transactions
                val transactions = selectedCustomerTransactions.toList()

                val transactionToDelete = transactions.find { transaction ->
                    formatDate(transaction.transactionDate) == formatDate(transactionItem.date) && formatCurrency(
                        transaction.amount
                    ) == formatCurrency(transactionItem.amount) && transaction.transactionType == transactionItem.transactionType
                }

                if (transactionToDelete != null) {
                    appDatabase.customerTransactionDao().deleteTransaction(transactionToDelete)
                    _snackBarState.value = "Transaction deleted successfully"

                    // Reload customer data to update the UI
                    loadCustomerDetails(transactionToDelete.customerMobile)
                    loadCustomerData()

                    // Force recomposition
                    transactionsUpdated.value++
                } else {
                    _snackBarState.value = "Transaction not found"
                }
            } catch (e: Exception) {
                log("Failed to delete transaction: ${e.message}")
                _snackBarState.value = "Failed to delete transaction: ${e.message}"
            }
        }
    }

    data class CustomerEditPermissions(
        val canDelete: Boolean,
        val canEditMobile: Boolean,
        val outstandingBalance: Double,
        val orderCount: Int,
        val khataBookCount: Int
    )
}
