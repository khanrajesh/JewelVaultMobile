package com.velox.jewelvault.ui.screen.sell_invoice

import android.annotation.SuppressLint
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Add
import androidx.compose.material.icons.twotone.CameraAlt
import androidx.compose.material.icons.twotone.Clear
import androidx.compose.material.icons.twotone.MoreVert
import androidx.compose.material.icons.twotone.Refresh
import androidx.compose.material.icons.twotone.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.velox.jewelvault.data.MetalRatesTicker
import com.velox.jewelvault.ui.components.CusOutlinedTextField
import com.velox.jewelvault.ui.components.ExchangeItemDialog
import com.velox.jewelvault.ui.components.InputFieldState
import com.velox.jewelvault.ui.components.QrBarScannerPage
import com.velox.jewelvault.ui.components.RowOrColumn
import com.velox.jewelvault.ui.components.TextListView
import com.velox.jewelvault.ui.components.WidthThenHeightSpacer
import com.velox.jewelvault.ui.components.bounceClick
import com.velox.jewelvault.ui.nav.Screens
import com.velox.jewelvault.utils.CalculationUtils
import com.velox.jewelvault.utils.ChargeType
import com.velox.jewelvault.utils.LocalBaseViewModel
import com.velox.jewelvault.utils.LocalNavController
import com.velox.jewelvault.utils.Purity
import com.velox.jewelvault.utils.ioScope
import com.velox.jewelvault.utils.isLandscape
import com.velox.jewelvault.utils.rememberCurrentDateTime
import com.velox.jewelvault.utils.to3FString
import kotlinx.coroutines.launch


@Composable
fun SellInvoiceScreen(invoiceViewModel: InvoiceViewModel) {
    val baseViewModel = LocalBaseViewModel.current
    val navControl = LocalNavController.current
    val context = LocalContext.current
    val showQrBarScanner = remember { mutableStateOf(false) }
    val itemId = remember { mutableStateOf("") }
    val isLandscapeMode = isLandscape()

    LaunchedEffect(true) {
        if (baseViewModel.metalRates.isEmpty()) {
            baseViewModel.refreshMetalRates(context = context)
        }
    }

    BackHandler {
        invoiceViewModel.customerGstin.text = ""
        invoiceViewModel.customerAddress.text = ""
        invoiceViewModel.customerName.text = ""
        invoiceViewModel.customerMobile.text = ""
        invoiceViewModel.customerSign.value = null
        invoiceViewModel.ownerSign.value = null
        invoiceViewModel.selectedItemList.clear()
        navControl.popBackStack()
    }

    if (!showQrBarScanner.value) {
        SellInvoiceContent(
            isLandscapeMode = isLandscapeMode,
            showQrBarScanner = showQrBarScanner,
            viewModel = invoiceViewModel,
            itemId = itemId
        )
    } else {
        QrBarScannerPage(showPage = showQrBarScanner, scanAndClose = true, onCodeScanned = { code ->
            showQrBarScanner.value = false
            itemId.value = code
            if (code.isNotEmpty()) {
                invoiceViewModel.getItemById(itemId.value, onFailure = {
                    invoiceViewModel.snackBarState.value = "No item found with the id: $code"
                }, onSuccess = {
                    invoiceViewModel.showAddItemDialog.value = true
                })
                itemId.value = ""
            } else {
                invoiceViewModel.snackBarState.value = "Please Scan Valid Code"
            }
        }, overlayContent = {
            BackHandler(enabled = true) {
                // Do nothing = disable back button
            }
            Box(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        "Please scan the item code to add.",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.TwoTone.Clear, null, modifier = Modifier
                        .bounceClick {
                            showQrBarScanner.value = false
                        }
                        .size(50.dp), tint = Color.White)
                }
            }
        })
    }
}


