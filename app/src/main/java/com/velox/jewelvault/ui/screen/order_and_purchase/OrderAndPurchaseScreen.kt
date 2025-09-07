package com.velox.jewelvault.ui.screen.order_and_purchase

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.velox.jewelvault.ui.components.TextListView
import com.velox.jewelvault.ui.nav.SubScreens
import com.velox.jewelvault.utils.CalculationUtils
import com.velox.jewelvault.utils.LocalBaseViewModel
import com.velox.jewelvault.utils.LocalSubNavController
import com.velox.jewelvault.utils.SortOrder
import com.velox.jewelvault.utils.to2FString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

@Composable
fun OrderAndPurchaseScreen(viewModel: OrderAndReportViewModel) {
    viewModel.currentScreenHeadingState.value = "Order & Purchase"
    val subNavController = LocalSubNavController.current

    BackHandler {
        subNavController.navigate(SubScreens.Dashboard.route) {
            popUpTo(SubScreens.Dashboard.route) {
                inclusive = true
            }
        }
    }

    val tabs = listOf("Order", "Purchase")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                MaterialTheme.colorScheme.surface, RoundedCornerShape(topStart = 18.dp)
            )
            .padding(5.dp)
    ) {
        TabRow(modifier = Modifier, selectedTabIndex = viewModel.selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = viewModel.selectedTabIndex == index,
                    onClick = { viewModel.selectedTabIndex = index },
                    text = { Text(title) })
            }
        }

        when (viewModel.selectedTabIndex) {
            0 -> OrderDetail(viewModel)
            1 -> PurchaseDetails(viewModel)
        }
    }
}


@Composable
private fun PurchaseDetails(viewModel: OrderAndReportViewModel) {
    val baseViewModel = LocalBaseViewModel.current
    val subNavController = LocalSubNavController.current

    LaunchedEffect(true) {
        CoroutineScope(Dispatchers.IO).async {
            viewModel.getAllPurchaseSorted(SortOrder.DESCENDING)
        }.await()
    }

    val purchaseItems = viewModel.purchase.mapIndexed { index, item ->
        val itemDetails = item.items.joinToString("\n") {
            "${it.catName}-${it.subCatName} ${it.gsWt}-${it.purity} ${it.fnWt}gm (${it.wastagePercent}) rate: ₹${it.fnRate}"
        }

        val r =
            "Items: ${item.items.size} Gst: ${item.order.cgstPercent + item.order.sgstPercent + item.order.igstPercent} %" + "\n$itemDetails"

        listOf(
            "${index + 1}",
            "${item.order.purchaseOrderId}",
            "${item.order.billDate}",
            "${item.order.billNo} ",  //firm name
            "${item.seller?.name} \n(${item.seller?.mobileNumber})",
            "${item.order.billNo} ",
            r,
            "Gold: ${
                CalculationUtils.calculateTotalExchangeWeightByMetal(
                    item.exchanges,
                    "Gold"
                )
            }0 gm" + "\nSilver: ${
                CalculationUtils.calculateTotalExchangeWeightByMetal(
                    item.exchanges,
                    "Silver"
                )
            }0 gm ",
        )
    }

    TextListView(
        headerList = viewModel.purchaseHeaderList,
        items = purchaseItems,
        modifier = Modifier.fillMaxSize(),
        maxColumnWidth = 450.dp, // Larger width for detailed purchase information
        onItemClick = { item ->
            // Get the purchase order ID from the second column (index 1)
            val purchaseOrderId = item[1]
            subNavController.navigate("${SubScreens.PurchaseItemDetail.route}/$purchaseOrderId")
        },
        onItemLongClick = { item ->
            baseViewModel.snackBarState = "Not yet implemented"
        })
}


@Composable
private fun OrderDetail(viewModel: OrderAndReportViewModel) {
    val subNavController = LocalSubNavController.current

    LaunchedEffect(true) {
        CoroutineScope(Dispatchers.IO).async {
            viewModel.getAllOrdersSorted(SortOrder.DESCENDING)
        }.await()
    }

    val orderItems = viewModel.orderList.mapIndexed { index, item ->
        //"S.No", "Order Id","Order Date","Customer Name","Customer No", "Total Amount", "Total Charge"
        listOf(
            "${index + 1}",
            "${item.orderId}",
            "${item.orderDate}",
            "${item.customerMobile} ",
            "${item.customerMobile}",
            "₹${item.totalAmount.to2FString()} ",
            "₹${item.totalCharge.to2FString()} ",
            "₹${item.totalTax.to2FString()} ",
            "₹${
                CalculationUtils.totalPrice(item.totalAmount, item.totalCharge, 0.0, item.totalTax)
                    .to2FString()
            }"
        )
    }

    TextListView(
        headerList = viewModel.orderHeaderList,
        items = orderItems,
        modifier = Modifier.fillMaxSize(),
        maxColumnWidth = 200.dp, // Standard width for order information
        onItemClick = { item ->
            // Get the order ID from the second column (index 1)
            val orderId = item[1]
            subNavController.navigate("${SubScreens.OrderItemDetail.route}/$orderId")
        },
        onItemLongClick = { item ->
            // Long click functionality can be added here if needed
        })
}