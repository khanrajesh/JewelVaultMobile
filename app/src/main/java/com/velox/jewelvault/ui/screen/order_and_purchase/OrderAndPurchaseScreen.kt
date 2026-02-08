package com.velox.jewelvault.ui.screen.order_and_purchase

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.velox.jewelvault.ui.components.TextListView
import com.velox.jewelvault.ui.components.baseBackground0
import com.velox.jewelvault.ui.nav.SubScreens
import com.velox.jewelvault.utils.CalculationUtils
import com.velox.jewelvault.utils.LocalBaseViewModel
import com.velox.jewelvault.utils.LocalSubNavController
import com.velox.jewelvault.utils.SortOrder
import com.velox.jewelvault.utils.to3FString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

@Composable
fun OrderAndPurchaseScreen(viewModel: OrderAndReportViewModel) {
    viewModel.currentScreenHeadingState.value = "Order & Purchase"
    val subNavController = LocalSubNavController.current
    val baseViewModel = LocalBaseViewModel.current

    // State for delete dialog
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedItemId by remember { mutableStateOf("") }
    var selectedItemType by remember { mutableStateOf("") } // "order" or "purchase"

    BackHandler {
        subNavController.navigate(SubScreens.Dashboard.route) {
            popUpTo(SubScreens.Dashboard.route) {
                inclusive = true
            }
        }
    }

    val tabs = listOf("Order", "Purchase")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .baseBackground0()
            .padding(5.dp)
    ) {
        TabRow(modifier = Modifier, selectedTabIndex = viewModel.selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = viewModel.selectedTabIndex == index,
                    onClick = { viewModel.selectedTabIndex = index },
                    text = { Text(title) })
            }
        }

        when (viewModel.selectedTabIndex) {
            0 -> OrderDetail(
                viewModel = viewModel,
                onItemLongClick = { itemId ->
                    selectedItemId = itemId
                    selectedItemType = "order"
                    showDeleteDialog = true
                }
            )
            1 -> PurchaseDetails(
                viewModel = viewModel,
                onItemLongClick = { itemId ->
                    selectedItemId = itemId
                    selectedItemType = "purchase"
                    showDeleteDialog = true
                }
            )
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        val title = if (selectedItemType == "order") "Delete Order" else "Delete Purchase"
        val message = if (selectedItemType == "order") 
            "Are you sure you want to delete this order and all its associated items? This action cannot be undone."
        else 
            "Are you sure you want to delete this purchase and all its associated items? This action cannot be undone."

        DeleteConfirmationDialog(
            title = title,
            message = message,
            onDismiss = { 
                showDeleteDialog = false
                selectedItemId = ""
                selectedItemType = ""
            },
            onConfirm = {
                if (selectedItemType == "order") {
                    viewModel.deleteOrderWithItems(
                        orderId = selectedItemId,
                        onSuccess = {
                            baseViewModel.snackBarState = "Order deleted successfully"
                            showDeleteDialog = false
                            selectedItemId = ""
                            selectedItemType = ""
                        },
                        onFailure = { error ->
                            baseViewModel.snackBarState = error
                            showDeleteDialog = false
                            selectedItemId = ""
                            selectedItemType = ""
                        }
                    )
                } else {
                    viewModel.deletePurchaseWithItems(
                        purchaseOrderId = selectedItemId,
                        onSuccess = {
                            baseViewModel.snackBarState = "Purchase deleted successfully"
                            showDeleteDialog = false
                            selectedItemId = ""
                            selectedItemType = ""
                        },
                        onFailure = { error ->
                            baseViewModel.snackBarState = error
                            showDeleteDialog = false
                            selectedItemId = ""
                            selectedItemType = ""
                        }
                    )
                }
            }
        )
    }
}


@Composable
private fun PurchaseDetails(
    viewModel: OrderAndReportViewModel,
    onItemLongClick: (String) -> Unit
) {
    val baseViewModel = LocalBaseViewModel.current
    val subNavController = LocalSubNavController.current

    LaunchedEffect(true) {
        CoroutineScope(Dispatchers.IO).async {
            viewModel.getAllPurchaseSorted(SortOrder.DESCENDING)
        }.await()
    }

    val purchaseItems = viewModel.purchase.mapIndexed { index, item ->
        val itemDetails = item.items.joinToString("\n") {
            "${it.catName}-${it.subCatName} ${it.gsWt}-${it.purity} ${it.fnWt}gm (${it.wastagePercent}) rate: ₹${it.fnRate}"
        }

        val r =
            "Items: ${item.items.size} Gst: ${item.order.cgstPercent + item.order.sgstPercent + item.order.igstPercent} %" + "\n$itemDetails"

        listOf(
            "${index + 1}",
            "${item.order.purchaseOrderId}",
            "${item.order.billDate}",
            "${item.order.billNo} ",  //firm name
            "${item.seller?.name} \n(${item.seller?.mobileNumber})",
            "${item.order.billNo} ",
            r,
            "Gold: ${
                CalculationUtils.calculateTotalExchangeWeightByMetal(
                    item.exchanges,
                    "Gold"
                )
            }0 gm" + "\nSilver: ${
                CalculationUtils.calculateTotalExchangeWeightByMetal(
                    item.exchanges,
                    "Silver"
                )
            }0 gm ",
        )
    }

    TextListView(
        headerList = viewModel.purchaseHeaderList,
        items = purchaseItems,
        modifier = Modifier.fillMaxSize(),
        maxColumnWidth = 450.dp, // Larger width for detailed purchase information
        onItemClick = { item ->
            // Get the purchase order ID from the second column (index 1)
            val purchaseOrderId = item[1]
            subNavController.navigate("${SubScreens.PurchaseItemDetail.route}/$purchaseOrderId")
        },
        onItemLongClick = { item ->
            // Get the purchase order ID from the second column (index 1)
            val purchaseOrderId = item[1]
            onItemLongClick(purchaseOrderId)
        })
}


@Composable
private fun OrderDetail(
    viewModel: OrderAndReportViewModel,
    onItemLongClick: (String) -> Unit
) {
    val subNavController = LocalSubNavController.current

    LaunchedEffect(true) {
        CoroutineScope(Dispatchers.IO).async {
            viewModel.getAllOrdersSorted(SortOrder.DESCENDING)
        }.await()
    }

    val orderItems = viewModel.orderList.mapIndexed { index, item ->
        //"S.No", "Order Id","Order Date","Customer Name","Customer No", "Total Amount", "Total Charge"
        listOf(
            "${index + 1}",
            "${item.orderId}",
            "${item.orderDate}",
            "${item.customerMobile} ",
            "${item.customerMobile}",
            "₹${item.totalAmount.to3FString()} ",
            "₹${item.totalCharge.to3FString()} ",
            "₹${item.totalTax.to3FString()} ",
            "₹${
               (item.totalAmount+ item.totalCharge+ 0.0+ item.totalTax)
                    .to3FString()
            }"
        )
    }

    TextListView(
        headerList = viewModel.orderHeaderList,
        items = orderItems,
        modifier = Modifier.fillMaxSize(),
        maxColumnWidth = 200.dp, // Standard width for order information
        onItemClick = { item ->
            // Get the order ID from the second column (index 1)
            val orderId = item[1]
            subNavController.navigate("${SubScreens.OrderItemDetail.route}/$orderId")
        },
        onItemLongClick = { item ->
            // Get the order ID from the second column (index 1)
            val orderId = item[1]
            onItemLongClick(orderId)
        })
}

@Composable
fun DeleteConfirmationDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = Color.Red
                )
            ) {
                Text("Delete", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
