package com.velox.jewelvault.ui.screen.order_and_report

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velox.jewelvault.data.roomdb.entity.order.OrderEntity
import com.velox.jewelvault.utils.OrderSort
import com.velox.jewelvault.utils.to2FString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

@OptIn(ExperimentalFoundationApi::class)
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun OrderDetailScreen(viewModel: OrderAndReportViewModel) {

    LaunchedEffect(true) {
        CoroutineScope(Dispatchers.IO).async {
            viewModel.getAllOrdersSorted(OrderSort.DESCENDING)
        }.await()
    }

    Column(
        modifier = Modifier.background(
            MaterialTheme.colorScheme.surface,
            RoundedCornerShape(topStart = 18.dp)
        )
    ) {
        val scrollState = rememberScrollState()

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val columnWidth = 100.dp
            val itemsWithHeader = listOf<OrderEntity?>(null) + viewModel.orderList

            Column(modifier = Modifier.horizontalScroll(scrollState)) {
                LazyColumn {
                    itemsIndexed(itemsWithHeader) { index, item ->
                        val isHeader = index == 0
                        val values = if (isHeader) {
                            viewModel.orderHeaderList
                        } else {
                            if (item != null) {
                                //"S.No", "Order Id","Order Date","Customer Name","Customer No", "Total Amount", "Total Charge"
                                listOf(
                                    "$index",
                                    "${item.orderId}",
                                    "${item.orderDate}",
                                    "${item.customerMobile} ",
                                    "${item.customerMobile} ",
                                    "${item.totalAmount.to2FString()} ",
                                    "${item.totalCharge.to2FString()} "
                                )
                            } else {
                                emptyList()
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(30.dp)
                                .combinedClickable(onClick = {

                                }, onLongClick = {
                                    if (!isHeader && item != null) {

                                    }
                                })
                        ) {
                            values.forEachIndexed { i, value ->
                                Box(
                                    modifier = Modifier
                                        .width(columnWidth)
                                        .padding(horizontal = 2.dp)
                                ) {
                                    Text(
                                        text = value,
                                        fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                if (i < values.size - 1) {
                                    Text(
                                        "|",
                                        fontSize = 12.sp,
                                        modifier = Modifier
                                            .width(2.dp)
                                            .padding(vertical = 8.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        if (isHeader) {
                            Spacer(
                                Modifier
                                    .height(2.dp)
                                    .fillMaxWidth()
                                    .background(Color.LightGray)
                            )
                        }
                    }
                }
            }
        }
    }
}