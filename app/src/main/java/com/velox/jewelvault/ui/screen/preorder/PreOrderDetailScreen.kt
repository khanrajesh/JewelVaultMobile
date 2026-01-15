package com.velox.jewelvault.ui.screen.preorder

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.velox.jewelvault.ui.components.CusOutlinedTextField
import com.velox.jewelvault.ui.components.InputFieldState
import com.velox.jewelvault.ui.components.TextListView
import com.velox.jewelvault.ui.nav.SubScreens
import com.velox.jewelvault.utils.LocalSubNavController
import com.velox.jewelvault.utils.to3FString
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun PreOrderDetailScreen(viewModel: PreOrderViewModel, preOrderId: String) {
    viewModel.currentScreenHeadingState.value = "Pre-Order"
    val subNavController = LocalSubNavController.current

    BackHandler {
        subNavController.popBackStack()
    }

    val preOrderState = viewModel.observePreOrder(preOrderId).collectAsStateWithLifecycle(initialValue = null)
    val itemState = viewModel.observePreOrderItems(preOrderId).collectAsStateWithLifecycle(initialValue = emptyList())
    val paymentState = viewModel.observePreOrderPayments(preOrderId).collectAsStateWithLifecycle(initialValue = emptyList())

    val preOrder = preOrderState.value
    val items = itemState.value
    val payments = paymentState.value

    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    val showAddPaymentDialog = remember { mutableStateOf(false) }
    val showDeleteDialog = remember { mutableStateOf(false) }

    val statusField = remember { InputFieldState() }
    LaunchedEffect(preOrder?.status) {
        statusField.text = preOrder?.status ?: ""
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(topStart = 18.dp))
            .padding(10.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (preOrder == null) {
                Text("Loading...", fontWeight = FontWeight.Bold)
                return@Column
            }

            Text("Summary", fontWeight = FontWeight.Bold)

            Text("Pre-Order ID: ${preOrder.preOrderId}")
            Text("Customer: ${preOrder.customerMobile}")
            Text("Order Date: ${dateFormatter.format(preOrder.orderDate)}")
            Text("Delivery Date: ${dateFormatter.format(preOrder.deliveryDate)}")

            val statuses = listOf("DRAFT", "CONFIRMED", "READY", "DELIVERED", "CANCELLED")
            CusOutlinedTextField(
                state = statusField,
                placeholderText = "Status",
                dropdownItems = statuses,
                onDropdownItemSelected = { selected ->
                    viewModel.updatePreOrderStatus(
                        preOrderId = preOrderId,
                        status = selected,
                        onFailure = { viewModel.snackBarState.value = it }
                    )
                },
                singleLine = true
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(modifier = Modifier.weight(1f), onClick = { showAddPaymentDialog.value = true }) {
                    Text("Add Payment")
                }
                Button(modifier = Modifier.weight(1f), onClick = { showDeleteDialog.value = true }) {
                    Text("Delete")
                }
            }

            Text("Items", fontWeight = FontWeight.Bold)

            val itemHeaders = listOf("Category", "Qty", "Est Wt (g)", "Est Price", "Extra", "Note")
            val itemRows = items.map { item ->
                listOf(
                    item.catName,
                    item.quantity.toString(),
                    item.estimatedGrossWt.to3FString(),
                    item.estimatedPrice.to3FString(),
                    if (item.addDesKey.isNotBlank() || item.addDesValue.isNotBlank()) "${item.addDesKey}: ${item.addDesValue}" else "",
                    item.note ?: ""
                )
            }
            TextListView(
                headerList = itemHeaders,
                items = itemRows,
                modifier = Modifier.fillMaxWidth().height(250.dp),
                maxColumnWidth = 220.dp,
                onItemClick = {},
                onItemLongClick = {}
            )

            Text("Payments", fontWeight = FontWeight.Bold)
            val paymentHeaders = listOf("Date", "Type", "Amount", "Method", "Ref", "Notes")
            val paymentRows = payments.map { t ->
                listOf(
                    dateFormatter.format(t.transactionDate),
                    t.transactionType,
                    t.amount.to3FString(),
                    t.paymentMethod ?: "",
                    t.referenceNumber ?: "",
                    t.notes ?: ""
                )
            }
            TextListView(
                headerList = paymentHeaders,
                items = paymentRows,
                modifier = Modifier.fillMaxWidth().height(250.dp),
                maxColumnWidth = 220.dp,
                onItemClick = {},
                onItemLongClick = {}
            )

            Spacer(Modifier.height(20.dp))
        }
    }

    if (showAddPaymentDialog.value) {
        val amountState = remember { InputFieldState() }
        val methodState = remember { InputFieldState(initValue = "Cash") }
        val refState = remember { InputFieldState() }
        val notesState = remember { InputFieldState() }

        AlertDialog(
            onDismissRequest = { showAddPaymentDialog.value = false },
            title = { Text("Add Payment") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    CusOutlinedTextField(
                        state = amountState,
                        placeholderText = "Amount",
                        keyboardType = KeyboardType.Number,
                        singleLine = true
                    )
                    CusOutlinedTextField(
                        state = methodState,
                        placeholderText = "Method",
                        dropdownItems = listOf("Cash", "UPI", "Bank", "Card"),
                        singleLine = true
                    )
                    CusOutlinedTextField(
                        state = refState,
                        placeholderText = "Reference (optional)",
                        singleLine = true
                    )
                    CusOutlinedTextField(
                        state = notesState,
                        placeholderText = "Notes (optional)",
                        maxLines = 2
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val amount = amountState.text.toDoubleOrNull() ?: 0.0
                        viewModel.addAdvancePayment(
                            preOrderId = preOrderId,
                            customerMobile = preOrderState.value?.customerMobile ?: "",
                            amount = amount,
                            paymentMethod = methodState.text,
                            referenceNumber = refState.text,
                            notes = notesState.text,
                            onSuccess = {
                                showAddPaymentDialog.value = false
                                viewModel.snackBarState.value = "Payment added"
                            },
                            onFailure = { viewModel.snackBarState.value = it }
                        )
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPaymentDialog.value = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteDialog.value) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog.value = false },
            title = { Text("Delete Pre-Order") },
            text = { Text("Delete this pre-order and its linked payments? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePreOrder(
                            preOrderId = preOrderId,
                            onSuccess = {
                                showDeleteDialog.value = false
                                viewModel.snackBarState.value = "Pre-order deleted"
                                subNavController.navigate(SubScreens.OrderAndPurchase.route) {
                                    popUpTo(SubScreens.Dashboard.route) { inclusive = false }
                                    launchSingleTop = true
                                }
                            },
                            onFailure = {
                                showDeleteDialog.value = false
                                viewModel.snackBarState.value = it
                            }
                        )
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog.value = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

