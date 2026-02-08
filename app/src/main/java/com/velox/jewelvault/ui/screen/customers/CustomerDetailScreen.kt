package com.velox.jewelvault.ui.screen.customers

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Add
import androidx.compose.material.icons.twotone.Check
import androidx.compose.material.icons.twotone.CheckCircle
import androidx.compose.material.icons.twotone.Delete
import androidx.compose.material.icons.twotone.Edit
import androidx.compose.material.icons.twotone.History
import androidx.compose.material.icons.twotone.Payment
import androidx.compose.material.icons.twotone.Receipt
import androidx.compose.material.icons.twotone.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.velox.jewelvault.data.roomdb.entity.customer.CustomerEntity
import com.velox.jewelvault.data.roomdb.entity.customer.CustomerKhataBookEntity
import com.velox.jewelvault.data.roomdb.entity.customer.CustomerTransactionEntity
import com.velox.jewelvault.data.roomdb.dto.TransactionItem
import com.velox.jewelvault.data.roomdb.entity.order.OrderEntity
import com.velox.jewelvault.ui.components.CusOutlinedTextField
import com.velox.jewelvault.ui.components.InputFieldState
import com.velox.jewelvault.ui.components.TextListView
import com.velox.jewelvault.ui.components.bounceClick
import com.velox.jewelvault.ui.components.baseBackground0
import com.velox.jewelvault.ui.components.baseBackground7
import com.velox.jewelvault.ui.nav.SubScreens
import com.velox.jewelvault.utils.LocalSubNavController
import com.velox.jewelvault.utils.TransactionUtils
import com.velox.jewelvault.utils.formatCurrency
import com.velox.jewelvault.utils.to1FString
import com.velox.jewelvault.utils.formatDate
import kotlinx.coroutines.delay
import kotlin.collections.isNotEmpty

// Dialog state enum for better state management
private enum class DialogState {
    None,
    AddOutstanding,
    AddKhataBook,
    AddKhataPayment,
    AddRegularPayment,
    KhataBookDetails,
    MonthPayment,
    CompletePlan,
    DeleteTransaction,
    DeleteTransactionHistory
}

// Helper function to get plan info from predefined plans
private fun getPlanInfo(planName: String): KhataBookPlan? {
    return getPredefinedPlans().find { it.name == planName }
}

