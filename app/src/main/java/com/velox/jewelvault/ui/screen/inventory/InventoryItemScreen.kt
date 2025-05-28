package com.velox.jewelvault.ui.screen.inventory

import android.annotation.SuppressLint
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
import com.velox.jewelvault.ui.components.InputFieldState
import com.velox.jewelvault.ui.components.ItemListViewComponent
import com.velox.jewelvault.ui.components.bounceClick
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
    val addToName = remember { InputFieldState() }
    val entryType = remember { InputFieldState() }
    val qty = remember { InputFieldState() }
    val grWt = remember { InputFieldState() }
    val ntWt = remember { InputFieldState() }
    val purity = remember { InputFieldState() }
    val fnWt = remember { InputFieldState() }
    val chargeType = remember { InputFieldState() }
    val charge = remember { InputFieldState() }
    val otherChargeDes = remember { InputFieldState() }
    val othCharge = remember { InputFieldState() }
    val cgst = remember { InputFieldState("1.5") }
    val sgst = remember { InputFieldState("1.5") }
    val igst = remember { InputFieldState() }
    val desKey = remember { InputFieldState() }
    val desValue = remember { InputFieldState() }
    val huid = remember { InputFieldState() }

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
                addToName,
                entryType,
                qty,
                grWt,
                ntWt,
                purity,
                fnWt,
                chargeType,
                charge,
                otherChargeDes,
                othCharge,
                cgst,
                sgst,
                igst,
                huid,
                catId,
                subCatId,
                catName,
                subCatName,
                inventoryViewModel,
                desKey,
                desValue

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
    addToName: InputFieldState,
    entryType: InputFieldState,
    qty: InputFieldState,
    grWt: InputFieldState,
    ntWt: InputFieldState,
    purity: InputFieldState,
    fnWt: InputFieldState,
    chargeType: InputFieldState,
    charge: InputFieldState,
    otherChargeDes: InputFieldState,
    othCharge: InputFieldState,
    cgst: InputFieldState,
    sgst: InputFieldState,
    igst: InputFieldState,
    huid: InputFieldState,
    catId: Int,
    subCatId: Int,
    catName: String,
    subCatName: String,
    inventoryViewModel: InventoryViewModel,
    desKey: InputFieldState,
    desValue: InputFieldState,
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
                Row(Modifier.fillMaxWidth()) {

                    CusOutlinedTextField(
                        modifier = Modifier.weight(1f),
                        state = addToName,
                        placeholderText = "Add to Name",
                        keyboardType = KeyboardType.Text
                    )

                    Spacer(Modifier.width(5.dp))

                    CusOutlinedTextField(modifier = Modifier.weight(1f),
                        state = entryType,
                        placeholderText = "Entry Type",
                        dropdownItems = EntryType.list(),
                        onDropdownItemSelected = { selected ->
                            when (selected) {
                                EntryType.Piece.type -> {
                                    qty.text = "1"
                                }

                                EntryType.Lot.type -> {

                                }

                                else -> {
                                    entryType.text = selected
                                }
                            }
                            entryType.text = selected
                        })


                    Spacer(Modifier.width(5.dp))

                    CusOutlinedTextField(
                        modifier = Modifier.weight(1f),
                        state = qty,
                        placeholderText = "Quantity",
                        keyboardType = KeyboardType.Number,
                    )

                }
                Spacer(Modifier.height(5.dp))

                Row {
                    CusOutlinedTextField(modifier = Modifier.weight(1f),
                        state = grWt,
                        placeholderText = "Gr.Wt/gm",
                        keyboardType = KeyboardType.Number,
                        onTextChange = {
                            ntWt.text = it
                        })
                    Spacer(Modifier.width(5.dp))

                    CusOutlinedTextField(
                        modifier = Modifier.weight(1f),
                        state = ntWt,
                        placeholderText = "NT.Wt/gm",
                        keyboardType = KeyboardType.Number,
                    )

                    Spacer(Modifier.width(5.dp))

                    CusOutlinedTextField(modifier = Modifier.weight(1f),
                        state = purity,
                        placeholderText = "Purity",
                        dropdownItems = Purity.list(),
                        onDropdownItemSelected = { selected ->
                            if (ntWt.text.isNotBlank()) {
                                val ntWtValue = ntWt.text.toDoubleOrNull() ?: 0.0
                                val multiplier = Purity.fromLabel(selected)?.multiplier ?: 1.0
                                fnWt.text = String.format("%.2f", ntWtValue * multiplier)
                            }
                            purity.text = selected
                        })

                    Spacer(Modifier.width(5.dp))

                    CusOutlinedTextField(
                        modifier = Modifier.weight(1f),
                        state = fnWt,
                        placeholderText = "Fn.Wt/gm",
                        keyboardType = KeyboardType.Number,
                    )
                }
                Spacer(Modifier.height(5.dp))

                Row {


                    CusOutlinedTextField(
                        modifier = Modifier.weight(1f),
                        state = chargeType,
                        placeholderText = "Charge Type",
                        dropdownItems = ChargeType.list(),
                    )
                    Spacer(Modifier.width(5.dp))

                    CusOutlinedTextField(
                        modifier = Modifier.weight(1f),
                        state = charge,
                        placeholderText = "charge",
                        keyboardType = KeyboardType.Number,
                    )
                    Spacer(Modifier.width(5.dp))

                    CusOutlinedTextField(
                        modifier = Modifier.weight(1f),
                        state = otherChargeDes,
                        placeholderText = "Oth Charge Des",
                    )
                    Spacer(Modifier.width(5.dp))

                    CusOutlinedTextField(
                        modifier = Modifier.weight(1f),
                        state = othCharge,
                        placeholderText = "Oth Charge",
                        keyboardType = KeyboardType.Number,
                    )
                }
                Spacer(Modifier.height(5.dp))

                Row {
                    CusOutlinedTextField(
                        modifier = Modifier.weight(1f),
                        state = cgst,
                        placeholderText = "CGST",
                        keyboardType = KeyboardType.Number,
                    )
                    Spacer(Modifier.width(5.dp))

                    CusOutlinedTextField(
                        modifier = Modifier.weight(1f),
                        state = sgst,
                        placeholderText = "SGST",
                        keyboardType = KeyboardType.Number,
                    )
                    Spacer(Modifier.width(5.dp))

                    CusOutlinedTextField(
                        modifier = Modifier.weight(1f),
                        state = igst,
                        placeholderText = "IGST",
                        keyboardType = KeyboardType.Number,
                    )

                    Spacer(Modifier.width(5.dp))

                    CusOutlinedTextField(
                        modifier = Modifier.weight(2f),
                        state = huid,
                        placeholderText = "H-UID",
                        keyboardType = KeyboardType.Number,
                    )
                    Spacer(Modifier.height(5.dp))
                }
                Spacer(Modifier.height(5.dp))
                Row {

                    CusOutlinedTextField(
                        modifier = Modifier.weight(1f),
                        state = desKey,
                        placeholderText = "Description",
                        keyboardType = KeyboardType.Text,
                    )
                    Spacer(Modifier.width(5.dp))
                    CusOutlinedTextField(
                        modifier = Modifier.weight(2f),
                        state = desValue,
                        placeholderText = "Value",
                        keyboardType = KeyboardType.Text,
                    )

                }

                Spacer(Modifier.height(5.dp))
                Row(Modifier.fillMaxWidth()) {
                    Spacer(Modifier.weight(1f))
                    Text("Cancel", Modifier
                        .bounceClick {
                            addToName.text = ""
                            entryType.text = ""
                            qty.text = ""
                            grWt.text = ""
                            ntWt.text = ""
                            purity.text = ""
                            fnWt.text = ""
                            chargeType.text = ""
                            charge.text = ""
                            otherChargeDes.text = ""
                            othCharge.text = ""
                            huid.text = ""
                            desValue.text = ""

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
                            addItem.value = false

                            val newItem = ItemEntity(
                                itemAddName = addToName.text,
                                userId = 1,
                                storeId = 1,
                                catId = catId,
                                subCatId = subCatId,
                                catName = catName,
                                subCatName = subCatName,
                                entryType = entryType.text,
                                quantity = qty.text.toIntOrNull() ?: 1,
                                gsWt = grWt.text.toDoubleOrNull() ?: 0.0,
                                ntWt = ntWt.text.toDoubleOrNull() ?: 0.0,
                                fnWt = fnWt.text.toDoubleOrNull() ?: 0.0,
                                purity = purity.text,
                                crgType = chargeType.text,
                                crg = charge.text.toDoubleOrNull() ?: 0.0,
                                othCrgDes = otherChargeDes.text,
                                othCrg = othCharge.text.toDoubleOrNull() ?: 0.0,
                                cgst = cgst.text.toDoubleOrNull() ?: 0.0,
                                sgst = sgst.text.toDoubleOrNull() ?: 0.0,
                                igst = igst.text.toDoubleOrNull() ?: 0.0,
                                addDesKey = desKey.text,
                                addDesValue = desValue.text,
                                huid = huid.text,
                                addDate = Timestamp(System.currentTimeMillis()),
                                modifiedDate = Timestamp(System.currentTimeMillis())
                            )

                            inventoryViewModel.safeInsertItem(newItem, onFailure = {

                            }, onSuccess = { itemEntity, l ->

                                inventoryViewModel.filterItems(
                                    catId = catId, subCatId = subCatId
                                )

                                addToName.text = ""
                                entryType.text = ""
                                qty.text = ""
                                grWt.text = ""
                                ntWt.text = ""
                                purity.text = ""
                                fnWt.text = ""
                                chargeType.text = ""
                                charge.text = ""
                                otherChargeDes.text = ""
                                othCharge.text = ""
                                desValue.text = ""
                                huid.text = ""
//                                    inventoryViewModel.loadingState.value = false
                            })
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
