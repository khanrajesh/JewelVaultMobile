package com.velox.jewelvault.ui.screen.inventory

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velox.jewelvault.BaseViewModel
import com.velox.jewelvault.data.roomdb.entity.ItemEntity
import com.velox.jewelvault.ui.components.CusOutlinedTextField
import com.velox.jewelvault.ui.components.RowOrColumn
import com.velox.jewelvault.ui.components.TextListView
import com.velox.jewelvault.ui.components.WidthThenHeightSpacer
import com.velox.jewelvault.ui.components.bounceClick
import com.velox.jewelvault.ui.components.baseBackground0
import com.velox.jewelvault.ui.components.baseBackground1
import com.velox.jewelvault.ui.components.baseBackground2
import com.velox.jewelvault.ui.screen.bluetooth.ManagePrintersViewModel
import com.velox.jewelvault.ui.theme.LightGreen
import com.velox.jewelvault.ui.theme.LightRed
import com.velox.jewelvault.utils.ChargeType
import com.velox.jewelvault.utils.EntryType
import com.velox.jewelvault.utils.FileManager
import com.velox.jewelvault.utils.InputValidator
import com.velox.jewelvault.utils.LocalBaseViewModel
import com.velox.jewelvault.utils.LocalSubNavController
import com.velox.jewelvault.utils.PrintUtils
import com.velox.jewelvault.utils.Purity
import com.velox.jewelvault.utils.generateId
import com.velox.jewelvault.utils.ioScope
import com.velox.jewelvault.utils.isLandscape
import com.velox.jewelvault.utils.to3FString
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.sql.Timestamp

// Helper function to convert ItemEntity to List<String>
private fun ItemEntity.toListString(index: Int): List<String> = listOf(
    "$index",
    "${catName} (${catId})",
    "${subCatName} (${subCatId})",
    itemId,
    itemAddName,
    entryType,
    quantity.toString(),
    gsWt.to3FString(),
    ntWt.to3FString(),
    unit,
    purity,
    fnWt.to3FString(),
    crgType,
    crg.to3FString(),
    othCrgDes,
    othCrg.to3FString(),
    (cgst + sgst + igst).to3FString(),
    huid,
    addDate.toString(),
    addDesKey,
    addDesValue,
    purchaseOrderId,
)


@Composable
fun InventoryItemScreen(
    managePrintersViewModel: ManagePrintersViewModel,
    inventoryViewModel: InventoryViewModel,
    catId: String,
    catName: String,
    subCatId: String,
    subCatName: String
) {
    inventoryViewModel.currentScreenHeadingState.value = "Sub Category"

    val baseViewModel = LocalBaseViewModel.current
    val addItem = remember { mutableStateOf(false) }
    val showDialog = remember { mutableStateOf(false) }
    val selectedItem = remember { mutableStateOf<ItemEntity?>(null) }
    val isUpdateMode = remember { mutableStateOf(false) }
    val itemBeingUpdated = remember { mutableStateOf<ItemEntity?>(null) }
    val context = LocalContext.current



    Box(Modifier.fillMaxSize()) {

        LandscapeInventoryItemScreen(
            inventoryViewModel,
            catId,
            catName,
            subCatId,
            subCatName,
            addItem,
            showDialog,
            selectedItem,
            isUpdateMode,
            itemBeingUpdated,
        )

        PrintInfoDialog(
            showDialog,
            selectedItem,
            context,
            itemBeingUpdated,
            inventoryViewModel,
            subCatId,
            addItem,
            isUpdateMode,
            baseViewModel,
            managePrintersViewModel
        )
    }
}