// Helper function to find transaction by data
private fun findTransactionByData(transactionHistory: List<TransactionItem>, selectedData: List<String>): TransactionItem? {
    if (selectedData.size < 4) return null
    
    val selectedDate = selectedData[0]
    val selectedAmount = selectedData[3]
    
    return transactionHistory.find { transaction ->
        formatDate(transaction.date) == selectedDate && 
        formatCurrency(transaction.amount) == selectedAmount
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CustomerDetailScreen(
    customerMobile: String, viewModel: CustomerViewModel = hiltViewModel()
) {
    val customer by viewModel.selectedCustomer
    val isLoadingDetails by viewModel.isLoadingCustomerDetails
    val isLoadingKhataBook by viewModel.isLoadingKhataBook
    val isLoadingPayment by viewModel.isLoadingPayment
    val transactionsUpdated by viewModel.transactionsUpdated

    // Consolidated dialog states for better performance
    var dialogState by remember { mutableStateOf(DialogState.None) }
    var selectedMonth by remember { mutableStateOf(0) }
    var selectedKhataBookForCompletion by remember { mutableStateOf<CustomerKhataBookEntity?>(null) }
    var selectedTransactionForDeletion by remember { mutableStateOf<CustomerTransactionEntity?>(null) }
    var selectedTransactionHistoryItem by remember { mutableStateOf<List<String>?>(null) }
    var showEditDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var editName by rememberSaveable { mutableStateOf("") }
    var editAddress by rememberSaveable { mutableStateOf("") }
    var editMobile by rememberSaveable { mutableStateOf("") }
    var editGstin by rememberSaveable { mutableStateOf("") }
    val editNameField = remember { InputFieldState() }
    val editAddressField = remember { InputFieldState() }
    val editGstinField = remember { InputFieldState() }
    val editMobileField = remember { InputFieldState() }
    val subNavController = LocalSubNavController.current

    LaunchedEffect(customerMobile) {
        viewModel.loadCustomerDetails(customerMobile)
    }

    // Clear data when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearSelectedCustomerData()
        }
    }

    // Auto-dismiss dialogs when operations complete
    LaunchedEffect(isLoadingKhataBook, isLoadingPayment) {
        if (!isLoadingKhataBook && !isLoadingPayment) {
            dialogState = DialogState.None
        }
    }

    if (!isLoadingDetails)  {

        viewModel.currentScreenHeadingState.value = "${customer?.name} (${customer?.mobileNo})"
        LaunchedEffect(customer?.mobileNo) {
            customer?.let { customerEntity ->
                editName = customerEntity.name
                editAddress = customerEntity.address ?: ""
                editMobile = customerEntity.mobileNo
                editGstin = customerEntity.gstin_pan ?: ""
                viewModel.refreshCustomerEditPermissions(customerEntity.mobileNo)
            }
        }
        val permissions by viewModel.customerEditPermissions
        val canEditMobile = permissions?.canEditMobile == true
        val mobileLockHint = if (!canEditMobile) {
            "Mobile number locked until outstanding balance, khata books, or orders are cleared."
        } else null

        LaunchedEffect(editName, editAddress, editGstin, editMobile) {
            editNameField.text = editName
            editAddressField.text = editAddress
            editGstinField.text = editGstin
            editMobileField.text = editMobile
        }
        Box (  modifier = Modifier
            .fillMaxSize()
            .baseBackground0()
            .padding(5.dp)){
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            )
            {
                // Customer Information Card (Compact)
                item {
                    CompactCustomerInfoCard(
                        customer = customer,
                        activeKhataBooks = viewModel.selectedCustomerKhataBooks,
                        onEditClick = { if (customer != null) showEditDialog = true }
                    )
                }

                // Khata Book Card (Second Position)
                item {
                    KhataBookCard(
                        khataBooks = viewModel.selectedCustomerKhataBooks,
                        transactions = viewModel.selectedCustomerTransactions,
                        onAddKhataBook = { dialogState = DialogState.AddKhataBook },
                        onAddPayment = { dialogState = DialogState.AddKhataPayment },
                        onViewDetails = { dialogState = DialogState.KhataBookDetails },
                        onToggleStatus = { status ->
                            // For now, update the first active khata book
                            viewModel.selectedCustomerKhataBooks.firstOrNull()?.let {
                                viewModel.updateKhataBookStatus(it.khataBookId, status)
                            }
                        },
                        onMonthClick = { month ->
                            selectedMonth = month
                            dialogState = DialogState.MonthPayment
                        },
                        viewModel = viewModel,
                        onCompletePlan = { khataBook ->
                            selectedKhataBookForCompletion = khataBook
                            dialogState = DialogState.CompletePlan
                        },
                        onDeleteTransaction = { transaction ->
                            selectedTransactionForDeletion = transaction
                            dialogState = DialogState.DeleteTransaction
                        })
                }

                // Completed Plans Card (if any completed plans exist)
                if (viewModel.selectedCustomerCompletedKhataBooks.isNotEmpty()) {
                    item {
                        CompletedPlansCard(
                            completedPlans = viewModel.selectedCustomerCompletedKhataBooks,
                            transactions = viewModel.selectedCustomerTransactions
                        )
                    }
                }

                // Combined Outstanding & Payments Card
                item {
                    CombinedPaymentsCard(
                        transactions = viewModel.selectedCustomerTransactions,
                        onAddOutstanding = { dialogState = DialogState.AddOutstanding },
                        onAddPayment = { dialogState = DialogState.AddRegularPayment },
                        onDeleteTransaction = { transaction ->
                            selectedTransactionForDeletion = transaction
                            dialogState = DialogState.DeleteTransaction
                        }
                    )
                }

                // Customer Orders Card
                item {
                    CustomerOrdersCard(
                        customerMobile = customerMobile, viewModel = viewModel
                    )
                }

                item {
                    TransactionHistoryCard(
                        customerMobile = customerMobile, 
                        viewModel = viewModel,
                        onTransactionLongClick = { selectedTransaction ->
                            selectedTransactionHistoryItem = selectedTransaction
                            dialogState = DialogState.DeleteTransactionHistory
                        }
                    )
                }
            }
        }

        customer?.let { customerEntity ->
            if (showEditDialog) {
                AlertDialog(
                    onDismissRequest = { showEditDialog = false },
                    title = { Text("Edit Customer") },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CusOutlinedTextField(
                                state = editNameField,
                                onTextChange = {
                                    editName = it
                                    editNameField.textChange(it)
                                },
                                placeholderText = "Customer Name"
                            )

                            CusOutlinedTextField(
                                state = editAddressField,
                                onTextChange = {
                                    editAddress = it
                                    editAddressField.textChange(it)
                                },
                                placeholderText = "Address"
                            )

                            CusOutlinedTextField(
                                state = editGstinField,
                                onTextChange = {
                                    val upper = it.uppercase()
                                    editGstin = upper
                                    editGstinField.textChange(upper)
                                },
                                placeholderText = "GSTIN / PAN"
                            )

                            CusOutlinedTextField(
                                state = editMobileField,
                                onTextChange = {},
                                placeholderText = "Mobile Number",
                                keyboardType = KeyboardType.Phone,
                                enabled = false,
                                readOnly = true,
                                supportingText = mobileLockHint?.let {
                                    {
                                        Text(
                                            it,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            )

                            if (permissions?.canDelete == true) {
                                TextButton(
                                    onClick = {
                                        showDeleteDialog = true
                                        showEditDialog = false
                                    }
                                ) {
                                    Text("Delete customer")
                                }
                            } else {
                                Text(
                                    text = buildString {
                                        permissions?.let { perms ->
                                            when {
                                                perms.orderCount > 0 -> append("Customer has ${perms.orderCount} order(s)")
                                                perms.khataBookCount > 0 -> append("Customer has ${perms.khataBookCount} khata book(s)")
                                                perms.outstandingBalance > 0.0 -> append("Outstanding ₹${perms.outstandingBalance.to1FString()}")
                                                else -> append("Delete eligibility pending...")
                                            }
                                        } ?: append("Checking delete eligibility...")
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.updateCustomerProfile(
                                    customerEntity.mobileNo,
                                    editName,
                                    editAddress,
                                    editMobile,
                                    editGstin.trim().uppercase()
                                )
                                showEditDialog = false
                            },
                            enabled = editName.isNotBlank() && editAddress.isNotBlank()
                        ) {
                            Text("Save changes")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEditDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = {
                        showDeleteDialog = false
                        showEditDialog = false
                    },
                    title = { Text("Delete Customer") },
                    text = {
                        Text("This will permanently remove the customer record. Are you sure?")
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.deleteCustomer(customerEntity.mobileNo)
                            showDeleteDialog = false
                            subNavController.popBackStack()
                        }) {
                            Text("Delete")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }

    }

    // Consolidated Dialog Management
    when (dialogState) {
        DialogState.AddOutstanding -> {
            AddOutstandingDialog(
                onDismiss = { dialogState = DialogState.None },
                onConfirm = { amount, type, description, notes ->
                    viewModel.outstandingAmount.text = amount
                    viewModel.outstandingType.text = type
                    viewModel.outstandingDescription.text = description
                    viewModel.outstandingNotes.text = notes
                    viewModel.addOutstandingTransaction(customerMobile)
                },
                isLoading = isLoadingPayment
            )
        }
        
        DialogState.AddKhataBook -> {
            AddKhataBookDialog(
                planOptions = viewModel.planList,
                onDismiss = { dialogState = DialogState.None },
                onConfirm = { monthlyAmount, totalMonths, planName, notes ->
                    viewModel.khataBookMonthlyAmount.text = monthlyAmount
                    viewModel.khataBookTotalMonths.text = totalMonths
                    viewModel.khataBookPlanName.text = planName
                    viewModel.khataBookNotes.text = notes
                    viewModel.createKhataBook(customerMobile)
                },
                isLoading = isLoadingKhataBook
            )
        }
        
        DialogState.AddKhataPayment -> {
            AddKhataPaymentDialog(
                khataBooks = viewModel.selectedCustomerKhataBooks,
                onDismiss = { dialogState = DialogState.None },
                onConfirm = { month, amount, type, notes ->
                    viewModel.khataPaymentMonth.text = month
                    viewModel.khataPaymentAmount.text = amount
                    viewModel.khataPaymentType.text = type
                    viewModel.khataPaymentNotes.text = notes
                    viewModel.addKhataPayment(customerMobile)
                },
                isLoading = isLoadingPayment
            )
        }
        
        DialogState.AddRegularPayment -> {
            AddRegularPaymentDialog(
                onDismiss = { dialogState = DialogState.None },
                onConfirm = { amount, type, method, reference, notes ->
                    viewModel.regularPaymentAmount.text = amount
                    viewModel.regularPaymentType.text = type
                    viewModel.regularPaymentMethod.text = method
                    viewModel.regularPaymentReference.text = reference
                    viewModel.regularPaymentNotes.text = notes
                    viewModel.addRegularPayment(customerMobile)
                },
                isLoading = isLoadingPayment
            )
        }
        
        DialogState.KhataBookDetails -> {
            KhataBookDetailsDialog(
                khataBooks = viewModel.selectedCustomerKhataBooks,
                transactions = viewModel.selectedCustomerTransactions,
                onDismiss = { dialogState = DialogState.None },
                onDeleteTransaction = { transaction ->
                    selectedTransactionForDeletion = transaction
                    dialogState = DialogState.DeleteTransaction
                }
            )
        }
        
        DialogState.MonthPayment -> {
            MonthPaymentDialog(
                month = selectedMonth, 
                khataBooks = viewModel.selectedCustomerKhataBooks, 
                onDismiss = { dialogState = DialogState.None }, 
                onConfirm = { amount, notes ->
                    viewModel.khataPaymentMonth.text = selectedMonth.toString()
                    viewModel.khataPaymentAmount.text = amount
                    viewModel.khataPaymentType.text = "full"
                    viewModel.khataPaymentNotes.text = notes
                    viewModel.addKhataPayment(customerMobile)
                }, 
                isLoading = isLoadingPayment
            )
        }
        
        DialogState.CompletePlan -> {
            selectedKhataBookForCompletion?.let { khataBook ->
                CompletePlanConfirmationDialog(
                    khataBook = khataBook,
                    transactions = viewModel.selectedCustomerTransactions,
                    onDismiss = { 
                        dialogState = DialogState.None
                        selectedKhataBookForCompletion = null
                    },
                    onConfirm = { 
                        viewModel.completeKhataBookPlan(khataBook.khataBookId)
                        dialogState = DialogState.None
                        selectedKhataBookForCompletion = null
                    },
                    isLoading = isLoadingKhataBook
                )
            }
        }
        
        DialogState.DeleteTransaction -> {
            selectedTransactionForDeletion?.let { transaction ->
                DeleteTransactionConfirmationDialog(
                    transaction = transaction,
                    onDismiss = { 
                        dialogState = DialogState.None
                        selectedTransactionForDeletion = null
                    },
                    onConfirm = { 
                        viewModel.deleteTransaction(transaction)
                        dialogState = DialogState.None
                        selectedTransactionForDeletion = null
                    }
                )
            }
        }
        
        DialogState.DeleteTransactionHistory -> {
            selectedTransactionHistoryItem?.let { transactionData ->
                TransactionHistoryDeleteDialog(
                    transactionData = transactionData,
                    onDismiss = { 
                        dialogState = DialogState.None
                        selectedTransactionHistoryItem = null
                    },
                    onConfirm = { 
                        val transactionHistory by viewModel.transactionHistory
                        val selectedTransaction = findTransactionByData(transactionHistory, transactionData)
                        if (selectedTransaction != null) {
                            viewModel.deleteTransactionHistoryItem(selectedTransaction)
                        }
                        dialogState = DialogState.None
                        selectedTransactionHistoryItem = null
                    }
                )
            }
        }
        
        DialogState.None -> { /* No dialog */ }
    }
}


@Composable
fun CompactCustomerInfoCard(
    customer: CustomerEntity?,
    activeKhataBooks: List<CustomerKhataBookEntity> = emptyList(),
    onEditClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(0.dp)
        ) {
            // Header with gradient background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .baseBackground7(),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 16.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.TwoTone.Visibility,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = customer?.name ?: "Customer",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    // Status chip
                    if (customer != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatusChip(isActive = activeKhataBooks.isNotEmpty())
                            IconButton(
                                onClick = onEditClick,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.TwoTone.Edit,
                                    contentDescription = "Edit customer",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Main info section
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Total Amount (highlighted)
                if (customer != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.TwoTone.Payment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = formatCurrency(customer.totalAmount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "(${customer.totalItemBought} items)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Info chips row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (customer?.mobileNo != null) {
                        InfoChip(Icons.TwoTone.Receipt, customer.mobileNo)
                    }
                    if (!customer?.gstin_pan.isNullOrEmpty()) {
                        InfoChip(Icons.TwoTone.History, customer?.gstin_pan ?: "")
                    }
                    if (!customer?.address.isNullOrEmpty()) {
                        InfoChip(Icons.TwoTone.Add, customer?.address ?: "")
                    }
                }

                // Added date and notes
                if (customer != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Added: ${formatDate(customer.addDate)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!customer.notes.isNullOrEmpty()) {
                        Text(
                            text = "Notes: ${customer.notes}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(isActive: Boolean) {
    Card(
        shape = RoundedCornerShape(50),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) Color(0xFF4CAF50) else Color(0xFFBDBDBD)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.TwoTone.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isActive) "Active" else "Inactive",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun InfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Card(
        shape = RoundedCornerShape(50),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.height(28.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun CombinedPaymentsCard(
    transactions: List<CustomerTransactionEntity>,
    onAddOutstanding: () -> Unit,
    onAddPayment: () -> Unit,
    onDeleteTransaction: (CustomerTransactionEntity) -> Unit = {}
) {
    // Calculate outstanding balance using the same logic as DAO - only outstanding category
    val outstandingBalance = transactions.filter { it.category == "outstanding" }
        .sumOf { transaction ->
            when (transaction.transactionType) {
                "debit" -> transaction.amount
                "credit" -> -transaction.amount
                else -> 0.0
            }
        }
    
    // Calculate outstanding debts (positive amounts that customer owes)
    val outstandingDebts = transactions.filter { 
        it.category == "outstanding" && it.transactionType == "debit" 
    }.sumOf { it.amount }
    
    // Calculate outstanding payments (negative amounts - payments made against outstanding)
    val outstandingPayments = transactions.filter { 
        it.category == "outstanding" && it.transactionType == "credit" 
    }.sumOf { it.amount }
    
    // Calculate regular payments (separate from outstanding)
    val regularPayments = transactions.filter { 
        it.category == "regular_payment" 
    }.sumOf { it.amount }
    
    // Total outstanding amount (debts - payments)
    val totalOutstanding = outstandingDebts - outstandingPayments
    
    // Total payments made (outstanding payments + regular payments)
    val totalPayments = outstandingPayments + regularPayments
    
    // Remaining balance = outstanding debts - all payments made
    val remainingBalance = outstandingDebts - totalPayments

    // Debug logging to verify calculations
    LaunchedEffect(transactions.size) {
        println("DEBUG: Payment Calculations:")
        println("  Outstanding Balance (DAO logic): $outstandingBalance")
        println("  Outstanding Debts: $outstandingDebts")
        println("  Outstanding Payments: $outstandingPayments")
        println("  Regular Payments: $regularPayments")
        println("  Total Outstanding: $totalOutstanding")
        println("  Total Payments: $totalPayments")
        println("  Remaining Balance: $remainingBalance")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Payments & Outstanding",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            // Summary and Action Row
            Row(
                modifier = Modifier.fillMaxWidth(), 
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Outstanding
                Column {
                    Text(
                        text = "Outstanding",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatCurrency(outstandingBalance),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (outstandingBalance > 0) Color.Red else Color.Green
                    )
                }
                
                // Total Paid
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Total Paid",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatCurrency(totalPayments),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Green
                    )
                }
                
                // Remaining Balance
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Remaining",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatCurrency(remainingBalance),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (remainingBalance > 0) Color.Red else Color.Green
                    )
                }
                
                // Action Buttons
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(
                        onClick = onAddOutstanding,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            Icons.TwoTone.Add,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Outstanding", style = MaterialTheme.typography.labelSmall)
                    }
                    Button(
                        onClick = onAddPayment,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            Icons.TwoTone.Payment,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Payment", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

        }
    }
}

@Composable
fun CompactTransactionItemWithDelete(
    transaction: CustomerTransactionEntity,
    onDelete: (CustomerTransactionEntity) -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (transaction.category) {
                        "outstanding" -> if (transaction.transactionType == "debit") "Outstanding Debt" else "Outstanding Payment"
                        "regular_payment" -> "Regular Payment"
                        "khata_book" -> "Khata Payment"
                        else -> transaction.category
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = formatDate(transaction.transactionDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (transaction.description?.isNotEmpty() == true) {
                    Text(
                        text = transaction.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatCurrency(transaction.amount),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (transaction.isDebit) Color.Red else Color.Green
                )
                
                // Delete button
                IconButton(
                    onClick = { onDelete(transaction) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.TwoTone.Delete,
                        contentDescription = "Delete Transaction",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}



@Composable
fun KhataBookCard(
    khataBooks: List<CustomerKhataBookEntity>,
    transactions: List<CustomerTransactionEntity>,
    onAddKhataBook: () -> Unit,
    onAddPayment: () -> Unit,
    onViewDetails: () -> Unit,
    onToggleStatus: (String) -> Unit,
    onMonthClick: (Int) -> Unit,
    viewModel: CustomerViewModel,
    onCompletePlan: (CustomerKhataBookEntity) -> Unit,
    onDeleteTransaction: (CustomerTransactionEntity) -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Khata Book Plans (${khataBooks.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (khataBooks.isEmpty()) {
                        Button(
                            onClick = onAddKhataBook,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Green)
                        ) {
                            Icon(
                                Icons.TwoTone.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Create Plan", style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        IconButton(
                            onClick = onViewDetails, 
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.TwoTone.Visibility,
                                contentDescription = "View Details",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            if (khataBooks.isNotEmpty()) {
                // Show summary for all khata books
                val totalAmount = khataBooks.sumOf { it.totalAmount }
                val khataPayments = transactions.filter { 
                    it.transactionType == "khata_payment" 
                }
                val paidAmount = khataPayments.sumOf { it.amount }
                val remainingAmount = totalAmount - paidAmount
                val progressPercentage = if (totalAmount > 0) (paidAmount / totalAmount) * 100 else 0.0

                // Progress Bar
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Overall Progress",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${progressPercentage.to1FString()}%",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    LinearProgressIndicator(
                        progress = { (progressPercentage / 100).toFloat() },
                        modifier = Modifier.fillMaxWidth(),
                        color = when {
                            progressPercentage >= 80 -> Color.Green
                            progressPercentage >= 50 -> Color(0xFFFF9800)
                            else -> Color.Red
                        },
                    )
                }

                // Summary Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Total Plans",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = khataBooks.size.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Total Paid",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatCurrency(paidAmount),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Green
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Remaining",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatCurrency(remainingAmount),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                    }
                }

                // Show individual khata book plans
                khataBooks.forEach { khataBook ->
                    KhataBookPlanItem(
                        khataBook = khataBook,
                        transactions = transactions,
                        onMonthClick = onMonthClick,
                        viewModel = viewModel,
                        onCompletePlan = onCompletePlan
                    )
                }
            }
        }
    }
}

@Composable
fun KhataBookPlanItem(
    khataBook: CustomerKhataBookEntity,
    transactions: List<CustomerTransactionEntity>,
    onMonthClick: (Int) -> Unit,
    viewModel: CustomerViewModel,
    onCompletePlan: (CustomerKhataBookEntity) -> Unit
) {
    val khataPayments = transactions.filter { 
        it.khataBookId == khataBook.khataBookId && it.transactionType == "khata_payment" 
    }
    val paidMonths = khataPayments.size
    val remainingMonths = khataBook.totalMonths - paidMonths
    val totalAmount = khataBook.monthlyAmount * khataBook.totalMonths
    val paidAmount = khataPayments.sumOf { it.amount }
    val remainingAmount = remainingMonths * khataBook.monthlyAmount
    val progressPercentage = if (totalAmount > 0) (paidAmount / totalAmount) * 100 else 0.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Plan header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = khataBook.planName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = khataBook.status.capitalize(),
                        style = MaterialTheme.typography.bodySmall,
                        color = when (khataBook.status) {
                            "active" -> Color.Green
                            "completed" -> Color.Blue
                            else -> Color.Gray
                        }
                    )
                    // Complete plan button (only show for active plans)
                    if (khataBook.status == "active") {
                        Button(
                            onClick = { 
                                onCompletePlan(khataBook)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Icon(
                                Icons.TwoTone.Check,
                                contentDescription = "Complete Plan",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Complete",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            // Progress
            LinearProgressIndicator(
                progress = { (progressPercentage / 100).toFloat() },
                modifier = Modifier.fillMaxWidth(),
                color = when {
                    progressPercentage >= 80 -> Color.Green
                    progressPercentage >= 50 -> Color(0xFFFF9800)
                    else -> Color.Red
                },
            )

            // Plan Structure (Pay vs Reward)
            val planInfo = getPlanInfo(khataBook.planName)
            if (planInfo != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Pay Months",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${planInfo.payMonths} months",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Blue
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Reward Months",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${planInfo.benefitMonths} months",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Total",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${planInfo.effectiveMonths} months",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Monthly",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatCurrency(khataBook.monthlyAmount),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Paid",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$paidMonths/${khataBook.totalMonths}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Green
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Remaining",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatCurrency(remainingAmount),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Red
                    )
                }
            }

            // Legend
            val legendPlanInfo = getPlanInfo(khataBook.planName)
            if (legendPlanInfo != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    LegendItem("Pay Months", Color(0xFF2196F3))
                    LegendItem("Reward Months", Color(0xFFFF9800))
                    LegendItem("Paid", Color(0xFF4CAF50))
                }
            }

            // Month grid
            KhataBookMonthGrid(
                totalMonths = khataBook.totalMonths,
                paidMonths = khataPayments.mapNotNull { it.monthNumber }.toSet(),
                onMonthClick = onMonthClick,
                enabled = khataBook.status == "active",
                planInfo = legendPlanInfo,
                startDate = khataBook.startDate.time,
                onClearPayment = { monthNumber ->
                    viewModel.clearMonthlyPayment(khataBook.khataBookId, monthNumber)
                }
            )
        }
    }
}


// Dialog components
@Composable
fun AddOutstandingDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String) -> Unit,
    isLoading: Boolean = false
) {
    var amount by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("debt") }
    var description by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("Add Outstanding Transaction") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CusOutlinedTextField(
                    state = InputFieldState(amount),
                    onTextChange = { amount = it },
                    placeholderText = "Amount",
                    keyboardType = KeyboardType.Number
                )

                CusOutlinedTextField(
                    state = InputFieldState(type),
                    onTextChange = { type = it },
                    placeholderText = "Type",
                    readOnly = true
                )

                CusOutlinedTextField(
                    state = InputFieldState(description),
                    onTextChange = { description = it },
                    placeholderText = "Description (Optional)"
                )

                CusOutlinedTextField(
                    state = InputFieldState(notes),
                    onTextChange = { notes = it },
                    placeholderText = "Notes (Optional)"
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(amount, type, description, notes) },
                enabled = amount.isNotEmpty() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Add")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        })
}

@Composable
fun AddKhataBookDialog(
    planOptions: List<KhataBookPlan>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String) -> Unit,
    isLoading: Boolean = false
) {
    val defaultPlan =
        planOptions.firstOrNull { it.name == "Standard Plan" } ?: planOptions.firstOrNull()
    var monthlyAmount by remember { mutableStateOf("") }
    var planName by remember(planOptions) { mutableStateOf(defaultPlan?.name.orEmpty()) }
    var selectedPlan by remember(planOptions) { mutableStateOf(defaultPlan) }
    var totalMonths by remember(planOptions) {
        mutableStateOf((defaultPlan?.effectiveMonths ?: 12).toString())
    }
    var notes by remember { mutableStateOf("") }
    val planNames = planOptions.map { it.name }

    AlertDialog(onDismissRequest = onDismiss, title = { Text("Create Khata Book Plan") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CusOutlinedTextField(
                state = InputFieldState(planName),
                onTextChange = { planName = it },
                placeholderText = "Plan Name",
                dropdownItems = planNames,
                onDropdownItemSelected = { selected ->
                    planName = selected
                    selectedPlan = planOptions.firstOrNull { it.name == selected }
                    totalMonths = (selectedPlan?.effectiveMonths ?: 12).toString()
                }
            )

            CusOutlinedTextField(
                state = InputFieldState(monthlyAmount),
                onTextChange = { monthlyAmount = it },
                placeholderText = "Monthly Amount",
                keyboardType = KeyboardType.Number
            )

            CusOutlinedTextField(
                state = InputFieldState(totalMonths),
                onTextChange = { totalMonths = it },
                placeholderText = "Total Months",
                keyboardType = KeyboardType.Number,
                readOnly = true
            )

            selectedPlan?.let { plan ->
                val amount = monthlyAmount.toDoubleOrNull() ?: 0.0
                val results = calculateKhataBook(amount, plan)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Plan Summary",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Pay: ${plan.payMonths}m",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Benefit: ${plan.benefitMonths}m",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Total: ${plan.effectiveMonths}m",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total Pay: ${formatCurrency(results.totalPayAmount)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Benefit: ${formatCurrency(results.totalBenefitAmount)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF4CAF50)
                            )
                        }
                        Text(
                            text = "Effective Monthly: ${formatCurrency(results.effectiveMonthlyAmount)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            CusOutlinedTextField(
                state = InputFieldState(notes),
                onTextChange = { notes = it },
                placeholderText = "Notes (Optional)"
            )
        }
    }, confirmButton = {
        Button(
            onClick = { onConfirm(monthlyAmount, totalMonths, planName, notes) },
            enabled = monthlyAmount.isNotEmpty() && totalMonths.isNotEmpty() && planName.isNotEmpty() && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Create")
            }
        }
    }, dismissButton = {
        TextButton(onClick = onDismiss) {
            Text("Cancel")
        }
    })
}

@Composable
fun AddKhataPaymentDialog(
    khataBooks: List<CustomerKhataBookEntity>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String) -> Unit,
    isLoading: Boolean = false
) {
    var month by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("full") }
    var notes by remember { mutableStateOf("") }

    val khataBook = khataBooks.firstOrNull()
    val planInfo = khataBook?.let { getPlanInfo(it.planName) }

    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add Khata Payment") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Show plan structure if available
            if (planInfo != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Plan: ${khataBook.planName}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Pay: ${planInfo.payMonths} months",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Blue
                            )
                            Text(
                                text = "Reward: ${planInfo.benefitMonths} months",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }
                }
            }

            CusOutlinedTextField(
                state = InputFieldState(month),
                onTextChange = { month = it },
                placeholderText = "Month Number (1-${khataBooks.firstOrNull()?.totalMonths ?: 12})",
                keyboardType = KeyboardType.Number
            )

            CusOutlinedTextField(
                state = InputFieldState(amount),
                onTextChange = { amount = it },
                placeholderText = "Amount",
                keyboardType = KeyboardType.Number
            )

            CusOutlinedTextField(
                state = InputFieldState(type),
                onTextChange = { type = it },
                placeholderText = "Payment Type",
                readOnly = true
            )

            CusOutlinedTextField(
                state = InputFieldState(notes),
                onTextChange = { notes = it },
                placeholderText = "Notes (Optional)"
            )
        }
    }, confirmButton = {
        Button(
            onClick = { onConfirm(month, amount, type, notes) },
            enabled = month.isNotEmpty() && amount.isNotEmpty() && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Add")
            }
        }
    }, dismissButton = {
        TextButton(onClick = onDismiss) {
            Text("Cancel")
        }
    })
}

