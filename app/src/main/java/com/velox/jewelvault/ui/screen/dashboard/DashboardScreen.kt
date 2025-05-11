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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velox.jewelvault.ui.components.bounceClick
import com.velox.jewelvault.ui.nav.Screens
import com.velox.jewelvault.ui.nav.SubScreens
import com.velox.jewelvault.utils.DataStoreManager
import com.velox.jewelvault.utils.LocalBaseViewModel
import com.velox.jewelvault.utils.LocalNavController
import com.velox.jewelvault.utils.LocalSubNavController
import com.velox.jewelvault.utils.VaultPreview
import com.velox.jewelvault.utils.isLandscape
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first


@VaultPreview
@Composable
fun DashboardScreenPreview() {
    DashboardScreen()
}

@Composable
fun DashboardScreen() {
    if ( isLandscape()) LandscapeMainScreen() else PortraitMainScreen()
}

@Composable
fun LandscapeMainScreen() {
    val navHost = LocalNavController.current
    val dashboardViewModel = LocalBaseViewModel.current
    val subNavController = LocalSubNavController.current

    LaunchedEffect(true) {
        delay(5000)
        val storeId = dashboardViewModel.dataStoreManager.getValue(DataStoreManager.STORE_ID_KEY).first()
        if (storeId == null) {
            dashboardViewModel.snackMessage = "Please Set Up Your Store First."
            delay(2000)
            subNavController.navigate(SubScreens.Profile.route)
        }
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
                FlowOverView()
                Spacer(Modifier.width(5.dp))
                TopFiveSales(Modifier.weight(1f))
//                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(5.dp))
                CategorySales()
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
                        Text("Create Invoice", textAlign = TextAlign.Center)
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

                                }
                                .weight(1f)
                                .fillMaxSize()
                                .background(
                                    MaterialTheme.colorScheme.surface,
                                    RoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Buy", textAlign = TextAlign.Center)
                        }
                    }
                }
            }

            Spacer(Modifier.height(5.dp))
            //Recent Item Sold
            RecentItemSold()
        }
    }
}

@Composable
fun PortraitMainScreen() {

}

@Composable
fun CategorySales() {
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
            items(15) {
                Row(
                    Modifier
                        .padding(3.dp)
                        .fillMaxWidth()
                        .height(20.dp)
                ) {
                    Text(
                        "Category Nmae",
                        fontSize = 10.sp,
                        modifier = Modifier
                            .weight(3f)
                            .height(20.dp)
                    )
                    Text(
                        "1200", fontSize = 10.sp, modifier = Modifier
                            .weight(1f)
                            .height(20.dp)
                    )
                    Text(
                        "700g", fontSize = 10.sp, modifier = Modifier
                            .weight(1f)
                            .height(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TopFiveSales(modifier: Modifier) {
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
            items(5) { it ->
                ItemViewItem()
                if (it < 4) {
                    Spacer(
                        Modifier
                            .padding(5.dp)
                            .fillMaxHeight()
                            .width(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                }
            }
        }
    }
}


@Composable
fun ItemViewItem() {
    Column(modifier = Modifier.width(100.dp)) {
        repeat(5) { index ->
            Row(
                modifier = Modifier.height(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Item ${index + 1}", fontSize = 10.sp, fontWeight = FontWeight.Normal)
                Spacer(modifier = Modifier.weight(1f))
                Text("${(10..50).random()}", fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }
        }
        Text(
            "Gold",
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp)
        )
    }
}

@Composable
fun FlowOverView() {
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
                Text("$ 100000", fontSize = 18.sp, fontWeight = FontWeight.Black)
                Text("Total Sales", fontSize = 10.sp, color = Color.Gray)
            }
            Spacer(Modifier.weight(1f))
            Column {
                Text("5", fontSize = 18.sp, fontWeight = FontWeight.Black)
                Text("Total Invoice", fontSize = 10.sp, color = Color.Gray)
            }
        }

    }
}

@Composable
fun RecentItemSold() {
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

                items(15) {
                    Column(
                        Modifier
                            .padding(2.dp)
                            .fillMaxWidth()
                            .wrapContentHeight()
                    ) {
                        Row(Modifier.fillMaxWidth()) {
                            Text(
                                "01-May-2025",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.DarkGray,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "Rakesh Khan",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.DarkGray,
                                modifier = Modifier.weight(2f)
                            )
                            Text(
                                "1234567890",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.DarkGray,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "Box Chain",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.DarkGray,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "2.5g",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.DarkGray,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "10000",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.DarkGray,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "100",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.DarkGray,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "Gold",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.DarkGray,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "Chain",
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

