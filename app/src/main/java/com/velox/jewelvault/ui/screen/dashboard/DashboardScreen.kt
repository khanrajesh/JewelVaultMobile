package com.velox.jewelvault.ui.screen.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.velox.jewelvault.data.roomdb.dao.IndividualSellItem
import com.velox.jewelvault.data.roomdb.dao.TopItemByCategory
import com.velox.jewelvault.ui.components.bounceClick
import com.velox.jewelvault.ui.nav.Screens
import com.velox.jewelvault.ui.nav.SubScreens
import com.velox.jewelvault.utils.DataStoreManager
import com.velox.jewelvault.utils.LocalBaseViewModel
import com.velox.jewelvault.utils.LocalNavController
import com.velox.jewelvault.utils.LocalSubNavController
import com.velox.jewelvault.utils.VaultPreview
import com.velox.jewelvault.utils.isLandscape
import com.velox.jewelvault.utils.mainScope
import com.velox.jewelvault.utils.to2FString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


@VaultPreview
@Composable
fun DashboardScreenPreview() {
    DashboardScreen(hiltViewModel<DashboardViewModel>())
}

@Composable
fun DashboardScreen(dashboardViewModel: DashboardViewModel) {
    if ( isLandscape()) LandscapeMainScreen(dashboardViewModel) else PortraitMainScreen()
}

