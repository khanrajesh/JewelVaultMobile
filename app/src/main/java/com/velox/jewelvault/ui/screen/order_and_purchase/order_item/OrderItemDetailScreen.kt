package com.velox.jewelvault.ui.screen.order_and_purchase.order_item

import android.provider.CalendarContract
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.velox.jewelvault.data.roomdb.entity.order.OrderEntity
import com.velox.jewelvault.data.roomdb.entity.order.OrderItemEntity
import com.velox.jewelvault.ui.components.TextListView
import com.velox.jewelvault.utils.LocalSubNavController
import com.velox.jewelvault.utils.VaultPreview
import com.velox.jewelvault.utils.to2FString
import com.velox.jewelvault.utils.formatDate
import com.velox.jewelvault.utils.CalculationUtils

@Composable
@VaultPreview
fun OrderItemDetailScreenPreview(){
    val viewModel = hiltViewModel<OrderItemViewModel>()
    val orderId = "2"
     OrderItemDetailScreen(viewModel, orderId)
}
@Composable
fun OrderItemDetailScreen(viewModel: OrderItemViewModel, orderId: String) {
    val subNavController = LocalSubNavController.current
    
    LaunchedEffect(orderId) {
        viewModel.loadOrderDetails(orderId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(topStart = 18.dp)
            )
            .padding(5.dp)) {

        if (viewModel.orderWithItems == null){
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
               Text("No Order Found")
            }
        }else{
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Compact Header
                item {
                    CompactOrderHeader(
                        order = viewModel.orderWithItems!!.order,
                        customer = viewModel.customer,
                        onCustomerClick = { customerMobile ->
                            subNavController.navigate("customer_detail/$customerMobile")
                        }
                    )
                }

                // Order Items using TextListView
                item {
                    OrderItemsTextListView(items = viewModel.orderWithItems!!.items)
                }

                // Order Notes (if any)
                if (viewModel.orderWithItems!!.order.note?.isNotEmpty() == true) {
                    item {
                        CompactNotesCard(note = viewModel.orderWithItems!!.order.note!!)
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactOrderHeader(
    order: OrderEntity,
    customer: com.velox.jewelvault.data.roomdb.entity.customer.CustomerEntity?,
    onCustomerClick: (String) -> Unit
) {
    Card(
        modifier = Modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
            Row(modifier = Modifier.padding(12.dp)) {

                //Order Details Section
                Column(modifier = Modifier.weight(1f).padding(12.dp)) {
                    Text(
                        text = "Order #${order.orderId}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatDate(order.orderDate),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Compact summary row
                    Row {
                        Text(
                            text = "Subtotal",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "₹${order.totalAmount.to2FString()}",
                            fontWeight = FontWeight.Medium
                        )

                        if (order.totalCharge > 0) {
                            Text(
                                text = "M.Charges",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "₹${order.totalCharge.to2FString()}",
                                fontWeight = FontWeight.Medium
                            )
                        }
                        if (order.totalTax > 0) {
                            Text(
                                text = "Tax",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "₹${order.totalTax.to2FString()}",
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    // Grand Total
                    Row(
                        modifier = Modifier,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Grand Total",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "₹${CalculationUtils.totalPrice(order.totalAmount, order.totalCharge, 0.0,order.totalTax).to2FString()}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                //Customer Section
                Column(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onCustomerClick(order.customerMobile) }
                        .padding(16.dp)
                ) {
                    Text(
                        text = customer?.name ?: "Unknown Customer",
                        fontWeight = FontWeight.Bold,
//                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = order.customerMobile,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!customer?.gstin_pan.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = customer.gstin_pan,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    if (!customer?.address.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = customer.address,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }


    }
}

@Composable
private fun OrderItemsTextListView(items: List<OrderItemEntity>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Items (${items.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Prepare data for TextListView
            val headerList = listOf(
                "Item Details",
                "Weight Info",
                "Additional Info",
                "Price Breakdown",
                "Total Amount",
                "Qty"
            )
            
            val itemsData = items.map { item ->
                listOf(
                    "${item.catName} • ${item.subCatName} • ${item.itemAddName}",
                    "GW: ${item.gsWt}g\nNW: ${item.ntWt}g\nFW: ${item.fnWt}g\nPurity: ${item.purity}",
                    buildString {
                        if (item.huid.isNotEmpty()) append("HUID: ${item.huid}\n")
                        if (item.addDesKey.isNotEmpty() && item.addDesValue.isNotEmpty()) {
                            append("${item.addDesKey}: ${item.addDesValue}")
                        }
                    }.trim(),
                    buildString {
                        append("Base: ₹${item.price.to2FString()}")
                        if (item.charge > 0) append("\nM.Charge: ₹${item.charge.to2FString()}")
                        if (item.tax > 0) append("\nTax: ₹${item.tax.to2FString()}")
                    },
                    "₹${CalculationUtils.totalPrice(item.price, item.charge, item.othCrg,item.tax).to2FString()}",
                    "${item.quantity}"
                )
            }
            
            TextListView(
                headerList = headerList,
                items = itemsData,
                modifier = Modifier.height(300.dp),
                onItemClick = { /* Handle item click if needed */ },
                onItemLongClick = { /* Handle long click if needed */ }
            )
        }
    }
}

@Composable
private fun CompactNotesCard(note: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Note",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = note,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

