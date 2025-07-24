package com.velox.jewelvault.ui.screen.customers

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velox.jewelvault.data.roomdb.AppDatabase
import com.velox.jewelvault.data.roomdb.dto.CustomerSummaryDto
import com.velox.jewelvault.data.roomdb.entity.customer.CustomerEntity
import com.velox.jewelvault.data.roomdb.entity.customer.CustomerKhataBookEntity
import com.velox.jewelvault.data.roomdb.entity.customer.CustomerTransactionEntity
import com.velox.jewelvault.ui.components.InputFieldState
import com.velox.jewelvault.utils.DataStoreManager
import com.velox.jewelvault.utils.TransactionUtils
import com.velox.jewelvault.utils.ioLaunch
import com.velox.jewelvault.utils.ioScope
import com.velox.jewelvault.utils.log
import com.velox.jewelvault.utils.mainScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.sql.Timestamp
import javax.inject.Inject

// Import the data classes from CustomerModels

// Data classes for Khata Book functionality
data class KhataBookProgress(
    val paidMonths: Int,
    val remainingMonths: Int,
    val paidAmount: Double,
    val remainingAmount: Double
)

data class KhataBookSummary(
    val totalAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val remainingAmount: Double = 0.0,
    val paidMonths: Int = 0,
    val remainingMonths: Int = 0,
    val progressPercentage: Double = 0.0,
    val status: String = "none"
)

