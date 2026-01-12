package com.velox.jewelvault.ui.screen.order_and_purchase.order_item

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Close
import androidx.compose.material.icons.twotone.Delete
import androidx.compose.material.icons.twotone.MoreVert
import androidx.compose.material.icons.twotone.PictureAsPdf
import androidx.compose.material.icons.twotone.Refresh
import androidx.compose.material.icons.twotone.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.velox.jewelvault.ui.components.CusOutlinedTextField
import com.velox.jewelvault.ui.components.InputFieldState
import com.velox.jewelvault.ui.components.RowOrColumn
import com.velox.jewelvault.ui.components.TextListView
import com.velox.jewelvault.ui.components.WidthThenHeightSpacer
import com.velox.jewelvault.ui.components.bounceClick
import com.velox.jewelvault.ui.nav.SubScreens
import com.velox.jewelvault.utils.LocalSubNavController
import com.velox.jewelvault.utils.PdfRendererPreview
import com.velox.jewelvault.utils.VaultPreview
import com.velox.jewelvault.utils.formatDate
import com.velox.jewelvault.utils.mainScope
import com.velox.jewelvault.utils.sharePdf
import com.velox.jewelvault.utils.to3FString

@Composable
@VaultPreview
fun OrderItemDetailScreenPreview() {
    val viewModel = hiltViewModel<OrderItemViewModel>()
    val orderId = "2"
    OrderItemDetailScreen(viewModel, orderId)
}

@Composable
fun OrderItemDetailScreen(viewModel: OrderItemViewModel, orderId: String) {

    val context = LocalContext.current
    val subNavigation = LocalSubNavController.current
    val pdfFile = viewModel.generatedPdfUri

    // State for dropdown menu
    var showDropdownMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val adminPin = remember { InputFieldState() }

    viewModel.currentScreenHeadingState.value = "Order Details ($orderId)"


    LaunchedEffect(true) {
        viewModel.loadOrderDetails(orderId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(topStart = 18.dp))
            .padding(top = 5.dp, start = 5.dp)
    ) {
        Column(
            modifier = Modifier
        ) {

            viewModel.orderDetailsEntity?.order?.let { order ->

                Row {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Order ID: ${order.orderId}, Date: ${formatDate(order.orderDate)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "Store: ${viewModel.store?.name ?: "N/A"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showDropdownMenu = true },
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Icon(
                                Icons.TwoTone.MoreVert,
                                contentDescription = "More options",
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showDropdownMenu,
                            onDismissRequest = { showDropdownMenu = false }) {
                            DropdownMenuItem(text = {
                                Row {
                                    Icon(
                                        Icons.TwoTone.Delete,
                                        contentDescription = null,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Text("Delete Order")
                                }
                            }, onClick = {
                                showDropdownMenu = false
                                showDeleteDialog = true
                            })
                        }
                    }

                }
            }

            viewModel.customer?.let { customer ->
                Column(
                    modifier = Modifier
                        .padding(8.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp)
                        .bounceClick {
                            mainScope {
                                subNavigation.navigate("${SubScreens.CustomersDetails}/${customer.mobileNo}")
                            }
                        }) {
                    Text(
                        text = "${customer.name} (${customer.mobileNo})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "Address: ${customer.address ?: "N/A"}, GSTIN/PAN: ${customer.gstin_pan ?: "N/A"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }


            }
        }


        if (pdfFile == null) {
            RowOrColumn {

                // Before PDF generation: Order details on left, summary on right
                OrderDetailsCard(
                    viewModel = viewModel, modifier = if (it) Modifier.weight(1f) else Modifier
                )

                WidthThenHeightSpacer()

                // Order Summary Card (similar to SellPreviewScreen)
                OrderSummaryCard(
                    viewModel = viewModel,
                    onGeneratePdf = {
                        viewModel.generateOrderPdf(context)
                    },
                    isGenerating = viewModel.isPdfGenerating,
                    modifier = if (it) Modifier.weight(1f) else Modifier
                )
            }
        } else {
            // After PDF generation: PDF on left, summary on right
            // PDF Viewer on the left
            RowOrColumn {
                val boxModifier =
                    if (it) Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surface)
                        .zIndex(1f)
                    else Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .zIndex(1f)
                Box(
                    modifier = boxModifier
                ) {
                    var scale by remember { mutableStateOf(1f) }
                    var offsetX by remember { mutableStateOf(0f) }
                    var offsetY by remember { mutableStateOf(0f) }
                    val minScale = 1f
                    val maxScale = 5f

                    Box(
                        Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(minScale, maxScale)
                                    val maxPanX = 1000f * (scale - 1)
                                    val maxPanY = 1000f * (scale - 1)
                                    offsetX = (offsetX + pan.x).coerceIn(-maxPanX, maxPanX)
                                    offsetY = (offsetY + pan.y).coerceIn(-maxPanY, maxPanY)
                                }
                            }) {
                        PdfRendererPreview(
                            uri = pdfFile,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offsetX,
                                    translationY = offsetY
                                )
                                .background(MaterialTheme.colorScheme.surface),
                            highQuality = true
                        )

                        // Reset button
                        IconButton(
                            onClick = {
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                                .size(36.dp)
                                .background(
                                    MaterialTheme.colorScheme.background, RoundedCornerShape(18.dp)
                                )
                        ) {
                            Icon(Icons.TwoTone.Refresh, contentDescription = "Reset Zoom/Pan")
                        }
                    }
                }
                // Summary on the right
                WidthThenHeightSpacer()

                Column(if (it) Modifier.weight(1f) else Modifier) {
                    OrderSummaryCard(
                        viewModel = viewModel,
                        onGeneratePdf = { },
                        isGenerating = false,
                        modifier = Modifier.fillMaxWidth().wrapContentHeight()
                    )

                    // Action buttons
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (pdfFile != null) {
                                    sharePdf(context, pdfFile)
                                }
                            }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            )
                        ) {
                            Icon(Icons.TwoTone.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share PDF")
                        }

                        Button(
                            onClick = {
                                viewModel.clearPdf()
                            }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Icon(Icons.TwoTone.Close, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Close PDF")
                        }
                    }
                }
            }


        }

    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(onDismissRequest = {
            showDeleteDialog = false
            adminPin.clear()
        }, title = { Text("Delete Order") }, text = {
            Column {
                Text("Are you sure you want to delete this order? This action cannot be undone and will also delete all associated items and exchange metals.")

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Enter Admin PIN to confirm:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                CusOutlinedTextField(
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .fillMaxWidth(),
                    state = adminPin,
                    placeholderText = "Enter Admin PIN",
                    keyboardType = KeyboardType.NumberPassword,
                )
            }
        }, confirmButton = {
            Button(
                onClick = {
                    if (adminPin.text.isNotEmpty()) {
                        showDeleteDialog = false
                        viewModel.deleteOrderWithItems(
                            orderId = orderId,
                            adminPin = adminPin.text,
                            onSuccess = {
                                // Navigate back to orders list
                                mainScope {
                                    subNavigation.popBackStack()
                                }
                            },
                            onFailure = { error ->
                                // Show error message via snackbar
                                viewModel.snackBar.value = error
                            })
                        adminPin.clear()
                    } else {
                        viewModel.snackBar.value = "Please enter Admin PIN"
                    }
                }, colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text("Delete")
            }
        }, dismissButton = {
            TextButton(
                onClick = {
                    showDeleteDialog = false
                    adminPin.clear()
                }) {
                Text("Cancel")
            }
        })
    }
}

