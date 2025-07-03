package com.velox.jewelvault.ui.screen.order_and_purchase.purchase_item

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.velox.jewelvault.data.roomdb.entity.ItemEntity
import com.velox.jewelvault.data.roomdb.entity.order.OrderEntity
import com.velox.jewelvault.ui.components.TextListView
import com.velox.jewelvault.utils.VaultPreview
import com.velox.jewelvault.utils.export.enqueueExportWorker
import com.velox.jewelvault.utils.mainScope
import com.velox.jewelvault.utils.to2FString

@Composable
@VaultPreview
fun PreviewPurchaseItemDetailScreen() {
    PurchaseItemDetailScreen(hiltViewModel(), "1")
}

@Composable
fun PurchaseItemDetailScreen(viewModel: PurchaseItemViewModel, purchaseOrderId: String) {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(true) {
        viewModel.getPurchaseOrderById(purchaseOrderId)
        viewModel.getInventoryItemByPurchaseOrderID(purchaseOrderId)
        viewModel.getOrderItemsByPurchaseOrderId(purchaseOrderId)
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        //region Purchase Order Summery
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp)
                )
                .padding(10.dp)
        ) {
            viewModel.firmEntity.value?.let { firm ->

                viewModel.purchaseOrderWithDetails.value?.let { pur ->
                    pur.seller?.let { sel ->
                        Text(
                            text = "${firm.firmName} (${firm.firmMobileNumber}), Seller: ${sel.name} (${sel.mobileNumber})",
                            fontWeight = FontWeight.Bold
                        )
                        Text(text = "   ID: ${firm.firmId}, GST: ${firm.gstNumber}, Address: ${firm.address}")
                    }
                    Spacer(Modifier.height(3.dp))
                    Text("  ${pur.order}")
                    Spacer(Modifier.height(3.dp))

                    Text("  Items :")
                    pur.items.forEachIndexed { index, it ->
                        Text("      ${index + 1}. ID: ${it.purchaseItemId}, ${it.catName}(${it.catId}) - ${it.subCatName}(${it.subCatId}), Gs.Wt: ${it.gsWt}, Net Wt: ${it.ntWt}, Fn.Wt: ${it.fnWt}, Purity: ${it.purity}, Wastage: ${it.wastagePercent}, Pur Fn Rate: ${it.fnRate}")
                    }
                    Spacer(Modifier.height(3.dp))
                    Text("  Exchange :")
                    pur.exchanges.forEachIndexed { index, it ->
                        Text("      ${index + 1}. ${it.catName} - Fn.Wt: ${it.fnWeight.to2FString()} gm")
                    }
                }
            }
        }
        Spacer(Modifier.height(5.dp))
        //endregion
        val state = remember { mutableStateOf(true) }
        //region Items
        Column(
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp)
                )
                .fillMaxWidth()
                .weight(if (state.value) 1f else 0.1f)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(5.dp,)
            ) {
                Text("Items in category", modifier = Modifier.padding(start = 5.dp))
                Spacer(Modifier.weight(1f))
                Text("Export", modifier = Modifier.clickable {
                    mainScope {
                        if(viewModel.itemList.isNotEmpty()){
                            val billNo = viewModel.purchaseOrderWithDetails.value?.order?.billNo ?: "Bill_No"
                            val fileName = "ItemExport_${billNo}_${System.currentTimeMillis()}.xlsx"
                            enqueueExportWorker(context,lifecycleOwner, fileName, viewModel.itemHeaderList, viewModel.itemList.toList())
                        }else{
                            viewModel.snackBar.value = "No Item Found."
                        }
                    }
                }.padding(start = 5.dp))
                Spacer(Modifier.width(30.dp))
                Icon(imageVector = if (state.value) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.clickable {
                        state.value = !state.value
                    }.width(200.dp))
            }

            TextListView(headerList = viewModel.itemHeaderList,
                items = viewModel.itemList,
                onItemClick = {},
                onItemLongClick = {})

        }
        Spacer(Modifier.height(5.dp))
        //endregion

        //region Sell Order Items
        Column(
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(topStart = 18.dp)
                )
                .fillMaxWidth()
                .weight(if (state.value) 0.1f else 1f)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(5.dp)
            ) {
                Text("Sold Items")
                Spacer(Modifier.weight(1f))
                Icon(imageVector = if (state.value) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                    contentDescription = null,
                    modifier = Modifier.clickable {
                        state.value = !state.value
                    })
            }


            TextListView(viewModel.orderHeaderList,
                viewModel.orderList,
                onItemClick = {},
                onItemLongClick = {})
        }


        //endregion
    }
}



