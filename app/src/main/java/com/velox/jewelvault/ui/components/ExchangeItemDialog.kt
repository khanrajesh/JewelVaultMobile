package com.velox.jewelvault.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.velox.jewelvault.data.roomdb.dto.ExchangeItemDto
import com.velox.jewelvault.utils.Purity
import com.velox.jewelvault.utils.generateId
import com.velox.jewelvault.utils.to2FString

@Composable
fun ExchangeItemDialog(
    existingExchangeItems: List<ExchangeItemDto> = emptyList(),
    metalRates: List<com.velox.jewelvault.data.MetalRate>,
    onDismiss: () -> Unit,
    onSave: (List<ExchangeItemDto>) -> Unit,
    onClearAll: () -> Unit = {}
) {
    // Form fields for new item
    var metalType by remember { mutableStateOf("") }
    var purity by remember { mutableStateOf("") }
    var grossWeight by remember { mutableStateOf("") }
    var fineWeight by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var isExchangedByMetal by remember { mutableStateOf(true) }
    
    // Local state to manage items within the dialog
    var localExchangeItems by remember { mutableStateOf(existingExchangeItems) }
    
    // Available metal types from metal rates
    val availableMetals = metalRates.map { it.metal }.distinct()
    
    // Calculate exchange value based on method
    val exchangeValue = remember(grossWeight, fineWeight, price, isExchangedByMetal, metalType, purity, metalRates) {
        when {
            isExchangedByMetal -> {
                val fineWt = fineWeight.toDoubleOrNull() ?: 0.0
                val unitPrice = com.velox.jewelvault.utils.CalculationUtils.metalUnitPrice(metalType, metalRates) ?: 0.0
                fineWt * unitPrice
            }
            else -> price.toDoubleOrNull() ?: 0.0
        }
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 700.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Exchange Items",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Show all exchange items (both existing and newly added)
                if (localExchangeItems.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Exchange Items (${localExchangeItems.size})",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        TextButton(
                            onClick = {
                                localExchangeItems = emptyList()
                                onClearAll()
                            }
                        ) {
                            Text("Clear All", color = Color.Red)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    localExchangeItems.forEach { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "${item.metalType} ${item.purity}",
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        "Gs: ${item.grossWeight.to2FString()}gm, Fn: ${item.fineWeight.to2FString()}gm",
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        "Value: ₹${item.exchangeValue.to2FString()}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Row {
                                    IconButton(
                                        onClick = { 
                                            // Edit existing item - remove it and add to form
                                            metalType = item.metalType
                                            purity = item.purity
                                            grossWeight = item.grossWeight.toString()
                                            fineWeight = item.fineWeight.toString()
                                            price = item.price.toString()
                                            isExchangedByMetal = item.isExchangedByMetal
                                            
                                            // Remove the item from the local list
                                            localExchangeItems = localExchangeItems.filter { it.exchangeItemId != item.exchangeItemId }
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Edit",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { 
                                            // Delete individual item from local list
                                            localExchangeItems = localExchangeItems.filter { it.exchangeItemId != item.exchangeItemId }
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = Color.Red
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Form for new item
                Text(
                    text = "Add New Exchange Item",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                // Metal Type Dropdown
                CusOutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    state = InputFieldState(initValue = metalType),
                    placeholderText = "Metal Type",
                    dropdownItems = availableMetals,
                    onDropdownItemSelected = { selected ->
                        metalType = selected
                    },
                    onTextChange = { metalType = it }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Purity Dropdown
                CusOutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    state = InputFieldState(initValue = purity),
                    placeholderText = "Purity",
                    dropdownItems = Purity.list(),
                    allowEditOnDropdown = true,
                    onDropdownItemSelected = { selected ->
                        purity = selected
                    },
                    onTextChange = { purity = it }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Weight Fields
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CusOutlinedTextField(
                        modifier = Modifier.weight(1f),
                        state = InputFieldState(initValue = grossWeight),
                        placeholderText = "Gross Weight (gm)",
                        keyboardType = KeyboardType.Number,
                        onTextChange = { grossWeight = it }
                    )
                    
                    CusOutlinedTextField(
                        modifier = Modifier.weight(1f),
                        state = InputFieldState(initValue = fineWeight),
                        placeholderText = "Fine Weight (gm)",
                        keyboardType = KeyboardType.Number,
                        onTextChange = { fineWeight = it }
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Price Field
                CusOutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    state = InputFieldState(initValue = price),
                    placeholderText = "Price (₹)",
                    keyboardType = KeyboardType.Number,
                    onTextChange = { price = it }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Exchange Method Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Exchange by:",
                        modifier = Modifier.weight(1f)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isExchangedByMetal,
                            onClick = { isExchangedByMetal = true }
                        )
                        Text("Metal Rate", modifier = Modifier.padding(start = 4.dp))
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        RadioButton(
                            selected = !isExchangedByMetal,
                            onClick = { isExchangedByMetal = false }
                        )
                        Text("Price", modifier = Modifier.padding(start = 4.dp))
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Exchange Value Display
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Exchange Value:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "₹ ${String.format("%.2f", exchangeValue)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (isExchangedByMetal) "Calculated from metal rate" else "Direct price input",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            val newExchangeItem = ExchangeItemDto(
                                exchangeItemId = generateId(),
                                metalType = metalType,
                                purity = purity,
                                grossWeight = grossWeight.toDoubleOrNull() ?: 0.0,
                                fineWeight = fineWeight.toDoubleOrNull() ?: 0.0,
                                price = price.toDoubleOrNull() ?: 0.0,
                                isExchangedByMetal = isExchangedByMetal,
                                exchangeValue = exchangeValue
                            )
                            
                            // Add new item to local list
                            localExchangeItems = localExchangeItems + newExchangeItem
                            
                            // Clear form
                            metalType = ""
                            purity = ""
                            grossWeight = ""
                            fineWeight = ""
                            price = ""
                            isExchangedByMetal = true
                        },
                        enabled = metalType.isNotBlank() && 
                                 purity.isNotBlank() && 
                                 grossWeight.isNotBlank() && 
                                 fineWeight.isNotBlank() &&
                                 (isExchangedByMetal || price.isNotBlank())
                    ) {
                        Text("Add")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            // Save all items (both existing and newly added)
                            onSave(localExchangeItems)
                            onDismiss()
                        },
                        enabled = localExchangeItems.isNotEmpty()
                    ) {
                        Text("Save All")
                    }
                }
            }
        }
    }
}