@Composable
fun LandscapeMainScreen(dashboardViewModel: DashboardViewModel) {
    val navHost = LocalNavController.current
    val baseViewModel = LocalBaseViewModel.current
    val subNavController = LocalSubNavController.current
    val context = LocalContext.current
    LaunchedEffect(true) {
        //refreshing the metal rate here
        CoroutineScope(Dispatchers.IO).async {
            if (baseViewModel.metalRates.isEmpty()){
                baseViewModel.refreshMetalRates(context = context)
            }

            dashboardViewModel.getRecentSellItem()
            dashboardViewModel.getSalesSummary()
            dashboardViewModel.getTopSellingItems()
            dashboardViewModel.getTopSellingSubCategories()
            //checking if the user setup the store or not
            delay(2000)
            val storeId = baseViewModel.dataStoreManager.getValue(DataStoreManager.STORE_ID_KEY).first()
            if (storeId == null) {
                baseViewModel.snackMessage = "Please Set Up Your Store First."
                delay(2000)
                mainScope {
                    subNavController.navigate(SubScreens.Profile.route)
                }
            }
        }.await()
    }



    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(topStart = 18.dp))
            .padding(5.dp)
    ) {

        Column(
            Modifier
                .fillMaxSize()
        ) {
            //cash flow over view
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                //flow over view
                FlowOverView(dashboardViewModel)
                Spacer(Modifier.width(5.dp))
                TopFiveSales(Modifier.weight(1f),dashboardViewModel)
//                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(5.dp))
                CategorySales(dashboardViewModel)
                Spacer(Modifier.width(5.dp))

                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(190.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(5.dp), verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .bounceClick {
                                navHost.navigate(Screens.SellInvoice.route)
                            }
                            .weight(2f)
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Create Invoice", textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(5.dp))

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                    ) {

                        Box(
                            modifier = Modifier
                                .bounceClick {
                                    navHost.navigate(Screens.QrScanScreen.route)
                                }
                                .weight(1f)
                                .fillMaxSize()
                                .background(
                                    MaterialTheme.colorScheme.surface,
                                    RoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Cam", textAlign = TextAlign.Center)
                        }
                        Spacer(Modifier.width(5.dp))
                        Box(
                            modifier = Modifier
                                .bounceClick {
                                    dashboardViewModel.getSubCategoryCount{
                                        if (it>2){
                                            mainScope {
                                                navHost.navigate(Screens.Purchase.route)
                                            }
                                        }else{
                                            baseViewModel.snackMessage = "Please add more sub categories."
                                        }
                                    }

                                }
                                .weight(1f)
                                .fillMaxSize()
                                .background(
                                    MaterialTheme.colorScheme.surface,
                                    RoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("P.", textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(5.dp))
            //Recent Item Sold
            RecentItemSold(dashboardViewModel.recentSellsItem)
        }
    }
}

@Composable
fun PortraitMainScreen() {

}

@Composable
fun CategorySales(dashboardViewModel: DashboardViewModel) {
    Column(
        Modifier
            .fillMaxHeight()
            .width(200.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Category Overview", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("Weekly", fontSize = 9.sp, color = Color.Gray)
        }

        LazyColumn(
            Modifier
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
        ) {

            items(dashboardViewModel.topSubCategories){
                Row(
                    Modifier
                        .padding(3.dp)
                        .fillMaxWidth()
                        .height(20.dp)
                ) {
                    Text(
                        "${it.subCatName} ",
                        fontSize = 10.sp,
                        modifier = Modifier
                            .weight(3f)
                            .height(20.dp)
                    )
                    Text(
                        "₹${it.totalPrice.to2FString()?:""}, ", fontSize = 10.sp, modifier = Modifier
                            .weight(1f)
                            .height(20.dp)
                    )
                    Text(
                        "${it.totalFnWt.to2FString()}g", fontSize = 10.sp, modifier = Modifier
                            .weight(1f)
                            .height(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TopFiveSales(modifier: Modifier, dashboardViewModel: DashboardViewModel) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Top 5 Selling Items", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("Weekly", fontSize = 9.sp, color = Color.Gray)
        }

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                .padding(8.dp)
        ) {
            dashboardViewModel.topSellingItemsMap.entries.forEachIndexed { index, entry ->
                item {
                    ItemViewItem(category = entry.key, items = entry.value)
                    if (index < dashboardViewModel.topSellingItemsMap.size - 1) {
                        Spacer(
                            Modifier
                                .padding(horizontal = 8.dp)
                                .fillMaxHeight()
                                .width(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun ItemViewItem(
    category: String,
    items: List<TopItemByCategory>
) {
    Column(modifier = Modifier.width(120.dp)) {
        items.take(5).forEach { item ->
            Row(
                modifier = Modifier.height(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    item.subCatName.take(10),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    item.totalFnWt.toString(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        Text(
            category,
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            fontWeight = FontWeight.SemiBold
        )
    }
}


@Composable
fun FlowOverView(dashboardViewModel: DashboardViewModel) {
    Column(
        Modifier
            .fillMaxHeight()
            .width(300.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(5.dp)
    ) {
        Row {
            Text("Flow Overview", fontSize = 10.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.weight(1f))
            Text("Weekly", fontSize = 10.sp, color = Color.Gray)
        }

        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                .padding(8.dp)
        ) {
            Spacer(Modifier.weight(1f))
            Column {
                Text("₹${(dashboardViewModel.salesSummary.value?.totalAmount?:0.0).to2FString()}", fontSize = 18.sp, fontWeight = FontWeight.Black)
                Text("Total Sales", fontSize = 10.sp, color = Color.Gray)
            }
            Spacer(Modifier.weight(1f))
            Column {
                Text("${dashboardViewModel.salesSummary.value?.invoiceCount?:0} ", fontSize = 18.sp, fontWeight = FontWeight.Black)
                Text("Total Invoice", fontSize = 10.sp, color = Color.Gray)
            }
        }

    }
}

@Composable
fun RecentItemSold(recentSellsItem: SnapshotStateList<IndividualSellItem>) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(5.dp)
    ) {
        Row {
            Text("Recent Sells")
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.ArrowDropDown, null)
        }
        Spacer(Modifier.height(5.dp))

        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                .padding(5.dp)
        ) {

            Column(
                Modifier
                    .padding(2.dp)
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                Row(Modifier.fillMaxWidth()) {
                    Text("Date", fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                    Text(
                        "Customer Name",
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.weight(2f)
                    )
                    Text("Mobile No", fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                    Text("Item", fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                    Text("Weight", fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                    Text(
                        "Total Price",
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.weight(1f)
                    )
                    Text("Tax", fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                    Text("Category", fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                    Text(
                        "Sub Category",
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
                }
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outline)
                )
            }
            LazyColumn(
                Modifier
                    .weight(1f)
            ) {

                items(recentSellsItem){
                    Column(
                        Modifier
                            .padding(2.dp)
                            .fillMaxWidth()
                            .wrapContentHeight()
                    ) {
                        Row(Modifier.fillMaxWidth()) {
                            Text(
                                "${it.orderDate}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.DarkGray,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "${it.name} ",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.DarkGray,
                                modifier = Modifier.weight(2f)
                            )
                            Text(
                                "${it.mobileNo} ",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.DarkGray,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "${it.itemAddName} ",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.DarkGray,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "${it.gsWt.to2FString()} ",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.DarkGray,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "${(it.price+it.charge+it.tax).to2FString()} ",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.DarkGray,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "${it.tax.to2FString()} ",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.DarkGray,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "${it.catName} (${it.catId})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.DarkGray,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "${it.subCatName} (${it.subCatId})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.DarkGray,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
                        }
                        Spacer(
                            Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.outline)
                        )
                    }

                }
            }
        }


    }
}

