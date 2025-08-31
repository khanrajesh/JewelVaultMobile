package com.velox.jewelvault.ui.screen.sell_invoice

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velox.jewelvault.data.MetalRatesTicker
import com.velox.jewelvault.data.roomdb.dto.ItemSelectedModel
import com.velox.jewelvault.ui.components.CusOutlinedTextField
import com.velox.jewelvault.ui.components.bounceClick
import com.velox.jewelvault.ui.components.TextListView
import com.velox.jewelvault.ui.nav.Screens
import com.velox.jewelvault.utils.*
import com.velox.jewelvault.utils.CalculationUtils
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun DraftInvoiceScreen(viewModel: InvoiceViewModel) {
    val context = LocalContext.current
    val navHost = LocalNavController.current
    val baseViewModel = LocalBaseViewModel.current
    val currentDateTime = rememberCurrentDateTime()
    val coroutineScope = rememberCoroutineScope()
    val showOption = remember { mutableStateOf(false) }
    val itemId = remember { mutableStateOf("") }

    Box(
        Modifier.fillMaxSize()
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(5.dp)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                    .padding(5.dp), verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    ioScope {
                        baseViewModel.refreshMetalRates(context = context)
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.Refresh, contentDescription = "Refresh"
                    )
                }
                MetalRatesTicker(
                    Modifier
                        .height(50.dp)
                        .weight(1f)
                )
                Text(text = currentDateTime.value)
                Spacer(Modifier.width(10.dp))
                IconButton(onClick = {
                    showOption.value = !showOption.value
                }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert, contentDescription = "Options"
                    )
                }
            }
            Spacer(Modifier.height(5.dp))
            Row(Modifier.fillMaxSize()) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .weight(2.5f)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)
                        )
                        .padding(5.dp)
                ) {
                    DraftCustomerDetails(viewModel)
                    Spacer(Modifier.height(5.dp))
                    DraftItemSection(modifier = Modifier.weight(1f), viewModel)
                    Spacer(Modifier.height(5.dp))
                    DraftAddItemSection(itemId, viewModel)
                }
                Spacer(Modifier.width(5.dp))
                Column(
                    Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)
                        )
                        .padding(5.dp)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                        .padding(3.dp)
                ) {
                    DraftDetailSection(Modifier.weight(1f), viewModel)
                    Box(modifier = Modifier
                        .bounceClick {
                            if (viewModel.customerMobile.text.isNotEmpty() && viewModel.selectedItemList.isNotEmpty()) {
                                navHost.navigate(Screens.SellPreview.route)
                            } else {
                                viewModel.snackBarState.value = "Please ensure to add customer and items details"
                            }
                        }
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)
                        )
                        .padding(10.dp), contentAlignment = Alignment.Center) {
                        Text("Proceed", textAlign = TextAlign.Center)
                    }
                }
            }
        }
        if (showOption.value) Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(y = 15.dp, x = (-55).dp)
                .wrapContentHeight()
                .wrapContentWidth()
                .background(Color.White, RoundedCornerShape(16.dp))
                .padding(5.dp)
        ) {
            Column {
                Row(modifier = Modifier.clickable {
                    viewModel.draftUpdateChargeView(!viewModel.showSeparateCharges.value)
                }, verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = viewModel.showSeparateCharges.value, onCheckedChange = {})
                    Text(
                        "Show Charge",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    "Add Sample Items",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        viewModel.draftAddSampleItem()
                    }
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    "Sample Customers:",
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "9876543210 - John Doe",
                    fontSize = 8.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    "9123456789 - Jane Smith",
                    fontSize = 8.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    "9988776655 - Mike Johnson",
                    fontSize = 8.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
        if (viewModel.draftShowAddItemDialog.value) {
            DraftViewAddItemDialog(viewModel)
        }
    }
}

@Composable
fun DraftCustomerDetails(viewModel: InvoiceViewModel) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .padding(5.dp)
    ) {
        Text("Customer Details", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))
        
        Column {
            Row(Modifier) {
                CusOutlinedTextField(
                    viewModel.customerName,
                    placeholderText = "Name",
                    modifier = Modifier.weight(2f)
                )
                Spacer(Modifier.width(5.dp))
                CusOutlinedTextField(
                    viewModel.customerMobile,
                    placeholderText = "Mobile No",
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    keyboardType = KeyboardType.Phone,
                    validation = { input -> if (input.length != 10) "Please Enter Valid Number" else null }
                )
            }
            Spacer(Modifier.height(5.dp))
            CusOutlinedTextField(
                viewModel.customerAddress,
                placeholderText = "Address",
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2
            )
            Spacer(Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "GSTIN/PAN Details : ", textAlign = TextAlign.Center, fontSize = 12.sp
                )
                Spacer(Modifier.width(5.dp))
                CusOutlinedTextField(
                    viewModel.customerGstin,
                    placeholderText = "GSTIN/PAN ID",
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
            }

        }
    }
}