@Composable
fun SellInvoiceContent(
    isLandscapeMode: Boolean,
    showQrBarScanner: MutableState<Boolean>,
    viewModel: InvoiceViewModel,
    itemId: MutableState<String>
) {
    val context = LocalContext.current
    val navHost = LocalNavController.current
    val baseViewModel = LocalBaseViewModel.current
    val currentDateTime = rememberCurrentDateTime()
    val coroutineScope = rememberCoroutineScope()

    val showOption = remember { mutableStateOf(false) }
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
                .background(MaterialTheme.colorScheme.surface)
                .padding(5.dp)
        ) {

            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
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
                        imageVector = Icons.TwoTone.MoreVert, contentDescription = "More options"
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            if (isLandscapeMode) {
                Row(
                    modifier = Modifier
                        .fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .weight(2.5f)
                            .fillMaxHeight()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)
                            )
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item { CustomerDetails(viewModel) }
                        item { AddItemSection(showQrBarScanner, viewModel, itemId) }
                        item {
                            ItemSection(
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
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)
                            )
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item { DetailSection(Modifier.fillMaxWidth(), viewModel) }
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
                                    .background(
                                        MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)
                                    )
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Proceed", textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(8.dp)
                        ) {
                            CustomerDetails(viewModel)

                            Spacer(Modifier.height(8.dp))
                            AddItemSection(showQrBarScanner, viewModel, itemId)
                        }
                    }

                    item {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(8.dp)
                        ) {
                            ItemSection(
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
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(8.dp)
                        ) {

                            DetailSection(Modifier.fillMaxWidth(), viewModel)

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
                                    .background(
                                        MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)
                                    )
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
                    viewModel.updateChargeView(!viewModel.showSeparateCharges.value)
                }, verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = viewModel.showSeparateCharges.value, onCheckedChange = {
                        viewModel.updateChargeView(it)
                    })
                    Text(
                        "Show Charge",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        if (viewModel.showAddItemDialog.value && viewModel.selectedItem.value != null) {
            ViewAddItemDialog(viewModel)
        }

        if (viewModel.showExchangeItemDialog.value) {
            ExchangeItemDialog(
                existingExchangeItems = viewModel.exchangeItemList.toList(),
                metalRates = baseViewModel.metalRates,
                onDismiss = {
                    viewModel.showExchangeItemDialog.value = false
                },
                onSave = { exchangeItemList ->
                    viewModel.updateExchangeItemList(exchangeItemList)
                },
                onClearAll = {
                    viewModel.clearAllExchangeItems()
                })
        }

    }
}

