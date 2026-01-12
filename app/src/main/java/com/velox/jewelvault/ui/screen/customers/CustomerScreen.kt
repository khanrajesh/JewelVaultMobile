package com.velox.jewelvault.ui.screen.customers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.ArrowForward
import androidx.compose.material.icons.twotone.AccountBalance
import androidx.compose.material.icons.twotone.Add
import androidx.compose.material.icons.twotone.Book
import androidx.compose.material.icons.twotone.CheckCircle
import androidx.compose.material.icons.twotone.Clear
import androidx.compose.material.icons.twotone.People
import androidx.compose.material.icons.twotone.Search
import androidx.compose.material.icons.twotone.Warning
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.velox.jewelvault.data.roomdb.dto.CustomerSummaryDto
import com.velox.jewelvault.ui.components.CusOutlinedTextField
import com.velox.jewelvault.ui.components.InputFieldState
import com.velox.jewelvault.ui.nav.SubScreens
import com.velox.jewelvault.utils.LocalSubNavController
import com.velox.jewelvault.utils.formatCurrency
import com.velox.jewelvault.utils.isLandscape
import com.velox.jewelvault.utils.to2FString

@Composable
fun CustomerScreen(
    viewModel: CustomerViewModel = hiltViewModel()
) {

    viewModel.currentScreenHeadingState.value = "Customers"
    val subNavController = LocalSubNavController.current
    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var showKhataBookPlans by remember { mutableStateOf(false) }
    val searchQuery = remember { InputFieldState() }
    val filterType = remember { InputFieldState("all") }

    LaunchedEffect(Unit) {
        viewModel.loadCustomerData()
    }

    // Debug logging to check values
    LaunchedEffect(viewModel.totalCustomers.value, viewModel.totalOutstandingAmount.value) {
        println("DEBUG CustomerScreen - Total Customers: ${viewModel.totalCustomers.value}")
        println("DEBUG CustomerScreen - Outstanding Amount: ${viewModel.totalOutstandingAmount.value}")
        println("DEBUG CustomerScreen - Current Month Khata: ${viewModel.currentMonthKhataBookPayments.value}")
        println("DEBUG CustomerScreen - Total Khata Paid: ${viewModel.totalKhataBookPaidAmount.value}")
        println("DEBUG CustomerScreen - Active Khata Customers: ${viewModel.activeKhataBookCustomersCount.value}")
    }

    // Retry mechanism if data loading fails
    LaunchedEffect(viewModel.snackBarState.value) {
        if (viewModel.snackBarState.value.contains("Failed to load customer data")) {
            // Auto-retry after 2 seconds
            kotlinx.coroutines.delay(2000)
            viewModel.retryLoadCustomerData()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(topStart = 18.dp))
            .padding(5.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        val onSearchByTrailing =  {
            viewModel.searchCustomers(searchQuery.text)
        }

        val onSearchByLeading = {
            searchQuery.text = ""
            viewModel.loadCustomerData()
        }

        if (isLandscape()){
            Row(
                modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
            ) {
                CustomerStatisticsCard(Modifier.weight(1f), viewModel)
                Spacer(Modifier.width(10.dp))
                CusOutlinedTextField(
                    state = searchQuery,
                    placeholderText = "Search By No",
                    modifier = Modifier.width(300.dp),
                    keyboardType = KeyboardType.Phone,
                    trailingIcon = Icons.TwoTone.Search,
                    onTrailingIconClick = onSearchByTrailing,
                    validation = { input -> if (input.length != 10) "Please Enter Valid Number" else null },
                    leadingIcon = Icons.TwoTone.Clear,
                    onLeadingIconClick = onSearchByLeading)

                IconButton(onClick = { showKhataBookPlans = true }) {
                    Icon(Icons.TwoTone.Book, contentDescription = "Khata Book Plans")
                }
                IconButton(onClick = { showAddCustomerDialog = true }) {
                    Icon(Icons.TwoTone.Add, contentDescription = "Add Customer")
                }
            }
        }else{
            Column(Modifier.fillMaxWidth()) {
                CustomerStatisticsCard(Modifier.wrapContentHeight().fillMaxWidth(), viewModel)
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
                ) {
                    CusOutlinedTextField(
                        state = searchQuery,
                        placeholderText = "Search By No",
                        modifier = Modifier.width(300.dp),
                        keyboardType = KeyboardType.Phone,
                        trailingIcon = Icons.TwoTone.Search,
                        onTrailingIconClick = onSearchByTrailing,
                        validation = { input -> if (input.length != 10) "Please Enter Valid Number" else null },
                        leadingIcon = Icons.TwoTone.Clear,
                        onLeadingIconClick = onSearchByLeading)

                    IconButton(onClick = { showKhataBookPlans = true }) {
                        Icon(Icons.TwoTone.Book, contentDescription = "Khata Book Plans")
                    }
                    IconButton(onClick = { showAddCustomerDialog = true }) {
                        Icon(Icons.TwoTone.Add, contentDescription = "Add Customer")
                    }
                }
            }
        }

        Box(Modifier.fillMaxWidth().weight(1f)) {
            LazyColumn(
                Modifier.fillMaxWidth()
            ) {


                // Filter Chips
                item {
                    FilterChips(selectedFilter = filterType.text, onFilterSelected = { filter ->
                        filterType.text = filter
                        viewModel.filterCustomers(filter)
                    })
                }

                // Customer List
                items(viewModel.customerList) { customer ->
                    CustomerCard(
                        customer = customer,
                        onClick = { subNavController.navigate("${SubScreens.CustomersDetails.route}/${customer.mobileNo}") })
                }
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
            })
    }

    // Khata Book Plans Navigation
    if (showKhataBookPlans) {
        LaunchedEffect(Unit) {
            subNavController.navigate(SubScreens.KhataBookPlans.route)
            showKhataBookPlans = false
        }
    }
}


