package com.velox.jewelvault.ui.screen.order_and_purchase.order_item

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun OrderItemDetailScreen(hiltViewModel: OrderItemViewModel, orderId: String) {
    Column(
        modifier = Modifier.fillMaxSize().background(
            MaterialTheme.colorScheme.surface, RoundedCornerShape(topStart = 18.dp)
        ).padding(5.dp)
    ) {
        Text("Order Details ")
    }
}