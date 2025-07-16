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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.velox.jewelvault.data.roomdb.entity.CustomerEntity
import com.velox.jewelvault.data.roomdb.entity.CustomerKhataBookEntity
import com.velox.jewelvault.data.roomdb.entity.CustomerTransactionEntity
import com.velox.jewelvault.ui.components.CusOutlinedTextField
import com.velox.jewelvault.ui.components.InputFieldState
import com.velox.jewelvault.utils.formatCurrency
import com.velox.jewelvault.utils.formatDate
import kotlinx.coroutines.delay

@Composable
fun CustomerDetailScreen(
    customerMobile: String, viewModel: CustomerViewModel = hiltViewModel()
) {
    val customer by viewModel.selectedCustomer
    val isLoadingDetails by viewModel.isLoadingCustomerDetails
    val isLoadingKhataBook by viewModel.isLoadingKhataBook
    val isLoadingPayment by viewModel.isLoadingPayment

    var showAddOutstandingDialog by remember { mutableStateOf(false) }
    var showAddKhataBookDialog by remember { mutableStateOf(false) }
    var showAddKhataPaymentDialog by remember { mutableStateOf(false) }
    var showAddRegularPaymentDialog by remember { mutableStateOf(false) }
    var showKhataBookDetails by remember { mutableStateOf(false) }
    var showMonthPaymentDialog by remember { mutableStateOf(false) }
    var selectedMonth by remember { mutableStateOf(0) }

    LaunchedEffect(customerMobile) {
        viewModel.loadCustomerDetails(customerMobile)
    }

    // Refresh data when screen is focused
    LaunchedEffect(Unit) {
        viewModel.loadCustomerDetails(customerMobile)
    }

    // Clear data when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearSelectedCustomerData()
        }
    }

    // Auto-dismiss dialogs when operations complete
    LaunchedEffect(isLoadingKhataBook) {
        if (!isLoadingKhataBook && showAddKhataBookDialog) {
            showAddKhataBookDialog = false
        }
    }

    LaunchedEffect(isLoadingPayment) {
        if (!isLoadingPayment) {
            if (showAddOutstandingDialog) showAddOutstandingDialog = false
            if (showAddKhataPaymentDialog) showAddKhataPaymentDialog = false
            if (showAddRegularPaymentDialog) showAddRegularPaymentDialog = false
            if (showMonthPaymentDialog) showMonthPaymentDialog = false
        }
    }

    if (isLoadingDetails) {
        Box(
            modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Customer Information Card (Compact)
            item {
                CompactCustomerInfoCard(customer)
            }

            // Khata Book Card (Second Position)
            item {
                KhataBookCard(
                    khataBooks = viewModel.selectedCustomerKhataBooks,
                    transactions = viewModel.selectedCustomerTransactions,
                    onAddKhataBook = { showAddKhataBookDialog = true },
                    onAddPayment = { showAddKhataPaymentDialog = true },
                    onViewDetails = { showKhataBookDetails = true },
                    onToggleStatus = { status ->
                        // For now, update the first active khata book
                        viewModel.selectedCustomerKhataBooks.firstOrNull()?.let { 
                            viewModel.updateKhataBookStatus(it.khataBookId, status) 
                        }
                    },
                    onMonthClick = { month ->
                        selectedMonth = month
                        showMonthPaymentDialog = true
                    })
            }

            // Combined Outstanding & Payments Card
            item {
                CombinedPaymentsCard(transactions = viewModel.selectedCustomerTransactions,
                    onAddOutstanding = { showAddOutstandingDialog = true },
                    onAddPayment = { showAddRegularPaymentDialog = true })
            }

            // Customer Orders Card
            item {
                CustomerOrdersCard(
                    customerMobile = customerMobile, viewModel = viewModel
                )
            }

            item {
                TransactionHistoryCard(
                    customerMobile = customerMobile, viewModel = viewModel
                )
            }
        }
    }

    // Dialogs
    if (showAddOutstandingDialog) {
        AddOutstandingDialog(onDismiss = { showAddOutstandingDialog = false },
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

    if (showAddKhataBookDialog) {
        AddKhataBookDialog(onDismiss = { showAddKhataBookDialog = false },
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

    if (showAddKhataPaymentDialog) {
        AddKhataPaymentDialog(
            khataBooks = viewModel.selectedCustomerKhataBooks,
            onDismiss = { showAddKhataPaymentDialog = false },
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

    if (showAddRegularPaymentDialog) {
        AddRegularPaymentDialog(onDismiss = { showAddRegularPaymentDialog = false },
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

    if (showKhataBookDetails) {
        KhataBookDetailsDialog(
            khataBooks = viewModel.selectedCustomerKhataBooks,
            transactions = viewModel.selectedCustomerTransactions,
            onDismiss = { showKhataBookDetails = false })
    }

    if (showMonthPaymentDialog) {
        MonthPaymentDialog(
            month = selectedMonth, 
            khataBooks = viewModel.selectedCustomerKhataBooks, 
            onDismiss = {
                showMonthPaymentDialog = false
            }, 
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
}


@Composable
fun CompactCustomerInfoCard(customer: CustomerEntity?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Customer Information",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            customer?.let { c ->
                // First row: Name and Mobile
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CompactInfoItem("Name", c.name, Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(8.dp))
                    CompactInfoItem("Mobile", c.mobileNo, Modifier.weight(1f))
                }

                // Second row: Address and GSTIN/PAN
                c.address?.let { address ->
                    CompactInfoItem("Address", address, Modifier.fillMaxWidth())
                }
                c.gstin_pan?.let { gstin ->
                    CompactInfoItem("GSTIN/PAN", gstin, Modifier.fillMaxWidth())
                }

                // Third row: Date and Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CompactInfoItem("Added", formatDate(c.addDate), Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(8.dp))
                    CompactInfoItem(
                        "Status", if (c.isActive) "Active" else "Inactive", Modifier.weight(1f)
                    )
                }

                // Fourth row: Items and Amount
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CompactInfoItem("Items", c.totalItemBought.toString(), Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(8.dp))
                    CompactInfoItem("Total", formatCurrency(c.totalAmount), Modifier.weight(1f))
                }

                // Notes if available
                c.notes?.let { notes ->
                    CompactInfoItem("Notes", notes, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
fun CompactInfoItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            maxLines = 2
        )
    }
}

@Composable
fun CombinedPaymentsCard(
    transactions: List<CustomerTransactionEntity>,
    onAddOutstanding: () -> Unit,
    onAddPayment: () -> Unit
) {
    val outstandingTransactions = transactions.filter { 
        it.category == "outstanding" && it.isDebit 
    }
    val paymentTransactions = transactions.filter { 
        it.category == "outstanding" && it.isCredit 
    }
    val regularPayments = transactions.filter { 
        it.category == "regular_payment" 
    }
    
    val totalOutstanding = outstandingTransactions.sumOf { it.amount }
    val totalPayments = paymentTransactions.sumOf { it.amount } + regularPayments.sumOf { it.amount }
    val remainingBalance = totalOutstanding - totalPayments

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
                        text = formatCurrency(totalOutstanding),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (totalOutstanding > 0) Color.Red else Color.Green
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
                            Icons.Default.Add,
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
                            Icons.Default.Payment,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Payment", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Recent Transactions
            if (transactions.isNotEmpty()) {
                Text(
                    text = "Recent Activity",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )

                val recentTransactions = transactions
                    .map { TransactionItem.fromTransaction(it) }
                    .sortedByDescending { it.date }
                    .take(3)

                recentTransactions.forEach { transaction ->
                    CompactTransactionItem(transaction)
                }
            }
        }
    }
}

@Composable
fun CompactTransactionItem(transaction: TransactionItem) {
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
                    text = transaction.title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = formatDate(transaction.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = formatCurrency(transaction.amount),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = if (transaction.isOutstanding) Color.Red else Color.Green
            )
        }
    }
}

data class TransactionItem(
    val title: String, 
    val amount: Double, 
    val date: java.sql.Timestamp, 
    val isOutstanding: Boolean,
    val transactionType: String,
    val category: String
) {
    companion object {
        fun fromTransaction(transaction: CustomerTransactionEntity): TransactionItem {
            val title = when (transaction.transactionType) {
                "debit" -> "Debit - ${transaction.description ?: "No description"}"
                "credit" -> "Credit - ${transaction.paymentMethod ?: "Payment"}"
                "khata_payment" -> "Khata Payment - Month ${transaction.monthNumber}"
                "khata_debit" -> "Khata Debit - ${transaction.notes ?: "Khata Book"}"
                else -> "${transaction.transactionType.capitalize()} - ${transaction.description ?: "Transaction"}"
            }
            
            return TransactionItem(
                title = title,
                amount = transaction.amount,
                date = transaction.transactionDate,
                isOutstanding = transaction.isDebit,
                transactionType = transaction.transactionType,
                category = transaction.category
            )
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
    onMonthClick: (Int) -> Unit
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
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Create Plan", style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        IconButton(
                            onClick = onViewDetails, modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Visibility,
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
                            text = "${String.format("%.1f", progressPercentage)}%",
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
                        onMonthClick = onMonthClick
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
    onMonthClick: (Int) -> Unit
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
                Text(
                    text = khataBook.status.capitalize(),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (khataBook.status == "active") Color.Green else Color.Gray
                )
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

            // Month grid
            KhataBookMonthGrid(
                totalMonths = khataBook.totalMonths,
                paidMonths = khataPayments.mapNotNull { it.monthNumber }.toSet(),
                onMonthClick = onMonthClick,
                enabled = khataBook.status == "active"
            )
        }
    }
}

@Composable
fun CustomerInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
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
    onDismiss: () -> Unit, 
    onConfirm: (String, String, String, String) -> Unit, 
    isLoading: Boolean = false
) {
    var monthlyAmount by remember { mutableStateOf("") }
    var totalMonths by remember { mutableStateOf("12") }
    var planName by remember { mutableStateOf("Standard Plan") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(onDismissRequest = onDismiss, title = { Text("Create Khata Book Plan") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CusOutlinedTextField(
                state = InputFieldState(planName),
                onTextChange = { planName = it },
                placeholderText = "Plan Name"
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
                keyboardType = KeyboardType.Number
            )

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

    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add Khata Payment") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
    onDismiss: () -> Unit
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
                                    Text("Total Months", style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        kb.totalMonths.toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Total Amount", style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        formatCurrency(totalAmount),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
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
                                        "${String.format("%.1f", progressPercentage)}%",
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
                                PaymentHistoryItem(payment)
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
fun PaymentHistoryItem(payment: CustomerTransactionEntity) {
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
                    text = "Month ${payment.monthNumber}",
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
            Text(
                text = formatCurrency(payment.amount),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Green
            )
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
                    Icons.Default.Receipt,
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
fun CompactOrderItem(order: com.velox.jewelvault.data.roomdb.entity.order.OrderEntity) {
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
                    text = formatCurrency(order.totalAmount),
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
    totalMonths: Int, paidMonths: Set<Int>, onMonthClick: (Int) -> Unit, enabled: Boolean
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
            MonthBox(
                monthNumber = monthNumber,
                isPaid = paidMonths.contains(monthNumber),
                onClick = { onMonthClick(monthNumber) },
                enabled = isEnabled
            )
        }
    }
}

@Composable
fun MonthBox(
    monthNumber: Int, isPaid: Boolean, onClick: () -> Unit, enabled: Boolean
) {
    var showTooltip by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .size(32.dp) // Make it a perfect square
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    if (isPaid) {
                        showTooltip = true
                        delay(2000)
                        showTooltip = false
                    } else if (enabled) {
                        onClick()
                    }
                })
            }, shape = RoundedCornerShape(4.dp), // Smaller corner radius
        colors = CardDefaults.cardColors(
            containerColor = if (isPaid) Color(0xFF4CAF50) else if (enabled) Color(0xFFBDBDBD) else Color(
                0xFF9E9E9E
            )
        ), elevation = CardDefaults.cardElevation(
            defaultElevation = if (enabled && !isPaid) 2.dp else 0.dp
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
        ) {
            Text(
                text = monthNumber.toString(),
                style = MaterialTheme.typography.labelSmall, // Smaller text
                fontWeight = FontWeight.Bold,
                color = if (isPaid) Color.White else Color.Black,
                // Accessibility
                modifier = Modifier
                    .then(if (isPaid) Modifier else Modifier)
                    .semantics { contentDescription = "Month $monthNumber" }
            )

            // Payment indicator for paid months
            if (isPaid) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Paid",
                    modifier = Modifier
                        .size(8.dp) // Smaller icon
                        .align(Alignment.TopEnd)
                        .padding(1.dp), // Smaller padding
                    tint = Color.White
                )
            }
        }
    }

    // Tooltip for paid months
    if (showTooltip && isPaid) {
        Card(
            modifier = Modifier
                .padding(4.dp)
                .zIndex(1f),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f)),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                text = "Month $monthNumber - Paid",
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
    viewModel: CustomerViewModel
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
                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(20.dp))
            }

            if (transactionHistory.isNotEmpty()) {
                transactionHistory.take(5).forEach { transaction ->
                   CompactTransactionItem(transaction)
                }

                if (transactionHistory.size > 5) {
                    Text(
                        text = "And ${transactionHistory.size - 5} more transactions...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
                    Icons.Default.Payment,
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

private fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}