@Composable
fun CustomerStatisticsCard(modifier: Modifier, viewModel: CustomerViewModel) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                StatisticItem(
                    modifier = Modifier.weight(1f),
                    label = "Customers",
                    value = viewModel.totalCustomers.value.toString(),
                    icon = Icons.TwoTone.People
                )
                StatisticItem(
                    modifier = Modifier.weight(1f),
                    label = "Outstanding Balance",
                    value = formatCurrency(viewModel.totalOutstandingAmount.value),
                    icon = Icons.TwoTone.AccountBalance
                )
                StatisticItem(
                    modifier = Modifier.weight(1f),
                    label = "Month's Expected Khata",
                    value = formatCurrency(viewModel.currentMonthKhataBookPayments.value),
                    icon = Icons.TwoTone.CheckCircle
                )
                StatisticItem(
                    modifier = Modifier.weight(1f),
                    label = "Total Khata Paid",
                    value = formatCurrency(viewModel.totalKhataBookPaidAmount.value),
                    icon = Icons.TwoTone.Book
                )
                StatisticItem(
                    modifier = Modifier.weight(1f),
                    label = "Active Khata",
                    value = viewModel.activeKhataBookCustomersCount.value.toString(),
                    icon = Icons.TwoTone.Warning
                )
            }
        }
    }
}

@Composable
fun StatisticItem(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        modifier = modifier.fillMaxWidth(),
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
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
        FilterChip(
            selected = selectedFilter == "all",
            onClick = { onFilterSelected("all") },
            label = { Text("All") })
        FilterChip(
            selected = selectedFilter == "outstanding",
            onClick = { onFilterSelected("outstanding") },
            label = { Text("Outstanding") })
        FilterChip(
            selected = selectedFilter == "khata",
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
                Column {
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
                    }
                    customer.address?.let { address ->
                        Text(
                            text = address,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                }

                Spacer(Modifier.width(10.dp))
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
                            val paidPercentage =
                                (customer.totalKhataBookPaidAmount / customer.totalKhataBookAmount * 100).toInt()
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

                Spacer(Modifier.weight(1f))


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
                Spacer(modifier = Modifier.width(4.dp))


                Row {
                    Column {
                        if (customer.outstandingBalance > 0) {
                            Icon(
                                imageVector = Icons.TwoTone.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        if (customer.hasKhataBook) {
                            Icon(
                                imageVector = Icons.TwoTone.Book,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }

                }
                Spacer(modifier = Modifier.width(4.dp))

                Icon(
                    imageVector = Icons.AutoMirrored.TwoTone.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
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
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
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
                validation = { input -> if (input.length != 10) "Please Enter Valid Number" else null })

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
