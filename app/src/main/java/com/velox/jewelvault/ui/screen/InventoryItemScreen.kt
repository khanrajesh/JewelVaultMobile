package com.velox.jewelvault.ui.screen

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velox.jewelvault.ui.components.CusOutlinedTextField
import com.velox.jewelvault.ui.components.InputFieldState
import com.velox.jewelvault.ui.components.bounceClick

@Composable
fun InventoryItemScreen() {
    LandscapeInventoryItemScreen()
}

@Composable
fun LandscapeInventoryItemScreen() {
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
                igst
            )

            LazyColumn {
                item {
                    Column {
                        Row {
                            Text("Add to Name", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(2.0f), fontSize = 12.sp, textAlign = TextAlign.Center)
                            Text("|", fontSize = 12.sp, textAlign = TextAlign.Center)
                            Text("Type", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1.0f), fontSize = 12.sp, textAlign = TextAlign.Center)
                            Text("|", fontSize = 12.sp, textAlign = TextAlign.Center)
                            Text("Qty", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1.0f), fontSize = 12.sp, textAlign = TextAlign.Center)
                            Text("|", fontSize = 12.sp, textAlign = TextAlign.Center)
                            Text("Gr.Wt", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1.0f), fontSize = 12.sp, textAlign = TextAlign.Center)
                            Text("|", fontSize = 12.sp, textAlign = TextAlign.Center)
                            Text("Nt.Wt", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1.0f), fontSize = 12.sp, textAlign = TextAlign.Center)
                            Text("|", fontSize = 12.sp, textAlign = TextAlign.Center)
                            Text("Purity", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1.0f), fontSize = 12.sp, textAlign = TextAlign.Center)
                            Text("|", fontSize = 12.sp, textAlign = TextAlign.Center)
                            Text("Fn.Wt", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1.0f), fontSize = 12.sp, textAlign = TextAlign.Center)
                            Text("|", fontSize = 12.sp, textAlign = TextAlign.Center)
                            Text("MC.Type", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1.0f), fontSize = 12.sp, textAlign = TextAlign.Center)
                            Text("|", fontSize = 12.sp, textAlign = TextAlign.Center)
                            Text("M.Chr", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1.0f), fontSize = 12.sp, textAlign = TextAlign.Center)
                            Text("|", fontSize = 12.sp, textAlign = TextAlign.Center)
                            Text("Oth Chr", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1.0f), fontSize = 12.sp, textAlign = TextAlign.Center)
                            Text("|", fontSize = 12.sp, textAlign = TextAlign.Center)
                            Text("Chr", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1.0f), fontSize = 12.sp, textAlign = TextAlign.Center)
                            Text("|", fontSize = 12.sp, textAlign = TextAlign.Center)
                            Text("C-Gst", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1.0f), fontSize = 12.sp, textAlign = TextAlign.Center)
                            Text("|", fontSize = 12.sp, textAlign = TextAlign.Center)
                            Text("S-Gst", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1.0f), fontSize = 12.sp, textAlign = TextAlign.Center)
                            Text("|", fontSize = 12.sp, textAlign = TextAlign.Center)
                            Text("I-Gst", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1.0f), fontSize = 12.sp, textAlign = TextAlign.Center)
                      Text("|", fontSize = 12.sp, textAlign = TextAlign.Center)
                            Text("DOA", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1.0f), fontSize = 12.sp, textAlign = TextAlign.Center)
                        }
                        Spacer(
                            Modifier
                                .height(2.dp)
                                .fillMaxWidth()
                                .background(Color.LightGray)
                        )
                    }
                       }

                item {
                    Row {
                        Text("Add to Name", modifier = Modifier.weight(2.0f), fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text("|", fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text("Type",  modifier = Modifier.weight(1.0f), fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text("|", fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text("Qty", modifier = Modifier.weight(1.0f), fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text("|", fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text("Gr.Wt",  modifier = Modifier.weight(1.0f), fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text("|", fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text("Nt.Wt",  modifier = Modifier.weight(1.0f), fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text("|", fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text("Purity", modifier = Modifier.weight(1.0f), fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text("|", fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text("Fn.Wt", modifier = Modifier.weight(1.0f), fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text("|", fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text("Pieces", modifier = Modifier.weight(1.0f), fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text("|", fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text("M.Chr", modifier = Modifier.weight(1.0f), fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text("|", fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text("Oth Chr", modifier = Modifier.weight(1.0f), fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text("|", fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text("Chr", modifier = Modifier.weight(1.0f), fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text("|", fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text("C-Gst", modifier = Modifier.weight(1.0f), fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text("|", fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text("S-Gst", modifier = Modifier.weight(1.0f), fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text("|", fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text("I-Gst", modifier = Modifier.weight(1.0f), fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text("|", fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text("DOA", modifier = Modifier.weight(1.0f), fontSize = 12.sp, textAlign = TextAlign.Center)
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
    igst: InputFieldState
) {
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
