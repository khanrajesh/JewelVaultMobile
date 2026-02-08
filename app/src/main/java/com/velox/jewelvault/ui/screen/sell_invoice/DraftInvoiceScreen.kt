package com.velox.jewelvault.ui.screen.sell_invoice

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.MoreVert
import androidx.compose.material.icons.twotone.Refresh
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
import com.velox.jewelvault.ui.components.RowOrColumn
import com.velox.jewelvault.ui.components.bounceClick
import com.velox.jewelvault.ui.components.TextListView
import com.velox.jewelvault.ui.components.WidthThenHeightSpacer
import com.velox.jewelvault.ui.components.baseBackground1
import com.velox.jewelvault.ui.components.baseBackground3
import com.velox.jewelvault.ui.components.baseBackground8
import com.velox.jewelvault.ui.components.baseBackground2
import com.velox.jewelvault.ui.nav.Screens
import com.velox.jewelvault.utils.*
import com.velox.jewelvault.utils.CalculationUtils
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.velox.jewelvault.data.roomdb.dto.ExchangeItemDto
import kotlinx.coroutines.launch

@Composable
fun DraftInvoiceScreen(viewModel: InvoiceViewModel) {
    val context = LocalContext.current
    val navHost = LocalNavController.current
    val baseViewModel = LocalBaseViewModel.current
    val currentDateTime = rememberCurrentDateTime()
    val coroutineScope = rememberCoroutineScope()
    val showOption = remember { mutableStateOf(false) }
    val itemId = remember { mutableStateOf("") }

    val metalRateRefresh = {
        coroutineScope.launch {
            baseViewModel.refreshMetalRates(context = context)
        }
    }
    val optionClick = {
        showOption.value = !showOption.value
    }

    Box(
        Modifier.fillMaxSize()
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .baseBackground8()
                .padding(5.dp)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .baseBackground1()
                    .padding(5.dp), verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { metalRateRefresh() }) {
                    Icon(
                        imageVector = Icons.TwoTone.Refresh, contentDescription = "Refresh"
                    )
                }
                MetalRatesTicker(
                    Modifier
                        .height(50.dp)
                        .weight(1f)
                )
                Text(text = currentDateTime.value)
                Spacer(Modifier.width(10.dp))
                IconButton(onClick = optionClick) {
                    Icon(
                        imageVector = Icons.TwoTone.MoreVert, contentDescription = "Options"
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            RowOrColumn(
                rowModifier = Modifier.fillMaxSize(),
                columnModifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) { isLandscape ->
                if (isLandscape) {
                    LazyColumn(
                        modifier = Modifier
                            .weight(2.5f)
                            .fillMaxHeight()
                            .baseBackground1()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item { DraftCustomerDetails(viewModel) }
                        item { DraftAddItemSection(itemId, viewModel) }
                        item {
                            DraftItemSection(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 250.dp, max = 600.dp),
                                viewModel = viewModel
                            )
                        }
                    }
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .baseBackground1()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item { DraftDetailSection(Modifier.fillMaxWidth(), viewModel) }
                        item {
                            Box(
                                modifier = Modifier
                                    .bounceClick {
                                        if (viewModel.customerMobile.text.isNotEmpty() && viewModel.selectedItemList.isNotEmpty()) {
                                            navHost.navigate(Screens.SellPreview.route)
                                        } else {
                                            viewModel.snackBarState.value =
                                                "Please ensure to add customer and items details"
                                        }
                                    }
                                    .fillMaxWidth()
                                    .baseBackground3()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Proceed", textAlign = TextAlign.Center)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .baseBackground1()
                                    .padding(8.dp)
                            ) {
                                DraftCustomerDetails(viewModel)

                                Spacer(Modifier.height(8.dp))
                                DraftAddItemSection(itemId, viewModel)
                            }
                        }

                        item {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .baseBackground1()
                                    .padding(8.dp)
                            ) {
                                DraftItemSection(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 250.dp, max = 600.dp),
                                    viewModel = viewModel
                                )
                            }
                        }
                        item {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .baseBackground1()
                                    .padding(8.dp)
                            ) {
                                DraftDetailSection(Modifier.fillMaxWidth(), viewModel)

                                Spacer(Modifier.height(12.dp))

                                Box(
                                    modifier = Modifier
                                        .bounceClick {
                                            if (viewModel.customerMobile.text.isNotEmpty() && viewModel.selectedItemList.isNotEmpty()) {
                                                navHost.navigate(Screens.SellPreview.route)
                                            } else {
                                                viewModel.snackBarState.value =
                                                    "Please ensure to add customer and items details"
                                            }
                                        }
                                        .fillMaxWidth()
                                        .baseBackground3()
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Proceed", textAlign = TextAlign.Center)
                                }
                            }
                        }
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
                    Checkbox(checked = viewModel.showSeparateCharges.value, onCheckedChange = {
                        viewModel.draftUpdateChargeView(it)
                    })
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
            .baseBackground2()
            .padding(5.dp)
    ) {
        Text("Customer Details")
        Column {
            RowOrColumn {
                CusOutlinedTextField(
                    viewModel.customerName,
                    placeholderText = "Name",
                    modifier = if (it) Modifier.weight(2f) else Modifier
                )
                WidthThenHeightSpacer(5.dp)
                CusOutlinedTextField(
                    viewModel.customerMobile,
                    placeholderText = "Mobile No",
                    modifier = if (it) Modifier.weight(1f) else Modifier,
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
            .baseBackground2()
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
        
        if (viewModel.selectedItemList.isNotEmpty()) {
            // Prepare header list for TextListView
            val headerList = listOf(
                "Sl.No",
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
                val price = CalculationUtils.baseMetalPrice(item.fnWt ?: 0.0, oneUnitPrice)

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
                    "${(item.cgst ?: 0.0) + (item.sgst ?: 0.0) + (item.igst ?: 0.0)} %",
                )
            }

            TextListView(
                headerList = headerList,
                items = itemsData,
                modifier = Modifier.fillMaxSize(),
                onItemClick = { clickedItemData ->
                    val itemIndex = clickedItemData[0].removeSuffix(".").toIntOrNull()?.minus(1)
                    if (itemIndex != null) {
                        viewModel.draftBeginEdit(itemIndex)
                    }
                },
                onItemLongClick = { }
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
            .baseBackground2()
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
                .baseBackground3()
                .padding(horizontal = 15.dp), contentAlignment = Alignment.Center
        ) {
            Text("Add Item", color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

@Composable
fun DraftDetailSection(modifier: Modifier, viewModel: InvoiceViewModel) {
    Column(modifier = modifier.padding(5.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text(
                "Item",
                modifier = Modifier.weight(0.7f),
                fontSize = 10.sp,
                textAlign = TextAlign.Start
            )
            Text(
                "M.Amt",
                modifier = Modifier.weight(1f),
                fontSize = 10.sp,
                textAlign = TextAlign.End
            )
            if (viewModel.showSeparateCharges.value) {
                Text(
                    "Charge",
                    modifier = Modifier.weight(1f),
                    fontSize = 10.sp,
                    textAlign = TextAlign.End
                )
                Text(
                    "O.Crg",
                    modifier = Modifier.weight(1f),
                    fontSize = 10.sp,
                    textAlign = TextAlign.End
                )
            }
            Text(
                "Total",
                modifier = Modifier.weight(1.5f),
                fontSize = 10.sp,
                textAlign = TextAlign.End
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            viewModel.selectedItemList.forEach { item ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 30.dp)
                ) {
                    Text(
                        "${item.itemAddName} ${item.subCatName}",
                        modifier = Modifier.weight(0.7f),
                        fontSize = 10.sp,
                        textAlign = TextAlign.Start
                    )

                    Text(
                        CalculationUtils.displayPrice(
                            item, viewModel.showSeparateCharges.value
                        ).to3FString(),
                        modifier = Modifier.weight(1f),
                        fontSize = 10.sp,
                        textAlign = TextAlign.End
                    )

                    if (viewModel.showSeparateCharges.value) {
                        Text(
                            item.chargeAmount.to3FString(),
                            modifier = Modifier.weight(1f),
                            fontSize = 10.sp,
                            textAlign = TextAlign.End
                        )
                        Text(
                            item.compCrg.to3FString(),
                            modifier = Modifier.weight(1f),
                            fontSize = 10.sp,
                            textAlign = TextAlign.End
                        )
                    }

                    val itemTotals =
                        item.price+ item.crg+ item.compCrg
                    Text(
                        itemTotals.to3FString(),
                        modifier = Modifier.weight(1.5f),
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
    val summary = CalculationUtils.summaryTotals(selectedItemList,
        emptyList<ExchangeItemDto>() as SnapshotStateList<ExchangeItemDto>, 0.0)

    Column(Modifier.padding(16.dp)) {
        Text("Summary", fontSize = 12.sp, fontWeight = FontWeight.Bold)

        summary.metalSummaries.forEach { metalSummary ->
            Row(Modifier.fillMaxWidth()) {
                Text(
                    "${metalSummary.metalType} Gs/Fn Wt",
                    modifier = Modifier.weight(1f),
                    fontSize = 10.sp
                )
                Text(
                    "${metalSummary.totalGrossWeight.to3FString()}/${metalSummary.totalFineWeight.to3FString()} gm",
                    modifier = Modifier.weight(1f),
                    fontSize = 10.sp,
                    textAlign = TextAlign.End
                )
            }
        }

        Spacer(Modifier.height(5.dp))
        HorizontalDivider(thickness = 1.dp)

        // Total calculations using CalculationUtils
        val totalPrice = summary.totalPriceBeforeTax
        val totalTax = summary.totalTax
        val grandTotal = summary.grandTotal

        Spacer(Modifier.height(5.dp))
        Row(Modifier.fillMaxWidth()) {
            Text("Price (before tax)", modifier = Modifier.weight(1.5f), fontSize = 10.sp)
            Text(
                "ƒ,1${totalPrice.to3FString()}",
                modifier = Modifier.weight(1f),
                fontSize = 10.sp,
                textAlign = TextAlign.End
            )
        }
        Row(Modifier.fillMaxWidth()) {
            Text("Total Tax", modifier = Modifier.weight(1.5f), fontSize = 10.sp)
            Text(
                "ƒ,1${totalTax.to3FString()}",
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
                "ƒ,1${grandTotal.to3FString()}",
                modifier = Modifier.weight(1f),
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
        title = { Text(if (viewModel.draftEditingItemIndex.value != null) "Edit Item" else "Add Item") },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(600.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Column() {
                        // Basic Item Info
                        Text("Basic Information", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(5.dp))

                        RowOrColumn {
                            CusOutlinedTextField(
                                modifier = if (it) Modifier.weight(1f) else Modifier,
                                state = viewModel.draftDialogItemName,
                                placeholderText = "Item Name",
                                keyboardType = KeyboardType.Text
                            )
                        }

                        RowOrColumn {
                            CusOutlinedTextField(
                                modifier = if (it) Modifier.weight(1f) else Modifier,
                                state = viewModel.draftDialogCategoryName,
                                placeholderText = "Category",
                                keyboardType = KeyboardType.Text
                            )
                            WidthThenHeightSpacer(5.dp)
                            CusOutlinedTextField(
                                modifier = if (it) Modifier.weight(1f) else Modifier,
                                state = viewModel.draftDialogSubCategoryName,
                                placeholderText = "Sub Category",
                                keyboardType = KeyboardType.Text
                            )
                        }

                        RowOrColumn {
                            CusOutlinedTextField(
                                modifier = if (it) Modifier.weight(1f) else Modifier,
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
                            WidthThenHeightSpacer(5.dp)
                            CusOutlinedTextField(
                                modifier = if (it) Modifier.weight(1f) else Modifier,
                                state = viewModel.draftDialogQuantity,
                                placeholderText = "Quantity",
                                keyboardType = KeyboardType.Number
                            )
                        }
                    }

                }
                
                item {
                    Column() {
                        // Weight Information
                        Spacer(Modifier.height(10.dp))
                        Text("Weight Information", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(5.dp))

                        RowOrColumn {
                            CusOutlinedTextField(
                                modifier = if (it) Modifier.weight(1f) else Modifier,
                                state = viewModel.draftDialogGrossWeight,
                                placeholderText = "Gross Wt (gm)",
                                keyboardType = KeyboardType.Number,
                                onTextChange = {
                                    viewModel.draftDialogNetWeight.text = it
                                }
                            )
                            WidthThenHeightSpacer(5.dp)
                            CusOutlinedTextField(
                                modifier = if (it) Modifier.weight(1f) else Modifier,
                                state = viewModel.draftDialogNetWeight,
                                placeholderText = "Net Wt (gm)",
                                keyboardType = KeyboardType.Number
                            )
                        }

                        RowOrColumn {
                            CusOutlinedTextField(
                                modifier = if (it) Modifier.weight(1f) else Modifier,
                                state = viewModel.draftDialogPurity,
                                placeholderText = "Purity",
                                dropdownItems = Purity.list(),
                                onDropdownItemSelected = { selected ->
                                    if (viewModel.draftDialogNetWeight.text.isNotBlank()) {
                                        val ntWtValue = viewModel.draftDialogNetWeight.text.toDoubleOrNull() ?: 0.0
                                        val multiplier = Purity.fromLabel(selected)?.multiplier ?: 1.0
                                        viewModel.draftDialogFineWeight.text = (ntWtValue * multiplier).to3FString()
                                    }
                                    viewModel.draftDialogPurity.text = selected
                                }
                            )
                            WidthThenHeightSpacer(5.dp)
                            CusOutlinedTextField(
                                modifier = if (it) Modifier.weight(1f) else Modifier,
                                state = viewModel.draftDialogFineWeight,
                                placeholderText = "Fine Wt (gm)",
                                keyboardType = KeyboardType.Number
                            )
                        }
                    }

                }
                
                item {
                    Column() {
                        // Charges Information
                        Spacer(Modifier.height(10.dp))
                        Text("Making Charges Information", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(5.dp))

                        RowOrColumn {
                            CusOutlinedTextField(
                                modifier = if (it) Modifier.weight(1f) else Modifier,
                                state = viewModel.draftDialogChargeType,
                                placeholderText = "Charge Type",
                                dropdownItems = ChargeType.list()
                            )
                            WidthThenHeightSpacer(5.dp)
                            CusOutlinedTextField(
                                modifier = if (it) Modifier.weight(1f) else Modifier,
                                state = viewModel.draftDialogCharge,
                                placeholderText = "Charge",
                                keyboardType = KeyboardType.Number
                            )
                        }

                        RowOrColumn {
                            CusOutlinedTextField(
                                modifier = if (it) Modifier.weight(1f) else Modifier,
                                state = viewModel.draftDialogOtherChargeDescription,
                                placeholderText = "Other Charge Desc",
                                keyboardType = KeyboardType.Text
                            )
                            WidthThenHeightSpacer(5.dp)
                            CusOutlinedTextField(
                                modifier = if (it) Modifier.weight(1f) else Modifier,
                                state = viewModel.draftDialogOtherCharge,
                                placeholderText = "Other Charge",
                                keyboardType = KeyboardType.Number
                            )
                        }
                    }

                }
                
                item {
                    Column() {
                        // Tax Information
                        Spacer(Modifier.height(10.dp))
                        Text("Tax Information", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(5.dp))

                        RowOrColumn {
                            CusOutlinedTextField(
                                modifier = if (it) Modifier.weight(1f) else Modifier,
                                state = viewModel.draftDialogCgst,
                                placeholderText = "CGST",
                                keyboardType = KeyboardType.Number
                            )
                            WidthThenHeightSpacer(5.dp)
                            CusOutlinedTextField(
                                modifier = if (it) Modifier.weight(1f) else Modifier,
                                state = viewModel.draftDialogSgst,
                                placeholderText = "SGST",
                                keyboardType = KeyboardType.Number
                            )
                            WidthThenHeightSpacer(5.dp)
                            CusOutlinedTextField(
                                modifier = if (it) Modifier.weight(1f) else Modifier,
                                state = viewModel.draftDialogIgst,
                                placeholderText = "IGST",
                                keyboardType = KeyboardType.Number
                            )
                        }
                    }
                }
                
                item {
                    Column() {
                        // Additional Information
                        Spacer(Modifier.height(10.dp))
                        Text("Additional Information", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(5.dp))

                        RowOrColumn {
                            CusOutlinedTextField(
                                modifier = if (it) Modifier.weight(2f) else Modifier,
                                state = viewModel.draftDialogHuid,
                                placeholderText = "H-UID",
                                keyboardType = KeyboardType.Text
                            )
                        }

                        RowOrColumn {
                            CusOutlinedTextField(
                                modifier = if (it) Modifier.weight(1f) else Modifier,
                                state = viewModel.draftDialogDescription,
                                placeholderText = "Description Key",
                                keyboardType = KeyboardType.Text
                            )
                            WidthThenHeightSpacer(5.dp)
                            CusOutlinedTextField(
                                modifier = if (it) Modifier.weight(2f) else Modifier,
                                state = viewModel.draftDialogDescriptionValue,
                                placeholderText = "Description Value",
                                keyboardType = KeyboardType.Text
                            )
                        }
                    }

                }
            }
        },
        confirmButton = {
            Row {
                if (viewModel.draftEditingItemIndex.value != null) {
                    TextButton(onClick = { viewModel.draftRemoveEditingItem() }) {
                        Text("Delete")
                    }
                }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = { viewModel.draftAddItem() }) {
                    Text(if (viewModel.draftEditingItemIndex.value != null) "Update" else "Add Item")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = {
                viewModel.draftShowAddItemDialog.value = false
            }) {
                Text("Cancel")
            }
        }
    )
}
