package com.velox.jewelvault.ui.screen.order_and_purchase

import android.annotation.SuppressLint
import android.widget.Toast
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velox.jewelvault.data.roomdb.dto.PurchaseOrderWithDetails
import com.velox.jewelvault.data.roomdb.entity.order.OrderEntity
import com.velox.jewelvault.ui.nav.SubScreens
import com.velox.jewelvault.utils.LocalSubNavController
import com.velox.jewelvault.utils.SortOrder
import com.velox.jewelvault.utils.to2FString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

@Composable
fun OrderAndPurchaseScreen(viewModel: OrderAndReportViewModel) {
    val tabs = listOf("Order", "Purchase")
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    Column(  modifier = Modifier.fillMaxSize().background(
        MaterialTheme.colorScheme.surface,
        RoundedCornerShape(topStart = 18.dp)
    ).padding(5.dp)) {
        TabRow( modifier = Modifier,selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) }
                )
            }
        }

        when (selectedTabIndex) {
            0 -> OrderDetail(viewModel)
            1 -> PurchaseDetails(viewModel)
        }
    }
}


@Composable
@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalFoundationApi::class)
private fun PurchaseDetails(viewModel: OrderAndReportViewModel) {
    val context = LocalContext.current
    LaunchedEffect(true) {
        CoroutineScope(Dispatchers.IO).async {
            viewModel.getAllPurchaseSorted(SortOrder.DESCENDING)
        }.await()
    }

    Column {
        val scrollState = rememberScrollState()

        val subNavController = LocalSubNavController.current

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val columnWidth = 150.dp
            val itemsWithHeader = listOf<PurchaseOrderWithDetails?>(null) + viewModel.purchase

            Column(modifier = Modifier.horizontalScroll(scrollState)) {
                LazyColumn {
                    itemsIndexed(itemsWithHeader) { index, item ->
                        val isHeader = index == 0
                        val values = if (isHeader) {
                            viewModel.purchaseHeaderList
                        } else {
                            if (item != null) {
                                //"S.No", "Order Id","Bill Date","Firm Name","Seller Name No", "Bill No", "Item Details","GST","Exchange Details"

                                val itemDetails = item.items.joinToString("\n") {
                                    "${it.catName}-${it.subCatName} ${it.gsWt}-${it.purity} ${it.fnWt}gm (${it.wastagePercent}) rate: ₹${it.fnRate}"
                                }

                                val r = "Items: ${item.items.size} Gst: ${item.order.cgstPercent+item.order.sgstPercent+item.order.igstPercent} %" +
                                        "\n$itemDetails" +
                                        "\n----------------------------------------------"

                                listOf(
                                    "$index",
                                    "${item.order.purchaseOrderId}",
                                    "${item.order.billDate}",
                                    "${item.order.billNo} ",  //firm name
                                    "${item.seller?.name} (${item.seller?.mobileNumber})",
                                    "${item.order.billNo} ",
                                    r,
                                    "Gold: ${item.exchanges.filter { it.catName == "Gold" }.sumOf { it.fnWeight }}0 gm" +
                                            "\nSilver: ${item.exchanges.filter { it.catName == "Silver" }.sumOf { it.fnWeight }}0 gm ",
                                )
                            } else {
                                emptyList()
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(onClick = {
                                    if (!isHeader && item != null) {
                                        subNavController.navigate("${SubScreens.PurchaseItemDetail.route}/${item.order.purchaseOrderId}")
                                    }
                                }, onLongClick = {
                                    if (!isHeader && item != null) {
                                        Toast.makeText(context, "Not yet implemented", Toast.LENGTH_SHORT).show()
                                    }
                                })
                        ) {
                            values.forEachIndexed { i, value ->
                                Box(
                                    modifier = Modifier
                                        .width(if (i == 6) 350.dp else columnWidth)
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
                                        "|\n|",
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


@Composable
@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalFoundationApi::class)
private fun OrderDetail(viewModel: OrderAndReportViewModel) {
    LaunchedEffect(true) {
        CoroutineScope(Dispatchers.IO).async {
            viewModel.getAllOrdersSorted(SortOrder.DESCENDING)
        }.await()
    }

    Column{
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
                                    if (!isHeader && item != null) {

                                    }
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