package com.velox.jewelvault.ui.screen.draft_invoice

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
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
import com.velox.jewelvault.ui.nav.Screens
import com.velox.jewelvault.utils.*

@Composable
fun DraftInvoiceScreen(viewModel: DraftInvoiceViewModel) {
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
                                navHost.navigate(Screens.DraftPreview.route)
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
                    viewModel.updateChargeView(!viewModel.showSeparateCharges.value)
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
                    "Add Sample Item",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        viewModel.addSampleItem()
                    }
                )
            }
        }
        if (viewModel.showAddItemDialog.value) {
            DraftViewAddItemDialog(viewModel)
        }
    }
}

@Composable
fun DraftCustomerDetails(viewModel: DraftInvoiceViewModel) {
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

@Composable
fun DraftItemSection(modifier: Modifier, viewModel: DraftInvoiceViewModel) {
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
            Text("Item List", fontWeight = FontWeight.SemiBold)
            Text("Items: ${viewModel.selectedItemList.size}", fontSize = 12.sp)
        }
        
        Spacer(Modifier.height(5.dp))
        
        if (viewModel.selectedItemList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No items added yet",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                items(viewModel.selectedItemList) { item ->
                    DraftItemCard(
                        item = item,
                        onRemove = { viewModel.removeItem(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun DraftItemCard(
    item: ItemSelectedModel,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.itemAddName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove Item",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${item.catName} > ${item.subCatName}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = "₹${item.price.to2FString()}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Qty: ${item.quantity} | Purity: ${item.purity}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = "Wt: ${item.ntWt.to2FString()}g",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun DraftAddItemSection(itemId: MutableState<String>, viewModel: DraftInvoiceViewModel) {
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
                    viewModel.showAddItemDialog.value = true
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
fun DraftDetailSection(modifier: Modifier, viewModel: DraftInvoiceViewModel) {
    Column(modifier = modifier.padding(5.dp)) {
        Text("Summary", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Spacer(Modifier.height(10.dp))
        
        // Totals Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Sub Total:", fontSize = 14.sp)
                    Text("₹${viewModel.subTotal.value.to2FString()}", fontWeight = FontWeight.SemiBold)
                }
                
                Spacer(Modifier.height(5.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Tax:", fontSize = 14.sp)
                    Text("₹${viewModel.totalTax.value.to2FString()}", fontWeight = FontWeight.SemiBold)
                }
                
                Spacer(Modifier.height(5.dp))
                Divider()
                Spacer(Modifier.height(5.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Grand Total:", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "₹${viewModel.grandTotal.value.to2FString()}", 
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        Spacer(Modifier.height(15.dp))
        
        // Customer Summary
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text("Customer Info", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                
                if (viewModel.customerName.text.isNotBlank()) {
                    Text("Name: ${viewModel.customerName.text}", fontSize = 12.sp)
                }
                if (viewModel.customerMobile.text.isNotBlank()) {
                    Text("Mobile: ${viewModel.customerMobile.text}", fontSize = 12.sp)
                }
                if (viewModel.customerAddress.text.isNotBlank()) {
                    Text("Address: ${viewModel.customerAddress.text}", fontSize = 12.sp)
                }
                if (viewModel.customerGstin.text.isNotBlank()) {
                    Text("GSTIN: ${viewModel.customerGstin.text}", fontSize = 12.sp)
                }
                
                if (viewModel.customerName.text.isBlank() && 
                    viewModel.customerMobile.text.isBlank() && 
                    viewModel.customerAddress.text.isBlank() && 
                    viewModel.customerGstin.text.isBlank()) {
                    Text(
                        "No customer details added",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun DraftViewAddItemDialog(viewModel: DraftInvoiceViewModel) {
    AlertDialog(
        onDismissRequest = { viewModel.showAddItemDialog.value = false },
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
                            state = viewModel.itemName,
                            placeholderText = "Item Name",
                            keyboardType = KeyboardType.Text
                        )
                    }
                    
                    Row(Modifier.fillMaxWidth()) {
                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.categoryName,
                            placeholderText = "Category",
                            keyboardType = KeyboardType.Text
                        )
                        Spacer(Modifier.width(5.dp))
                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.subCategoryName,
                            placeholderText = "Sub Category",
                            keyboardType = KeyboardType.Text
                        )
                    }
                    
                    Row(Modifier.fillMaxWidth()) {
                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.entryType,
                            placeholderText = "Entry Type",
                            dropdownItems = EntryType.list(),
                            onDropdownItemSelected = { selected ->
                                when (selected) {
                                    EntryType.Piece.type -> {
                                        viewModel.quantity.text = "1"
                                    }
                                    EntryType.Lot.type -> {
                                        // Keep current quantity
                                    }
                                }
                                viewModel.entryType.text = selected
                            }
                        )
                        Spacer(Modifier.width(5.dp))
                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.quantity,
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
                            state = viewModel.grossWeight,
                            placeholderText = "Gross Wt (gm)",
                            keyboardType = KeyboardType.Number,
                            onTextChange = {
                                viewModel.netWeight.text = it
                            }
                        )
                        Spacer(Modifier.width(5.dp))
                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.netWeight,
                            placeholderText = "Net Wt (gm)",
                            keyboardType = KeyboardType.Number
                        )
                    }
                    
                    Row(Modifier.fillMaxWidth()) {
                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.purity,
                            placeholderText = "Purity",
                            dropdownItems = Purity.list(),
                            onDropdownItemSelected = { selected ->
                                if (viewModel.netWeight.text.isNotBlank()) {
                                    val ntWtValue = viewModel.netWeight.text.toDoubleOrNull() ?: 0.0
                                    val multiplier = Purity.fromLabel(selected)?.multiplier ?: 1.0
                                    viewModel.fineWeight.text = String.format("%.2f", ntWtValue * multiplier)
                                }
                                viewModel.purity.text = selected
                            }
                        )
                        Spacer(Modifier.width(5.dp))
                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.fineWeight,
                            placeholderText = "Fine Wt (gm)",
                            keyboardType = KeyboardType.Number
                        )
                    }
                }
                
                item {
                    // Charges Information
                    Spacer(Modifier.height(10.dp))
                    Text("Charges Information", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(5.dp))
                    
                    Row(Modifier.fillMaxWidth()) {
                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.chargeType,
                            placeholderText = "Charge Type",
                            dropdownItems = ChargeType.list()
                        )
                        Spacer(Modifier.width(5.dp))
                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.charge,
                            placeholderText = "Charge",
                            keyboardType = KeyboardType.Number
                        )
                    }
                    
                    Row(Modifier.fillMaxWidth()) {
                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.otherChargeDescription,
                            placeholderText = "Other Charge Desc",
                            keyboardType = KeyboardType.Text
                        )
                        Spacer(Modifier.width(5.dp))
                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.otherCharge,
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
                            state = viewModel.cgst,
                            placeholderText = "CGST",
                            keyboardType = KeyboardType.Number
                        )
                        Spacer(Modifier.width(5.dp))
                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.sgst,
                            placeholderText = "SGST",
                            keyboardType = KeyboardType.Number
                        )
                        Spacer(Modifier.width(5.dp))
                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.igst,
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
                            state = viewModel.huid,
                            placeholderText = "H-UID",
                            keyboardType = KeyboardType.Text
                        )
                    }
                    
                    Row(Modifier.fillMaxWidth()) {
                        CusOutlinedTextField(
                            modifier = Modifier.weight(1f),
                            state = viewModel.description,
                            placeholderText = "Description Key",
                            keyboardType = KeyboardType.Text
                        )
                        Spacer(Modifier.width(5.dp))
                        CusOutlinedTextField(
                            modifier = Modifier.weight(2f),
                            state = viewModel.descriptionValue,
                            placeholderText = "Description Value",
                            keyboardType = KeyboardType.Text
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { viewModel.addItem() }) {
                Text("Add Item")
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.showAddItemDialog.value = false }) {
                Text("Cancel")
            }
        }
    )
}