@Composable
fun DraftItemSection(modifier: Modifier, viewModel: InvoiceViewModel) {
    Column(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .padding(5.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Item List", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text("Items: ${viewModel.selectedItemList.size}", fontSize = 12.sp)
        }
        
        Spacer(Modifier.height(5.dp))
        
        if (!viewModel.selectedItemList.isEmpty()) {
            // Prepare header list for TextListView
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
            val itemsData = viewModel.selectedItemList.mapIndexed { index, item ->
                // Use CalculationUtils for price and charge calculations
                val oneUnitPrice =
                    CalculationUtils.metalUnitPrice(item.catName, LocalBaseViewModel.current.metalRates) ?: 0.0
                val price = CalculationUtils.basePrice(item.fnWt ?: 0.0, oneUnitPrice)

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
                    "${(item.cgst ?: 0.0) + (item.sgst ?: 0.0) + (item.igst ?: 0.0)} %",
                )
            }

            TextListView(
                headerList = headerList,
                items = itemsData,
                modifier = Modifier.fillMaxSize(),
                onItemClick = { clickedItemData ->
                    // Find the corresponding item from selectedItemList
                    val itemIndex =
                        clickedItemData[0].removeSuffix(".").toIntOrNull()?.minus(1) ?: 0
                    if (itemIndex >= 0 && itemIndex < viewModel.selectedItemList.size) {
                        val itemToEdit = viewModel.selectedItemList[itemIndex]
                        viewModel.selectedItem.value = itemToEdit
                        viewModel.showAddItemDialog.value = true
                    }
                },
                onItemLongClick = {
                    // Handle long click if needed
                }
            )
        }
    }
}



