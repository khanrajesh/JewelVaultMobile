package com.velox.jewelvault.ui.screen.preorder

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import com.velox.jewelvault.data.DataStoreManager
import com.velox.jewelvault.data.roomdb.AppDatabase
import com.velox.jewelvault.data.roomdb.dto.PreOrderSummary
import com.velox.jewelvault.data.roomdb.entity.category.CategoryEntity
import com.velox.jewelvault.data.roomdb.entity.customer.CustomerEntity
import com.velox.jewelvault.data.roomdb.entity.customer.CustomerTransactionEntity
import com.velox.jewelvault.data.roomdb.entity.preorder.PreOrderEntity
import com.velox.jewelvault.data.roomdb.entity.preorder.PreOrderItemEntity
import com.velox.jewelvault.ui.components.InputFieldState
import com.velox.jewelvault.utils.generateId
import com.velox.jewelvault.utils.generatePreOrderReceiptPdf
import com.velox.jewelvault.utils.ioLaunch
import com.velox.jewelvault.utils.mainScope
import com.velox.jewelvault.utils.withMain
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import java.sql.Timestamp
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class PreOrderViewModel @Inject constructor(
    private val appDatabase: AppDatabase,
    private val dataStoreManager: DataStoreManager,
    @Named("snackMessage") private val _snackBarState: MutableState<String>,
    @Named("currentScreenHeading") private val _currentScreenHeadingState: MutableState<String>,
) : ViewModel() {

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

    // List state
    val preOrders: SnapshotStateList<PreOrderSummary> = SnapshotStateList()

    // Form state
    val customerMobile = InputFieldState()
    val customerName = InputFieldState()
    val customerAddress = InputFieldState()

    val categoryName = InputFieldState()
    val quantity = InputFieldState(initValue = "1")
    val estimatedWeight = InputFieldState()
    val estimatedPrice = InputFieldState()
    val addDesKey = InputFieldState()
    val addDesValue = InputFieldState()
    val itemNote = InputFieldState()
    val preOrderNote = InputFieldState()

    val deliveryDateText = InputFieldState()

    val advanceAmount = InputFieldState()
    val paymentMethod = InputFieldState(initValue = "Cash")
    val paymentReference = InputFieldState()
    val paymentNotes = InputFieldState()

    val categories: SnapshotStateList<CategoryEntity> = SnapshotStateList()
    private val selectedCategory: MutableState<CategoryEntity?> = mutableStateOf(null)

    fun loadCategories() {
        ioLaunch {
            try {
                val userId = admin.first.first()
                val storeId = store.first.first()
                val list = appDatabase.categoryDao().getCategoriesByUserIdAndStoreId(userId, storeId)
                categories.clear()
                categories.addAll(list)
            } catch (e: Exception) {
                snackBarState.value = "Failed to load categories: ${e.message}"
            }
        }
    }

    fun onCategorySelected(catName: String) {
        val cat = categories.firstOrNull { it.catName == catName }
        selectedCategory.value = cat
        categoryName.text = catName
    }

    fun getCustomerByMobile(onFound: (CustomerEntity?) -> Unit = {}) {
        ioLaunch {
            try {
                val mobile = customerMobile.text.trim()
                if (mobile.isEmpty()) {
                    onFound(null)
                    return@ioLaunch
                }
                val customer = appDatabase.customerDao().getCustomerByMobile(mobile)
                if (customer != null) {
                    customerName.text = customer.name
                    customerAddress.text = customer.address ?: ""
                    snackBarState.value = "Customer Found"
                } else {
                    snackBarState.value = "No Customer Found"
                }
                onFound(customer)
            } catch (e: Exception) {
                snackBarState.value = "Error fetching customer details"
                onFound(null)
            }
        }
    }

    fun observePreOrders() {
        // Avoid starting multiple collectors when the user switches tabs.
        if (isObservingPreOrders) return
        isObservingPreOrders = true
        ioLaunch {
            appDatabase.preOrderDao().observePreOrderSummaries().collectLatest {
                preOrders.clear()
                preOrders.addAll(it)
            }
        }
    }

    fun observePreOrder(preOrderId: String): Flow<PreOrderEntity?> =
        appDatabase.preOrderDao().observePreOrder(preOrderId)

    fun observePreOrderItems(preOrderId: String): Flow<List<PreOrderItemEntity>> =
        appDatabase.preOrderDao().observePreOrderItems(preOrderId)

    fun observePreOrderPayments(preOrderId: String): Flow<List<CustomerTransactionEntity>> =
        appDatabase.customerTransactionDao().getTransactionsByPreOrderId(preOrderId)

    fun generatePreOrderReceipt(
        context: Context,
        preOrderId: String,
        onFileReady: (Uri) -> Unit,
        onFailure: (String) -> Unit = {}
    ) {
        ioLaunch {
            try {
                val preOrder = appDatabase.preOrderDao().observePreOrder(preOrderId).first()
                    ?: return@ioLaunch withMain { onFailure("Pre-order not found") }

                val items = appDatabase.preOrderDao().observePreOrderItems(preOrderId).first()
                val payments = appDatabase.customerTransactionDao().getTransactionsByPreOrderId(preOrderId).first()
                val store = appDatabase.storeDao().getStoreById(preOrder.storeId)
                val customer = appDatabase.customerDao().getCustomerByMobile(preOrder.customerMobile)

                generatePreOrderReceiptPdf(
                    context = context,
                    store = store,
                    customer = customer,
                    preOrder = preOrder,
                    items = items,
                    payments = payments,
                    onFileReady = { uri -> mainScope { onFileReady(uri) } }
                )
            } catch (e: Exception) {
                withMain { onFailure("Failed to generate receipt: ${e.message}") }
            }
        }
    }

    fun deletePreOrder(preOrderId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        ioLaunch {
            try {
                val result = appDatabase.preOrderDao().deletePreOrderById(preOrderId)
                if (result > 0) onSuccess() else onFailure("Failed to delete pre-order")
            } catch (e: Exception) {
                onFailure("Failed to delete pre-order: ${e.message}")
            }
        }
    }

    fun updatePreOrderStatus(preOrderId: String, status: String, onFailure: (String) -> Unit = {}) {
        ioLaunch {
            try {
                val res = appDatabase.preOrderDao()
                    .updatePreOrderStatus(preOrderId, status, Timestamp(System.currentTimeMillis()))
                if (res <= 0) {
                    onFailure("Failed to update status")
                }
            } catch (e: Exception) {
                onFailure("Failed to update status: ${e.message}")
            }
        }
    }

    fun addAdvancePayment(
        preOrderId: String,
        customerMobile: String,
        amount: Double,
        paymentMethod: String?,
        referenceNumber: String?,
        notes: String?,
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        ioLaunch {
            try {
                if (amount <= 0) return@ioLaunch onFailure("Amount must be greater than 0")

                val userId = admin.first.first()
                val storeId = store.first.first()
                val now = Timestamp(System.currentTimeMillis())

                val payment = CustomerTransactionEntity(
                    transactionId = generateId(),
                    customerMobile = customerMobile,
                    transactionDate = now,
                    amount = amount,
                    transactionType = "credit",
                    category = "pre_order",
                    description = "Advance received for pre-order",
                    referenceNumber = referenceNumber?.trim().takeIf { !it.isNullOrBlank() },
                    paymentMethod = paymentMethod?.trim().takeIf { !it.isNullOrBlank() },
                    notes = notes?.trim().takeIf { !it.isNullOrBlank() },
                    userId = userId,
                    storeId = storeId,
                    linkedPreOrderId = preOrderId
                )
                val result = appDatabase.customerTransactionDao().insertTransaction(payment)
                if (result != -1L) onSuccess() else onFailure("Failed to add payment")
            } catch (e: Exception) {
                onFailure("Failed to add payment: ${e.message}")
            }
        }
    }

    fun createPreOrder(
        deliveryDate: LocalDate,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        ioLaunch {
            try {
                val mobile = customerMobile.text.trim()
                val name = customerName.text.trim()
                val address = customerAddress.text.trim().takeIf { it.isNotBlank() }

                if (mobile.isEmpty()) return@ioLaunch onFailure("Customer mobile is required")
                if (name.isEmpty()) return@ioLaunch onFailure("Customer name is required")

                val category = selectedCategory.value
                    ?: categories.firstOrNull { it.catName == categoryName.text.trim() }
                    ?: return@ioLaunch onFailure("Please select a category")

                val qty = quantity.text.toIntOrNull() ?: 1
                val wt = estimatedWeight.text.toDoubleOrNull() ?: 0.0
                val price = estimatedPrice.text.toDoubleOrNull() ?: 0.0
                val extraKey = addDesKey.text.trim()
                val extraValue = addDesValue.text.trim()

                val userId = admin.first.first()
                val storeId = store.first.first()

                // Ensure customer exists.
                val existingCustomer = appDatabase.customerDao().getCustomerByMobile(mobile)
                if (existingCustomer == null) {
                    val now = Timestamp(System.currentTimeMillis())
                    val newCustomer = CustomerEntity(
                        mobileNo = mobile,
                        name = name,
                        address = address,
                        addDate = now,
                        lastModifiedDate = now,
                        userId = userId,
                        storeId = storeId
                    )
                    appDatabase.customerDao().insertCustomer(newCustomer)
                } else {
                    val updated = existingCustomer.copy(
                        name = name,
                        address = address,
                        lastModifiedDate = Timestamp(System.currentTimeMillis())
                    )
                    appDatabase.customerDao().updateCustomer(updated)
                }

                val preOrderId = generateId()
                val now = Timestamp(System.currentTimeMillis())
                val deliveryTimestamp = Timestamp(
                    deliveryDate
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                )

                // Keep a friendly string in the UI field even if the user never opens the date picker.
                if (deliveryDateText.text.isBlank()) {
                    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                    deliveryDateText.textChange(deliveryDate.format(formatter))
                }

                val preOrder = PreOrderEntity(
                    preOrderId = preOrderId,
                    customerMobile = mobile,
                    storeId = storeId,
                    userId = userId,
                    orderDate = now,
                    deliveryDate = deliveryTimestamp,
                    status = "CONFIRMED",
                    note = preOrderNote.text.trim().takeIf { it.isNotBlank() },
                    createdAt = now,
                    updatedAt = now
                )
                appDatabase.preOrderDao().insertPreOrder(preOrder)

                val item = PreOrderItemEntity(
                    preOrderItemId = generateId(),
                    preOrderId = preOrderId,
                    catId = category.catId,
                    catName = category.catName,
                    quantity = qty,
                    estimatedGrossWt = wt,
                    estimatedPrice = price,
                    addDesKey = extraKey,
                    addDesValue = extraValue,
                    note = itemNote.text.trim().takeIf { it.isNotBlank() }
                )
                appDatabase.preOrderDao().insertPreOrderItems(listOf(item))

                val advance = advanceAmount.text.toDoubleOrNull() ?: 0.0
                if (advance > 0) {
                    val payment = CustomerTransactionEntity(
                        transactionId = generateId(),
                        customerMobile = mobile,
                        transactionDate = now,
                        amount = advance,
                        transactionType = "credit",
                        category = "pre_order",
                        description = "Advance received for pre-order",
                        referenceNumber = paymentReference.text.trim().takeIf { it.isNotBlank() },
                        paymentMethod = paymentMethod.text.trim().takeIf { it.isNotBlank() },
                        notes = paymentNotes.text.trim().takeIf { it.isNotBlank() },
                        userId = userId,
                        storeId = storeId,
                        linkedPreOrderId = preOrderId
                    )
                    appDatabase.customerTransactionDao().insertTransaction(payment)
                }

                clearForm()
                snackBarState.value = "Pre-order created"
                onSuccess(preOrderId)
            } catch (e: Exception) {
                onFailure("Failed to create pre-order: ${e.message}")
            }
        }
    }

    fun clearForm() {
        customerMobile.clear()
        customerName.clear()
        customerAddress.clear()
        categoryName.clear()
        quantity.text = "1"
        estimatedWeight.clear()
        estimatedPrice.clear()
        addDesKey.clear()
        addDesValue.clear()
        itemNote.clear()
        preOrderNote.clear()
        deliveryDateText.clear()
        advanceAmount.clear()
        paymentMethod.text = "Cash"
        paymentReference.clear()
        paymentNotes.clear()
        selectedCategory.value = null
    }

    private var isObservingPreOrders: Boolean = false
}
