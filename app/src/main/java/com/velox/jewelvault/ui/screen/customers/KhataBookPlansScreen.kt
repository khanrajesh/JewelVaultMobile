package com.velox.jewelvault.ui.screen.customers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.velox.jewelvault.data.roomdb.entity.CustomerKhataBookEntity
import com.velox.jewelvault.ui.components.CusOutlinedTextField
import com.velox.jewelvault.ui.components.InputFieldState
import com.velox.jewelvault.ui.theme.LightGreen
import com.velox.jewelvault.utils.formatCurrency
import com.velox.jewelvault.utils.formatDate
import com.velox.jewelvault.utils.to2FString


@Composable
fun KhataBookPlansScreen(
    navController: NavController,
    viewModel: CustomerViewModel = hiltViewModel()
) {
    var showAddPlanDialog by remember { mutableStateOf(false) }
    var showCalculatorDialog by remember { mutableStateOf(false) }
    var selectedPlan by remember { mutableStateOf<KhataBookPlan?>(null) }
    var showEditPlanDialog by remember { mutableStateOf<KhataBookPlan?>(null) }
    var showDeletePlanDialog by remember { mutableStateOf<KhataBookPlan?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadKhataBookPlans()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(topStart = 18.dp))
            .padding(5.dp)
    ) {
        // Header with action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "Khata Book Plans",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row {
                IconButton(onClick = { showCalculatorDialog = true }) {
                    Icon(Icons.Default.Calculate, contentDescription = "Calculator")
                }
                IconButton(onClick = { showAddPlanDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Plan")
                }
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Statistics Cards
            item {
                KhataBookStatisticsCard(viewModel)
            }

            // Predefined Plans
            item {
                Text(
                    text = "Predefined Plans",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            // Plan Cards Grid
            item {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    items(viewModel.planList) { plan ->
                        KhataBookPlanCard(
                            plan = plan,
                            onClick = { selectedPlan = plan },
                            onEdit = { showEditPlanDialog = plan },
                            onDelete = { showDeletePlanDialog = plan }
                        )
                    }
                }
            }

            // Active Customers with Khata Book
            item {
                Text(
                    text = "Active Khata Book Customers",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            items(viewModel.activeKhataBookCustomers) { customer ->
                ActiveKhataBookCustomerCard(
                    customer = customer,
                    onClick = { navController.navigate("customer_detail/${customer.customerMobile}") }
                )
            }
        }

        // Dialogs
        if (showAddPlanDialog) {
            AddKhataBookPlanDialog(
                onDismiss = { showAddPlanDialog = false },
                onConfirm = { name, payMonths, benefitMonths, description ->
                    viewModel.addPlan(name, payMonths, benefitMonths, description)
                    showAddPlanDialog = false
                }
            )
        }

        if (showCalculatorDialog) {
            KhataBookCalculatorDialog(
                onDismiss = { showCalculatorDialog = false },
                viewModel
            )
        }

        selectedPlan?.let { plan ->
            PlanDetailsDialog(
                plan = plan,
                onDismiss = { selectedPlan = null },
                onApply = { customerMobile ->
                    viewModel.applyKhataBookPlan(customerMobile, plan)
                    selectedPlan = null
                }
            )
        }

        // Edit Plan Dialog
        showEditPlanDialog?.let { plan ->
            AddKhataBookPlanDialog(
                initialPlan = plan,
                onDismiss = { showEditPlanDialog = null },
                onConfirm = { name, payMonths, benefitMonths, description ->
                    viewModel.editPlan(plan, name, payMonths, benefitMonths, description)
                    showEditPlanDialog = null
                }
            )
        }
        // Delete Plan Dialog
        showDeletePlanDialog?.let { plan ->
            ConfirmDeleteDialog(
                plan = plan,
                onDismiss = { showDeletePlanDialog = null },
                onConfirm = {
                    viewModel.deletePlan(plan)
                    showDeletePlanDialog = null
                }
            )
        }
    }
}

@Composable
fun KhataBookStatisticsCard(viewModel: CustomerViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            KhataBookStatisticItem(
                modifier = Modifier.weight(1f),
                label = "Active",
                value = viewModel.activeKhataBookCustomers.size.toString(),
                icon = Icons.Default.Book
            )
            KhataBookStatisticItem(
                modifier = Modifier.weight(1f),
                label = "Total",
                value = formatCurrency(viewModel.totalKhataBookAmount.value),
                icon = Icons.Default.AccountBalance
            )
            KhataBookStatisticItem(
                modifier = Modifier.weight(1f),
                label = "Matured",
                value = viewModel.maturedKhataBookPlans.size.toString(),
                icon = Icons.Default.CheckCircle
            )
            KhataBookStatisticItem(
                modifier = Modifier.weight(1f),
                label = "Pending",
                value = formatCurrency(viewModel.pendingKhataBookAmount.value),
                icon = Icons.Default.Pending
            )
        }
    }
}

