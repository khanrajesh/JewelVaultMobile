package com.velox.jewelvault.ui.screen.order_and_purchase.purchase_item

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.velox.jewelvault.ui.components.TextListView
import com.velox.jewelvault.utils.LocalSubNavController
import com.velox.jewelvault.utils.VaultPreview
import com.velox.jewelvault.utils.export.enqueueExportWorker
import com.velox.jewelvault.utils.mainScope
import com.velox.jewelvault.utils.to3FString

@Composable
@VaultPreview
fun PreviewPurchaseItemDetailScreen() {
    PurchaseItemDetailScreen(hiltViewModel(), "1")
}

@Composable
fun PurchaseItemDetailScreen(viewModel: PurchaseItemViewModel, purchaseOrderId: String) {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val subNavigation = LocalSubNavController.current
    
    // State for dropdown menu
    var showDropdownMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    viewModel.currentScreenHeadingState.value = "Purchase Order Details"

    LaunchedEffect(true) {
        viewModel.getPurchaseOrderById(purchaseOrderId)
        viewModel.getInventoryItemByPurchaseOrderID(purchaseOrderId)
        viewModel.getOrderItemsByPurchaseOrderId(purchaseOrderId)
    }

    val isItemsExpanded = remember { mutableStateOf(true) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Purchase Order Summary Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                )
                .padding(8.dp)
        ) {

            viewModel.purchaseOrderWithDetails.value?.let { pur ->

                Row() {
                    // Order Details
                    Column(
                        modifier = Modifier
                    ) {
                        Text(
                            text = "Order Details (${pur.order.purchaseOrderId})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Bill No: ${pur.order.billNo}, Bill Date: ${pur.order.billDate}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Text(
                            text = "Entry Date: ${pur.order.entryDate}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Total Final Weight: ${pur.order.totalFinalWeight?.to3FString() ?: "0.00"} gm, Total Final Amount: ₹${pur.order.totalFinalAmount?.to3FString() ?: "0.00"}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        if (pur.order.extraCharge != null && pur.order.extraCharge > 0) {
                            Text(
                                text = "Extra Charge: ₹${pur.order.extraCharge.to3FString()}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            if (!pur.order.extraChargeDescription.isNullOrEmpty()) {
                                Text(
                                    text = "Description: ${pur.order.extraChargeDescription}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Text(
                            text = "Tax: CGST ${pur.order.cgstPercent}% | SGST ${pur.order.sgstPercent}% | IGST ${pur.order.igstPercent}%",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (!pur.order.notes.isNullOrEmpty()) {
                            Text(
                                text = "Notes: ${pur.order.notes}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(Modifier.weight(1f))
                    

                    
                    viewModel.firmEntity.value?.let { firm ->
                        // Firm and Seller Information
                        Column(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(5.dp)
                        ) {
                            Text(
                                text = "Firm: ${firm.firmName} (${firm.firmMobileNumber})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Firm ID: ${firm.firmId}", fontSize = 14.sp
                            )
                            Text(
                                text = "Address: ${firm.address}", fontSize = 14.sp
                            )
                            Text(
                                text = "GST: ${firm.gstNumber}", fontSize = 14.sp
                            )

                            pur.seller?.let { sel ->
                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Seller: ${sel.name} (${sel.mobileNumber})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }

                    // More options dropdown
                    Box {
                        IconButton(
                            onClick = { showDropdownMenu = true },
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "More options",
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showDropdownMenu,
                            onDismissRequest = { showDropdownMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = null,
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                        Text("Delete Purchase")
                                    }
                                },
                                onClick = {
                                    showDropdownMenu = false
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Items and Exchanges Section
        val isItemsAndExchangesExpanded = remember { mutableStateOf(true) }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                )
        ) {
            // Header with expand/collapse
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(
                    text = "Items and Exchanges",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.weight(1f))

                Icon(
                    imageVector = if (isItemsAndExchangesExpanded.value) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Toggle Items and Exchanges",
                    modifier = Modifier.clickable {
                        isItemsAndExchangesExpanded.value = !isItemsAndExchangesExpanded.value
                    })
            }

            // Items and Exchanges Content
            if (isItemsAndExchangesExpanded.value) {
                viewModel.purchaseOrderWithDetails.value?.let { pur ->
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        // Items Section
                        if (pur.items.isNotEmpty()) {
                            Text(
                                text = "Items (${pur.items.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            pur.items.forEachIndexed { index, item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = "${index + 1}. ${item.catName} - ${item.subCatName}",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = " | ID: ${item.purchaseItemId} | Gs Wt: ${item.gsWt} | Nt Wt: ${item.ntWt} | Fn Wt: ${item.fnWt} | Purity: ${item.purity} | Wastage: ${item.wastagePercent}% | Rate: ${item.fnRate}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (index < pur.items.size - 1) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Exchange Section
                        if (pur.exchanges.isNotEmpty()) {
                            Text(
                                text = "Exchanges (${pur.exchanges.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            pur.exchanges.forEachIndexed { index, exchange ->

                                Text(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(12.dp),
                                    text = "${index + 1}. ${exchange.catName} | Fine Weight: ${exchange.fnWeight.to3FString()} gm",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )

                                if (index < pur.exchanges.size - 1) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Items in Category Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                )
        ) {
            // Header with expand/collapse
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(
                    text = "Items in Category",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "Export", modifier = Modifier.clickable {
                        mainScope {
                            if (viewModel.itemList.isNotEmpty()) {
                                val billNo = viewModel.purchaseOrderWithDetails.value?.order?.billNo
                                    ?: "Bill_No"
                                val fileName =
                                    "ItemExport_${billNo}_${System.currentTimeMillis()}.xlsx"
                                enqueueExportWorker(
                                    context,
                                    lifecycleOwner,
                                    fileName,
                                    viewModel.itemHeaderList,
                                    viewModel.itemList.toList()
                                )
                            } else {
                                viewModel.snackBar.value = "No Item Found."
                            }
                        }
                    }, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.width(16.dp))

                Icon(
                    imageVector = if (isItemsExpanded.value) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Toggle Items",
                    modifier = Modifier.clickable {
                        isItemsExpanded.value = !isItemsExpanded.value
                    })
            }

            // Items List
            if (isItemsExpanded.value) {
                TextListView(
                    headerList = viewModel.itemHeaderList,
                    items = viewModel.itemList,
                    modifier = Modifier.heightIn(max = 400.dp),
                    onItemClick = {},
                    onItemLongClick = {})
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Sold Items Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(topStart = 12.dp)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(
                    text = "Sold Items",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.weight(1f))

                Icon(
                    imageVector = if (isItemsExpanded.value) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                    contentDescription = "Toggle Sold Items",
                    modifier = Modifier.clickable {
                        isItemsExpanded.value = !isItemsExpanded.value
                    })
            }

            // Sold Items List
            if (!isItemsExpanded.value) {
                TextListView(
                    headerList = viewModel.orderHeaderList,
                    items = viewModel.orderList,
                    modifier = Modifier.heightIn(max = 400.dp),
                    onItemClick = {},
                    onItemLongClick = {})
            }
        }

    }
    
    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Purchase") },
            text = { 
                Text("Are you sure you want to delete this purchase order? This action cannot be undone and will also delete all associated items and exchange metals.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deletePurchaseWithItems(
                            purchaseOrderId = purchaseOrderId,
                            onSuccess = {
                                // Navigate back to purchases list
                                mainScope {
                                    subNavigation.popBackStack()
                                }
                            },
                            onFailure = { error ->
                                // Show error message via snackbar
                                viewModel.snackBar.value = error
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