@Composable
fun AddRegularPaymentDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String) -> Unit,
    isLoading: Boolean = false
) {
    var amount by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("regular") }
    var method by remember { mutableStateOf("cash") }
    var reference by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add Regular Payment") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CusOutlinedTextField(
                state = InputFieldState(amount),
                onTextChange = { amount = it },
                placeholderText = "Amount",
                keyboardType = KeyboardType.Number
            )

            CusOutlinedTextField(
                state = InputFieldState(type),
                onTextChange = { type = it },
                placeholderText = "Payment Type",
                readOnly = true
            )

            CusOutlinedTextField(
                state = InputFieldState(method),
                onTextChange = { method = it },
                placeholderText = "Payment Method",
                readOnly = true
            )

            CusOutlinedTextField(
                state = InputFieldState(reference),
                onTextChange = { reference = it },
                placeholderText = "Reference Number (Optional)"
            )

            CusOutlinedTextField(
                state = InputFieldState(notes),
                onTextChange = { notes = it },
                placeholderText = "Notes (Optional)"
            )
        }
    }, confirmButton = {
        Button(
            onClick = { onConfirm(amount, type, method, reference, notes) },
            enabled = amount.isNotEmpty() && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Add")
            }
        }
    }, dismissButton = {
        TextButton(onClick = onDismiss) {
            Text("Cancel")
        }
    })
}

