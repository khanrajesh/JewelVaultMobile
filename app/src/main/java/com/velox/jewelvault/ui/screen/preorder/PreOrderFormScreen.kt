package com.velox.jewelvault.ui.screen.preorder

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Search
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.velox.jewelvault.ui.components.CusOutlinedTextField
import com.velox.jewelvault.ui.nav.SubScreens
import com.velox.jewelvault.utils.LocalSubNavController
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun PreOrderFormScreen(viewModel: PreOrderViewModel) {
    viewModel.currentScreenHeadingState.value = "New Pre-Order"
    val subNavController = LocalSubNavController.current
    val deliveryDate = remember { mutableStateOf(LocalDate.now()) }
    val deliveryFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }

    LaunchedEffect(Unit) {
        viewModel.loadCategories()
        if (viewModel.deliveryDateText.text.isBlank()) {
            viewModel.deliveryDateText.textChange(deliveryDate.value.format(deliveryFormatter))
        }
    }

    BackHandler {
        viewModel.clearForm()
        subNavController.popBackStack()
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(topStart = 18.dp))
            .padding(10.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Customer", fontWeight = FontWeight.Bold)

            CusOutlinedTextField(
                state = viewModel.customerMobile,
                placeholderText = "Customer Mobile",
                trailingIcon = Icons.TwoTone.Search,
                onTrailingIconClick = { viewModel.getCustomerByMobile() },
                keyboardType = KeyboardType.Number,
                singleLine = true
            )

            CusOutlinedTextField(
                state = viewModel.customerName,
                placeholderText = "Customer Name",
                singleLine = true
            )

            CusOutlinedTextField(
                state = viewModel.customerAddress,
                placeholderText = "Customer Address",
                maxLines = 2
            )

            Text("Pre-Order", fontWeight = FontWeight.Bold)

            CusOutlinedTextField(
                state = viewModel.deliveryDateText,
                placeholderText = "Delivery Date",
                isDatePicker = true,
                initialDate = deliveryDate.value,
                onDateSelected = { deliveryDate.value = it },
                singleLine = true
            )

            val categoryNames = remember(viewModel.categories.toList()) {
                viewModel.categories.map { it.catName }.distinct()
            }

            CusOutlinedTextField(
                state = viewModel.categoryName,
                placeholderText = "Category",
                dropdownItems = categoryNames,
                onDropdownItemSelected = viewModel::onCategorySelected,
                singleLine = true
            )

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                CusOutlinedTextField(
                    modifier = Modifier.weight(1f),
                    state = viewModel.quantity,
                    placeholderText = "Qty",
                    keyboardType = KeyboardType.Number,
                    singleLine = true
                )
                Spacer(Modifier.width(10.dp))
                CusOutlinedTextField(
                    modifier = Modifier.weight(1f),
                    state = viewModel.estimatedWeight,
                    placeholderText = "Est. Weight (g)",
                    keyboardType = KeyboardType.Number,
                    singleLine = true
                )
                Spacer(Modifier.width(10.dp))
                CusOutlinedTextField(
                    modifier = Modifier.weight(1f),
                    state = viewModel.estimatedPrice,
                    placeholderText = "Est. Price",
                    keyboardType = KeyboardType.Number,
                    singleLine = true
                )
            }

            Text("Extra Details (optional)", fontWeight = FontWeight.Bold)

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                CusOutlinedTextField(
                    modifier = Modifier.weight(1f),
                    state = viewModel.addDesKey,
                    placeholderText = "Description",
                    singleLine = true
                )
                Spacer(Modifier.width(10.dp))
                CusOutlinedTextField(
                    modifier = Modifier.weight(1f),
                    state = viewModel.addDesValue,
                    placeholderText = "Value",
                    singleLine = true
                )
            }

            CusOutlinedTextField(
                state = viewModel.itemNote,
                placeholderText = "Item Note (optional)",
                maxLines = 2
            )

            CusOutlinedTextField(
                state = viewModel.preOrderNote,
                placeholderText = "Pre-Order Note (optional)",
                maxLines = 2
            )

            Text("Advance Payment (optional)", fontWeight = FontWeight.Bold)

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                CusOutlinedTextField(
                    modifier = Modifier.weight(1f),
                    state = viewModel.advanceAmount,
                    placeholderText = "Amount",
                    keyboardType = KeyboardType.Number,
                    singleLine = true
                )
                Spacer(Modifier.width(10.dp))
                CusOutlinedTextField(
                    modifier = Modifier.weight(1f),
                    state = viewModel.paymentMethod,
                    placeholderText = "Method",
                    dropdownItems = listOf("Cash", "UPI", "Bank", "Card"),
                    singleLine = true
                )
            }

            CusOutlinedTextField(
                state = viewModel.paymentReference,
                placeholderText = "Reference No (optional)",
                singleLine = true
            )

            CusOutlinedTextField(
                state = viewModel.paymentNotes,
                placeholderText = "Payment Note (optional)",
                maxLines = 2
            )

            Spacer(Modifier.height(10.dp))

            Button(
                onClick = {
                    viewModel.createPreOrder(
                        deliveryDate = deliveryDate.value,
                        onSuccess = { preOrderId ->
                            subNavController.navigate("${SubScreens.PreOrderDetail.route}/$preOrderId") {
                                popUpTo(SubScreens.PreOrderForm.route) { inclusive = true }
                            }
                        },
                        onFailure = { viewModel.snackBarState.value = it }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create Pre-Order")
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}
