package com.velox.jewelvault.ui.screen.sell_invoice

import android.annotation.SuppressLint
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
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
import com.velox.jewelvault.ui.components.CusOutlinedTextField
import com.velox.jewelvault.ui.components.PaymentInfo
import com.velox.jewelvault.ui.components.RowOrColumn
import com.velox.jewelvault.ui.components.SignatureBox
import com.velox.jewelvault.ui.components.TextListView
import com.velox.jewelvault.ui.components.WidthThenHeightSpacer
import com.velox.jewelvault.ui.components.baseBackground1
import com.velox.jewelvault.ui.components.baseBackground8
import com.velox.jewelvault.ui.components.generateUpiQrCode
import com.velox.jewelvault.ui.nav.Screens
import com.velox.jewelvault.utils.CalculationUtils
import com.velox.jewelvault.utils.LocalBaseViewModel
import com.velox.jewelvault.utils.LocalNavController
import com.velox.jewelvault.utils.PdfRendererPreview
import com.velox.jewelvault.utils.isLandscape
import com.velox.jewelvault.utils.sharePdf
import com.velox.jewelvault.utils.to3FString


@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("DefaultLocale")
@Composable
fun SellPreviewScreen(invoiceViewModel: InvoiceViewModel) {

    val context = LocalContext.current
    val pdfFile = invoiceViewModel.generatedPdfFile
    val navController = LocalNavController.current
    val orderCompleted = remember { mutableStateOf(false) }

    // Calculate summary totals once at the beginning for use throughout the UI

    // Check if we're in draft mode by looking at the context
    // Draft invoices typically come from DraftInvoiceScreen and don't have payment info
    // Regular invoices from SellInvoiceScreen should not be treated as drafts
    val isDraftMode =
        invoiceViewModel.selectedItemList.isNotEmpty() && invoiceViewModel.selectedItemList.any {
            it.itemId.startsWith("DB_")
        } // Only draft items have DB_ prefix

    BackHandler {
        if (orderCompleted.value) {
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
        } else {
            navController.popBackStack()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .baseBackground8()
            .padding(8.dp)
    ) {
        // Mode indicator at the top
        if (isDraftMode) {
            Text(
                text = "Draft Invoice Mode",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Main content area

        if (pdfFile == null) {
            // Before PDF generation: Item details on left, signatures on right
            // Item Details Card
            /*  if (isLandscape()) {
                  Row(
                      modifier = Modifier
                          .fillMaxWidth()
                          .weight(1f)
                  ) {
                      ItemSummaryCard(
                          title = "Bill Details",
                          invoiceViewModel = invoiceViewModel,
                          modifier = Modifier.weight(1f)
                      )

                      // Signatures section on the right
                      Spacer(modifier = Modifier.width(8.dp))

                      PaymentDetailsSection(invoiceViewModel, isDraftMode, context, orderCompleted)
                  }

              } else {
                  Column(Modifier
                      .fillMaxWidth()
                      .verticalScroll(rememberScrollState())) {
  //                    ItemSummaryCard(
  //                        title = "Bill Details",
  //                        invoiceViewModel = invoiceViewModel,
  //                        modifier = Modifier.wrapContentHeight()
  //                    )
  //
  //                    // Signatures section on the right
  //                    Spacer(modifier = Modifier.height(8.dp))

                      PaymentDetailsSection(invoiceViewModel, isDraftMode, context, orderCompleted)
                  }
              }*/

            RowOrColumn(
                rowModifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                columnModifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {

                ItemSummaryCard(
                    title = "Bill Details",
                    invoiceViewModel = invoiceViewModel,
                    modifier = if (it) Modifier.weight(1f) else Modifier
                        .wrapContentHeight()
                )

                // Signatures section on the right
//                    Spacer(modifier = Modifier.width(8.dp))
                WidthThenHeightSpacer(5.dp)


                PaymentDetailsSection(
                    if (it) Modifier.weight(1f) else Modifier
                        .wrapContentHeight(),
                    invoiceViewModel,
                    isDraftMode,
                    context,
                    orderCompleted
                )
            }
        } else {
            var scale by remember { mutableStateOf(1f) }
            var offsetX by remember { mutableStateOf(0f) }
            var offsetY by remember { mutableStateOf(0f) }
            val minScale = 1f
            val maxScale = 5f
            val panZoomModifier = Modifier
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

            val pdfRendererModifier = Modifier
                .graphicsLayer(
                    scaleX = scale, scaleY = scale, translationX = offsetX, translationY = offsetY
                )
                .baseBackground8()

            val iconBtnOnClick = {
                scale = 1f
                offsetX = 0f
                offsetY = 0f
            }

            // Main content area
            RowOrColumn(
                rowModifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                columnModifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // After PDF generation: PDF on left, item details on right
                // PDF Viewer on the left

                val boxModifier = if (it) Modifier
                    .weight(1f)
                    .baseBackground8()
                    .zIndex(1f)
                else Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .baseBackground8()
                    .zIndex(1f)

                Box(
                    modifier = boxModifier
                ) {

                    Box(panZoomModifier) {
                        PdfRendererPreview(
                            uri = pdfFile,
                            modifier = pdfRendererModifier.align(Alignment.Center),
                            highQuality = true
                        )

                        // Reset button
                        IconButton(
                            onClick = iconBtnOnClick,
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

                // Item details on the right
//                Spacer(modifier = Modifier.width(16.dp))

                val colModifier = if (it) Modifier.weight(1f) else Modifier.wrapContentHeight()


                Column(colModifier) {
                    ItemSummaryCard(
                        title = "Invoice Summary",
                        invoiceViewModel = invoiceViewModel,
                        modifier = Modifier.wrapContentHeight()
                    )

                    // Action buttons
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { sharePdf(context, pdfFile) }, modifier = Modifier.weight(1f)
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
                            }, modifier = Modifier.weight(1f)
                        ) {
                            Text("Exit")
                        }
                    }
                }
            }
        }


    }


}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun PaymentDetailsSection(
    modifier: Modifier = Modifier,
    invoiceViewModel: InvoiceViewModel,
    isDraftMode: Boolean,
    context: Context,
    orderCompleted: MutableState<Boolean>
) {
    Column(
        modifier = modifier
            .heightIn(200.dp)
            .wrapContentHeight()
    ) {
        //payment section
        val totalAmount = invoiceViewModel.getTotalOrderAmount()
        val upiId = invoiceViewModel.upiId.value
        var selectedPaymentMethod by remember { mutableStateOf("Cash") }
        var selectedPaymentType by remember { mutableStateOf("Paid in Full") }
        var paidAmountText by remember {
            mutableStateOf(
                invoiceViewModel.getTotalOrderAmount().to3FString()
            )
        }
        var paymentMethodExpanded by remember { mutableStateOf(false) }
        var paymentTypeExpanded by remember { mutableStateOf(false) }

        val paymentMethods = listOf("Cash", "Check", "Card", "UPI/Digital")
        val paymentTypes = listOf("Paid in Full", "Partial Payment")

        val isPaidInFull = selectedPaymentType == "Paid in Full"
        val paidAmount = paidAmountText.toDoubleOrNull() ?: 0.0
        val outstandingAmount =
            (invoiceViewModel.getTotalOrderAmount() - paidAmount).coerceAtLeast(0.0)

        // Generate QR code for UPI payment - regenerate when payment method, amount, or payment type changes
        val qrCodeBitmap = remember(selectedPaymentMethod, paidAmount, selectedPaymentType) {
            if (selectedPaymentMethod == "UPI/Digital" && invoiceViewModel.upiId.value.isNotEmpty() && paidAmount > 0) {
                val amountForQr =
                    if (isPaidInFull) invoiceViewModel.getTotalOrderAmount() else paidAmount
                generateUpiQrCode(
                    invoiceViewModel.upiId.value, amountForQr, invoiceViewModel.storeName.value
                )
            } else null
        }

        val isLandscape = isLandscape()

        // Update paid amount when payment type changes
        LaunchedEffect(selectedPaymentType) {
            if (selectedPaymentType == "Paid in Full") {
                paidAmountText = invoiceViewModel.getTotalOrderAmount().to3FString()
            }
        }

        val contentModifier =
            if (isLandscape) Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(8.dp)
            else Modifier
                .fillMaxWidth()
                .padding(8.dp)

        Column(
            modifier = contentModifier
        ) {
            Text(
                text = "Payment Details",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )


            // Total Amount Display
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                RowOrColumn {
                    CusOutlinedTextField(
                        state = invoiceViewModel.invoiceNo,
                        placeholderText = "Invoice number",
                        modifier = if(isLandscape)Modifier.weight(1f)else Modifier,
                        keyboardType = KeyboardType.Number
                    )

                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Net Amount",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "Net Amount: \n₹${totalAmount.to3FString()}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        if (selectedPaymentType == "Partial Payment") {
                            Text(
                                text = "Outstanding: ₹${outstandingAmount.to3FString()}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    CusOutlinedTextField(
                        state = invoiceViewModel.discount,
                             onTextChange = { invoiceViewModel.discount.text = it },
                        placeholderText = "Discount Amount",
                        modifier = if(isLandscape)Modifier.weight(1f) else Modifier,
                        keyboardType = KeyboardType.Number
                    )
                }
            }

            // Discount

            // Payment Method Selection
            Text(
                text = "Payment Method",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            RowOrColumn {
                ExposedDropdownMenuBox(
                    modifier = if(isLandscape)Modifier.weight(1f)else Modifier,
                    expanded = paymentMethodExpanded, onExpandedChange = {
                        paymentMethodExpanded = !paymentMethodExpanded
                    }) {
                    OutlinedTextField(
                        value = selectedPaymentMethod,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Select Payment Method") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = paymentMethodExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = paymentMethodExpanded,
                        onDismissRequest = { paymentMethodExpanded = false }) {
                        paymentMethods.forEach { method ->
                            DropdownMenuItem(text = { Text(method) }, onClick = {
                                selectedPaymentMethod = method
                                paymentMethodExpanded = false
                            })
                        }
                    }
                }
                WidthThenHeightSpacer()
                ExposedDropdownMenuBox(
                    modifier = if(isLandscape)Modifier.weight(1f)else Modifier,
                    expanded = paymentTypeExpanded,
                    onExpandedChange = { paymentTypeExpanded = !paymentTypeExpanded }) {
                    OutlinedTextField(
                        value = selectedPaymentType,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Select Payment Type") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = paymentTypeExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = paymentTypeExpanded,
                        onDismissRequest = { paymentTypeExpanded = false }) {
                        paymentTypes.forEach { type ->
                            DropdownMenuItem(text = { Text(type) }, onClick = {
                                selectedPaymentType = type
                                paymentTypeExpanded = false
                            })
                        }
                    }
                }
            }


            // Paid Amount Input (only show if partial payment)
            if (selectedPaymentType == "Partial Payment") {
                OutlinedTextField(
                    value = paidAmountText,
                    onValueChange = { paidAmountText = it },
                    label = { Text("Amount Paid") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    prefix = { Text("₹") },
                    isError = paidAmount > totalAmount
                )

                if (paidAmount > totalAmount) {
                    Text(
                        text = "Paid amount cannot exceed total amount",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // UPI QR Code Display
            if (selectedPaymentMethod == "UPI/Digital" && qrCodeBitmap != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Scan QR Code to Pay",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Image(
                            bitmap = qrCodeBitmap,
                            contentDescription = "UPI QR Code",
                            modifier = Modifier.size(200.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Amount: ₹${(if (selectedPaymentType == "Paid in Full") totalAmount else paidAmount).to3FString()}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else if (selectedPaymentMethod == "UPI/Digital" && upiId.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "UPI ID not configured. Please contact administrator.",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }


        }

        Column(
            Modifier
//                .weight(1f)
                .fillMaxWidth()
        ) {


            // Customer Signature
            SignatureBox(
                modifier = Modifier
//                    .weight(1f)
                    .fillMaxWidth()
                    .height(200.dp)
                    .baseBackground1(),
                title = "Customer Signature",
                check = invoiceViewModel.customerSign.value != null,
                onSignatureCaptured = { bitmap ->
                    invoiceViewModel.customerSign.value = bitmap
                })



            Spacer(modifier = Modifier.height(8.dp))

            SignatureBox(
                modifier = Modifier
//                    .weight(1f)
                    .fillMaxWidth()
                    .height(200.dp)
                    .baseBackground1(),
                title = "Owner Signature",
                check = invoiceViewModel.ownerSign.value != null,
                onSignatureCaptured = { bitmap ->
                    invoiceViewModel.ownerSign.value = bitmap
                })

            Spacer(modifier = Modifier.height(10.dp))

            if (isDraftMode) {
                // Draft mode: Generate PDF button
                Button(
                    onClick = {
                        if (invoiceViewModel.customerSign.value != null && invoiceViewModel.ownerSign.value != null) {
                            invoiceViewModel.draftCompleteOrder(
                                context = context,
                                invoiceNo = invoiceViewModel.invoiceNo.text,
                                onSuccess = {
                                    invoiceViewModel.snackBarState.value = "Draft Invoice Generated"
                                },
                                onFailure = { error ->
                                    invoiceViewModel.snackBarState.value = "Error: $error"
                                })
                        } else {
                            invoiceViewModel.snackBarState.value = "Please Sign"
                        }
                    }, modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Generate Draft Invoice")
                }
            } else {
                // Regular mode: Payment and order completion
                Button(
                    onClick = {
                        if (invoiceViewModel.invoiceNo.text.isNotEmpty() && invoiceViewModel.invoiceNo.text.isNotBlank()) {
                            if (invoiceViewModel.customerSign.value != null && invoiceViewModel.ownerSign.value != null) {
                                val finalPaidAmount =
                                    if (selectedPaymentType == "Paid in Full") totalAmount else paidAmount
                                val finalOutstandingAmount = totalAmount - finalPaidAmount
                                val isPaidInFullValue = selectedPaymentType == "Paid in Full"

                                val paymentInfo = PaymentInfo(
                                    paymentMethod = selectedPaymentMethod,
                                    totalAmount = totalAmount,
                                    paidAmount = finalPaidAmount,
                                    outstandingAmount = finalOutstandingAmount,
                                    isPaidInFull = isPaidInFullValue,
                                    notes = ""
                                )
                                invoiceViewModel.paymentInfo.value = paymentInfo

                                invoiceViewModel.completeOrder(
                                    invoiceNo = invoiceViewModel.invoiceNo.text,
                                    onSuccess = {

                                        orderCompleted.value = true
                                        invoiceViewModel.snackBarState.value = "Order Completed"
                                    },
                                    onFailure = {
                                        invoiceViewModel.snackBarState.value = it
                                    })
                            } else {
                                invoiceViewModel.snackBarState.value = "Please Sign"
                            }

                        } else {
                            invoiceViewModel.snackBarState.value = "invoice number can't be blank"
                        }
                    }, modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Complete Order")
                }
            }
        }
    }
}


@Composable
fun ItemSummaryCard(
    title: String, invoiceViewModel: InvoiceViewModel, modifier: Modifier = Modifier
) {

    val summary = CalculationUtils.summaryTotals(
        invoiceViewModel.selectedItemList.toList(),
        invoiceViewModel.exchangeItemList,
        invoiceViewModel.discount.text.toDoubleOrNull() ?: 0.0
    )

    val innerModifier = if (isLandscape()) Modifier.padding(8.dp)
    else Modifier.padding(8.dp)

    val cardModifier = if (isLandscape()) {
        modifier.fillMaxHeight()
    } else {
        modifier
            .fillMaxWidth()
            .wrapContentHeight()
    }

    Card(
        modifier = cardModifier
            .defaultMinSize(minHeight = 200.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = innerModifier
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (invoiceViewModel.selectedItemList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No items added yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(modifier = Modifier) {
                    // Compact items list using TextListView
                    val headerList = listOf(
                        "S.No",
                        "Id",
                        "Item",
                        "Qty",
                        "Gs/Nt.Wt",
                        "Fn.Wt",
                        "Metal",
                        "Chr/unit",
                        "M.Chr",
                        "Tax",
                    )

                    // Prepare items data for TextListView
                    val itemsData = invoiceViewModel.selectedItemList.mapIndexed { index, item ->
                        // Use CalculationUtils for price and charge calculations
                        val oneUnitPrice = CalculationUtils.metalUnitPrice(
                            item.catName, LocalBaseViewModel.current.metalRates
                        ) ?: 0.0
                        val price = CalculationUtils.baseMetalPrice(
                            item.fnWt ?: 0.0, oneUnitPrice
                        )

                        val charge = CalculationUtils.makingCharge(
                            chargeType = item.crgType,
                            chargeRate = item.crg,
                            basePrice = price,
                            quantity = item.quantity,
                            weight = item.ntWt ?: 0.0
                        )
                        val char = charge.to3FString()

                        listOf(
                            "${index + 1}.",
                            "${item.itemId}",
                            "${item.subCatName} ${item.itemAddName}",
                            "${item.quantity} P",
                            "${item.gsWt}/${item.ntWt}gm",
                            "${item.fnWt}/gm\n${oneUnitPrice.to3FString()}",
                            "${item.catName} (${item.purity})",
                            "${item.crg} ${item.crgType}",
                            "${char}\n+ ${item.compCrg}",
                            "${item.cgst + item.sgst + item.igst} %",
                        )
                    }

                    TextListView(
                        headerList = headerList,
                        items = itemsData,
                        modifier = Modifier.heightIn(max = 250.dp),
                        onItemClick = { clickedItemData ->
                            // Handle item click if needed
                        },
                        onItemLongClick = {
                            // Handle long click if needed
                        })

                    // Exchange Items Section
                    if (invoiceViewModel.exchangeItemList.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(thickness = 1.dp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            "Exchange Items (${invoiceViewModel.exchangeItemList.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Exchange items header
                        val exchangeHeaderList =
                            listOf("Metal", "Purity", "Gs/Fn.Wt", "Method", "Value")

                        // Exchange items data
                        val exchangeItemsData =
                            invoiceViewModel.exchangeItemList.mapIndexed { index, exchangeItem ->
                                listOf(
                                    exchangeItem.metalType,
                                    exchangeItem.purity,
                                    "${exchangeItem.grossWeight.to3FString()}/${exchangeItem.fineWeight.to3FString()}gm",
                                    if (exchangeItem.isExchangedByMetal) "Metal Rate" else "Price",
                                    "₹${exchangeItem.exchangeValue.to3FString()}"
                                )
                            }

                        TextListView(
                            headerList = exchangeHeaderList,
                            items = exchangeItemsData,
                            modifier = Modifier.heightIn(max = 120.dp),
                            onItemClick = { clickedItemData ->
                                // Handle exchange item click if needed
                            },
                            onItemLongClick = {
                                // Handle long click if needed
                            })

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(Modifier.fillMaxWidth()) {
                            Text(
                                "Total Exchange Value: ",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "₹${invoiceViewModel.getTotalExchangeValue().to3FString()}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.End
                            )
                        }
                    }

                    // Compact totals
                    Spacer(modifier = Modifier.height(2.dp))

                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            "Summary", fontSize = 12.sp, fontWeight = FontWeight.Bold
                        )

                        val groupedItems = invoiceViewModel.selectedItemList.groupBy { it.catName }

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
                                    "${totalGsWt.to3FString()}/${totalFnWt.to3FString()} gm",
                                    modifier = Modifier.weight(1f),
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.End
                                )
                            }
                        }

                        Spacer(Modifier.height(5.dp))
                        HorizontalDivider(
                            thickness = 1.dp
                        )

                        // Charge Details Section
                        Text(
                            "Charges Breakdown",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
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
                                    "₹${totalMakingCharges.to3FString()}",
                                    modifier = Modifier.weight(1f),
                                    fontSize = 9.sp,
                                    textAlign = TextAlign.End,
                                    color = MaterialTheme.colorScheme.onSurface.copy(
                                        alpha = 0.8f
                                    )
                                )
                            }
                        }

                        // Other Charges
                        val totalOtherCharges =
                            invoiceViewModel.selectedItemList.sumOf { it.compCrg }
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
                                    "₹${totalOtherCharges.to3FString()}",
                                    modifier = Modifier.weight(1f),
                                    fontSize = 9.sp,
                                    textAlign = TextAlign.End,
                                    color = MaterialTheme.colorScheme.onSurface.copy(
                                        alpha = 0.8f
                                    )
                                )
                            }
                        }

                        // Charge Type Information
                        val chargeTypes = invoiceViewModel.selectedItemList.mapNotNull { item ->
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
                        HorizontalDivider(
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
                                "₹${totalPrice.to3FString()}",
                                modifier = Modifier.weight(1f),
                                fontSize = 10.sp,
                                textAlign = TextAlign.End
                            )
                        }
                        Row(Modifier.fillMaxWidth()) {
                            Text(
                                "Total Tax", modifier = Modifier.weight(1.5f), fontSize = 10.sp
                            )
                            Text(
                                "₹${totalTax.to3FString()}",
                                modifier = Modifier.weight(1f),
                                fontSize = 10.sp,
                                textAlign = TextAlign.End
                            )
                        }
                        Row(Modifier.fillMaxWidth()) {
                            Text(
                                "Grand Total (after tax)",
                                modifier = Modifier.weight(1.5f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "₹${grandTotal.to3FString()}",
                                modifier = Modifier.weight(1f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.End
                            )
                        }

                        // Manual Discount Section
                        if (invoiceViewModel.discount.text.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(thickness = 1.dp)
                            Spacer(modifier = Modifier.height(5.dp))

                            Row(Modifier.fillMaxWidth()) {
                                Text(
                                    "Manual Discount",
                                    modifier = Modifier.weight(1.5f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    "-₹${invoiceViewModel.discount.text}",
                                    modifier = Modifier.weight(1f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.End,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        // Exchange Deduction Section
                        val hasReductions =
                            invoiceViewModel.exchangeItemList.isNotEmpty() || (invoiceViewModel.discount.text.toDoubleOrNull()
                                ?: 0.0) > 0
                        if (hasReductions) {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(thickness = 1.dp)
                            Spacer(modifier = Modifier.height(5.dp))

                            val totalExchangeValue = invoiceViewModel.getTotalExchangeValue()
                            val netPayableAmount = invoiceViewModel.getTotalOrderAmount()

                            Row(Modifier.fillMaxWidth()) {
                                Text(
                                    "Exchange Deduction",
                                    modifier = Modifier.weight(1.5f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    "-₹${totalExchangeValue.to3FString()}",
                                    modifier = Modifier.weight(1f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.End,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(thickness = 2.dp)
                            Spacer(modifier = Modifier.height(5.dp))

                            Row(Modifier.fillMaxWidth()) {
                                Text(
                                    "Net Payable Amount",
                                    modifier = Modifier.weight(1.5f),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "₹${netPayableAmount.to3FString()}",
                                    modifier = Modifier.weight(1f),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }

                }
            }
        }
    }
}