@Composable
fun KhataBookDetailsDialog(
    khataBooks: List<CustomerKhataBookEntity>,
    transactions: List<CustomerTransactionEntity>,
    onDismiss: () -> Unit,
    onDeleteTransaction: (CustomerTransactionEntity) -> Unit = {}
) {
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("Khata Book Details", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                khataBooks.firstOrNull()?.let { kb ->
                    val totalAmount = kb.monthlyAmount * kb.totalMonths
                    val paidAmount = transactions.filter { 
                        it.khataBookId == kb.khataBookId && it.transactionType == "khata_payment" 
                    }.sumOf { it.amount }
                    val remainingAmount = totalAmount - paidAmount
                    val progressPercentage =
                        if (totalAmount > 0) (paidAmount / totalAmount) * 100 else 0.0

                    // Plan Summary
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Plan Summary",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )

                            // Plan Structure
                            val planInfo = getPlanInfo(kb.planName)
                            if (planInfo != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            "Pay Months", style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            "${planInfo.payMonths} months",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Blue
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Reward Months", style = MaterialTheme.typography.bodySmall)
                                        Text(
                                            "${planInfo.benefitMonths} months",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF4CAF50)
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Total Months", style = MaterialTheme.typography.bodySmall)
                                        Text(
                                            "${planInfo.effectiveMonths} months",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        "Monthly Amount", style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        formatCurrency(kb.monthlyAmount),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Total Amount", style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        formatCurrency(totalAmount),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Benefit", style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        "${planInfo?.benefitPercentage?.to1FString() ?: "0.0"}%",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4CAF50)
                                    )
                                }
                            }

                            // Progress
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Progress", style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        "${progressPercentage.to1FString()}%",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { (progressPercentage / 100).toFloat() },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }

                            // Status and Dates
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Status", style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        kb.status.capitalize(),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = when (kb.status) {
                                            "active" -> Color.Green
                                            "paused" -> Color(0xFFFF9800)
                                            "completed" -> Color.Blue
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Start Date", style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        formatDate(kb.startDate),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Paid Amount", style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        formatCurrency(paidAmount),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Green
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Remaining", style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        formatCurrency(remainingAmount),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Red
                                    )
                                }
                            }
                        }
                    }

                    // Payment History
                    Text(
                        text = "Payment History",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    if (transactions.filter { 
                        it.khataBookId == kb.khataBookId && it.transactionType == "khata_payment" 
                    }.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier.height(200.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(transactions.filter { 
                                it.khataBookId == kb.khataBookId && it.transactionType == "khata_payment" 
                            }.sortedBy { it.monthNumber }) { payment ->
                                PaymentHistoryItem(
                                    payment = payment,
                                    onDelete = onDeleteTransaction
                                )
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Text(
                                text = "No payments recorded yet",
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Notes if available
                    kb.notes?.let { notes ->
                        if (notes.isNotEmpty()) {
                            Text(
                                text = "Notes",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Text(
                                    text = notes,
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        })
}

@Composable
fun PaymentHistoryItem(
    payment: CustomerTransactionEntity,
    onDelete: (CustomerTransactionEntity) -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (payment.monthNumber != null) "Month ${payment.monthNumber}" else payment.description ?: "Transaction",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${payment.transactionType.capitalize()} Payment",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatDate(payment.transactionDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = formatCurrency(payment.amount),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (payment.transactionType == "debit") Color.Red else Color.Green
                )
                
                // Delete button
                IconButton(
                    onClick = { onDelete(payment) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.TwoTone.Delete,
                        contentDescription = "Delete Transaction",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun CustomerOrdersCard(
    customerMobile: String, viewModel: CustomerViewModel
) {
    val customerOrders by viewModel.customerOrders

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Customer Orders",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    Icons.TwoTone.Receipt,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (customerOrders.isNotEmpty()) {
                customerOrders.take(5).forEach { order ->
                    CompactOrderItem(order)
                }

                if (customerOrders.size > 5) {
                    Text(
                        text = "And ${customerOrders.size - 5} more orders...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = "No orders found",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CompactOrderItem(order: OrderEntity) {
    val subNavController = LocalSubNavController.current
    Card(
        modifier = Modifier
            .bounceClick{
                subNavController.navigate("${SubScreens.OrderItemDetail.route}/${order.orderId}")
            }
            .fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Order #${order.orderId}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = formatDate(order.orderDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatCurrency(order.totalTax+order.totalCharge+order.totalAmount),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Order items",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun KhataBookMonthGrid(
    totalMonths: Int, 
    paidMonths: Set<Int>, 
    onMonthClick: (Int) -> Unit, 
    enabled: Boolean,
    planInfo: KhataBookPlan? = null,
    startDate: Long? = null,
    onClearPayment: ((Int) -> Unit)? = null
) {
    val columns = 8 // Show 8 months per row for more compact layout

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
        modifier = Modifier.heightIn(max = 150.dp)
    ) {
        items(totalMonths) { index ->
            val monthNumber = index + 1
            val isEnabled = enabled && !paidMonths.contains(monthNumber)
            val isPayMonth = planInfo?.let { monthNumber <= it.payMonths } ?: true
            val isRewardMonth = planInfo?.let { monthNumber > it.payMonths } ?: false
            
            MonthBox(
                monthNumber = monthNumber,
                isPaid = paidMonths.contains(monthNumber),
                onClick = { onMonthClick(monthNumber) },
                enabled = isEnabled,
                isPayMonth = isPayMonth,
                isRewardMonth = isRewardMonth,
                startDate = startDate,
                onClearPayment = onClearPayment?.let { { it(monthNumber) } }
            )
        }
    }
}

@Composable
fun MonthBox(
    monthNumber: Int, 
    isPaid: Boolean, 
    onClick: () -> Unit, 
    enabled: Boolean,
    isPayMonth: Boolean = true,
    isRewardMonth: Boolean = false,
    startDate: Long? = null,
    onClearPayment: (() -> Unit)? = null
) {
    var showTooltip by remember { mutableStateOf(false) }

    // Determine colors based on month type and payment status
    val backgroundColor = when {
        isPaid -> Color(0xFF4CAF50) // Green for paid
        isRewardMonth && enabled -> Color(0xFFFF9800) // Orange for reward months
        isPayMonth && enabled -> Color(0xFF2196F3) // Blue for pay months
        else -> Color(0xFF9E9E9E) // Gray for disabled
    }

    val textColor = if (isPaid) Color.White else Color.Black

    Card(
        modifier = Modifier
            .size(32.dp) // Make it a perfect square
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        if (isPaid) {
                            showTooltip = true
                            delay(2000)
                            showTooltip = false
                        } else if (enabled) {
                            onClick()
                        }
                    },
                    onLongPress = {
                        if (isPaid && onClearPayment != null) {
                            onClearPayment()
                        }
                    }
                )
            }, shape = RoundedCornerShape(4.dp), // Smaller corner radius
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (enabled && !isPaid) 2.dp else 0.dp
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
        ) {
            // Calculate actual month and year
            val (monthText, yearText) = if (startDate != null) {
                val calendar = java.util.Calendar.getInstance()
                calendar.timeInMillis = startDate
                calendar.add(java.util.Calendar.MONTH, monthNumber - 1)
                val month = calendar.get(java.util.Calendar.MONTH) + 1
                val year = calendar.get(java.util.Calendar.YEAR) % 100 // Last 2 digits
                Pair(month.toString(), year.toString())
            } else {
                Pair(monthNumber.toString(), "")
            }
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (yearText.isNotEmpty()) "$monthText\n$yearText" else "$monthText",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = if (yearText.isNotEmpty()) 10.sp else MaterialTheme.typography.labelSmall.fontSize
                    ),
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    lineHeight = 12.sp
                )
            }

            // Payment indicator for paid months
            if (isPaid) {
                Icon(
                    imageVector = Icons.TwoTone.Check,
                    contentDescription = "Paid",
                    modifier = Modifier
                        .size(8.dp) // Smaller icon
                        .align(Alignment.TopEnd)
                        .padding(1.dp), // Smaller padding
                    tint = Color.White
                )
            } else if (isRewardMonth) {
                // Show "R" indicator for reward months
                Text(
                    text = "R",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                )
            }
        }
    }

    // Tooltip for paid months
    if (showTooltip && isPaid) {
        val monthType = if (isRewardMonth) "Reward" else "Pay"
        Card(
            modifier = Modifier
                .padding(4.dp)
                .zIndex(1f),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f)),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                text = "Month $monthNumber - $monthType (Paid)",
                modifier = Modifier.padding(6.dp),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )
        }
    }
}

