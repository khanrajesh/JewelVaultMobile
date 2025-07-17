package com.velox.jewelvault.ui.screen.draft_invoice

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.velox.jewelvault.utils.LocalBaseViewModel
import com.velox.jewelvault.utils.LocalNavController
import com.velox.jewelvault.utils.ioScope
import com.velox.jewelvault.utils.rememberCurrentDateTime

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
//                            if (viewModel.customerMobile.text.isNotEmpty() && viewModel.selectedItemList.isNotEmpty()) {
                                navHost.navigate(Screens.DraftPreview.route)
//                            } else {
//                                viewModel.snackBarState.value = "Please ensure to add customer and items details"
//                            }
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
            }
        }
        if (viewModel.showAddItemDialog.value && viewModel.selectedItem.value != null) {
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
    // Placeholder for item list, can be expanded as needed
    Column(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .padding(5.dp)
    ) {
        Text("Item List (Draft)")
        // TODO: Add item list display
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
                .background(
                    MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)
                ), contentAlignment = Alignment.Center
        ) {
            Text("Add Item")
        }
    }
}

@Composable
fun DraftDetailSection(modifier: Modifier, viewModel: DraftInvoiceViewModel) {
    Column(modifier = modifier.padding(5.dp)) {
        Text("Details Section (Draft)")
        // TODO: Add details summary, totals, etc.
    }
}

@Composable
fun DraftViewAddItemDialog(viewModel: DraftInvoiceViewModel) {
    // Placeholder dialog for adding item
    AlertDialog(
        onDismissRequest = { viewModel.showAddItemDialog.value = false },
        title = { Text("Add Item (Draft)") },
        text = { Text("Item details form goes here.") },
        confirmButton = {
            TextButton(onClick = { viewModel.showAddItemDialog.value = false }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.showAddItemDialog.value = false }) {
                Text("Cancel")
            }
        }
    )
}