@Composable
fun KhataBookStatisticItem(
    modifier: Modifier,
    label: String,
    value: String,
    icon: ImageVector
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun KhataBookPlanCard(
    plan: KhataBookPlan,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = plan.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.semantics { contentDescription = "Edit Plan" }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Plan")
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.semantics { contentDescription = "Delete Plan" }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Plan")
                    }
                }
            }
            
            // Description
            Text(
                text = plan.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
            
            // Plan details in compact format
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Pay: ${plan.payMonths}m",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Free: ${plan.benefitMonths}m",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = LightGreen
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total: ${plan.payMonths + plan.benefitMonths}m",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Eff: ${plan.effectiveMonths}m",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ActiveKhataBookCustomerCard(
    customer: CustomerKhataBookEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = customer.customerMobile,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                FilterChip(
                    selected = false,
                    onClick = { },
                    label = { Text(customer.status.capitalize()) }
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Monthly: ${formatCurrency(customer.monthlyAmount)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Total: ${formatCurrency(customer.totalAmount)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Start: ${formatDate(customer.startDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "End: ${formatDate(customer.endDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            customer.notes?.let { notes ->
                Text(
                    text = "Notes: $notes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ConfirmDeleteDialog(plan: KhataBookPlan, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Plan") },
        text = { Text("Are you sure you want to delete the plan '${plan.name}'?") },
        confirmButton = {
            Button(onClick = onConfirm) { Text("Delete") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddKhataBookPlanDialog(
    initialPlan: KhataBookPlan? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, Int, Int, String) -> Unit
) {
    var name by remember { mutableStateOf(initialPlan?.name ?: "") }
    var payMonths by remember { mutableStateOf(initialPlan?.payMonths?.toString() ?: "") }
    var benefitMonths by remember { mutableStateOf(initialPlan?.benefitMonths?.toString() ?: "") }
    var description by remember { mutableStateOf(initialPlan?.description ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialPlan == null) "Add Khata Book Plan" else "Edit Khata Book Plan") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CusOutlinedTextField(
                    state = InputFieldState(name),
                    onTextChange = { name = it },
                    placeholderText = "Plan Name"
                )
                CusOutlinedTextField(
                    state = InputFieldState(payMonths),
                    onTextChange = { payMonths = it },
                    placeholderText = "Pay Months",
                    keyboardType = KeyboardType.Number
                )
                CusOutlinedTextField(
                    state = InputFieldState(benefitMonths),
                    onTextChange = { benefitMonths = it },
                    placeholderText = "Benefit Months",
                    keyboardType = KeyboardType.Number
                )
                CusOutlinedTextField(
                    state = InputFieldState(description),
                    onTextChange = { description = it },
                    placeholderText = "Description (Optional)"
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val pay = payMonths.toIntOrNull() ?: 0
                    val benefit = benefitMonths.toIntOrNull() ?: 0
                    onConfirm(name, pay, benefit, description)
                },
                enabled = name.isNotEmpty() && payMonths.isNotEmpty() && benefitMonths.isNotEmpty()
            ) {
                Text(if (initialPlan == null) "Add" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun KhataBookCalculatorDialog(
    onDismiss: () -> Unit,
    viewModel: CustomerViewModel
) {
    var monthlyAmount by remember { mutableStateOf("") }
    var selectedPlan by remember { mutableStateOf<KhataBookPlan?>(null) }
    var calculatedResults by remember { mutableStateOf<CalculatorResults?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Khata Book Calculator") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .heightIn(max = 600.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CusOutlinedTextField(
                    state = InputFieldState(monthlyAmount),
                    onTextChange = { monthlyAmount = it },
                    placeholderText = "Monthly Amount",
                    keyboardType = KeyboardType.Number
                )
                
                Text(
                    text = "Select Plan",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                // Grid for plan selection
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 200.dp)
                ) {
                    items(viewModel.planList) { plan ->
                        PlanSelectionCard(
                            plan = plan,
                            isSelected = selectedPlan == plan,
                            onClick = { selectedPlan = plan }
                        )
                    }
                }
                
                selectedPlan?.let { plan ->
                    Button(
                        onClick = {
                            val amount = monthlyAmount.toDoubleOrNull() ?: 0.0
                            calculatedResults = calculateKhataBook(amount, plan)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = monthlyAmount.isNotEmpty()
                    ) {
                        Text("Calculate")
                    }
                }
                
                calculatedResults?.let { results ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Calculation Results",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            
                            InfoRow("Total Pay Amount", formatCurrency(results.totalPayAmount))
                            InfoRow("Total Benefit Amount", formatCurrency(results.totalBenefitAmount))
                            InfoRow("Effective Monthly Amount", formatCurrency(results.effectiveMonthlyAmount))
                            InfoRow("Total Savings", formatCurrency(results.totalSavings))
                            InfoRow("Savings Percentage", "${results.savingsPercentage.to2FString()}%")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun PlanDetailsDialog(
    plan: KhataBookPlan,
    onDismiss: () -> Unit,
    onApply: (String) -> Unit
) {
    var customerMobile by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Apply ${plan.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = plan.description,
                    style = MaterialTheme.typography.bodyMedium
                )
                
                CusOutlinedTextField(
                    state = InputFieldState(customerMobile),
                    onTextChange = { customerMobile = it },
                    placeholderText = "Customer Mobile Number",
                    keyboardType = KeyboardType.Phone,
                    validation = { input -> if (input.length != 10) "Please Enter Valid Number" else null }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onApply(customerMobile) },
                enabled = customerMobile.isNotEmpty()
            ) {
                Text("Apply Plan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun PlanSelectionCard(
    plan: KhataBookPlan,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = plan.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Text(
                text = "${plan.payMonths} + ${plan.benefitMonths}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Text(
                text = "${plan.benefitPercentage}% Benefit",
                style = MaterialTheme.typography.bodySmall,
                color = LightGreen,
                fontWeight = FontWeight.Medium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
} 

private fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
} 