@Composable
fun OrderDetailsCard(
    viewModel: OrderItemViewModel, modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .wrapContentHeight()
            .padding(8.dp)

    ) {
        Text(
            text = "Order Items",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))

        viewModel.orderDetailsEntity?.let { orderDetails ->
            if (orderDetails.items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No items in this order.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Items list using TextListView
                val headerList = listOf(
                    "S.No", "Item", "Qty", "Weight", "Price", "Charge", "Tax", "Total"
                )

                val itemsData = orderDetails.items.mapIndexed { index, item ->
                    val itemTotal = item.price + item.charge + item.tax
                    listOf(
                        "${index + 1}.",
                        "${item.catName} ${item.subCatName}",
                        "${item.quantity} P",
                        "${item.gsWt}/${item.ntWt}gm\n${item.fnWt}g",
                        "₹${item.price.to3FString()}",
                        "₹${item.charge.to3FString()}",
                        "₹${item.tax.to3FString()}",
                        "₹${itemTotal.to3FString()}"
                    )
                }

                TextListView(
                    headerList = headerList,
                    items = itemsData,
                    modifier = Modifier.heightIn(max = 300.dp),
                    onItemClick = { },
                    onItemLongClick = { })

                // Exchange Items Section
                if (orderDetails.exchangeItems.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Metal Exchanges",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val exchangeHeaderList = listOf(
                        "S.No", "Metal", "Purity", "Weight", "Value"
                    )

                    val exchangeItemsData =
                        orderDetails.exchangeItems.mapIndexed { index, exchange ->
                            listOf(
                                "${index + 1}.",
                                exchange.metalType,
                                exchange.purity,
                                "${exchange.fineWeight}g",
                                "₹${exchange.exchangeValue.to3FString()}"
                            )
                        }

                    TextListView(
                        headerList = exchangeHeaderList,
                        items = exchangeItemsData,
                        modifier = Modifier.heightIn(max = 120.dp),
                        onItemClick = { },
                        onItemLongClick = { })

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth()) {
                        Text(
                            "Total Exchange Value: ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "₹${
                                orderDetails.exchangeItems.sumOf { it.exchangeValue }.to3FString()
                            }",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }

}

@Composable
fun OrderSummaryCard(
    viewModel: OrderItemViewModel,
    onGeneratePdf: () -> Unit,
    isGenerating: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(8.dp)
            .defaultMinSize(minHeight = 300.dp)
//            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Order Summary",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        CusOutlinedTextField(
            state = viewModel.invoiceNo,
//                             onTextChange = { invoiceViewModel.discount.text = it },
            placeholderText = "Invoice number",
            modifier = Modifier,
            keyboardType = KeyboardType.Number,
            enabled = false
        )

        viewModel.orderDetailsEntity?.let { orderDetails ->
            val order = orderDetails.order

            // Order Summary
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    "Summary",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Metal summaries
                val groupedItems = orderDetails.items.groupBy { it.catName }
                groupedItems.forEach { (metalType, items) ->
                    val totalGrossWeight = items.sumOf { it.gsWt ?: 0.0 }
                    val totalFineWeight = items.sumOf { it.fnWt ?: 0.0 }

                    Row(Modifier.fillMaxWidth()) {
                        Text(
                            "${metalType} Gs/Fn Wt",
                            modifier = Modifier.weight(1f),
                            fontSize = 10.sp
                        )
                        Text(
                            "${totalGrossWeight.to3FString()}/${totalFineWeight.to3FString()} gm",
                            modifier = Modifier.weight(1f),
                            fontSize = 10.sp,
                            textAlign = TextAlign.End
                        )
                    }
                }

                Spacer(modifier = Modifier.height(5.dp))
                HorizontalDivider(thickness = 1.dp)

                // Financial totals
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        "Subtotal", modifier = Modifier.weight(1f), fontSize = 10.sp
                    )
                    Text(
                        "₹${order.totalAmount.to3FString()}",
                        modifier = Modifier.weight(1f),
                        fontSize = 10.sp,
                        textAlign = TextAlign.End
                    )
                }

                if (order.discount > 0) {
                    Row(Modifier.fillMaxWidth()) {
                        Text(
                            "Discount",
                            modifier = Modifier.weight(1f),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            "-₹${order.discount.to3FString()}",
                            modifier = Modifier.weight(1f),
                            fontSize = 10.sp,
                            textAlign = TextAlign.End,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                if (order.totalCharge > 0) {
                    Row(Modifier.fillMaxWidth()) {
                        Text(
                            "Making Charges", modifier = Modifier.weight(1f), fontSize = 10.sp
                        )
                        Text(
                            "₹${order.totalCharge.to3FString()}",
                            modifier = Modifier.weight(1f),
                            fontSize = 10.sp,
                            textAlign = TextAlign.End
                        )
                    }
                }

                if (order.totalTax > 0) {
                    Row(Modifier.fillMaxWidth()) {
                        Text(
                            "Tax", modifier = Modifier.weight(1f), fontSize = 10.sp
                        )
                        Text(
                            "₹${order.totalTax.to3FString()}",
                            modifier = Modifier.weight(1f),
                            fontSize = 10.sp,
                            textAlign = TextAlign.End
                        )
                    }
                }

                Spacer(modifier = Modifier.height(5.dp))
                HorizontalDivider(thickness = 1.dp)

                // Exchange Value (if any)
                val totalExchangeValue = orderDetails.exchangeItems.sumOf { it.exchangeValue }
                if (totalExchangeValue > 0) {
                    Row(Modifier.fillMaxWidth()) {
                        Text(
                            "Exchange Value",
                            modifier = Modifier.weight(1f),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "-₹${totalExchangeValue.to3FString()}",
                            modifier = Modifier.weight(1f),
                            fontSize = 10.sp,
                            textAlign = TextAlign.End,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(5.dp))
                HorizontalDivider(thickness = 1.dp)

                // Grand Total
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        "Grand Total",
                        modifier = Modifier.weight(1f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    val grandTotal =
                        order.totalAmount - order.discount + order.totalCharge + order.totalTax - totalExchangeValue
                    Text(
                        "₹${grandTotal.to3FString()}",
                        modifier = Modifier.weight(1f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End
                    )
                }

                // Notes
                if (!order.note.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Notes:", fontSize = 10.sp, fontWeight = FontWeight.Bold
                    )
                    Text(
                        order.note,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Generate PDF Button
        Button(
            onClick = {
                if (viewModel.invoiceNo.text.isNotEmpty() && viewModel.invoiceNo.text.isNotBlank()) {
                    onGeneratePdf()
                } else {
                    viewModel.snackBar.value = "Please enter invoice number"
                }
            },
            enabled = !isGenerating,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            if (isGenerating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generating PDF...")
            } else {
                Icon(Icons.TwoTone.PictureAsPdf, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generate PDF")
            }
        }
    }

}