@Composable
fun LandscapeInventoryItemScreen(
    inventoryViewModel: InventoryViewModel,
    catId: String,
    catName: String,
    subCatId: String,
    subCatName: String,
    addItem: MutableState<Boolean>,
    showDialog: MutableState<Boolean>,
    selectedItem: MutableState<ItemEntity?>,
    isUpdateMode: MutableState<Boolean>,
    itemBeingUpdated: MutableState<ItemEntity?>,
) {
    LocalSubNavController.current
    LaunchedEffect(true) {
        delay(200)
        inventoryViewModel.setCategoryOverrides(catId, catName, subCatId, subCatName)

        // Call filterItems() which is async
        inventoryViewModel.filterItems()
    }
    val clipboardManager = LocalClipboardManager.current


    val haptic = LocalHapticFeedback.current


    Box(
        Modifier
            .fillMaxSize()
            .baseBackground0()
            .padding(5.dp)
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth()) {
                Text("$catName > $subCatName")
                Spacer(Modifier.weight(1f))
                Text("Add Item", modifier = Modifier.clickable {
                    addItem.value = true
                })

            }
            Spacer(Modifier.height(5.dp))
            Spacer(
                Modifier
                    .height(2.dp)
                    .fillMaxWidth()
                    .background(Color.LightGray)
            )
            Spacer(Modifier.height(5.dp))
            AddItemSection(
                addItem,
                catId,
                subCatId,
                catName,
                subCatName,
                inventoryViewModel,
                isUpdateMode,
                itemBeingUpdated.value
            )


            // Show loader while loading, otherwise show the list
            val isLoading by inventoryViewModel.loadingState
            if (isLoading) {
                // Loading state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Loading items...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                TextListView(
                    headerList = inventoryViewModel.itemHeaderList,
                    items = inventoryViewModel.itemList.mapIndexed { index, item ->
                        item.toListString(index + 1)
                    },
                    onItemClick = { item ->
                        item[0]
                        clipboardManager.setText(AnnotatedString(item[3]))
                        inventoryViewModel.snackBarState.value = "Item ID copied: ${item[3]}"
                    },
                    onItemLongClick = { itemData ->
                        val itemId = itemData[3] // itemId is at index 3
                        val item = inventoryViewModel.itemList.find { it.itemId == itemId }
                        item?.let {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            selectedItem.value = it
                            showDialog.value = true
                        }
                    })
            }
        }


    }


}

