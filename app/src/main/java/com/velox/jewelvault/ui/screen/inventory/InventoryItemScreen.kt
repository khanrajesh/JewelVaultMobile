package com.velox.jewelvault.ui.screen.inventory

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velox.jewelvault.data.roomdb.entity.ItemEntity
import com.velox.jewelvault.ui.components.CusOutlinedTextField
import com.velox.jewelvault.ui.components.ItemListViewComponent
import com.velox.jewelvault.ui.components.bounceClick
import com.velox.jewelvault.ui.theme.LightGreen
import com.velox.jewelvault.ui.theme.LightRed
import com.velox.jewelvault.utils.ChargeType
import com.velox.jewelvault.utils.EntryType
import com.velox.jewelvault.utils.LocalBaseViewModel
import com.velox.jewelvault.utils.Purity
import java.sql.Timestamp

@Composable
fun InventoryItemScreen(
    inventoryViewModel: InventoryViewModel,
    catId: Int,
    catName: String,
    subCatId: Int,
    subCatName: String
) {
    LandscapeInventoryItemScreen(inventoryViewModel, catId, catName, subCatId, subCatName)
}

@Composable
fun LandscapeInventoryItemScreen(
    inventoryViewModel: InventoryViewModel,
    catId: Int,
    catName: String,
    subCatId: Int,
    subCatName: String
) {


    LaunchedEffect(true) {
        inventoryViewModel.filterItems(catId = catId, subCatId = subCatId)
    }

    val showOption = remember { mutableStateOf(false) }


    val addItem = remember { mutableStateOf(false) }
    val showDialog = remember { mutableStateOf(false) }
    val selectedItem = remember { mutableStateOf<ItemEntity?>(null) }

    val baseViewModel = LocalBaseViewModel.current
    val haptic = LocalHapticFeedback.current


    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(topStart = 18.dp))
            .padding(5.dp)
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth()) {
                Text("$catName ($catId) > $subCatName ($subCatId)")
                Spacer(Modifier.weight(1f))
                Text("Add Item", modifier = Modifier.clickable {
                    addItem.value = true
                })
                Spacer(Modifier.width(10.dp))
                Icon(Icons.Default.MoreVert, null, modifier = Modifier.clickable {
                    showOption.value = !showOption.value
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
            )

            ItemListViewComponent(inventoryViewModel.itemHeaderList,
                inventoryViewModel.itemList,
                onItemLongClick = { item ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    selectedItem.value = item
                    showDialog.value = true
                })
        }

        if (showOption.value) Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(y = 40.dp)
                .wrapContentHeight()
                .wrapContentWidth()
                .background(Color.White, RoundedCornerShape(16.dp))
                .padding(5.dp)
        ) {
            Text("Add Sub Category",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.clickable {

                })
        }

        if (showDialog.value && selectedItem.value != null) {
            AlertDialog(onDismissRequest = { showDialog.value = false },
                title = { Text("Item Details") },
                text = {
                    Column {
                        Text("Name: ${selectedItem.value?.itemAddName}")
                        Text("id: ${selectedItem.value?.itemId},cat: ${selectedItem.value?.catId},id: ${selectedItem.value?.subCatId}")
                        Text("Purity: ${selectedItem.value?.purity}")
                        Text("Quantity: ${selectedItem.value?.quantity}")
                        Text("Net Weight: ${selectedItem.value?.gsWt}")
                        // Add more fields as needed...
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (selectedItem.value != null) {
                            inventoryViewModel.safeDeleteItem(itemId = selectedItem.value!!.itemId,
                                catId = selectedItem.value!!.catId,
                                subCatId = selectedItem.value!!.subCatId,
                                onSuccess = {
                                    baseViewModel.snackMessage = "Item deleted successfully"
                                },
                                onFailure = {
                                    baseViewModel.snackMessage = "Unable to delete item."
                                })

                            selectedItem.value = null
                        } else {
                            baseViewModel.snackMessage = "Please select valid item"
                        }

                        showDialog.value = false
                    }) {
                        Text("Delete", color = Color.Red)
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


}

@SuppressLint("DefaultLocale")
@Composable
private fun AddItemSection(
    addItem: MutableState<Boolean>,
    catId: Int,
    subCatId: Int,
    catName: String,
    subCatName: String,
    viewModel: InventoryViewModel,
) {
    val context = LocalContext.current
    if (addItem.value) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                .padding(5.dp)
        ) {
            Text("Add Item", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            Spacer(Modifier.height(5.dp))

            Column(
                Modifier
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                    .padding(5.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
                ) { //purchase order section

                    Button(
                        onClick = {
                            viewModel.isSelf.value = !viewModel.isSelf.value
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (viewModel.isSelf.value) LightGreen else LightRed
                        )
                    ) {
                        Text("SELF", fontWeight = FontWeight.SemiBold)
                    }
                    if (!viewModel.isSelf.value){
                    Spacer(Modifier.width(5.dp))
                        CusOutlinedTextField(viewModel.billDate,
                            placeholderText = "Bill Date",
                            modifier = Modifier.weight(1f),
                            isDatePicker = true,
                            onDateSelected = {
                                viewModel.getBillsFromDate()
                            })

                        if (viewModel.purchaseOrdersByDate.isNotEmpty()) {
                            Spacer(Modifier.width(5.dp))
                            CusOutlinedTextField(modifier = Modifier.weight(1f),
                                state = viewModel.billNo,
                                placeholderText = "Bill No",
                                readOnly = true,
                                dropdownItems = viewModel.purchaseOrdersByDate.map { it.billNo },
                                onDropdownItemSelected = { sel ->
                                    val item = viewModel.purchaseOrdersByDate.find { it.billNo == sel }
                                    if (item != null) {
                                        viewModel.getPurchaseOrderItemDetails(item, subCatName)
                                    } else {
                                        viewModel.snackBarState.value =
                                            "Unable to find purchase order item"
                                    }
                                })
                        }

                        if (viewModel.billItemDetails.value.isNotBlank()) {
                            Spacer(Modifier.width(5.dp))
                            Text(
                                modifier = Modifier.weight(1f),
                                text = "${viewModel.billItemDetails.value} ",
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                if (viewModel.billItemDetails.value.isNotBlank() || viewModel.isSelf.value) {

                    Spacer(Modifier.height(5.dp))

                    Row(Modifier.fillMaxWidth()) {

                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.addToName,
                            placeholderText = "Add to Name",
                            keyboardType = KeyboardType.Text
                        )

                        Spacer(Modifier.width(5.dp))

                        CusOutlinedTextField(modifier = Modifier.weight(1f),
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


                        Spacer(Modifier.width(5.dp))

                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.qty,
                            placeholderText = "Quantity",
                            keyboardType = KeyboardType.Number,
                        )

                    }
                    Spacer(Modifier.height(5.dp))

                    Row {
                        CusOutlinedTextField(modifier = Modifier.weight(1f),
                            state = viewModel.grWt,
                            placeholderText = "Gs.Wt/gm",
                            keyboardType = KeyboardType.Number,
                            onTextChange = {
                                viewModel.ntWt.text = it
                            })
                        Spacer(Modifier.width(5.dp))

                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.ntWt,
                            placeholderText = "Nt.Wt/gm",
                            keyboardType = KeyboardType.Number,
                        )

                        Spacer(Modifier.width(5.dp))

                        CusOutlinedTextField(modifier = Modifier.weight(1f),
                            state = viewModel.purity,
                            placeholderText = "Purity",
                            dropdownItems = Purity.list(),
                            onDropdownItemSelected = { selected ->
                                if (viewModel.ntWt.text.isNotBlank()) {
                                    val ntWtValue = viewModel.ntWt.text.toDoubleOrNull() ?: 0.0
                                    val multiplier = Purity.fromLabel(selected)?.multiplier ?: 1.0
                                    viewModel.fnWt.text =
                                        String.format("%.2f", ntWtValue * multiplier)
                                }
                                viewModel.purity.text = selected
                            })

                        Spacer(Modifier.width(5.dp))

                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.fnWt,
                            placeholderText = "Fn.Wt/gm",
                            keyboardType = KeyboardType.Number,
                        )
                    }
                    Spacer(Modifier.height(5.dp))

                    Row {


                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.chargeType,
                            placeholderText = "Charge Type",
                            dropdownItems = ChargeType.list(),
                        )
                        Spacer(Modifier.width(5.dp))

                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.charge,
                            placeholderText = "charge",
                            keyboardType = KeyboardType.Number,
                        )
                        Spacer(Modifier.width(5.dp))

                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.otherChargeDes,
                            placeholderText = "Oth Charge Des",
                        )
                        Spacer(Modifier.width(5.dp))

                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.othCharge,
                            placeholderText = "Oth Charge",
                            keyboardType = KeyboardType.Number,
                        )
                    }
                    Spacer(Modifier.height(5.dp))

                    Row {
                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.cgst,
                            placeholderText = "CGST",
                            keyboardType = KeyboardType.Number,
                        )
                        Spacer(Modifier.width(5.dp))

                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.sgst,
                            placeholderText = "SGST",
                            keyboardType = KeyboardType.Number,
                        )
                        Spacer(Modifier.width(5.dp))

                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.igst,
                            placeholderText = "IGST",
                            keyboardType = KeyboardType.Number,
                        )

                        Spacer(Modifier.width(5.dp))

                        CusOutlinedTextField(
                            modifier = Modifier.weight(2f),
                            state = viewModel.huid,
                            placeholderText = "H-UID",
                            keyboardType = KeyboardType.Number,
                        )
                        Spacer(Modifier.height(5.dp))
                    }
                    Spacer(Modifier.height(5.dp))
                    Row {

                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.desKey,
                            placeholderText = "Description",
                            keyboardType = KeyboardType.Text,
                        )
                        Spacer(Modifier.width(5.dp))
                        CusOutlinedTextField(
                            modifier = Modifier.weight(2f),
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
                                    .padding(5.dp),
                                fontSize = 16.sp
                            )

                        }else{
                            Spacer(Modifier.weight(1f))
                        }



                        Spacer(Modifier.width(5.dp))
                        Text("Cancel", Modifier
                            .bounceClick {
                                viewModel.addToName.clear()
                                viewModel.entryType.clear()
                                viewModel.qty.clear()
                                viewModel.grWt.clear()
                                viewModel.ntWt.clear()
                                viewModel.purity.clear()
                                viewModel.fnWt.clear()
                                viewModel.chargeType.clear()
                                viewModel.charge.clear()
                                viewModel.otherChargeDes.clear()
                                viewModel.othCharge.clear()
                                viewModel.huid.clear()
                                viewModel.desValue.clear()

                                addItem.value = false
                            }
                            .background(
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(16.dp),
                            )
                            .padding(10.dp), fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(15.dp))
                        Text("Add", Modifier
                            .bounceClick {
                                if (purItem.isEmpty() && !viewModel.isSelf.value) {
                                    viewModel.snackBarState.value =
                                        "No corresponding item found in purchase order"
                                    return@bounceClick
                                }

                                val newItem = ItemEntity(
                                    itemAddName = viewModel.addToName.text,
                                    userId = 1,
                                    storeId = 1,
                                    catId = catId,
                                    subCatId = subCatId,
                                    catName = catName,
                                    subCatName = subCatName,
                                    entryType = viewModel.entryType.text,
                                    quantity = viewModel.qty.text.toIntOrNull() ?: 1,
                                    gsWt = viewModel.grWt.text.toDoubleOrNull() ?: 0.0,
                                    ntWt = viewModel.ntWt.text.toDoubleOrNull() ?: 0.0,
                                    fnWt = viewModel.fnWt.text.toDoubleOrNull() ?: 0.0,
                                    purity = viewModel.purity.text,
                                    crgType = viewModel.chargeType.text,
                                    crg = viewModel.charge.text.toDoubleOrNull() ?: 0.0,
                                    othCrgDes = viewModel.otherChargeDes.text,
                                    othCrg = viewModel.othCharge.text.toDoubleOrNull() ?: 0.0,
                                    cgst = viewModel.cgst.text.toDoubleOrNull() ?: 0.0,
                                    sgst = viewModel.sgst.text.toDoubleOrNull() ?: 0.0,
                                    igst = viewModel.igst.text.toDoubleOrNull() ?: 0.0,
                                    addDesKey = viewModel.desKey.text,
                                    addDesValue = viewModel.desValue.text,
                                    huid = viewModel.huid.text,
                                    addDate = Timestamp(System.currentTimeMillis()),
                                    modifiedDate = Timestamp(System.currentTimeMillis()),

                                    //seller info todo
                                    sellerFirmId = 0,
                                    purchaseOrderId = if (viewModel.isSelf.value) 0 else if (purItem.isEmpty()) 0 else purItem[0].purchaseOrderId,
                                    purchaseItemId = if (viewModel.isSelf.value) 0 else if (purItem.isEmpty()) 0 else purItem[0].purchaseItemId,

                                    )

                                viewModel.safeInsertItem(newItem, onFailure = {
                                    viewModel.snackBarState.value = "Add Item Failed"
                                }, onSuccess = { itemEntity, l ->

                                    viewModel.filterItems(
                                        catId = catId, subCatId = subCatId
                                    )

                                    viewModel.addToName.text = ""
                                    viewModel.entryType.text = ""
                                    viewModel.qty.text = ""
                                    viewModel.grWt.text = ""
                                    viewModel.ntWt.text = ""
                                    viewModel.purity.text = ""
                                    viewModel.fnWt.text = ""
                                    viewModel.chargeType.text = ""
                                    viewModel.charge.text = ""
                                    viewModel.otherChargeDes.text = ""
                                    viewModel.othCharge.text = ""
                                    viewModel.desValue.text = ""
                                    viewModel.huid.text = ""

//                                    inventoryViewModel.loadingState.value = false
                                })

                                addItem.value = false
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
    }
}