@Composable
fun TransactionHistoryCard(
    customerMobile: String,
    viewModel: CustomerViewModel,
    onTransactionLongClick: (List<String>) -> Unit
) {
    val transactionHistory by viewModel.transactionHistory

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transaction History",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Icon(Icons.TwoTone.History, contentDescription = null, modifier = Modifier.size(20.dp))
            }

            if (transactionHistory.isNotEmpty()) {
                // Prepare data for TextListView
                val headers = listOf("Date", "Type", "Description", "Amount", "Status")
                val transactionData = transactionHistory.map { transaction ->
                    listOf(
                        formatDate(transaction.date),
                        transaction.transactionType.uppercase(),
                        transaction.title,
                        formatCurrency(transaction.amount),
                        if (transaction.isOutstanding) "Outstanding" else "Paid"
                    )
                }

                TextListView(
                    headerList = headers,
                    items = transactionData,
                    modifier = Modifier.height(300.dp),
                    onItemClick = { selectedTransaction ->
                        // Handle transaction click if needed
                    },
                    onItemLongClick = { selectedTransaction ->
                        onTransactionLongClick(selectedTransaction)
                    }
                )
            } else {
                Text(
                    text = "No transactions found",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun MonthPaymentDialog(
    month: Int,
    khataBooks: List<CustomerKhataBookEntity>,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
    isLoading: Boolean = false
) {
    var amount by remember { mutableStateOf(khataBooks.firstOrNull()?.monthlyAmount?.toString() ?: "") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(onDismissRequest = onDismiss, title = {
        Text(
            text = "Add Payment for Month $month", style = MaterialTheme.typography.titleMedium
        )
    }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            khataBooks.firstOrNull()?.let { kb ->
                // Plan information
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Plan Details",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Monthly Amount: ${formatCurrency(kb.monthlyAmount)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Due Date: ${formatDate(getMonthDueDate(kb, month))}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            CusOutlinedTextField(
                state = InputFieldState(amount),
                onTextChange = { amount = it },
                placeholderText = "Payment Amount",
                keyboardType = KeyboardType.Number
            )

            CusOutlinedTextField(
                state = InputFieldState(notes),
                onTextChange = { notes = it },
                placeholderText = "Notes (Optional)"
            )

            // Payment date info
            Text(
                text = "Payment will be recorded with today's date",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }, confirmButton = {
        Button(
            onClick = { onConfirm(amount, notes) },
            enabled = amount.isNotEmpty() && amount.toDoubleOrNull() != null && amount.toDoubleOrNull()!! > 0 && !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Green)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(
                    Icons.TwoTone.Payment,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Payment")
            }
        }
    }, dismissButton = {
        TextButton(onClick = onDismiss) {
            Text("Cancel")
        }
    })
}

private fun getMonthDueDate(
    khataBook: CustomerKhataBookEntity, monthNumber: Int
): java.sql.Timestamp {
    val monthInMillis = (monthNumber - 1) * 30L * 24 * 60 * 60 * 1000
    return java.sql.Timestamp(khataBook.startDate.time + monthInMillis)
}

@Composable
fun CompletedPlansCard(
    completedPlans: List<CustomerKhataBookEntity>,
    transactions: List<CustomerTransactionEntity>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Completed Plans (${completedPlans.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    Icons.TwoTone.CheckCircle,
                    contentDescription = "Completed Plans",
                    modifier = Modifier.size(20.dp),
                    tint = Color.Blue
                )
            }

            // Show completed plans
            completedPlans.forEach { plan ->
                CompletedPlanItem(
                    plan = plan,
                    transactions = transactions
                )
            }
        }
    }
}

