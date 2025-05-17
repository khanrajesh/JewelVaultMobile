package com.velox.jewelvault.ui.screen.sell_invoice

import android.annotation.SuppressLint
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.twotone.Add
import androidx.compose.material.icons.twotone.DateRange
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
import androidx.compose.ui.input.pointer.pointerInput
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
import com.velox.jewelvault.data.roomdb.dto.ItemSelectedModel
import com.velox.jewelvault.ui.components.CusOutlinedTextField
import com.velox.jewelvault.ui.components.InputFieldState
import com.velox.jewelvault.ui.components.QrBarScannerPage
import com.velox.jewelvault.ui.components.bounceClick
import com.velox.jewelvault.utils.LocalNavController
import com.velox.jewelvault.ui.nav.Screens
import com.velox.jewelvault.utils.ChargeType
import com.velox.jewelvault.utils.LocalBaseViewModel
import com.velox.jewelvault.utils.Purity
import com.velox.jewelvault.utils.ioScope
import com.velox.jewelvault.utils.isLandscape
import com.velox.jewelvault.utils.mainScope
import com.velox.jewelvault.utils.rememberCurrentDateTime
import com.velox.jewelvault.utils.to2FString
import kotlinx.coroutines.launch


@Composable
fun SellInvoiceScreen(sellInvoiceViewModel: SellInvoiceViewModel) {
    val baseViewModel = LocalBaseViewModel.current
    val navControl = LocalNavController.current
    val context = LocalContext.current
    val showQrBarScanner = remember { mutableStateOf(false) }

    LaunchedEffect(true) {
        if (baseViewModel.metalRates.isEmpty()) {
            baseViewModel.refreshMetalRates(context = context)
        }
    }

    BackHandler {
        sellInvoiceViewModel.selectedItemList.clear()
        navControl.popBackStack()
    }

    if (!showQrBarScanner.value) {
        if (isLandscape()) {
            SellInvoiceLandscape(showQrBarScanner, sellInvoiceViewModel)
        } else {
            SellInvoicePortrait()
        }
    } else {
        QrBarScannerPage(showPage = showQrBarScanner, scanAndClose = true, onCodeScanned = {
            mainScope {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
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
                    Icon(Icons.Default.Clear, null, modifier = Modifier
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
fun SellInvoicePortrait() {
    Text("Sell invoice Landscape view")
}

@Composable
fun SellInvoiceLandscape(
    showQrBarScanner: MutableState<Boolean>, viewModel: SellInvoiceViewModel
) {
    val context = LocalContext.current
    val navHost = LocalNavController.current
    val baseViewModel = LocalBaseViewModel.current
    val currentDateTime = rememberCurrentDateTime()
    val coroutineScope = rememberCoroutineScope()

    val showOption = remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
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
                    coroutineScope.launch {
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
                        imageVector = Icons.Default.MoreVert, contentDescription = "Refresh"
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
                    CustomerDetails(viewModel)

                    Spacer(Modifier.height(5.dp))

                    ItemSection(modifier = Modifier.weight(1f), viewModel)

                    Spacer(Modifier.height(5.dp))

                    AddItemSection(showQrBarScanner, viewModel)
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

                    DetailSection(Modifier.weight(1f), viewModel)

                    Box(modifier = Modifier
                        .bounceClick {
                            navHost.navigate(Screens.SellPreview.route)
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
                    viewModel.updateChargeView(!viewModel.showSeparateCharges.value)
                }, verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = viewModel.showSeparateCharges.value, onCheckedChange = {})
                    Text(
                        "Show Charge",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Spacer(Modifier.height(3.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = true, onCheckedChange = {

                    })

                    Text("what",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.clickable {

                        })
                }
            }


        }

        if (viewModel.showAddItemDialog.value && viewModel.selectedItem.value != null) {
            ViewAddItemDialog(viewModel)
        }

    }
}

@SuppressLint("DefaultLocale")
@Composable
fun ViewAddItemDialog(
    viewModel: SellInvoiceViewModel,
) {

    val item = viewModel.selectedItem.value!!
    val context = LocalContext.current
    val takeHUID = remember { (InputFieldState(item.huid)) }
    val takeQuantity = remember { (InputFieldState("${item.quantity}")) }
    val takeGsWt = remember { (InputFieldState("${item.gsWt}")) }
    val takeNtWt = remember { (InputFieldState("${item.ntWt}")) }

    val fn = if (takeNtWt.text.isNotBlank()) {
        val ntWtValue = takeNtWt.text.toDoubleOrNull() ?: 0.0
        val multiplier = Purity.fromLabel(item.purity)?.multiplier ?: 1.0
        String.format("%.2f", ntWtValue * multiplier)
    } else ""

    val takeFnWt = remember { (InputFieldState(fn)) }

    val othCrgDes = remember { (InputFieldState("${item.othCrgDes} ")) }
    val othCrg = remember { (InputFieldState("${item.othCrg}")) }

    val isSingleItem = item.quantity == 1

    val baseViewModel = LocalBaseViewModel.current


    baseViewModel.metalRates.forEach {
        Log.d("ViewAddItemDialog", "|${it.metal}|${it.caratOrPurity}|${it.price}")
    }

    val price = if (item.catName.trim().lowercase() == "gold") {
        val price24kOneGram =
            baseViewModel.metalRates.firstOrNull { price -> price.metal == "Gold" && price.caratOrPurity == "24K" }?.price
        val gold100: Double = (100 / 99.9) * (price24kOneGram?.toDoubleOrNull() ?: 0.0)

        if (price24kOneGram == null || gold100 == 0.0) {
            mainScope {
                Toast.makeText(context, "Please load the metal prices", Toast.LENGTH_SHORT)
                    .show()
            }
            viewModel.showAddItemDialog.value = false
            viewModel.selectedItem.value = null
        }

        gold100 * (takeFnWt.text.toDoubleOrNull() ?: 0.0)
    } else
        if (item.catName.trim().lowercase() == "silver") {

            val silverOneGm =
                baseViewModel.metalRates.firstOrNull { price -> price.metal == "Silver" && price.caratOrPurity == "Silver /g" }?.price
                    ?.toDoubleOrNull() ?: 0.0

            if (silverOneGm == 0.0) {
                mainScope {
                    Toast.makeText(context, "Please load the metal prices", Toast.LENGTH_SHORT)
                        .show()
                }
                viewModel.showAddItemDialog.value = false
                viewModel.selectedItem.value = null
                return
            }
            silverOneGm * (takeFnWt.text.toDoubleOrNull() ?: 0.0)
        } else {
            viewModel.showAddItemDialog.value = false
            viewModel.selectedItem.value = null
            mainScope {
                Toast.makeText(context, "Only Gold and Silver", Toast.LENGTH_SHORT).show()
            }
            return
        }

    val charge = when (item.crgType) {
        ChargeType.Percentage.type -> price * (item.crg / 100)
        ChargeType.Piece.type -> item.crg * (takeQuantity.text.toIntOrNull() ?: 0)
        ChargeType.PerGm.type -> {
            item.crg * (takeFnWt.text.toDoubleOrNull() ?: 0.0)
        }

        else -> 0.0
    }


    val tax = (price + charge) * ((item.cgst + item.igst + item.sgst) / 100)


    val onDismiss = {
        viewModel.showAddItemDialog.value = false
        viewModel.selectedItem.value = null
    }

    val onAdd = {
        ioScope {

            val addItem = item.copy(
                quantity = takeQuantity.text.toIntOrNull() ?: 0,
                gsWt = takeGsWt.text.toDoubleOrNull() ?: 0.0,
                ntWt = takeNtWt.text.toDoubleOrNull() ?: 0.0,
                fnWt = takeFnWt.text.toDoubleOrNull() ?: 0.0,
                othCrgDes = othCrgDes.text,
                othCrg = othCrg.text.toDoubleOrNull() ?: 0.0,
                huid = takeHUID.text,
                price = price,
                charge = charge,
                tax = tax
            )


            if (!viewModel.selectedItemList.contains(addItem)) {
                viewModel.selectedItemList.add(addItem)
            }

            viewModel.showAddItemDialog.value = false
            viewModel.selectedItem.value = null
        }
    }

    Dialog(onDismissRequest = onDismiss) {
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
                    .padding(16.dp)
            ) {
                Text("${item.catName} Item Details", style = MaterialTheme.typography.titleLarge)
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
                    "Making Charge Type" to item.crgType,
                    "Making Charges" to item.crg.toString(),
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
                    Row {
                        CusOutlinedTextField(
                            othCrgDes,
                            placeholderText = "O.Charge Des",
                            modifier = Modifier.weight(1f),
                            maxLines = 3,
                            keyboardType = KeyboardType.Text
                        )
                        Spacer(Modifier.width(5.dp))
                        CusOutlinedTextField(
                            othCrg,
                            placeholderText = "Other Charge",
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            keyboardType = KeyboardType.Number
                        )

                    }
                    Spacer(Modifier.height(10.dp))
                    Row {
                        CusOutlinedTextField(
                            takeQuantity,
                            placeholderText = "Take Qty",
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            keyboardType = KeyboardType.Number
                        )
                        Spacer(Modifier.width(5.dp))
                        CusOutlinedTextField(
                            takeHUID,
                            placeholderText = "HUID",
                            modifier = Modifier.weight(1f),
                            maxLines = 3,
                            keyboardType = KeyboardType.Text
                        )
                    }

                    if (!isSingleItem) {
                        Spacer(Modifier.height(10.dp))
                        Row {

                            CusOutlinedTextField(
                                takeGsWt,
                                placeholderText = "Take Gs Wt",
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                keyboardType = KeyboardType.Number
                            )
                            Spacer(Modifier.width(5.dp))
                            CusOutlinedTextField(
                                takeNtWt,
                                placeholderText = "Take Nt Wt",
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                keyboardType = KeyboardType.Number
                            )
                            Spacer(Modifier.width(5.dp))
                            CusOutlinedTextField(
                                takeFnWt,
                                placeholderText = "Take Fn Wt",
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                keyboardType = KeyboardType.Number
                            )
                        }

                    }

                    Spacer(Modifier.height(10.dp))

                    Row(Modifier.height(50.dp)) {
                        Text(
                            "Price: ${String.format("%.2f", price)}",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "Charge: ${String.format("%.2f", charge)}",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "Tax: ${String.format("%.2f", tax)}",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )

                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                    Spacer(Modifier.width(30.dp))
                    TextButton(onClick = { onAdd() }) {
                        Text("Add")
                    }
                }
            }
        }
    }
}


@Composable
fun DetailSection(modifier: Modifier, viewModel: SellInvoiceViewModel) {

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
            Text("Tax", modifier = Modifier.weight(1f), fontSize = 10.sp, textAlign = TextAlign.End)
            Text(
                "Total",
                modifier = Modifier.weight(1.5f),
                fontSize = 10.sp,
                textAlign = TextAlign.End
            )
        }

        LazyColumn(Modifier.fillMaxWidth()) {


            items(viewModel.selectedItemList) {

                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                ) {
                    // Name
                    Text(
                        "${it.itemAddName} ${it.subCatName}",
                        modifier = Modifier.weight(0.7f),
                        fontSize = 10.sp, textAlign = TextAlign.Start
                    )

                    // M.Amt
                    Text(
                        (if (viewModel.showSeparateCharges.value) it.price else (it.price + it.charge + it.othCrg)).to2FString(),
                        modifier = Modifier.weight(1f),
                        fontSize = 10.sp, textAlign = TextAlign.End
                    )

                    // Charge
                    if (viewModel.showSeparateCharges.value) {

                        Text(
                            it.charge.to2FString(),
                            modifier = Modifier.weight(1f),
                            fontSize = 10.sp, textAlign = TextAlign.End
                        )
                        // O.Charge
                        Text(
                            it.othCrg.to2FString(),
                            modifier = Modifier.weight(1f),
                            fontSize = 10.sp, textAlign = TextAlign.End
                        )
                    }


                    // Tax
                    Text(
                        it.tax.to2FString(),
                        modifier = Modifier.weight(1f),
                        fontSize = 10.sp, textAlign = TextAlign.End
                    )

                    // Total
                    val total = it.price + it.charge + it.othCrg + it.tax
                    Text(
                        total.to2FString(),
                        modifier = Modifier.weight(1.5f),
                        fontSize = 10.sp, textAlign = TextAlign.End
                    )
                }
            }
        }


        viewModel.selectedItemList


        Spacer(Modifier.weight(1f))
        SummarySection(viewModel.selectedItemList)
    }
}