@Composable
fun DraftAddItemSection(itemId: MutableState<String>, viewModel: InvoiceViewModel) {
    Row(
        Modifier
            .height(50.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .padding(3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .background(
                    MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 10.dp), contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Add Different Item",
            )
        }
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .bounceClick {
                    viewModel.draftShowAddItemDialog.value = true
                }
                .background(
                    MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 15.dp), contentAlignment = Alignment.Center
        ) {
            Text("Add Item", color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

@Composable
fun DraftDetailSection(modifier: Modifier, viewModel: InvoiceViewModel) {
    Column(modifier = modifier.padding(5.dp)) {
        Text("Summary", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Spacer(Modifier.height(10.dp))
        
        // Item Summary Table - Using same design as SellInvoiceScreen
        Row(Modifier.fillMaxWidth()) {
            Text(
                "Item",
                fontSize = 10.sp,
                textAlign = TextAlign.Start
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "M.Amt",
                fontSize = 10.sp,
                textAlign = TextAlign.End
            )
            if (viewModel.showSeparateCharges.value) {
                Spacer(Modifier.width(8.dp))
                Text(
                    "Charge",
                    fontSize = 10.sp,
                    textAlign = TextAlign.End
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "O.Crg",
                    fontSize = 10.sp,
                    textAlign = TextAlign.End
                )
            }
            Spacer(Modifier.width(8.dp))
            Text("Tax", fontSize = 10.sp, textAlign = TextAlign.End)
            Spacer(Modifier.width(8.dp))
            Text(
                "Total",
                fontSize = 10.sp,
                textAlign = TextAlign.End
            )
        }

        LazyColumn(Modifier.fillMaxWidth()) {
            items(viewModel.selectedItemList) { item ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                ) {
                    // Name
                    Text(
                        "${item.itemAddName} ${item.subCatName}",
                        fontSize = 10.sp, 
                        textAlign = TextAlign.Start
                    )
                    Spacer(Modifier.width(8.dp))

                    // M.Amt
                    Text(
                        CalculationUtils.displayPrice(item, viewModel.showSeparateCharges.value).to2FString(),
                        fontSize = 10.sp, 
                        textAlign = TextAlign.End
                    )

                    // Charge
                    if (viewModel.showSeparateCharges.value) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            item.chargeAmount.to2FString(),
                            fontSize = 10.sp, 
                            textAlign = TextAlign.End
                        )
                        // O.Charge
                        Spacer(Modifier.width(8.dp))
                        Text(
                            item.othCrg.to2FString(),
                            fontSize = 10.sp, 
                            textAlign = TextAlign.End
                        )
                    }

                    // Tax
                    Spacer(Modifier.width(8.dp))
                    Text(
                        item.tax.to2FString(),
                        fontSize = 10.sp, 
                        textAlign = TextAlign.End
                    )

                    // Total
                    val itemTotals = CalculationUtils.totalPrice(item.price,item.crg,item.othCrg,item.tax)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        itemTotals.to2FString(),
                        fontSize = 10.sp, 
                        textAlign = TextAlign.End
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))
        DraftSummarySection(viewModel.selectedItemList)
    }
}

@Composable
fun DraftSummarySection(selectedItemList: List<ItemSelectedModel>) {
    val groupedItems = selectedItemList.groupBy { it.catName }

    Column(Modifier.padding(16.dp)) {
        Text("Summary", fontSize = 12.sp, fontWeight = FontWeight.Bold)

        groupedItems.forEach { (metalType, items) ->
            val totalGsWt = items.sumOf { it.gsWt }
            val totalFnWt = items.sumOf { it.fnWt }

                    Row(Modifier.fillMaxWidth()) {
            Text("$metalType Gs/Fn Wt", fontSize = 10.sp)
            Spacer(Modifier.weight(1f))
            Text(
                "${totalGsWt.to2FString()}/${totalFnWt.to2FString()} gm",
                fontSize = 10.sp, 
                textAlign = TextAlign.End
            )
        }
        }

        Spacer(Modifier.height(5.dp))
        HorizontalDivider(thickness = 1.dp)

        // Total calculations using CalculationUtils
        val summary = CalculationUtils.summaryTotals(selectedItemList.toList())
        val totalPrice = summary.totalPriceBeforeTax
        val totalTax = summary.totalTax
        val grandTotal = summary.grandTotal

        Spacer(Modifier.height(5.dp))
        Row(Modifier.fillMaxWidth()) {
            Text("Price (before tax)", fontSize = 10.sp)
            Spacer(Modifier.weight(1f))
            Text(
                "₹${"%.2f".format(totalPrice)}",
                fontSize = 10.sp,
                textAlign = TextAlign.End
            )
        }
        Row(Modifier.fillMaxWidth()) {
            Text("Total Tax", fontSize = 10.sp)
            Spacer(Modifier.weight(1f))
            Text(
                "₹${"%.2f".format(totalTax)}",
                fontSize = 10.sp,
                textAlign = TextAlign.End
            )
        }
        Row(Modifier.fillMaxWidth()) {
            Text(
                "Grand Total (after tax)",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.weight(1f))
            Text(
                "₹${"%.2f".format(grandTotal)}",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold, 
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
fun DraftViewAddItemDialog(viewModel: InvoiceViewModel) {
    AlertDialog(
        onDismissRequest = { viewModel.draftShowAddItemDialog.value = false },
        title = { Text("Add Item") },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(600.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    // Basic Item Info
                    Text("Basic Information", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(5.dp))
                    
                    Row(Modifier.fillMaxWidth()) {
                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.draftDialogItemName,
                            placeholderText = "Item Name",
                            keyboardType = KeyboardType.Text
                        )
                    }
                    
                    Row(Modifier.fillMaxWidth()) {
                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.draftDialogCategoryName,
                            placeholderText = "Category",
                            keyboardType = KeyboardType.Text
                        )
                        Spacer(Modifier.width(5.dp))
                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.draftDialogSubCategoryName,
                            placeholderText = "Sub Category",
                            keyboardType = KeyboardType.Text
                        )
                    }
                    
                    Row(Modifier.fillMaxWidth()) {
                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.draftDialogEntryType,
                            placeholderText = "Entry Type",
                            dropdownItems = EntryType.list(),
                            onDropdownItemSelected = { selected ->
                                when (selected) {
                                    EntryType.Piece.type -> {
                                        viewModel.draftDialogQuantity.text = "1"
                                    }
                                    EntryType.Lot.type -> {
                                        // Keep current quantity
                                    }
                                }
                                viewModel.draftDialogEntryType.text = selected
                            }
                        )
                        Spacer(Modifier.width(5.dp))
                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.draftDialogQuantity,
                            placeholderText = "Quantity",
                            keyboardType = KeyboardType.Number
                        )
                    }
                }
                
                item {
                    // Weight Information
                    Spacer(Modifier.height(10.dp))
                    Text("Weight Information", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(5.dp))
                    
                    Row(Modifier.fillMaxWidth()) {
                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.draftDialogGrossWeight,
                            placeholderText = "Gross Wt (gm)",
                            keyboardType = KeyboardType.Number,
                            onTextChange = {
                                viewModel.draftDialogNetWeight.text = it
                            }
                        )
                        Spacer(Modifier.width(5.dp))
                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.draftDialogNetWeight,
                            placeholderText = "Net Wt (gm)",
                            keyboardType = KeyboardType.Number
                        )
                    }
                    
                    Row(Modifier.fillMaxWidth()) {
                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.draftDialogPurity,
                            placeholderText = "Purity",
                            dropdownItems = Purity.list(),
                            onDropdownItemSelected = { selected ->
                                if (viewModel.draftDialogNetWeight.text.isNotBlank()) {
                                    val ntWtValue = viewModel.draftDialogNetWeight.text.toDoubleOrNull() ?: 0.0
                                    val multiplier = Purity.fromLabel(selected)?.multiplier ?: 1.0
                                    viewModel.draftDialogFineWeight.text = String.format("%.2f", ntWtValue * multiplier)
                                }
                                viewModel.draftDialogPurity.text = selected
                            }
                        )
                        Spacer(Modifier.width(5.dp))
                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.draftDialogFineWeight,
                            placeholderText = "Fine Wt (gm)",
                            keyboardType = KeyboardType.Number
                        )
                    }
                }
                
                item {
                    // Charges Information
                    Spacer(Modifier.height(10.dp))
                    Text("Making Charges Information", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(5.dp))
                    
                    Row(Modifier.fillMaxWidth()) {
                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.draftDialogChargeType,
                            placeholderText = "Charge Type",
                            dropdownItems = ChargeType.list()
                        )
                        Spacer(Modifier.width(5.dp))
                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.draftDialogCharge,
                            placeholderText = "Charge",
                            keyboardType = KeyboardType.Number
                        )
                    }
                    
                    Row(Modifier.fillMaxWidth()) {
                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.draftDialogOtherChargeDescription,
                            placeholderText = "Other Charge Desc",
                            keyboardType = KeyboardType.Text
                        )
                        Spacer(Modifier.width(5.dp))
                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.draftDialogOtherCharge,
                            placeholderText = "Other Charge",
                            keyboardType = KeyboardType.Number
                        )
                    }
                }
                
                item {
                    // Tax Information
                    Spacer(Modifier.height(10.dp))
                    Text("Tax Information", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(5.dp))
                    
                    Row(Modifier.fillMaxWidth()) {
                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.draftDialogCgst,
                            placeholderText = "CGST",
                            keyboardType = KeyboardType.Number
                        )
                        Spacer(Modifier.width(5.dp))
                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.draftDialogSgst,
                            placeholderText = "SGST",
                            keyboardType = KeyboardType.Number
                        )
                        Spacer(Modifier.width(5.dp))
                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.draftDialogIgst,
                            placeholderText = "IGST",
                            keyboardType = KeyboardType.Number
                        )
                    }
                }
                
                item {
                    // Additional Information
                    Spacer(Modifier.height(10.dp))
                    Text("Additional Information", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(5.dp))
                    
                    Row(Modifier.fillMaxWidth()) {
                        CusOutlinedTextField(
                            modifier = Modifier.weight(2f),
                            state = viewModel.draftDialogHuid,
                            placeholderText = "H-UID",
                            keyboardType = KeyboardType.Text
                        )
                    }
                    
                    Row(Modifier.fillMaxWidth()) {
                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.draftDialogDescription,
                            placeholderText = "Description Key",
                            keyboardType = KeyboardType.Text
                        )
                        Spacer(Modifier.width(5.dp))
                        CusOutlinedTextField(
                            modifier = Modifier.weight(2f),
                            state = viewModel.draftDialogDescriptionValue,
                            placeholderText = "Description Value",
                            keyboardType = KeyboardType.Text
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { viewModel.draftAddItem() }) {
                Text("Add Item")
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.draftShowAddItemDialog.value = false }) {
                Text("Cancel")
            }
        }
    )
}