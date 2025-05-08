package com.velox.jewelvault.ui.screen.inventory

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velox.jewelvault.data.roomdb.entity.ItemEntity
import com.velox.jewelvault.ui.components.CusOutlinedTextField
import com.velox.jewelvault.ui.components.InputFieldState
import com.velox.jewelvault.ui.components.bounceClick
import com.velox.jewelvault.utils.mainScope
import kotlinx.coroutines.launch
import java.sql.Timestamp

@Composable
fun InventoryItemScreen(inventoryViewModel: InventoryViewModel) {
    LandscapeInventoryItemScreen(inventoryViewModel)
}

@Composable
fun LandscapeInventoryItemScreen(inventoryViewModel: InventoryViewModel) {

    val addItem = remember { mutableStateOf(false) }
    val addToName = remember { InputFieldState() }
    val type = remember { InputFieldState() }
    val qty = remember { InputFieldState() }
    val GrWt = remember { InputFieldState() }
    val NtWt = remember { InputFieldState() }
    val Purity = remember { InputFieldState() }
    val FnWt = remember { InputFieldState() }
    val ChargeType = remember { InputFieldState() }
    val Charge = remember { InputFieldState() }
    val OtherChargeDes = remember { InputFieldState() }
    val OthCharge = remember { InputFieldState() }
    val cgst = remember { InputFieldState() }
    val sgst = remember { InputFieldState() }
    val igst = remember { InputFieldState() }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(topStart = 18.dp))
            .padding(5.dp)
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth()) {
                Text("Gold > Ring")
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
                addToName,
                type,
                qty,
                GrWt,
                NtWt,
                Purity,
                FnWt,
                ChargeType,
                Charge,
                OtherChargeDes,
                OthCharge,
                cgst,
                sgst,
                igst,
                inventoryViewModel
            )

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val columnCount = 15
                val columnWidth = maxWidth / columnCount

                val itemsWithHeader = listOf(null) + inventoryViewModel.itemList

                LazyColumn {
                    itemsIndexed(itemsWithHeader) { index, item ->
                        val isHeader = index == 0
                        val values = if (isHeader) {
                            listOf(
                                "To Name",
                                "Type",
                                "Qty",
                                "Gr.Wt",
                                "Nt.Wt",
                                "Purity",
                                "Fn.Wt",
                                "MC.Type",
                                "M.Chr",
                                "Oth Chr",
                                "Chr",
                                "C-Gst",
                                "S-Gst",
                                "I-Gst",
                                "DOA"
                            )
                        } else {
                            listOf(
                                item!!.itemAddName,
                                item.type,
                                item.quantity.toString(),
                                item.gsWt.toString(),
                                item.ntWt.toString(),
                                item.purity,
                                item.fnWt.toString(),
                                item.quantity.toString(),
                                item.crg.toString(),
                                item.othCrgDes,
                                item.othCrg.toString(),
                                item.cgst.toString(),
                                item.sgst.toString(),
                                item.igst.toString(),
                                item.addDate.toString()
                            )
                        }

                        Row(modifier = Modifier
                            .fillMaxWidth()
                            .height(30.dp)) {
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
    addItem: MutableState<Boolean>,
    addToName: InputFieldState,
    type: InputFieldState,
    qty: InputFieldState,
    GrWt: InputFieldState,
    NtWt: InputFieldState,
    Purity: InputFieldState,
    FnWt: InputFieldState,
    ChargeType: InputFieldState,
    Charge: InputFieldState,
    OtherChargeDes: InputFieldState,
    OthCharge: InputFieldState,
    cgst: InputFieldState,
    sgst: InputFieldState,
    igst: InputFieldState,
    inventoryViewModel: InventoryViewModel
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

                    CusOutlinedTextField(
                        modifier = Modifier.weight(1f),
                        state = type,
                        placeholderText = "Type",
                        dropdownItems = listOf("piece", "lot"),
                    )

                    Spacer(Modifier.width(5.dp))

                    CusOutlinedTextField(
                        modifier = Modifier.weight(1f),
                        state = qty,
                        placeholderText = "Qty",
                        keyboardType = KeyboardType.Number,
                    )

                }
                Spacer(Modifier.height(5.dp))

                Row {
                    CusOutlinedTextField(
                        modifier = Modifier.weight(1f),
                        state = GrWt,
                        placeholderText = "Gr.Wt/gm",
                        keyboardType = KeyboardType.Number,
                    )
                    Spacer(Modifier.width(5.dp))

                    CusOutlinedTextField(
                        modifier = Modifier.weight(1f),
                        state = NtWt,
                        placeholderText = "NT.Wt/gm",
                        keyboardType = KeyboardType.Number,
                    )
                    Spacer(Modifier.width(5.dp))

                    CusOutlinedTextField(
                        modifier = Modifier.weight(1f),
                        state = Purity,
                        placeholderText = "purity",
                        dropdownItems = listOf("750", "916")
                    )
                    Spacer(Modifier.width(5.dp))

                    CusOutlinedTextField(
                        modifier = Modifier.weight(1f),
                        state = FnWt,
                        placeholderText = "Fn.Wt/gm",
                        keyboardType = KeyboardType.Number,
                    )
                }
                Spacer(Modifier.height(5.dp))

                Row {

                    CusOutlinedTextField(
                        modifier = Modifier.weight(1f),
                        state = ChargeType,
                        placeholderText = "Charge type",
                        dropdownItems = listOf("%", "piece"),
                    )
                    Spacer(Modifier.width(5.dp))

                    CusOutlinedTextField(
                        modifier = Modifier.weight(1f),
                        state = Charge,
                        placeholderText = "charge",
                        keyboardType = KeyboardType.Number,
                    )
                    Spacer(Modifier.width(5.dp))

                    CusOutlinedTextField(
                        modifier = Modifier.weight(1f),
                        state = OtherChargeDes,
                        placeholderText = "Oth Charge Des",
                    )
                    Spacer(Modifier.width(5.dp))

                    CusOutlinedTextField(
                        modifier = Modifier.weight(1f),
                        state = OthCharge,
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
                    Spacer(Modifier.height(5.dp))
                }
                Spacer(Modifier.height(5.dp))
                Row(Modifier.fillMaxWidth()) {
                    Spacer(Modifier.weight(1f))
                    Text(
                        "Cancel", Modifier
                            .bounceClick {
                                addItem.value = false
                            }
                            .background(
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(16.dp),
                            )
                            .padding(10.dp),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(15.dp))
                    Text(
                        "Add", Modifier
                            .bounceClick {
                                addItem.value = false

                                val newItem = ItemEntity(
                                    itemAddName = addToName.text,
                                    userId = 1,
                                    storeId = 1,
                                    catId = 1,
                                    subCatId = 1,
                                    catName = "Jewelry",
                                    subCatName = "Rings",
                                    type = type.text,
                                    quantity = qty.text.toInt(),
                                    gsWt = GrWt.text.toDouble(),
                                    ntWt = NtWt.text.toDouble(),
                                    fnWt = NtWt.text.toDouble(),
                                    purity = Purity.text,
                                    crgType = ChargeType.text,
                                    crg = Charge.text.toDouble(),
                                    othCrgDes = OtherChargeDes.text,
                                    othCrg = OthCharge.text.toDouble(),
                                    cgst = 1.5,
                                    sgst = 1.5,
                                    igst = 0.0,
                                    huid = "",
                                    addDate = Timestamp(System.currentTimeMillis()),
                                    modifiedDate = Timestamp(System.currentTimeMillis())
                                )

                                inventoryViewModel.safeInsertItem(
                                    newItem,
                                    onFailure = {
                                        mainScope.launch {
                                            Toast.makeText(
                                                context,
                                                "failed to add item",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },
                                    onSuccess = { itemEntity, l ->
                                        inventoryViewModel.getAllItems()
                                        mainScope.launch {
                                            Toast.makeText(
                                                context,
                                                "item added with id: $l",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    })

                            }
                            .background(
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(16.dp),
                            )
                            .padding(10.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