@Composable
fun CompletedPlanItem(
    plan: CustomerKhataBookEntity,
    transactions: List<CustomerTransactionEntity>
) {
    val planPayments = transactions.filter { 
        it.khataBookId == plan.khataBookId && it.transactionType == "khata_payment" 
    }
    val totalPaid = planPayments.sumOf { it.amount }
    val planInfo = getPlanInfo(plan.planName)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Plan header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = plan.planName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Completed",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Blue,
                    fontWeight = FontWeight.Bold
                )
            }

            // Plan details
            if (planInfo != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Pay Months",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${planInfo.payMonths} months",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Blue
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Reward Months",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${planInfo.benefitMonths} months",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Total Paid",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatCurrency(totalPaid),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Green
                        )
                    }
                }
            }

            // Completion date
            Text(
                text = "Completed: ${formatDate(plan.endDate)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CompletePlanConfirmationDialog(
    khataBook: CustomerKhataBookEntity,
    transactions: List<CustomerTransactionEntity>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    isLoading: Boolean = false
) {
    val planPayments = transactions.filter { 
        it.khataBookId == khataBook.khataBookId && it.transactionType == "khata_payment" 
    }
    val paidMonths = planPayments.mapNotNull { it.monthNumber }.toSet()
    val totalPaid = planPayments.sumOf { it.amount }
    val planInfo = getPlanInfo(khataBook.planName)
    val requiredPayMonths = planInfo?.payMonths ?: khataBook.totalMonths
    val completedPayMonths = paidMonths.count { it <= requiredPayMonths }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = "Complete Khata Book Plan",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Plan Summary Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Plan Summary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        // Plan Details
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Plan Name",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = khataBook.planName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Monthly Amount",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatCurrency(khataBook.monthlyAmount),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        // Plan Structure
                        if (planInfo != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Pay Months",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${planInfo.payMonths} months",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Blue
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Reward Months",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${planInfo.benefitMonths} months",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4CAF50)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Total Months",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${planInfo.effectiveMonths} months",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Payment Summary Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Payment Summary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Pay Months Completed",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$completedPayMonths / $requiredPayMonths",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (completedPayMonths >= requiredPayMonths) Color.Green else Color.Red
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Total Paid",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatCurrency(totalPaid),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Green
                                )
                            }
                        }
                        
                        // Progress Bar
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Progress",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${((completedPayMonths.toDouble() / requiredPayMonths) * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            LinearProgressIndicator(
                                progress = { (completedPayMonths.toFloat() / requiredPayMonths) },
                                modifier = Modifier.fillMaxWidth(),
                                color = if (completedPayMonths >= requiredPayMonths) Color.Green else Color(0xFFFF9800)
                            )
                        }
                    }
                }
                
                // Completion Message
                if (completedPayMonths >= requiredPayMonths) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.TwoTone.CheckCircle,
                                contentDescription = "Ready to Complete",
                                tint = Color.Green,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "✅ All pay months completed! Customer is now eligible for reward months.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = Color.Green
                            )
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.TwoTone.CheckCircle,
                                contentDescription = "Not Ready",
                                tint = Color.Red,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "❌ Cannot complete plan. $completedPayMonths out of $requiredPayMonths pay months completed.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = Color.Red
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = completedPayMonths >= requiredPayMonths && !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        Icons.TwoTone.Check,
                        contentDescription = "Complete Plan",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Complete Plan")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DeleteTransactionConfirmationDialog(
    transaction: CustomerTransactionEntity,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Delete Transaction",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Are you sure you want to delete this transaction?",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                // Transaction details
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Type:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = TransactionUtils.getTransactionTypeDisplayName(transaction.transactionType),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Amount:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatCurrency(transaction.amount),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = if (transaction.transactionType == "debit") Color.Red else Color.Green
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Date:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatDate(transaction.transactionDate),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        if (!transaction.description.isNullOrEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Description:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = transaction.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }
                }
                
                Text(
                    text = "This action cannot be undone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Icon(
                    Icons.TwoTone.Delete,
                    contentDescription = "Delete",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

@Composable
fun TransactionHistoryDeleteDialog(
    transactionData: List<String>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Delete Transaction",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Are you sure you want to delete this transaction?",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                if (transactionData.size >= 4) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Date:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = transactionData[0],
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Type:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = transactionData[1],
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Description:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = transactionData[2],
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.End
                                )
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Amount:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = transactionData[3],
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
                
                Text(
                    text = "This action cannot be undone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