@SuppressLint("DefaultLocale")
@Composable
fun ViewAddItemDialog(
    viewModel: InvoiceViewModel,
) {

    val item = viewModel.selectedItem.value!!
    LocalContext.current
    val takeHUID = remember { (InputFieldState(item.huid)) }
    val takeQuantity = remember { (InputFieldState("${item.quantity}")) }
    val takeGsWt = remember { (InputFieldState("${item.gsWt}")) }
    val takeNtWt = remember { (InputFieldState("${item.ntWt}")) }
    val takeFnWt = remember { (InputFieldState("${item.fnWt}")) }

    val chargeTypeState = remember(item) { InputFieldState(item.crgType) }
    val chargeRateState = remember(item) { InputFieldState("${item.crg}") }
    val othCrgDes = remember { (InputFieldState("${item.othCrgDes} ")) }
    val othCrg = remember { (InputFieldState("${item.othCrg}")) }

    val isSingleItem = item.quantity == 1
    val isEditing = viewModel.selectedItemList.contains(item)

    val baseViewModel = LocalBaseViewModel.current

    baseViewModel.metalRates.forEach {
        Log.d("ViewAddItemDialog", "|${it.metal}|${it.caratOrPurity}|${it.price}")
    }

    // Validate metal rates first
    val metalRatesValidation = CalculationUtils.validateMetalRates(baseViewModel.metalRates)
    if (!metalRatesValidation.isValid) {
        baseViewModel.snackBarState =
            metalRatesValidation.errorMessage ?: "Please load the metal prices"
        viewModel.showAddItemDialog.value = false
        viewModel.selectedItem.value = null
        return
    }

    // Calculate unit price using CalculationUtils
    val oneUnitPrice = CalculationUtils.metalUnitPrice(item.catName, baseViewModel.metalRates)
    if (oneUnitPrice == null) {
        baseViewModel.snackBarState = "Only Gold and Silver are supported"
        viewModel.showAddItemDialog.value = false
        viewModel.selectedItem.value = null
        return
    }

    val fineWeight = takeFnWt.text.toDoubleOrNull() ?: 0.0
    val ntWeight = takeNtWt.text.toDoubleOrNull() ?: 0.0
    val quantity = takeQuantity.text.toIntOrNull() ?: 0

    // Calculate base price
    val price = CalculationUtils.basePrice(fineWeight, oneUnitPrice)

    // Calculate making charge
    val charge = CalculationUtils.makingCharge(
        chargeType = chargeTypeState.text,
        chargeRate = chargeRateState.text.toDoubleOrNull() ?: 0.0,
        basePrice = price,
        quantity = quantity,
        weight = ntWeight
    )

    // Calculate tax
    val tax = CalculationUtils.calculateTax(
        basePrice = price,
        charge = charge + item.othCrg,
        cgstRate = item.cgst,
        sgstRate = item.sgst,
        igstRate = item.igst
    )

    val onDismiss = {
        viewModel.showAddItemDialog.value = false
        viewModel.selectedItem.value = null
    }

    val onSave = {
        ioScope {
            val updatedItem = item.copy(
                quantity = takeQuantity.text.toIntOrNull() ?: 0,
                gsWt = takeGsWt.text.toDoubleOrNull() ?: 0.0,
                ntWt = takeNtWt.text.toDoubleOrNull() ?: 0.0,
                fnWt = takeFnWt.text.toDoubleOrNull() ?: 0.0,
                fnMetalPrice = oneUnitPrice,
                othCrgDes = othCrgDes.text,
                othCrg = othCrg.text.toDoubleOrNull() ?: 0.0,
                crgType = chargeTypeState.text,
                crg = chargeRateState.text.toDoubleOrNull() ?: 0.0,
                huid = takeHUID.text,
                price = price,
                chargeAmount = charge,
                tax = tax,
            )

            if (isEditing) {
                // Update existing item
                val index = viewModel.selectedItemList.indexOf(item)
                if (index != -1) {
                    viewModel.selectedItemList[index] = updatedItem
                }
            } else {
                // Add new item
                if (!viewModel.selectedItemList.contains(updatedItem)) {
                    viewModel.selectedItemList.add(updatedItem)
                }
            }

            viewModel.showAddItemDialog.value = false
            viewModel.selectedItem.value = null
        }
    }

    val onRemove = {
        if (isEditing) {
            viewModel.selectedItemList.remove(item)
            viewModel.showAddItemDialog.value = false
            viewModel.selectedItem.value = null
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        val dialogScrollState = rememberScrollState()
        Surface(
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 640.dp)
                    .verticalScroll(dialogScrollState)
                    .padding(16.dp)
            ) {
                Text(
                    if (isEditing) "Edit ${item.catName} Item" else "${item.catName} Item Details",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(12.dp))

                val details = listOf(
                    "Item Name" to "${item.itemAddName} (${item.itemId})",
                    "Category Name" to "${item.catName} (${item.catId})",
                    "Sub-Category Name" to "${item.subCatName} (${item.subCatId})",
                    "Type" to item.entryType,
                    "Quantity" to item.quantity.toString(),
                    "Gross Weight" to item.gsWt.toString(),
                    "Net Weight" to item.ntWt.toString(),
                    "Fine Weight" to item.fnWt.toString(),
                    "Purity" to item.purity,
                    "Making Charge Type" to chargeTypeState.text,
                    "Making Charges" to chargeRateState.text,
                    "Other Charge Des" to item.othCrgDes,
                    "Other Charges" to item.othCrg.toString(),
                    "CGST" to item.cgst.toString(),
                    "SGST" to item.sgst.toString(),
                    "IGST" to item.igst.toString(),
                    "HUID" to item.huid,
                    "Added On" to item.addDate.toString(),
//                    "Modified On" to item.modifiedDate.toString(),
                    "Store ID" to item.storeId.toString(),
//                    "User ID" to item.userId.toString()
                )
                Column {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .heightIn(max = 270.dp)
                            .padding(vertical = 8.dp)
                    ) {
                        items(details) { (label, value) ->
                            Column(
                                modifier = Modifier.padding(6.dp)
                            ) {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(value, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    RowOrColumn() {
                        CusOutlinedTextField(
                            othCrgDes,
                            placeholderText = "O.Charge Des",
                            modifier = if(it) Modifier.weight(1f) else Modifier,
                            maxLines = 3,
                            keyboardType = KeyboardType.Text
                        )
                        WidthThenHeightSpacer(5.dp)
                        CusOutlinedTextField(
                            othCrg,
                            placeholderText = "Other Charge",
                            modifier = if(it) Modifier.weight(1f) else Modifier,
                            maxLines = 1,
                            keyboardType = KeyboardType.Number
                        )

                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Adjust Making Charge",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(Modifier.height(6.dp))
                    RowOrColumn {
                        CusOutlinedTextField(
                            modifier = if(it) Modifier.weight(1f) else Modifier,
                            state = chargeTypeState,
                            placeholderText = "Making Charge Type",
                            dropdownItems = ChargeType.list(),
                            maxLines = 1
                        )
                        WidthThenHeightSpacer(5.dp)
                        CusOutlinedTextField(
                            modifier = if(it) Modifier.weight(1f) else Modifier,
                            state = chargeRateState,
                            placeholderText = "Making Charge",
                            keyboardType = KeyboardType.Number,
                            maxLines = 1
                        )
                    }
                    RowOrColumn {
                        CusOutlinedTextField(
                            takeQuantity,
                            placeholderText = "Take Qty",
                            modifier = if(it) Modifier.weight(1f) else Modifier,
                            maxLines = 1,
                            keyboardType = KeyboardType.Number,
                            onTextChange = {
                                takeNtWt.clear()
                                takeGsWt.clear()
                                takeFnWt.clear()
                            }
                        )
                        WidthThenHeightSpacer(5.dp)
                        CusOutlinedTextField(
                            takeHUID,
                            placeholderText = "HUID",
                            modifier = if(it) Modifier.weight(1f) else Modifier,
                            maxLines = 3,
                            keyboardType = KeyboardType.Text
                        )
                    }

                    if (!isSingleItem) {
                        Spacer(Modifier.height(10.dp))
                        RowOrColumn {

                            CusOutlinedTextField(
                                takeGsWt,
                                placeholderText = "Take Gs Wt",
                                modifier =if(it) Modifier.weight(1f) else Modifier,
                                maxLines = 1,
                                keyboardType = KeyboardType.Number
                            )
                            WidthThenHeightSpacer(5.dp)
                            CusOutlinedTextField(
                                takeNtWt,
                                placeholderText = "Take Nt Wt",
                                modifier = if(it) Modifier.weight(1f) else Modifier,
                                maxLines = 1,
                                keyboardType = KeyboardType.Number,
                                onTextChange = {
                                    if (it.isNotBlank()) {
                                        val ntWtValue = takeNtWt.text.toDoubleOrNull() ?: 0.0
                                        if (ntWtValue > takeGsWt.text.toDoubleOrNull() ?: 0.0) {
                                            takeNtWt.error = "Nt Wt cannot be greater than Gs Wt"
                                        } else {
                                            val multiplier =
                                                Purity.fromLabel(item.purity)?.multiplier ?: 1.0
                                                Purity.fromLabel(item.purity)?.multiplier ?: 1.0
                                            takeFnWt.text = (ntWtValue * multiplier).to3FString()
                                        }
                                    }
                                })


                            Spacer(Modifier.width(5.dp))
                            CusOutlinedTextField(
                                takeFnWt,
                                placeholderText = "Take Fn Wt",
                                modifier = if(it) Modifier.weight(1f) else Modifier,
                                maxLines = 1,
                                keyboardType = KeyboardType.Number
                            )
                        }

                    }

                    Spacer(Modifier.height(10.dp))

                    RowOrColumn(Modifier.height(50.dp)) {
                        Text(
                            "Price: ${price.to3FString()}",
                            textAlign = TextAlign.Center,
                            modifier = if(it) Modifier.weight(1f) else Modifier
                        )
                        Text(
                            "Charge: ${charge.to3FString()}",
                            textAlign = TextAlign.Center,
                            modifier = if(it) Modifier.weight(1f) else Modifier
                        )
                        Text(
                            "Tax: ${tax.to3FString()}",
                            textAlign = TextAlign.Center,
                            modifier = if(it) Modifier.weight(1f) else Modifier
                        )

                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Remove button (only show when editing)
                    if (isEditing) {
                        TextButton(
                            onClick = onRemove, colors = ButtonDefaults.textButtonColors(
                                contentColor = Color.Red
                            )
                        ) {
                            Text("Remove")
                        }
                    } else {
                        Spacer(Modifier.weight(1f))
                    }

                    // Action buttons
                    Row {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = { onSave() }) {
                            Text(if (isEditing) "Update" else "Add")
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun DetailSection(modifier: Modifier, viewModel: InvoiceViewModel) {

    Column(modifier = modifier.padding(5.dp)) {

        Row(Modifier.fillMaxWidth()) {
            Text(
                "Item",
                modifier = Modifier.weight(0.7f),
                fontSize = 10.sp,
                textAlign = TextAlign.Start
            )
            Text(
                "M.Amt", modifier = Modifier.weight(1f), fontSize = 10.sp, textAlign = TextAlign.End
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
            Text("Tax", modifier = Modifier.weight(1f), fontSize = 10.sp, textAlign = TextAlign.End)
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
                            item.othCrg.to3FString(),
                            modifier = Modifier.weight(1f),
                            fontSize = 10.sp,
                            textAlign = TextAlign.End
                        )
                    }

                    Text(
                        item.tax.to3FString(),
                        modifier = Modifier.weight(1f),
                        fontSize = 10.sp,
                        textAlign = TextAlign.End
                    )

                    val itemTotals = CalculationUtils.totalPrice(
                        item.price, item.chargeAmount, item.othCrg, item.tax
                    )
                    Text(
                        itemTotals.to3FString(),
                        modifier = Modifier.weight(1.5f),
                        fontSize = 10.sp,
                        textAlign = TextAlign.End
                    )
                }
            }
        }

        viewModel.selectedItemList
        Spacer(Modifier.weight(1f))
        SummarySection(viewModel)
    }
}


@Composable
fun SummarySection(viewModel: InvoiceViewModel) {
    // Use CalculationUtils for summary calculations
    val summary = CalculationUtils.summaryTotals(viewModel.selectedItemList)
    val totalExchangeValue = viewModel.getTotalExchangeValue()
    val netPayableAmount = viewModel.getNetPayableAmount()

    Column(Modifier.padding(16.dp)) {
        Text("Summary", fontSize = 12.sp, fontWeight = FontWeight.Bold)

        // Display metal summaries
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

        // Display financial totals using CalculationUtils
        Spacer(Modifier.height(5.dp))
        Row(Modifier.fillMaxWidth()) {
            Text("Price (before tax)", modifier = Modifier.weight(1.5f), fontSize = 10.sp)
            Text(
                "₹${summary.totalPriceBeforeTax.to3FString()}",
                modifier = Modifier.weight(1f),
                fontSize = 10.sp,
                textAlign = TextAlign.End
            )
        }
        Row(Modifier.fillMaxWidth()) {
            Text("Total Tax", modifier = Modifier.weight(1.5f), fontSize = 10.sp)
            Text(
                "₹${summary.totalTax.to3FString()}",
                modifier = Modifier.weight(1f),
                fontSize = 10.sp,
                textAlign = TextAlign.End
            )
        }
        Row(Modifier.fillMaxWidth()) {
            Text(
                "Grand Total (after tax)",
                modifier = Modifier.weight(1.5f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                "₹${summary.grandTotal.to3FString()}",
                modifier = Modifier.weight(1f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.End
            )
        }

        // Show exchange items if any
        if (viewModel.exchangeItemList.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(thickness = 1.dp)
            Spacer(modifier = Modifier.height(5.dp))

            Text(
                "Exchange Items (${viewModel.exchangeItemList.size})",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )

            // Display exchange items in a more structured way
            viewModel.exchangeItemList.forEachIndexed { index, exchangeItem ->
                Card(
                    modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${index + 1}. ${exchangeItem.metalType} ${exchangeItem.purity}, Gs: ${exchangeItem.grossWeight.to3FString()}gm, Fn: ${exchangeItem.fineWeight.to3FString()}gm",
                            modifier = Modifier.weight(2f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            "₹${exchangeItem.exchangeValue.to3FString()}",
                            modifier = Modifier,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End
                        )
                    }
                }
                Spacer(modifier = Modifier.height(1.dp))
            }

            Spacer(modifier = Modifier.height(5.dp))
            Row(Modifier.fillMaxWidth()) {
                Text(
                    "Total Exchange Value",
                    modifier = Modifier.weight(1.5f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "-₹${totalExchangeValue.to3FString()}",
                    modifier = Modifier.weight(1f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.End
                )
            }
        }

        // Net payable amount
        if (viewModel.exchangeItemList.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(thickness = 2.dp)
            Spacer(Modifier.height(5.dp))
            Row(Modifier.fillMaxWidth()) {
                Text(
                    "Net Payable Amount",
                    modifier = Modifier.weight(1.5f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
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


@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun ItemSection(modifier: Modifier, viewModel: InvoiceViewModel) {
    val haptic = LocalHapticFeedback.current
    val baseViewModel = LocalBaseViewModel.current

    baseViewModel.metalRates.forEach {
        Log.d("ViewAddItemDialog", "|${it.metal}|${it.caratOrPurity}|${it.price}")
    }

    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = 250.dp, max = 700.dp)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                .padding(5.dp)
        ) {
            // Prepare header list
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

            // Prepare items data
            val itemsData = viewModel.selectedItemList.mapIndexed { index, item ->
                // Use CalculationUtils for price and charge calculations
                val oneUnitPrice =
                    CalculationUtils.metalUnitPrice(item.catName, baseViewModel.metalRates) ?: 0.0
                val price = CalculationUtils.basePrice(item.fnWt ?: 0.0, oneUnitPrice)

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
                    "${char}\n+ ${item.othCrg}",
                    "${(item.cgst ?: 0.0) + (item.sgst ?: 0.0) + (item.igst ?: 0.0)} %",
                )
            }

            TextListView(
                headerList = headerList,
                items = itemsData,
                modifier = Modifier,
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
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    // Handle long click if needed
                })

            // Exchange Items Section
            if (viewModel.exchangeItemList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "Exchange Items (${viewModel.exchangeItemList.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Exchange items header
                val exchangeHeaderList =
                    listOf("Metal", "Purity", "Gs/Fn.Wt", "Method", "Value", "")

                // Exchange items data
                val exchangeItemsData =
                    viewModel.exchangeItemList.mapIndexed { index, exchangeItem ->
                        listOf(
                            exchangeItem.metalType,
                            exchangeItem.purity,
                            "${exchangeItem.grossWeight.to3FString()}/${exchangeItem.fineWeight.to3FString()}gm",
                            if (exchangeItem.isExchangedByMetal) "Metal Rate" else "Price",
                            "₹${exchangeItem.exchangeValue.to3FString()}",
                            "Edit/Del"
                        )
                    }

                TextListView(
                    headerList = exchangeHeaderList,
                    items = exchangeItemsData,
                    modifier = Modifier.heightIn(max = 200.dp),
                    onItemClick = { clickedItemData ->
                        // Find the corresponding exchange item
                        val itemIndex = exchangeItemsData.indexOf(clickedItemData)
                        if (itemIndex >= 0 && itemIndex < viewModel.exchangeItemList.size) {
                            val exchangeItemToEdit = viewModel.exchangeItemList[itemIndex]
                            viewModel.editExchangeItem(exchangeItemToEdit)
                        }
                    },
                    onItemLongClick = { clickedItemData ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        // Find the corresponding exchange item and delete it
                        val itemIndex = exchangeItemsData.indexOf(clickedItemData)
                        if (itemIndex >= 0 && itemIndex < viewModel.exchangeItemList.size) {
                            val exchangeItemToDelete = viewModel.exchangeItemList[itemIndex]
                            viewModel.deleteExchangeItem(exchangeItemToDelete)
                        }
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
                        "₹${viewModel.getTotalExchangeValue().to3FString()}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}


@Composable
private fun AddItemSection(
    showQrBarScanner: MutableState<Boolean>,
    viewModel: InvoiceViewModel,
    itemId: MutableState<String>
) {
    val focusManager = LocalFocusManager.current

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
                .bounceClick {
                    viewModel.showExchangeItemDialog.value = true
                }
                .padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
            Text(
                text = "Exchange Item (${viewModel.exchangeItemList.size})",
            )
        }
        Spacer(Modifier.weight(1f))
        Row(
            Modifier
                .fillMaxHeight()
                .wrapContentWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp)
                )
                .padding(3.dp).align(Alignment.CenterVertically),

        ) {
            Icon(Icons.TwoTone.CameraAlt, null, modifier = Modifier
                .bounceClick {
                    showQrBarScanner.value = !showQrBarScanner.value
                }
                .fillMaxHeight()
                .aspectRatio(1f)
                .background(
                    MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)
                )
                .padding(5.dp))
            Spacer(Modifier.width(5.dp))
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(100.dp)
                    .background(
                        MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center // this centers the text vertically and horizontally
            ) {
                BasicTextField(
                    value = itemId.value, onValueChange = {
                        itemId.value = it
                    }, keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number, imeAction = ImeAction.Done
                    ), keyboardActions = KeyboardActions(onDone = {
                        if (itemId.value.isNotEmpty()) {
                            viewModel.getItemById(itemId.value, onFailure = {}, onSuccess = {
                                viewModel.showAddItemDialog.value = true
                            })
                            itemId.value = ""
                        }
                        focusManager.clearFocus()
                    }), textStyle = TextStyle(
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color =MaterialTheme.colorScheme.onSurface,
                    ),
                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp), // optional inner padding
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.Center) {
                            if (itemId.value.isEmpty()) {
                                Text(
                                    text = "Item Id",
                                    style = TextStyle(
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    )
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }
            Spacer(Modifier.width(5.dp))
            Box(modifier = Modifier
                .bounceClick {
                    if (itemId.value.isNotEmpty()) {
                        viewModel.getItemById(itemId.value, onFailure = {

                        }, onSuccess = {
                            viewModel.showAddItemDialog.value = true
                        })
                        itemId.value = ""
                    }
                    focusManager.clearFocus()
                }
                .fillMaxHeight()
                .background(
                    MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)
                ), contentAlignment = Alignment.Center) {
                Row {
                    Spacer(Modifier.width(5.dp))
                    Icon(
                        Icons.TwoTone.Add,
                        null,
                    )
                    Spacer(Modifier.width(5.dp))
                }
            }
        }
    }
}


@Composable
fun CustomerDetails(viewModel: InvoiceViewModel) {
    LocalContext.current
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .padding(5.dp)
    ) {
        Text("Customer Details")
        Column {
            if (isLandscape()) {
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
                        trailingIcon = Icons.TwoTone.Search,
                        onTrailingIconClick = {
                            viewModel.getCustomerByMobile()
                        },
                        maxLines = 1,
                        keyboardType = KeyboardType.Phone,
                        validation = { input -> if (input.length != 10) "Please Enter Valid Number" else null })
                }

            } else {
                CusOutlinedTextField(
                    viewModel.customerName, placeholderText = "Name", modifier = Modifier
                )
                Spacer(Modifier.width(5.dp))
                CusOutlinedTextField(
                    viewModel.customerMobile,
                    placeholderText = "Mobile No",
                    modifier = Modifier,
                    trailingIcon = Icons.TwoTone.Search,
                    onTrailingIconClick = {
                        viewModel.getCustomerByMobile()
                    },
                    maxLines = 1,
                    keyboardType = KeyboardType.Phone,
                    validation = { input -> if (input.length != 10) "Please Enter Valid Number" else null })
            }
            Spacer(Modifier.height(5.dp))
            CusOutlinedTextField(
                viewModel.customerAddress,
                placeholderText = "Address",
                modifier = Modifier.fillMaxWidth(),
                maxLines = 1
            )
            Spacer(Modifier.height(5.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "GSTIN/PAN Details : ", textAlign = TextAlign.Center
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




