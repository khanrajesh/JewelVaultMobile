package com.velox.jewelvault.ui.screen.customers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.velox.jewelvault.data.roomdb.dto.CustomerSummaryDto
import com.velox.jewelvault.ui.components.CusOutlinedTextField
import com.velox.jewelvault.ui.components.InputFieldState
import com.velox.jewelvault.utils.LocalSubNavController
import com.velox.jewelvault.utils.formatCurrency

@Composable
fun CustomerScreen(
     viewModel: CustomerViewModel = hiltViewModel()
) {
    val subNavController = LocalSubNavController.current
    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var showKhataBookPlans by remember { mutableStateOf(false) }
    val searchQuery = remember { InputFieldState() }
    val filterType = remember { InputFieldState("all") }

    LaunchedEffect(Unit) {
        viewModel.loadCustomerData()
    }

    Column (
        modifier = Modifier

            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(topStart = 18.dp))
            .padding(5.dp), verticalArrangement = Arrangement.spacedBy(16.dp)
    ){

        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Customer Details", fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(start = 10.dp))
            Spacer(Modifier.weight(1f))
            CusOutlinedTextField(
                state = searchQuery,
                placeholderText = "Search by name or mobile number",
                modifier = Modifier.width(400.dp),
                keyboardType = KeyboardType.Phone,
                trailingIcon = Icons.Default.Search,
                onTrailingIconClick = {
                    viewModel.searchCustomers(searchQuery.text)
                },
                validation = { input -> if (input.length != 10) "Please Enter Valid Number" else null },
                leadingIcon = Icons.Default.Clear,
                onLeadingIconClick = {
                    searchQuery.text = ""
                    viewModel.loadCustomerData()
                }
            )

            IconButton(onClick = { showKhataBookPlans = true }) {
                Icon(Icons.Default.Book, contentDescription = "Khata Book Plans")
            }
            IconButton(onClick = { showAddCustomerDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Customer")
            }
        }
        LazyColumn(
            Modifier.fillMaxWidth()
        ) {

            // Statistics Cards
            item {
                CustomerStatisticsCard(viewModel)
            }

            // Filter Chips
            item {
                FilterChips(selectedFilter = filterType.text, onFilterSelected = { filter ->
                    filterType.text = filter
                    viewModel.filterCustomers(filter)
                })
            }

            // Customer List
            items(viewModel.customerList) { customer ->
                CustomerCard(customer = customer,
                    onClick = { subNavController.navigate("customer_detail/${customer.mobileNo}") })
            }
        }

    }


    // Add Customer Dialog
    if (showAddCustomerDialog) {
        AddCustomerDialog(
            onDismiss = { showAddCustomerDialog = false },
            onConfirm = { name, mobile, address, gstin, notes ->
                // Validation
                val phoneRegex = Regex("^[6-9][0-9]{9}")
                if (!phoneRegex.matches(mobile.trim())) {
                    viewModel.snackBarState.value = "Invalid mobile number format."
                    return@AddCustomerDialog
                }
                if (viewModel.customerList.any { it.mobileNo == mobile.trim() }) {
                    viewModel.snackBarState.value = "Customer with this mobile already exists."
                    return@AddCustomerDialog
                }
                viewModel.customerName.text = name
                viewModel.customerMobile.text = mobile
                viewModel.customerAddress.text = address
                viewModel.customerGstin.text = gstin
                viewModel.customerNotes.text = notes
                viewModel.addCustomer()
                showAddCustomerDialog = false
            }
        )
    }

    // Khata Book Plans Navigation
    if (showKhataBookPlans) {
        LaunchedEffect(Unit) {
            subNavController.navigate("khata_book_plans")
            showKhataBookPlans = false
        }
    }
}


@Composable
fun CustomerStatisticsCard(viewModel: CustomerViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatisticItem(
                    label = "Total Customers",
                    value = viewModel.totalCustomers.value.toString(),
                    icon = Icons.Default.People
                )
                StatisticItem(
                    label = "Outstanding Amount",
                    value = formatCurrency(viewModel.totalOutstandingAmount.value),
                    icon = Icons.Default.AccountBalance
                )
                StatisticItem(
                    label = "Khata Book Amount",
                    value = formatCurrency(viewModel.totalKhataBookAmount.value),
                    icon = Icons.Default.Book
                )
                StatisticItem(
                    label = "Active Customers",
                    value = viewModel.customerList.count { it.isActive }.toString(),
                    icon = Icons.Default.CheckCircle
                )
            }
        }
    }
}

@Composable
fun StatisticItem(
    label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun FilterChips(
    selectedFilter: String, onFilterSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(selected = selectedFilter == "all",
            onClick = { onFilterSelected("all") },
            label = { Text("All") })
        FilterChip(selected = selectedFilter == "outstanding",
            onClick = { onFilterSelected("outstanding") },
            label = { Text("Outstanding") })
        FilterChip(selected = selectedFilter == "khata",
            onClick = { onFilterSelected("khata") },
            label = { Text("Khata Book") })
    }
}

@Composable
fun CustomerCard(
    customer: CustomerSummaryDto, onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    Row {
                        Text(
                            text = customer.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "(${customer.mobileNo})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(5.dp))
                        customer.address?.let { address ->
                            Text(
                                text = address,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatCurrency(customer.totalAmount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${customer.totalOrders} orders",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }



            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    if (customer.outstandingBalance > 0) {
                        Text(
                            text = "Outstanding: ${formatCurrency(customer.outstandingBalance)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (customer.hasKhataBook) {
                        val progressText = if (customer.totalKhataBookAmount > 0) {
                            val paidPercentage = (customer.totalKhataBookPaidAmount / customer.totalKhataBookAmount * 100).toInt()
                            "${customer.activeKhataBookCount} plan(s) - ${paidPercentage}% paid"
                        } else {
                            "${customer.activeKhataBookCount} plan(s)"
                        }
                        
                        Text(
                            text = "Khata: $progressText",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (customer.outstandingBalance > 0) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    if (customer.hasKhataBook) {
                        Icon(
                            imageVector = Icons.Default.Book,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AddCustomerDialog(
    onDismiss: () -> Unit, onConfirm: (String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var gstin by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add New Customer") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CusOutlinedTextField(
                state = InputFieldState(name),
                onTextChange = { name = it },
                placeholderText = "Customer Name *"
            )

            CusOutlinedTextField(
                state = InputFieldState(mobile),
                onTextChange = { mobile = it },
                placeholderText = "Mobile Number *",
                keyboardType = KeyboardType.Phone,
                validation = { input -> if (input.length != 10) "Please Enter Valid Number" else null }
            )

            CusOutlinedTextField(
                state = InputFieldState(address),
                onTextChange = { address = it },
                placeholderText = "Address *"
            )

            CusOutlinedTextField(
                state = InputFieldState(gstin),
                onTextChange = { gstin = it },
                placeholderText = "GSTIN/PAN (Optional)"
            )

            CusOutlinedTextField(
                state = InputFieldState(notes),
                onTextChange = { notes = it },
                placeholderText = "Notes (Optional)"
            )
        }
    }, confirmButton = {
        Button(
            onClick = { onConfirm(name, mobile, address, gstin, notes) },
            enabled = name.isNotEmpty() && mobile.isNotEmpty() && address.isNotEmpty()
        ) {
            Text("Add Customer")
        }
    }, dismissButton = {
        TextButton(onClick = onDismiss) {
            Text("Cancel")
        }
    })
} 