@Composable
private fun PrintInfoDialog(
    showDialog: MutableState<Boolean>,
    selectedItem: MutableState<ItemEntity?>,
    context: Context,
    itemBeingUpdated: MutableState<ItemEntity?>,
    inventoryViewModel: InventoryViewModel,
    subCatId: String,
    addItem: MutableState<Boolean>,
    isUpdateMode: MutableState<Boolean>,
    baseViewModel: BaseViewModel,
    managePrintersViewModel: ManagePrintersViewModel
) {
    if (showDialog.value && selectedItem.value != null) {
        val itemForDialog = selectedItem.value!!
        val qrBitmap = remember(itemForDialog.itemId) {
            PrintUtils.generateQRCode(PrintUtils.buildItemQrPayload(itemForDialog), 128)
        }
        var qrUri by remember(itemForDialog.itemId) { mutableStateOf<Uri?>(null) }
        val logoUri = remember { FileManager.getLogoFileUri(context) }
        LaunchedEffect(qrBitmap) {
            if (qrBitmap != null) {
                try {
                    val cacheFile = File(context.cacheDir, "qr_${itemForDialog.itemId}.png")
                    FileOutputStream(cacheFile).use { out ->
                        qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    qrUri = Uri.fromFile(cacheFile)
                } catch (_: Exception) {
                }
            } else {
                qrUri = null
            }
        }

        AlertDialog(
            onDismissRequest = { showDialog.value = false },
            title = { Text("Item Details") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text("Name: ${selectedItem.value?.itemAddName}")
                    Text("id: ${selectedItem.value?.itemId},cat: ${selectedItem.value?.catId},id: ${selectedItem.value?.subCatId}")
                    Text("Purity: ${selectedItem.value?.purity}")
                    Text("Quantity: ${selectedItem.value?.quantity}")
                    Text("Net Weight: ${selectedItem.value?.gsWt}")
                    // Add more fields as needed...

                    Row {
                        if (qrBitmap != null) {
                            Column {
                                Spacer(Modifier.height(8.dp))
                                Text("QR Preview:")
                                Spacer(Modifier.height(4.dp))
                                Image(
                                    bitmap = qrBitmap.asImageBitmap(),
                                    contentDescription = "QR",
                                    modifier = Modifier
                                        .width(80.dp)
                                        .height(80.dp)
                                )
                            }
                        }
                    }


                }
            },
            confirmButton = {
                Row {
                    Column {
                        TextButton(onClick = {
                            ioScope {
                                if (selectedItem.value != null) {
                                    itemBeingUpdated.value = selectedItem.value
                                    itemBeingUpdated.value?.let {
                                        inventoryViewModel.populateUpdateFields(it, subCatId) {
                                            showDialog.value = false
                                            addItem.value = true
                                            isUpdateMode.value = true
                                        }
                                    }
                                }
                            }
                        }) {
                            Text("Update", color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = {
                            if (selectedItem.value != null) {
                                inventoryViewModel.safeDeleteItem(
                                    itemId = selectedItem.value!!.itemId,
                                    catId = selectedItem.value!!.catId,
                                    subCatId = selectedItem.value!!.subCatId,
                                    onSuccess = {
                                        baseViewModel.snackBarState = "Item deleted successfully"
                                    },
                                    onFailure = {
                                        baseViewModel.snackBarState = "Unable to delete item."
                                    })

                                selectedItem.value = null
                            } else {
                                baseViewModel.snackBarState = "Please select valid item"
                            }

                            showDialog.value = false
                        }) {
                            Text("Delete", color = Color.Red)
                        }

                    }
                    Spacer(Modifier.width(8.dp))

//                    Column {
//                        TextButton(onClick = {
//                            if (selectedItem.value != null) {
//                                PrintUtils.generateItemExcelAndPrint(
//                                    context, selectedItem.value!!
//                                ) {
//                                    showDialog.value = false
//                                }
//                            }
//                        }) {
//                            Text("Print", color = MaterialTheme.colorScheme.primary)
//                        }
//                        Spacer(Modifier.height(8.dp))
//                        TextButton(onClick = {
//                            if (selectedItem.value != null) {
//                                managePrintersViewModel.printItemLabel(
//                                    selectedItem.value!!, context, qrUri, logoUri
//                                )
//                                showDialog.value = false
//                            }
//                        }) {
//                            Text("Direct Print", color = MaterialTheme.colorScheme.secondary)
//                        }

//                    }
//                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = {
                        if (selectedItem.value != null) {
                            managePrintersViewModel.printItemWithDefaultTemplate(
                                selectedItem.value!!, context
                            )
                            showDialog.value = false
                        }
                    }) {
                        Text("Print with Template", color = MaterialTheme.colorScheme.tertiary)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDialog.value = false
                }) {
                    Text("Cancel")
                }
            })
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun AddItemSection(
    addItem: MutableState<Boolean>,
    catId: String,
    subCatId: String,
    catName: String,
    subCatName: String,
    viewModel: InventoryViewModel,
    isUpdateMode: MutableState<Boolean>,
    itemBeingUpdated: ItemEntity?
) {
    if (addItem.value) {
        Column(
            Modifier
                .fillMaxWidth()
                .baseBackground1()
                .padding(5.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                if (isUpdateMode.value) "Update Item" else "Add Item",
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp
            )
            Spacer(Modifier.height(5.dp))

            Column(
                Modifier
                    .baseBackground2()
                    .padding(5.dp)
            ) {
                RowOrColumn(
                    rowModifier = Modifier.fillMaxWidth(), columnModifier = Modifier.fillMaxWidth()
                ) { //purchase order section

                    Button(
                        onClick = {
                            viewModel.isSelf.value = !viewModel.isSelf.value
                            // Clear validation errors when switching modes
                            viewModel.purity.error = ""
                            viewModel.fnWt.error = ""
                        }, colors = ButtonDefaults.buttonColors(
                            containerColor = if (viewModel.isSelf.value) LightGreen else LightRed
                        )
                    ) {
                        Text(
                            if (viewModel.isSelf.value) "SELF" else "PURCHASED",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (!viewModel.isSelf.value) {
                        WidthThenHeightSpacer(5.dp)
                        CusOutlinedTextField(
                            viewModel.billDate,
                            placeholderText = "Bill Date",
                            modifier = Modifier,
                            isDatePicker = true,
                            onDateSelected = {
                                viewModel.getBillsFromDate()
                            })

                        if (viewModel.purchaseOrdersByDate.isNotEmpty()) {
                            WidthThenHeightSpacer(5.dp)
                            CusOutlinedTextField(
                                modifier = Modifier,
                                state = viewModel.billNo,
                                placeholderText = "Bill No",
                                readOnly = isUpdateMode.value, // Make read-only in update mode
                                dropdownItems = if (isUpdateMode.value) emptyList() else viewModel.purchaseOrdersByDate.map { it.billNo },
                                onDropdownItemSelected = { sel ->
                                    val item =
                                        viewModel.purchaseOrdersByDate.find { it.billNo == sel }
                                    if (item != null) {
                                        viewModel.getPurchaseOrderItemDetails(item, subCatId)
                                    } else {
                                        viewModel.snackBarState.value =
                                            "Unable to find purchase order item"
                                    }
                                })
                        }

                        if (viewModel.billItemDetails.value.isNotBlank()) {
                            WidthThenHeightSpacer(5.dp)
                            var showDetailsDialog by remember { mutableStateOf(false) }

                            Text(
                                modifier = Modifier.clickable { showDetailsDialog = true },
                                text = "${viewModel.billItemDetails.value} ",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                lineHeight = 12.sp
                            )

                            // Detailed Purchase Order Dialog
                            if (showDetailsDialog) {
                                val selectedPurchase = viewModel.purchaseOrdersByDate.find {
                                    viewModel.billItemDetails.value.contains(it.billNo)
                                }

                                if (selectedPurchase != null) {
                                    var detailedReport by remember { mutableStateOf("Loading...") }

                                    LaunchedEffect(selectedPurchase, subCatName) {
                                        detailedReport = viewModel.getDetailedPurchaseOrderReport(
                                            selectedPurchase, subCatId, subCatName
                                        )
                                    }

                                    AlertDialog(
                                        onDismissRequest = { showDetailsDialog = false },
                                        title = {
                                            Text(
                                                text = "Purchase Order Details",
                                                style = MaterialTheme.typography.headlineSmall
                                            )
                                        },
                                        text = {
                                            LazyColumn(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(max = 400.dp)
                                            ) {
                                                item {
                                                    Text(
                                                        text = detailedReport,
                                                        fontSize = 12.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        lineHeight = 16.sp
                                                    )
                                                }
                                            }
                                        },
                                        confirmButton = {
                                            TextButton(
                                                onClick = { showDetailsDialog = false }) {
                                                Text("Close")
                                            }
                                        })
                                }
                            }
                        }
                    }

                    // Show remaining weight guidance for fine weight input
                    if (!viewModel.isSelf.value && viewModel.purchaseItems.isNotEmpty() && viewModel.purity.text.isNotBlank()) {
                        Spacer(Modifier.width(5.dp))

                        var remainingWeight by remember { mutableStateOf(0.0) }

                        LaunchedEffect(viewModel.purity.text, viewModel.purchaseItems.size) {
                            remainingWeight = viewModel.getRemainingFineWeightForPurity(
                                viewModel.purity.text, subCatName
                            )
                        }

                        if (remainingWeight > 0) {
                            val inputWeight = viewModel.fnWt.text.toDoubleOrNull() ?: 0.0
                            val difference = remainingWeight - inputWeight

                            val guidanceText = if (viewModel.fnWt.text.isBlank()) {
                                "Remaining:  ${remainingWeight.to3FString()} g"
                            } else if (difference > 0) {
                                "Still need: ${difference.to3FString()}g (Remaining: ${remainingWeight.to3FString()}g)"
                            } else if (difference < -0.01) {
                                "Exceeds by: ${(-difference).to3FString()}g (Remaining: ${remainingWeight.to3FString()}g)"
                            } else {
                                "✅ Complete (${remainingWeight.to3FString()}g)"
                            }

                            val guidanceColor = when {
                                difference > 0 -> MaterialTheme.colorScheme.primary
                                difference < -0.01 -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.primary
                            }

                            Text(
                                modifier = Modifier,
                                text = guidanceText,
                                fontSize = 11.sp,
                                color = guidanceColor,
                                lineHeight = 12.sp
                            )
                        }
                    }
                }
            }


            Spacer(Modifier.height(5.dp))


            RowOrColumn(Modifier.fillMaxWidth(),   Modifier.fillMaxWidth()) {

                CusOutlinedTextField(
                    modifier = if(it) Modifier.weight(1f) else Modifier,
                    state = viewModel.addToName,
                    placeholderText = "Add to Name",
                    keyboardType = KeyboardType.Text
                )

                WidthThenHeightSpacer(5.dp)

                CusOutlinedTextField(
                    modifier = if(it) Modifier.weight(1f) else Modifier,
                    state = viewModel.entryType,
                    placeholderText = "Entry Type",
                    dropdownItems = EntryType.list(),
                    onDropdownItemSelected = { selected ->
                        when (selected) {
                            EntryType.Piece.type -> {
                                viewModel.qty.text = "1"
                            }

                            EntryType.Lot.type -> {

                            }

                            else -> {
                                viewModel.entryType.text = selected
                            }
                        }
                        viewModel.entryType.text = selected
                    })


                WidthThenHeightSpacer(5.dp)

                CusOutlinedTextField(
                    modifier = if(it) Modifier.weight(1f) else Modifier,
                    state = viewModel.qty,
                    placeholderText = "Quantity",
                    keyboardType = KeyboardType.Number,
                )

            }
            Spacer(Modifier.height(5.dp))

            RowOrColumn {
                CusOutlinedTextField(
                    modifier = if(it) Modifier.weight(1f) else Modifier,
                    state = viewModel.grWt,
                    placeholderText = "Gs.Wt/gm",
                    keyboardType = KeyboardType.Number,
                    validation = { text ->
                        if (text.isNotBlank()) {
                            val grWtValue = text.toDoubleOrNull() ?: 0.0
                            val ntWtValue = viewModel.ntWt.text.toDoubleOrNull() ?: 0.0

                            if (ntWtValue > 0 && grWtValue < ntWtValue) {
                                "Gross weight cannot be less than net weight"
                            } else null
                        } else null
                    },
                    onTextChange = { text ->
                        viewModel.grWt.text = text
                        viewModel.ntWt.text = text

                        // Recalculate fine weight if purity is already selected
                        if (viewModel.purity.text.isNotBlank() && text.isNotBlank()) {
                            val ntWtValue = text.toDoubleOrNull() ?: 0.0
                            val multiplier =
                                Purity.fromLabel(viewModel.purity.text)?.multiplier ?: 1.0
                            viewModel.fnWt.text = (ntWtValue * multiplier).to3FString()
                        }
                    })
                WidthThenHeightSpacer(5.dp)

                CusOutlinedTextField(
                    modifier = if(it) Modifier.weight(1f) else Modifier,
                    state = viewModel.ntWt,
                    placeholderText = "Nt.Wt/gm",
                    keyboardType = KeyboardType.Number,
                    validation = { text ->
                        if (text.isNotBlank()) {
                            val ntWtValue = text.toDoubleOrNull() ?: 0.0
                            val grWtValue = viewModel.grWt.text.toDoubleOrNull() ?: 0.0

                            if (grWtValue > 0 && ntWtValue > grWtValue) {
                                "Net weight cannot be greater than gross weight"
                            } else null
                        } else null
                    },
                    onTextChange = { text ->
                        viewModel.ntWt.text = text

                        // Recalculate fine weight if purity is already selected
                        if (viewModel.purity.text.isNotBlank() && text.isNotBlank()) {
                            val ntWtValue = text.toDoubleOrNull() ?: 0.0
                            val multiplier =
                                Purity.fromLabel(viewModel.purity.text)?.multiplier ?: 1.0
                            viewModel.fnWt.text = (ntWtValue * multiplier).to3FString()
                        }
                    })

                WidthThenHeightSpacer(5.dp)

                CusOutlinedTextField(
                    modifier = if(it) Modifier.weight(1f) else Modifier,
                    state = viewModel.purity,
                    placeholderText = "Purity",
                    dropdownItems = Purity.catList(catName), // Purity.list(),
                    onDropdownItemSelected = { selected ->
                        if (viewModel.ntWt.text.isNotBlank()) {
                            val ntWtValue = viewModel.ntWt.text.toDoubleOrNull() ?: 0.0
                            val multiplier = Purity.fromLabel(selected)?.multiplier ?: 1.0
                            viewModel.fnWt.text = (ntWtValue * multiplier).to3FString()
                        }
                        viewModel.purity.text = selected

                        // Validate fine weight when purity changes
                        if (!viewModel.isSelf.value && viewModel.purchaseItems.isNotEmpty() && viewModel.fnWt.text.isNotBlank()) {
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
                                .launch {
                                    val error = viewModel.validateFineWeightInput(
                                        viewModel.fnWt.text, selected, subCatName
                                    )
                                    viewModel.fnWt.error = error ?: ""
                                }
                        } else {
                            viewModel.fnWt.error = ""
                        }
                    })

                WidthThenHeightSpacer(5.dp)

                CusOutlinedTextField(
                    modifier = if(it) Modifier.weight(1f) else Modifier,
                    state = viewModel.fnWt,
                    placeholderText = "Fn.Wt/gm",
                    keyboardType = KeyboardType.Number
                )
            }

            // Validate fine weight when it changes
            LaunchedEffect(
                viewModel.fnWt.text, viewModel.purity.text, viewModel.isSelf.value
            ) {
                if (!viewModel.isSelf.value && viewModel.purchaseItems.isNotEmpty() && viewModel.fnWt.text.isNotBlank()) {
                    val error = viewModel.validateFineWeightInput(
                        viewModel.fnWt.text, viewModel.purity.text, subCatName
                    )
                    viewModel.fnWt.error = error ?: ""
                } else {
                    viewModel.fnWt.error = ""
                }
            }

            Spacer(Modifier.height(5.dp))

            RowOrColumn {


                CusOutlinedTextField(
                    modifier = if(it) Modifier.weight(1f) else Modifier,
                    state = viewModel.chargeType,
                    placeholderText = "Making Charge Type",
                    dropdownItems = ChargeType.list(),
                )
                WidthThenHeightSpacer(5.dp)

                CusOutlinedTextField(
                    modifier = if(it) Modifier.weight(1f) else Modifier,
                    state = viewModel.charge,
                    placeholderText = "Making Charge",
                    keyboardType = KeyboardType.Number,
                )
                WidthThenHeightSpacer(5.dp)

                CusOutlinedTextField(
                    modifier = if(it) Modifier.weight(1f) else Modifier,
                    state = viewModel.otherChargeDes,
                    placeholderText = "Jewel Component Description",
                )
                WidthThenHeightSpacer(5.dp)

                CusOutlinedTextField(
                    modifier = if(it) Modifier.weight(1f) else Modifier,
                    state = viewModel.othCharge,
                    placeholderText = "Jewel Component Price (With TAX)",
                    keyboardType = KeyboardType.Number,
                )
            }
            Spacer(Modifier.height(5.dp))

            RowOrColumn {
                CusOutlinedTextField(
                    modifier = if(it) Modifier.weight(1f) else Modifier,
                    state = viewModel.cgst,
                    placeholderText = "CGST",
                    keyboardType = KeyboardType.Number,
                )
                WidthThenHeightSpacer(5.dp)

                CusOutlinedTextField(
                    modifier = if(it) Modifier.weight(1f) else Modifier,
                    state = viewModel.sgst,
                    placeholderText = "SGST",
                    keyboardType = KeyboardType.Number,
                )
                WidthThenHeightSpacer(5.dp)

                CusOutlinedTextField(
                    modifier = if(it) Modifier.weight(1f) else Modifier,
                    state = viewModel.igst,
                    placeholderText = "IGST",
                    keyboardType = KeyboardType.Number,
                )

                WidthThenHeightSpacer(5.dp)

                CusOutlinedTextField(
                    modifier =if(it) Modifier.weight(2f) else Modifier,
                    state = viewModel.huid,
                    placeholderText = "H-UID",
                )
            }
            Spacer(Modifier.height(5.dp))
            RowOrColumn() {

                CusOutlinedTextField(
                    modifier = if(it) Modifier.weight(1f) else Modifier,
                    state = viewModel.desKey,
                    placeholderText = "Description",
                    keyboardType = KeyboardType.Text,
                )
                WidthThenHeightSpacer(5.dp)
                CusOutlinedTextField(
                    modifier = if(it) Modifier.weight(1f) else Modifier,
                    state = viewModel.desValue,
                    placeholderText = "Value",
                    keyboardType = KeyboardType.Text,
                )

            }




            Spacer(Modifier.height(5.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {

                val purItem =
                    viewModel.purchaseItems.filter { it.subCatName.lowercase() == subCatName.lowercase() && it.purity == viewModel.purity.text }

                val text = purItem.joinToString(", ") {
                    "${it.purity} - Gs.Wt${it.gsWt},  Fn.Wt: ${it.fnWt}/₹${it.fnRate},  wastage: ${it.wastagePercent}%"
                }

                if (!viewModel.isSelf.value) {
                    Text(
                        text, modifier = Modifier
                            .weight(1f)
                            .padding(5.dp), fontSize = 16.sp
                    )

                } else {
                    Spacer(Modifier.weight(1f))
                }



                Spacer(Modifier.width(5.dp))
                Text("Cancel", Modifier
                    .bounceClick {
                        viewModel.clearAddItemFields()
                        isUpdateMode.value = false
                        addItem.value = false
                    }
                    .background(
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(16.dp),
                    )
                    .padding(10.dp), fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(15.dp))
                Text(if (isUpdateMode.value) "Update" else "Add", Modifier
                    .bounceClick {

                        return@bounceClick ioScope {
                            if (purItem.isEmpty() && !viewModel.isSelf.value) {
                                viewModel.snackBarState.value =
                                    "No corresponding item found in purchase order"
                                return@ioScope
                            }

                            if (!InputValidator.isValidQuantity(viewModel.qty.text)) {
                                viewModel.snackBarState.value = "Invalid quantity"
                                return@ioScope
                            }

                            if (!InputValidator.isValidWeight(viewModel.grWt.text)) {
                                viewModel.snackBarState.value = "Invalid gross weight"
                                return@ioScope
                            }

                            if (!InputValidator.isValidWeight(viewModel.ntWt.text)) {
                                viewModel.snackBarState.value = "Invalid net weight"
                                return@ioScope
                            }

                            if (!InputValidator.isValidWeight(viewModel.fnWt.text)) {
                                viewModel.snackBarState.value = "Invalid fine weight"
                                return@ioScope
                            }

                            if (viewModel.purity.text.isBlank()) {
                                viewModel.snackBarState.value = "Purity is required"
                                return@ioScope
                            }

                            if (!InputValidator.isValidHUID(viewModel.huid.text)) {
                                viewModel.snackBarState.value = "Invalid HUID format"
                                return@ioScope
                            }


                            val userId = viewModel.dataStoreManager.getAdminInfo().first.first()
                            val storeId =
                                viewModel.dataStoreManager.getSelectedStoreInfo().first.first()

                            if (isUpdateMode.value) {
                                // Update existing item
                                val updatedItem = itemBeingUpdated?.copy(
                                    itemAddName = InputValidator.sanitizeText(viewModel.addToName.text),
                                    entryType = InputValidator.sanitizeText(viewModel.entryType.text),
                                    quantity = viewModel.qty.text.toIntOrNull() ?: 1,
                                    gsWt = viewModel.grWt.text.toDoubleOrNull() ?: 0.0,
                                    ntWt = viewModel.ntWt.text.toDoubleOrNull() ?: 0.0,
                                    fnWt = viewModel.fnWt.text.toDoubleOrNull() ?: 0.0,
                                    purity = InputValidator.sanitizeText(viewModel.purity.text),
                                    crgType = InputValidator.sanitizeText(viewModel.chargeType.text),
                                    crg = viewModel.charge.text.toDoubleOrNull() ?: 0.0,
                                    othCrgDes = InputValidator.sanitizeText(viewModel.otherChargeDes.text),
                                    othCrg = viewModel.othCharge.text.toDoubleOrNull() ?: 0.0,
                                    cgst = viewModel.cgst.text.toDoubleOrNull() ?: 0.0,
                                    sgst = viewModel.sgst.text.toDoubleOrNull() ?: 0.0,
                                    igst = viewModel.igst.text.toDoubleOrNull() ?: 0.0,
                                    addDesKey = InputValidator.sanitizeText(viewModel.desKey.text),
                                    addDesValue = InputValidator.sanitizeText(viewModel.desValue.text),
                                    huid = viewModel.huid.text.trim().uppercase(),
                                    modifiedDate = Timestamp(System.currentTimeMillis())
                                )
                                updatedItem?.let {
                                    viewModel.safeUpdateItem(it, onFailure = {
                                        viewModel.snackBarState.value = "Update Item Failed"
                                    }, onSuccess = {
                                        // Refresh category data and filter items
                                        viewModel.refreshAndFilterItems()
                                        viewModel.clearAddItemFields()
                                        isUpdateMode.value = false
                                        viewModel.snackBarState.value = "Item updated successfully"
                                    })
                                }
                            } else {
                                // Check validation errors for purchase order items
                                if (!viewModel.isSelf.value && viewModel.purchaseItems.isNotEmpty()) {
                                    val fnWtError = viewModel.validateFineWeightInput(
                                        viewModel.fnWt.text, viewModel.purity.text, subCatName
                                    )
                                    if (fnWtError != null) {
                                        viewModel.fnWt.error = fnWtError
                                        viewModel.snackBarState.value = fnWtError
                                        return@ioScope
                                    }
                                }
                                // Add new item
                                val newItem = ItemEntity(
                                    itemId = viewModel.prefilledItemId.value?.takeIf { it.isNotBlank() }
                                        ?: generateId(),
                                    itemAddName = InputValidator.sanitizeText(viewModel.addToName.text),
                                    userId = userId,
                                    storeId = storeId,
                                    catId = catId,
                                    subCatId = subCatId,
                                    catName = catName,
                                    subCatName = subCatName,
                                    entryType = InputValidator.sanitizeText(viewModel.entryType.text),
                                    quantity = viewModel.qty.text.toIntOrNull() ?: 1,
                                    gsWt = viewModel.grWt.text.toDoubleOrNull() ?: 0.0,
                                    ntWt = viewModel.ntWt.text.toDoubleOrNull() ?: 0.0,
                                    fnWt = viewModel.fnWt.text.toDoubleOrNull() ?: 0.0,
                                    purity = InputValidator.sanitizeText(viewModel.purity.text),
                                    crgType = InputValidator.sanitizeText(viewModel.chargeType.text),
                                    crg = viewModel.charge.text.toDoubleOrNull() ?: 0.0,
                                    othCrgDes = InputValidator.sanitizeText(viewModel.otherChargeDes.text),
                                    othCrg = viewModel.othCharge.text.toDoubleOrNull() ?: 0.0,
                                    cgst = viewModel.cgst.text.toDoubleOrNull() ?: 0.0,
                                    sgst = viewModel.sgst.text.toDoubleOrNull() ?: 0.0,
                                    igst = viewModel.igst.text.toDoubleOrNull() ?: 0.0,
                                    addDesKey = InputValidator.sanitizeText(viewModel.desKey.text),
                                    addDesValue = InputValidator.sanitizeText(viewModel.desValue.text),
                                    huid = viewModel.huid.text.trim().uppercase(),
                                    addDate = Timestamp(System.currentTimeMillis()),
                                    modifiedDate = Timestamp(System.currentTimeMillis()),
                                    sellerFirmId = storeId,
                                    purchaseOrderId = if (viewModel.isSelf.value) storeId else if (purItem.isEmpty()) storeId else purItem[0].purchaseOrderId,
                                    purchaseItemId = if (viewModel.isSelf.value) storeId else if (purItem.isEmpty()) storeId else purItem[0].purchaseItemId,

                                    )

                                viewModel.safeInsertItem(newItem, onFailure = {
                                    viewModel.snackBarState.value = "Add Item Failed"
                                }, onSuccess = { itemEntity, l ->

                                    // Refresh category data and filter items
                                    viewModel.refreshAndFilterItems()

                                    viewModel.clearAddItemFields()

//                                    inventoryViewModel.loadingState.value = false
                                })
                            }

                            addItem.value = false
                        }


                    }
                    .background(
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(16.dp),
                    )
                    .padding(10.dp), fontWeight = FontWeight.Bold)
            }


        }
    }
}