@Composable
fun SummarySection(selectedItemList: List<ItemSelectedModel>) {
    val groupedItems = selectedItemList.groupBy { it.catName }

    Column(Modifier.padding(16.dp)) {
        Text("Summary", fontSize = 12.sp, fontWeight = FontWeight.Bold)

        groupedItems.forEach { (metalType, items) ->
            val totalGsWt = items.sumOf { it.gsWt }
            val totalFnWt = items.sumOf { it.fnWt }

            Spacer(modifier = Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                Text("$metalType Gs/Fn Wt", modifier = Modifier.weight(1f), fontSize = 10.sp)
                Text(
                    "${totalGsWt.to2FString()}/${totalFnWt.to2FString()} gm",
                    modifier = Modifier.weight(1f),
                    fontSize = 10.sp, textAlign = TextAlign.End
                )
            }

        }

        Spacer(Modifier.height(5.dp))
        HorizontalDivider(thickness = 1.dp)

        // Total calculations
        val totalPrice = selectedItemList.sumOf { it.price }
        val totalTax = selectedItemList.sumOf { it.tax }
        val grandTotal = totalPrice + totalTax

        Spacer(Modifier.height(5.dp))
        Row(Modifier.fillMaxWidth()) {
            Text("Price (before tax)", modifier = Modifier.weight(1.5f), fontSize = 10.sp)
            Text(
                "₹${"%.2f".format(totalPrice)}",
                modifier = Modifier.weight(1f),
                fontSize = 10.sp,
                textAlign = TextAlign.End
            )
        }
        Row(Modifier.fillMaxWidth()) {
            Text("Total Tax", modifier = Modifier.weight(1.5f), fontSize = 10.sp)
            Text(
                "₹${"%.2f".format(totalTax)}",
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
                "₹${"%.2f".format(grandTotal)}",
                modifier = Modifier.weight(1f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End
            )
        }

    }
}


@Composable
fun ItemSection(modifier: Modifier, viewModel: SellInvoiceViewModel) {
    val haptic = LocalHapticFeedback.current

    Box(
        modifier.fillMaxWidth()
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                .padding(5.dp)
        ) {

            BoxWithConstraints(Modifier.fillMaxSize()) {
                val columnCount = 9
                val columnWidth = maxWidth / columnCount
                val itemsWithHeader = listOf(null) + viewModel.selectedItemList

                LazyColumn {
                    itemsIndexed(itemsWithHeader) { index, item ->
                        /*    Column(
                                Modifier
                                    .padding(2.dp)
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                            ) {
                                Row(Modifier.fillMaxWidth()) {
                                    Text(
                                        "${index + 1}. ",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.DarkGray,
                                        modifier = Modifier.weight(0.5f),
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        "${it.itemId}. ",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.DarkGray,
                                        modifier = Modifier.weight(0.5f),
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        "${it.itemAddName} ${it.subCatName} ",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.DarkGray,
                                        modifier = Modifier.weight(2f),
                                        textAlign = TextAlign.Center
                                    )

                                    Text(
                                        "${it.gsWt}gm",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.DarkGray,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        "${it.fnWt}gm",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.DarkGray,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        "${it.catName}/(${it.purity})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.DarkGray,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        "${it.crg}${if (it.crgType == "%") "%" else ""}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.DarkGray,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Center
                                    )

                                    Text(
                                        "${it.cgst + it.igst + it.sgst}%",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.DarkGray,
                                        modifier = Modifier.weight(0.5f),
                                        textAlign = TextAlign.Center
                                    )

                                    Text(
                                        "${it.huid}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.DarkGray,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Center
                                    )

                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
                                }
                                Spacer(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(MaterialTheme.colorScheme.outline)
                                )
                            }*/

                        val isHeader = index == 0
                        val values = if (isHeader) {
                            listOf(
                                "Sl.No",
                                "Id",
                                "Item",
                                "Qty",
                                "Gs/Nt.Wt",
                                "Fn.Wt",
                                "Metal",
                                "M.Chr",
                                "Tax",
                            )
                        } else {
                            listOf(
                                "${index}.",
                                "${item?.itemId}",
                                "${item?.subCatName} ${item?.itemAddName}",
                                "${item?.quantity} P",
                                "${item?.gsWt}/${item?.ntWt}gm",
                                "${item?.fnWt}/gm",
                                "${item?.catName} (${item?.purity})",
                                "${item?.crg}+${item?.othCrg}",
                                "${(item?.cgst ?: 0.0) + (item?.sgst ?: 0.0) + (item?.igst ?: 0.0)} %",
                            )
                        }

                        Row(
                            modifier = Modifier
                                .pointerInput(item) {
                                    if (!isHeader && item != null) {
                                        detectTapGestures(
                                            onLongPress = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                                            }
                                        )
                                    }
                                }
                                .fillMaxWidth()
                                .height(30.dp)
                        ) {
                            values.forEachIndexed { i, value ->
                                Box(modifier = Modifier.width(columnWidth)) {
                                    Text(
                                        text = value,
                                        fontWeight = if (isHeader) FontWeight.Normal else FontWeight.Normal,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier

                                            .fillMaxWidth()
                                            .padding(horizontal = 2.dp)
                                    )
                                }
                                if (i < values.size - 1) {
                                    Text(
                                        "|",
                                        fontSize = 12.sp,
                                        modifier = Modifier.width(2.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        if (isHeader) {
                            Spacer(
                                Modifier
                                    .height(2.dp)
                                    .fillMaxWidth()
                                    .background(Color.LightGray)
                            )
                        }
                    }
                }
            }


        }
    }
}

@Composable
private fun AddItemSection(
    showQrBarScanner: MutableState<Boolean>, viewModel: SellInvoiceViewModel
) {
    val focusManager = LocalFocusManager.current
    val itemId = remember { mutableStateOf("") }
    val baseViewModel = LocalBaseViewModel.current

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

        Row(
            Modifier
                .fillMaxHeight()
                .background(
                    MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp)
                )
                .padding(3.dp)
        ) {
            Icon(Icons.TwoTone.DateRange, null, modifier = Modifier
                .bounceClick {
                    showQrBarScanner.value = !showQrBarScanner.value
                }
                .fillMaxHeight()
                .aspectRatio(1f)
                .background(
                    MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)
                ))
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxHeight()
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
                            viewModel.getItemById(
                                itemId.value.toInt(),
                                onFailure = {},
                                onSuccess = {
                                    viewModel.showAddItemDialog.value = true
                                })
                            itemId.value = ""
                        }
                        focusManager.clearFocus()
                    }), textStyle = TextStyle(
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                    ), modifier = Modifier.padding(horizontal = 8.dp) // optional inner padding
                )
            }
            Spacer(Modifier.width(10.dp))
            Box(modifier = Modifier
                .bounceClick {
                    if (itemId.value.isNotEmpty()) {
                        viewModel.getItemById(
                            itemId.value.toInt(), onFailure = {

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
                    Text("Add Item")
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
fun CustomerDetails(viewModel: SellInvoiceViewModel) {
    val context = LocalContext.current

    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .padding(5.dp)
    ) {
        Text("Customer Details")
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
                    trailingIcon = Icons.Default.Search,
                    onTrailingIconClick = {
                        viewModel.getCustomerByMobile {
                            if (it == null) {
                                mainScope {
                                    Toast.makeText(
                                        context,
                                        "No Customer Found!",
                                        Toast.LENGTH_SHORT
                                    )
                                        .show()
                                }
                            }
                        }
                    },
                    maxLines = 1,
                    keyboardType = KeyboardType.Phone,
                    validation = { input ->
                        when {
                            input.length != 10 -> "Please Enter Valid Number"
                            else -> null
                        }
                    }
                )
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