@HiltViewModel
class CustomerViewModel @Inject constructor(
    private val appDatabase: AppDatabase,
    private val _dataStoreManager: DataStoreManager,
    private val _snackBarState: MutableState<String>
) : ViewModel() {

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
                it.name.lowercase().contains(query) ||
                it.mobileNo.contains(query) ||
                (it.address?.lowercase()?.contains(query) == true)
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

    // Selected customer for detail view
    val selectedCustomer = mutableStateOf<CustomerEntity?>(null)
    val selectedCustomerTransactions = mutableStateListOf<CustomerTransactionEntity>()
    val selectedCustomerKhataBooks = mutableStateListOf<CustomerKhataBookEntity>()

    // Khata book plans data
    val activeKhataBookCustomers = mutableStateListOf<CustomerKhataBookEntity>()
    val maturedKhataBookPlans = mutableStateListOf<CustomerKhataBookEntity>()
    val pendingKhataBookAmount = mutableStateOf(0.0)
    
    // Customer orders and transactions
    val customerOrders = mutableStateOf<List<com.velox.jewelvault.data.roomdb.entity.order.OrderEntity>>(emptyList())
    val transactionHistory = mutableStateOf<List<TransactionItem>>(emptyList())
    
    // Loading states
    val isLoadingCustomerDetails = mutableStateOf(false)
    val isLoadingKhataBook = mutableStateOf(false)
    val isLoadingPayment = mutableStateOf(false)

    val planList = mutableStateListOf<KhataBookPlan>()

    fun addPlan(name: String, payMonths: Int, benefitMonths: Int, description: String) {
        val benefitPercentage = if (payMonths + benefitMonths > 0) benefitMonths * 100.0 / (payMonths + benefitMonths) else 0.0
        planList.add(KhataBookPlan(name, payMonths, benefitMonths, description, benefitPercentage))
    }
    fun editPlan(plan: KhataBookPlan, name: String, payMonths: Int, benefitMonths: Int, description: String) {
        val idx = planList.indexOf(plan)
        val benefitPercentage = if (payMonths + benefitMonths > 0) benefitMonths * 100.0 / (payMonths + benefitMonths) else 0.0
        if (idx >= 0) planList[idx] = plan.copy(name = name, payMonths = payMonths, benefitMonths = benefitMonths, description = description, benefitPercentage = benefitPercentage)
    }
    fun deletePlan(plan: KhataBookPlan) {
        planList.remove(plan)
    }

//    init {
//        loadCustomerData()
//    }

    fun loadCustomerData() {
        ioLaunch {
            try {
                val userId = dataStoreManager.userId.first()
                val storeId = dataStoreManager.storeId.first()

                // Load customers
                appDatabase.customerDao().getAllCustomers()
                    .collectLatest { customers ->
                        ioScope {
                            // Convert to CustomerSummaryDto with outstanding and khata book data
                            val customerSummaries = customers.map { customer ->
                                createCustomerSummary(customer)
                            }
                            allCustomers.clear()
                            allCustomers.addAll(customerSummaries)
                            // Default: show all
                            applyFilterAndSearch()
                            totalCustomers.value = customers.size
                        }
                    }

                // Load outstanding balance statistics using new unified system
                val totalOutstanding = appDatabase.customerTransactionDao().getTotalOutstandingAmount(userId, storeId)
                ioScope {
                    totalOutstandingAmount.value = totalOutstanding
                }

                // Load khata book statistics
                val khataSummaries = appDatabase.customerKhataBookDao().getKhataBookSummaries(userId, storeId)
                val totalKhataRemaining = khataSummaries.sumOf { it.remainingAmount }
                mainScope {
                    totalKhataBookAmount.value = totalKhataRemaining
                }

            } catch (e: Exception) {
                log("Failed to load customer data: ${e.message}")
                _snackBarState.value = "Failed to load customer data: ${e.message}"
            }
        }
    }

    fun loadKhataBookPlans() {
        ioLaunch {
            try {
                val userId = dataStoreManager.userId.first()
                val storeId = dataStoreManager.storeId.first()

                // Load active khata book customers
                val activeKhataBooks = appDatabase.customerKhataBookDao().getActiveKhataBooks(userId, storeId)
                mainScope {
                    activeKhataBookCustomers.clear()
                    activeKhataBookCustomers.addAll(activeKhataBooks)
                }

                // Load matured khata book plans
                val maturedKhataBooks = appDatabase.customerKhataBookDao().getMaturedKhataBooks(userId, storeId)
                mainScope {
                    maturedKhataBookPlans.clear()
                    maturedKhataBookPlans.addAll(maturedKhataBooks)
                }

                // Calculate pending amount using new transaction system
                val pendingAmount = activeKhataBooks.sumOf { khataBook ->
                    val paidAmount = appDatabase.customerTransactionDao().getKhataBookTotalPaidAmount(khataBook.khataBookId)
                    (khataBook.totalMonths * khataBook.monthlyAmount) - paidAmount
                }
                mainScope {
                    pendingKhataBookAmount.value = pendingAmount
                }

            } catch (e: Exception) {
                log("Failed to load khata book plans: ${e.message}")
                _snackBarState.value = "Failed to load khata book plans: ${e.message}"
            }
        }
    }

    fun addKhataBookPlan(name: String, payMonths: Int, benefitMonths: Int, description: String) {
        // This would typically save to a separate plans table
        // For now, we'll just show a success message
        _snackBarState.value = "Khata book plan '$name' added successfully"
    }

    fun applyKhataBookPlan(customerMobile: String, plan: KhataBookPlan) {
        ioLaunch {
            try {
                val userId = dataStoreManager.userId.first()
                val storeId = dataStoreManager.storeId.first()
                val currentTime = Timestamp(System.currentTimeMillis())

                val monthlyAmount = 0.0 // This would be calculated based on the plan
                val totalMonths = plan.payMonths + plan.benefitMonths

                val khataBook = CustomerKhataBookEntity(
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
        val outstandingBalance = appDatabase.customerTransactionDao().getCustomerOutstandingBalance(customer.mobileNo)
        
        // Get active khata books (now supports multiple)
        val activeKhataBooks = appDatabase.customerKhataBookDao().getActiveKhataBooks(customer.mobileNo)
        
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
            isActive = customer.isActive,
            outstandingBalance = outstandingBalance,
            activeKhataBookCount = activeKhataBooks.size,
            totalKhataBookAmount = totalKhataAmount,
            totalKhataBookPaidAmount = totalKhataPaidAmount,
            totalKhataBookRemainingAmount = totalKhataRemainingAmount,
            khataBookStatus = if (activeKhataBooks.isNotEmpty()) "active" else "none"
        )
    }

    fun loadCustomerDetails(customerMobile: String) {
        viewModelScope.launch {
            try {
                isLoadingCustomerDetails.value = true
                val userId = dataStoreManager.userId.first()
                val storeId = dataStoreManager.storeId.first()

                // Load customer
                val customer = appDatabase.customerDao().getCustomerByMobile(customerMobile)
                selectedCustomer.value = customer

                // Load all transactions using new unified system
                val transactions = appDatabase.customerTransactionDao()
                    .getCustomerTransactions(customerMobile).first()
                selectedCustomerTransactions.clear()
                selectedCustomerTransactions.addAll(transactions)

                // Load active khata books (now supports multiple)
                val khataBooks = appDatabase.customerKhataBookDao().getActiveKhataBooks(customerMobile)
                selectedCustomerKhataBooks.clear()
                selectedCustomerKhataBooks.addAll(khataBooks)

                // Load customer orders
                loadCustomerOrders(customerMobile)
                
                // Load transaction history
                loadTransactionHistory(customerMobile)

            } catch (e: Exception) {
                log("Failed to load customer details: ${e.message}")
                _snackBarState.value = "Failed to load customer details: ${e.message}"
            } finally {
                isLoadingCustomerDetails.value = false
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
                val transactions = appDatabase.customerTransactionDao()
                    .getCustomerTransactions(customerMobile).first()
                
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
        if (customerName.text.isEmpty() || customerMobile.text.isEmpty()) {
            _snackBarState.value = "Name and mobile number are required"
            return
        }

        ioLaunch {
            try {
                val userId = dataStoreManager.userId.first()
                val storeId = dataStoreManager.storeId.first()
                val currentTime = Timestamp(System.currentTimeMillis())

                val customer = CustomerEntity(
                    mobileNo = customerMobile.text.trim(),
                    name = customerName.text.trim(),
                    address = customerAddress.text.trim().takeIf { it.isNotEmpty() },
                    gstin_pan = customerGstin.text.trim().takeIf { it.isNotEmpty() },
                    addDate = currentTime,
                    lastModifiedDate = currentTime,
                    notes = customerNotes.text.trim().takeIf { it.isNotEmpty() }
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
                val userId = dataStoreManager.userId.first()
                val storeId = dataStoreManager.storeId.first()

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
                val userId = dataStoreManager.userId.first()
                val storeId = dataStoreManager.storeId.first()
                val currentTime = Timestamp(System.currentTimeMillis())

                val monthlyAmount = khataBookMonthlyAmount.text.toDoubleOrNull() ?: 0.0
                val totalMonths = khataBookTotalMonths.text.toIntOrNull() ?: 12
                val totalAmount = monthlyAmount * totalMonths
                val planName = khataBookPlanName.text.trim().takeIf { it.isNotEmpty() } ?: "Standard Plan"

                val khataBook = CustomerKhataBookEntity(
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
                        khataBookId = result.toInt(),
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
                val userId = dataStoreManager.userId.first()
                val storeId = dataStoreManager.storeId.first()

                val amount = khataPaymentAmount.text.toDoubleOrNull() ?: 0.0
                val monthNumber = khataPaymentMonth.text.toIntOrNull() ?: 1
                val paymentType = khataPaymentType.text
                val notes = khataPaymentNotes.text.trim().takeIf { it.isNotEmpty() }

                // Get active khata books for this customer
                val khataBooks = appDatabase.customerKhataBookDao().getActiveKhataBooks(customerMobile)
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
                val userId = dataStoreManager.userId.first()
                val storeId = dataStoreManager.storeId.first()

                val amount = regularPaymentAmount.text.toDoubleOrNull() ?: 0.0
                val paymentType = regularPaymentType.text
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

    fun updateKhataBookStatus(khataBookId: Int, status: String) {
        viewModelScope.launch {
            try {
                val khataBook = selectedCustomerKhataBooks.find { it.khataBookId == khataBookId }
                khataBook?.let { kb ->
                    val updatedKhataBook = kb.copy(status = status)
                    val result = appDatabase.customerKhataBookDao().updateKhataBook(updatedKhataBook)
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
        customerOrders.value = emptyList()
        transactionHistory.value = emptyList()
    }
    
    // Enhanced Khata Book Functions
    
    fun getKhataBookProgress(khataBookId: Int): KhataBookProgress {
        val khataBook = selectedCustomerKhataBooks.find { it.khataBookId == khataBookId }
        val khataTransactions = selectedCustomerTransactions.filter { 
            it.khataBookId == khataBookId && it.transactionType == "khata_payment" 
        }
        
        if (khataBook == null) return KhataBookProgress(0, 0, 0.0, 0.0)
        
        val paidMonths = khataTransactions.size
        val remainingMonths = khataBook.totalMonths - paidMonths
        val paidAmount = khataTransactions.sumOf { it.amount }
        val remainingAmount = remainingMonths * khataBook.monthlyAmount
        
        return KhataBookProgress(
            paidMonths = paidMonths,
            remainingMonths = remainingMonths,
            paidAmount = paidAmount,
            remainingAmount = remainingAmount
        )
    }
    
    fun isMonthPaid(khataBookId: Int, monthNumber: Int): Boolean {
        return selectedCustomerTransactions.any { 
            it.khataBookId == khataBookId && 
            it.transactionType == "khata_payment" && 
            it.monthNumber == monthNumber 
        }
    }
    
    fun getNextUnpaidMonth(khataBookId: Int): Int {
        val khataBook = selectedCustomerKhataBooks.find { it.khataBookId == khataBookId } ?: return 1
        val paidMonths = selectedCustomerTransactions
            .filter { it.khataBookId == khataBookId && it.transactionType == "khata_payment" }
            .map { it.monthNumber }
            .toSet()
        
        for (month in 1..khataBook.totalMonths) {
            if (!paidMonths.contains(month)) {
                return month
            }
        }
        return khataBook.totalMonths + 1
    }
    
    fun calculateKhataBookSummary(): KhataBookSummary {
        val khataBooks = selectedCustomerKhataBooks.toList()
        
        if (khataBooks.isEmpty()) return KhataBookSummary()
        
        val totalAmount = khataBooks.sumOf { it.totalAmount }
        val paidAmount = selectedCustomerTransactions
            .filter { it.transactionType == "khata_payment" }
            .sumOf { it.amount }
        val remainingAmount = totalAmount - paidAmount
        val paidMonths = selectedCustomerTransactions
            .filter { it.transactionType == "khata_payment" }
            .size
        val totalMonths = khataBooks.sumOf { it.totalMonths }
        val remainingMonths = totalMonths - paidMonths
        val progressPercentage = if (totalAmount > 0) (paidAmount / totalAmount) * 100 else 0.0
        
        return KhataBookSummary(
            totalAmount = totalAmount,
            paidAmount = paidAmount,
            remainingAmount = remainingAmount,
            paidMonths = paidMonths,
            remainingMonths = remainingMonths,
            progressPercentage = progressPercentage,
            status = if (khataBooks.any { it.status == "active" }) "active" else "none"
        )
    }
    
    fun validateKhataPayment(monthNumber: Int, amount: Double): Boolean {
        val khataBooks = selectedCustomerKhataBooks.toList()
        if (khataBooks.isEmpty()) return false
        
        // For now, validate against the first khata book (you can add UI to select specific plan)
        val khataBook = khataBooks.first()
        
        // Check if month is already paid
        if (isMonthPaid(khataBook.khataBookId, monthNumber)) {
            _snackBarState.value = "Month $monthNumber is already paid"
            return false
        }
        
        // Check if month number is valid
        if (monthNumber < 1 || monthNumber > khataBook.totalMonths) {
            _snackBarState.value = "Invalid month number. Must be between 1 and ${khataBook.totalMonths}"
            return false
        }
        
        // Check if amount is valid
        if (amount <= 0) {
            _snackBarState.value = "Amount must be greater than 0"
            return false
        }
        
        // Check if amount exceeds monthly amount (for full payments)
        if (khataPaymentType.text == "full" && amount > khataBook.monthlyAmount) {
            _snackBarState.value = "Amount cannot exceed monthly amount of ${khataBook.monthlyAmount}"
            return false
        }
        
        return true
    }
    
    fun getKhataBookPaymentHistory(khataBookId: Int): List<CustomerTransactionEntity> {
        return selectedCustomerTransactions
            .filter { it.khataBookId == khataBookId && it.transactionType == "khata_payment" }
            .sortedBy { it.monthNumber }
    }
    
    fun getKhataBookDueDate(khataBookId: Int, monthNumber: Int): java.sql.Timestamp {
        val khataBook = selectedCustomerKhataBooks.find { it.khataBookId == khataBookId } 
            ?: return java.sql.Timestamp(System.currentTimeMillis())
        val monthInMillis = (monthNumber - 1) * 30L * 24 * 60 * 60 * 1000
        return java.sql.Timestamp(khataBook.startDate.time + monthInMillis)
    }
    
    fun isKhataBookOverdue(khataBookId: Int): Boolean {
        val khataBook = selectedCustomerKhataBooks.find { it.khataBookId == khataBookId } ?: return false
        val currentTime = System.currentTimeMillis()
        val nextUnpaidMonth = getNextUnpaidMonth(khataBookId)
        val dueDate = getKhataBookDueDate(khataBookId, nextUnpaidMonth)
        
        return currentTime > dueDate.time && khataBook.status == "active"
    }
    
    fun getOverdueMonths(khataBookId: Int): List<Int> {
        val khataBook = selectedCustomerKhataBooks.find { it.khataBookId == khataBookId } ?: return emptyList()
        val currentTime = System.currentTimeMillis()
        val overdueMonths = mutableListOf<Int>()
        
        for (month in 1..khataBook.totalMonths) {
            if (!isMonthPaid(khataBookId, month)) {
                val dueDate = getKhataBookDueDate(khataBookId, month)
                if (currentTime > dueDate.time) {
                    overdueMonths.add(month)
                }
            }
        }
        
        return overdueMonths
    }
    
    fun calculateLateFees(khataBookId: Int, overdueMonths: List<Int>): Double {
        val khataBook = selectedCustomerKhataBooks.find { it.khataBookId == khataBookId } ?: return 0.0
        val lateFeeRate = 0.05 // 5% late fee per month
        var totalLateFees = 0.0
        
        for (month in overdueMonths) {
            val dueDate = getKhataBookDueDate(khataBookId, month)
            val monthsOverdue = ((System.currentTimeMillis() - dueDate.time) / (30L * 24 * 60 * 60 * 1000)).toInt()
            val lateFee = khataBook.monthlyAmount * lateFeeRate * monthsOverdue
            totalLateFees += lateFee
        }
        
        return totalLateFees
    }

    fun searchCustomers(query: String) {
        lastSearchQuery = query
        applyFilterAndSearch()
    }
    fun filterCustomers(filterType: String) {
        this.filterType.value = filterType
        applyFilterAndSearch()
    }
} 