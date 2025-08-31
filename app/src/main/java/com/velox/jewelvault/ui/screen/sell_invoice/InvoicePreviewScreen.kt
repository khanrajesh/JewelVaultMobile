package com.velox.jewelvault.ui.screen.sell_invoice

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.velox.jewelvault.ui.components.SignatureBox
import com.velox.jewelvault.ui.components.PaymentDialog
import com.velox.jewelvault.ui.components.TextListView
import com.velox.jewelvault.ui.nav.Screens
import com.velox.jewelvault.utils.LocalNavController
import com.velox.jewelvault.utils.sharePdf
import com.velox.jewelvault.utils.PdfRendererPreview
import com.velox.jewelvault.utils.to2FString
import com.velox.jewelvault.utils.CalculationUtils
import com.velox.jewelvault.utils.LocalBaseViewModel


@SuppressLint("DefaultLocale")
@Composable
fun SellPreviewScreen(invoiceViewModel: InvoiceViewModel) {

    val context = LocalContext.current
    val pdfFile = invoiceViewModel.generatedPdfFile
    val navController = LocalNavController.current

    // Calculate summary totals once at the beginning for use throughout the UI
    val summary = CalculationUtils.summaryTotals(invoiceViewModel.selectedItemList.toList())

    // Check if we're in draft mode by looking at the context
    // Draft invoices typically come from DraftInvoiceScreen and don't have payment info
    // Regular invoices from SellInvoiceScreen should not be treated as drafts
    val isDraftMode = invoiceViewModel.selectedItemList.isNotEmpty() &&
            invoiceViewModel.selectedItemList.any { it.itemId.startsWith("DB_") } // Only draft items have DB_ prefix

    BackHandler {
        if (isDraftMode) {
            invoiceViewModel.draftClearData()
        } else {
            invoiceViewModel.clearData()
        }
        navController.navigate(Screens.Main.route) {
            popUpTo(Screens.Main.route) {
                inclusive = true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        // Mode indicator at the top
        if (isDraftMode) {
            Text(
                text = "Draft Invoice Mode",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        }

        // Main content area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
        {
            if (pdfFile == null) {
                // Before PDF generation: Item details on left, signatures on right
                    // Item Details Card
                    ItemSummaryCard(
                        title = "Bill Details",
                        invoiceViewModel = invoiceViewModel,
                        summary = summary,
                        modifier = Modifier.weight(1f)
                    )

                // Signatures section on the right
                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                ) {
                    // Customer Signature
                    SignatureBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(
                                MaterialTheme.colorScheme.surface,
                                RoundedCornerShape(12.dp)
                            ),
                        title = "Customer Signature",
                        check = invoiceViewModel.customerSign.value != null,
                        onSignatureCaptured = { bitmap ->
                            invoiceViewModel.customerSign.value = bitmap
                        }
                    )



                    Spacer(modifier = Modifier.height(16.dp))

                    SignatureBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(
                                MaterialTheme.colorScheme.surface,
                                RoundedCornerShape(12.dp)
                            ),
                        title = "Owner Signature",
                        check = invoiceViewModel.ownerSign.value != null,
                        onSignatureCaptured = { bitmap ->
                            invoiceViewModel.ownerSign.value = bitmap
                        }
                    )
                }
            } else
            {

                // After PDF generation: PDF on left, item details on right
                // PDF Viewer on the left
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surface)
                        .zIndex(1f)
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
                            }
                    ) {
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
                                    MaterialTheme.colorScheme.background,
                                    RoundedCornerShape(18.dp)
                                )
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset Zoom/Pan")
                        }
                    }
                }

                // Item details on the right
                Spacer(modifier = Modifier.width(16.dp))

                Column(Modifier.weight(1f)) {
                    ItemSummaryCard(
                        title = "Invoice Summary",
                        invoiceViewModel = invoiceViewModel,
                        summary = summary,
                        modifier = Modifier.weight(1f)
                    )

                    // Action buttons
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { sharePdf(context, pdfFile) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Share PDF")
                        }

                        Button(
                            onClick = {
                                if (isDraftMode) {
                                    invoiceViewModel.draftClearData()
                                } else {
                                    invoiceViewModel.clearData()
                                }
                                navController.navigate(Screens.Main.route) {
                                    popUpTo(Screens.Main.route) {
                                        inclusive = true
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Exit")
                        }
                    }
                }
            }
        }

        if (pdfFile == null) {
            Spacer(modifier = Modifier.height(16.dp))

            if (isDraftMode) {
                // Draft mode: Generate PDF button
                Button(
                    onClick = {
                        if (invoiceViewModel.customerSign.value != null && invoiceViewModel.ownerSign.value != null) {
                            invoiceViewModel.draftCompleteOrder(
                                context = context,
                                onSuccess = {
                                    invoiceViewModel.snackBarState.value = "Draft Invoice Generated"
                                },
                                onFailure = { error ->
                                    invoiceViewModel.snackBarState.value = "Error: $error"
                                }
                            )
                        } else {
                            invoiceViewModel.snackBarState.value = "Please Sign"
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Generate Draft Invoice")
                }
            } else {
                // Regular mode: Payment and order completion
                Button(
                    onClick = {
                        if (invoiceViewModel.customerSign.value != null && invoiceViewModel.ownerSign.value != null) {
                            if (invoiceViewModel.paymentInfo.value == null) {
                                invoiceViewModel.showPaymentDialog.value = true
                            } else {
                                invoiceViewModel.completeOrder(
                                    onSuccess = {
                                        invoiceViewModel.snackBarState.value = "Order Completed"
                                    },
                                    onFailure = {
                                        invoiceViewModel.snackBarState.value = it
                                    }
                                )
                            }
                        } else {
                            invoiceViewModel.snackBarState.value = "Please Sign"
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (invoiceViewModel.paymentInfo.value == null) "Proceed to Payment" else "Complete Order")
                }
            }
        }
    }


    // Payment Dialog - Only show for regular invoices
    if (!isDraftMode && invoiceViewModel.showPaymentDialog.value) {
        PaymentDialog(
            totalAmount = invoiceViewModel.getTotalOrderAmount(),
            upiId = invoiceViewModel.upiId.value,
            merchantName = invoiceViewModel.storeName.value,
            onPaymentConfirmed = { paymentInfo,discount ->
                invoiceViewModel.onPaymentConfirmed(paymentInfo,discount)
                // Automatically proceed with order completion after payment confirmation
                invoiceViewModel.completeOrder(onSuccess = {
                    invoiceViewModel.snackBarState.value = "Order Completed"
                }, onFailure = {
                    invoiceViewModel.snackBarState.value = it
                })
            },
            onDismiss = {
                invoiceViewModel.showPaymentDialog.value = false
            }
        )
    }
}

@Composable
fun ItemSummaryCard(
    title: String,
    invoiceViewModel: InvoiceViewModel,
    summary: com.velox.jewelvault.utils.SummaryCalculationResult,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (invoiceViewModel.selectedItemList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No items added yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column (modifier = Modifier){
                        // Compact items list using TextListView
                        val headerList = listOf(
                            "Id",
                            "Item",
                            "Qty",
                            "Gs/Nt.Wt",
                            "Fn.Wt",
                            "Metal",
                            "Chr",
                            "M.Chr",
                            "Tax",
                        )

                        // Prepare items data for TextListView
                        val itemsData =
                            invoiceViewModel.selectedItemList.mapIndexed { index, item ->
                                // Use CalculationUtils for price and charge calculations
                                val oneUnitPrice =
                                    CalculationUtils.metalUnitPrice(
                                        item.catName,
                                        LocalBaseViewModel.current.metalRates
                                    ) ?: 0.0
                                val price = CalculationUtils.basePrice(
                                    item.fnWt ?: 0.0,
                                    oneUnitPrice
                                )

                                val charge = CalculationUtils.makingCharge(
                                    chargeType = item.crgType,
                                    chargeRate = item.crg,
                                    basePrice = price,
                                    quantity = item.quantity,
                                    weight = item.ntWt ?: 0.0
                                )
                                val char = charge.to2FString()

                                listOf(
                                    "${index + 1}.",
                                    "${item.itemId}",
                                    "${item.subCatName} ${item.itemAddName}",
                                    "${item.quantity} P",
                                    "${item.gsWt}/${item.ntWt}gm",
                                    "${item.fnWt}/gm\n${oneUnitPrice.to2FString()}",
                                    "${item.catName} (${item.purity})",
                                    "${item.crg} ${item.crgType}",
                                    "${char}\n+ ${item.othCrg}",
                                    "${item.cgst + item.sgst + item.igst} %",
                                )
                            }

                        TextListView(
                            headerList = headerList,
                            items = itemsData,
                            modifier = Modifier.height(300.dp),
                            onItemClick = { clickedItemData ->
                                // Handle item click if needed
                            },
                            onItemLongClick = {
                                // Handle long click if needed
                            }
                        )

                        // Compact totals
                        Spacer(modifier = Modifier.height(8.dp))

                        Column(
                            modifier = Modifier.weight(1f).padding(12.dp)
                        ) {
                            Text(
                                "Summary",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )

                            val groupedItems =
                                invoiceViewModel.selectedItemList.groupBy { it.catName }

                            groupedItems.forEach { (metalType, items) ->
                                val totalGsWt = items.sumOf { it.gsWt }
                                val totalFnWt = items.sumOf { it.fnWt }

                                Row(Modifier.fillMaxWidth()) {
                                    Text(
                                        "$metalType Gs/Fn Wt",
                                        modifier = Modifier.weight(1f),
                                        fontSize = 10.sp
                                    )
                                    Text(
                                        "${totalGsWt.to2FString()}/${totalFnWt.to2FString()} gm",
                                        modifier = Modifier.weight(1f),
                                        fontSize = 10.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                                    )
                                }
                            }

                            Spacer(Modifier.height(5.dp))
                            androidx.compose.material3.HorizontalDivider(
                                thickness = 1.dp
                            )

                            // Charge Details Section
                            Text(
                                "Charges Breakdown",
                                fontSize = 11.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(3.dp))

                            // Making Charges
                            val totalMakingCharges = summary.totalMakingCharges
                            if (totalMakingCharges > 0) {
                                Row(Modifier.fillMaxWidth()) {
                                    Text(
                                        "Making Charges",
                                        modifier = Modifier.weight(1.5f),
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(
                                            alpha = 0.8f
                                        )
                                    )
                                    Text(
                                        "₹${"%.2f".format(totalMakingCharges)}",
                                        modifier = Modifier.weight(1f),
                                        fontSize = 9.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                        color = MaterialTheme.colorScheme.onSurface.copy(
                                            alpha = 0.8f
                                        )
                                    )
                                }
                            }

                            // Other Charges
                            val totalOtherCharges =
                                invoiceViewModel.selectedItemList.sumOf { it.othCrg }
                            if (totalOtherCharges > 0) {
                                Row(Modifier.fillMaxWidth()) {
                                    Text(
                                        "Other Charges",
                                        modifier = Modifier.weight(1.5f),
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(
                                            alpha = 0.8f
                                        )
                                    )
                                    Text(
                                        "₹${"%.2f".format(totalOtherCharges)}",
                                        modifier = Modifier.weight(1f),
                                        fontSize = 9.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                        color = MaterialTheme.colorScheme.onSurface.copy(
                                            alpha = 0.8f
                                        )
                                    )
                                }
                            }

                            // Charge Type Information
                            val chargeTypes =
                                invoiceViewModel.selectedItemList.mapNotNull { item ->
                                    when (item.crgType) {
                                        "Percentage" -> "Making: ${item.crg}% of price"
                                        "Piece" -> "Making: ₹${item.crg} per piece"
                                        "PerGm" -> "Making: ₹${item.crg} per gram"
                                        else -> null
                                    }
                                }.distinct()

                            if (chargeTypes.isNotEmpty()) {
                                Spacer(Modifier.height(3.dp))
                                chargeTypes.forEach { chargeInfo ->
                                    Text(
                                        text = "• $chargeInfo",
                                        fontSize = 8.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(
                                            alpha = 0.6f
                                        ),
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(5.dp))
                            androidx.compose.material3.HorizontalDivider(
                                thickness = 1.dp
                            )

                            // Total calculations using CalculationUtils
                            val totalPrice = summary.totalPriceBeforeTax
                            val totalTax = summary.totalTax
                            val grandTotal = summary.grandTotal

                            Spacer(Modifier.height(5.dp))
                            Row(Modifier.fillMaxWidth()) {
                                Text(
                                    "Price (before tax)",
                                    modifier = Modifier.weight(1.5f),
                                    fontSize = 10.sp
                                )
                                Text(
                                    "₹${"%.2f".format(totalPrice)}",
                                    modifier = Modifier.weight(1f),
                                    fontSize = 10.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                                )
                            }
                            Row(Modifier.fillMaxWidth()) {
                                Text(
                                    "Total Tax",
                                    modifier = Modifier.weight(1.5f),
                                    fontSize = 10.sp
                                )
                                Text(
                                    "₹${"%.2f".format(totalTax)}",
                                    modifier = Modifier.weight(1f),
                                    fontSize = 10.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                                )
                            }
                            Row(Modifier.fillMaxWidth()) {
                                Text(
                                    "Grand Total (after tax)",
                                    modifier = Modifier.weight(1.5f),
                                    fontSize = 13.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                                )
                                Text(
                                    "₹${"%.2f".format(grandTotal)}",
                                    modifier = Modifier.weight(1f),
                                    fontSize = 13.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                                )
                            }
                        }

                }
            }
        }
    }
}


