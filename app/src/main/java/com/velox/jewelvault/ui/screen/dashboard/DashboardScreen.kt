package com.velox.jewelvault.ui.screen.dashboard

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.TrendingUp
import androidx.compose.material.icons.twotone.Analytics
import androidx.compose.material.icons.twotone.Category
import androidx.compose.material.icons.twotone.CloudOff
import androidx.compose.material.icons.twotone.Pentagon
import androidx.compose.material.icons.twotone.People
import androidx.compose.material.icons.twotone.Refresh
import androidx.compose.material.icons.twotone.Scale
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.PullRefreshState
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.velox.jewelvault.data.MetalRate
import com.velox.jewelvault.data.roomdb.dao.IndividualSellItem
import com.velox.jewelvault.data.roomdb.dao.TimeRange
import com.velox.jewelvault.data.roomdb.dao.TopItemByCategory
import com.velox.jewelvault.ui.components.OptionalUpdateDialog
import com.velox.jewelvault.ui.components.PermissionRequester
import com.velox.jewelvault.ui.components.TextListView
import com.velox.jewelvault.ui.components.bounceClick
import com.velox.jewelvault.ui.nav.Screens
import com.velox.jewelvault.ui.nav.SubScreens
import com.velox.jewelvault.utils.CalculationUtils
import com.velox.jewelvault.utils.LocalBaseViewModel
import com.velox.jewelvault.utils.LocalNavController
import com.velox.jewelvault.utils.LocalSubNavController
import com.velox.jewelvault.utils.VaultPreview
import com.velox.jewelvault.utils.ioScope
import com.velox.jewelvault.utils.isLandscape
import com.velox.jewelvault.utils.mainScope
import com.velox.jewelvault.utils.to1FString
import com.velox.jewelvault.utils.to3FString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@VaultPreview
@Composable
fun DashboardScreenPreview() {
    DashboardScreen(hiltViewModel<DashboardViewModel>())
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DashboardScreen(dashboardViewModel: DashboardViewModel) {
    dashboardViewModel.currentScreenHeadingState.value = "Dashboard"

    val navHost = LocalNavController.current
    val baseViewModel = LocalBaseViewModel.current
    val subNavController = LocalSubNavController.current
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val isRefreshing = remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing.value, onRefresh = {
            ioScope {
                isRefreshing.value = true
                dashboardViewModel.snackBarState.value = "Refreshing the values"
                dashboardViewModel.getRecentSellItem()
                dashboardViewModel.getSalesSummary()
                dashboardViewModel.getTopSellingItems()
                dashboardViewModel.getTopSellingSubCategories()
                dashboardViewModel.getCustomerSummary()
                isRefreshing.value = false
            }
        })

    // Double back press to exit state
    var backPressCount by remember { mutableStateOf(0) }

    // Check for updates on dashboard load
    LaunchedEffect(Unit) {
        baseViewModel.checkForUpdates(context)
        // Ensure keyboard is hidden on entering dashboard
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }

    // Dialog state for time range selection
    val showTimeRangeDialog = remember { mutableStateOf(false) }

    // Handle back press
    BackHandler {
        when (backPressCount) {
            0 -> {
                backPressCount = 1
                baseViewModel.snackBarState = "Press back again to exit"
            }

            1 -> {
                // Exit the application
                android.os.Process.killProcess(android.os.Process.myPid())
            }
        }
    }

    // Reset back press count when navigating away
    DisposableEffect(navHost) {
        onDispose {
            backPressCount = 0
        }
    }

    val isFirstLogin = remember { mutableStateOf(false) }

    LaunchedEffect(true) {
        //refreshing the metal rate here
        withContext(Dispatchers.IO) {
            val storeId = baseViewModel.dataStoreManager.getSelectedStoreInfo().first.first()
            if (storeId.isBlank()) {
                isFirstLogin.value = true
                baseViewModel.snackBarState = "Please Set Up Your Store First."
                mainScope {
                    subNavController.navigate("${SubScreens.Profile.route}/${true}")
                }
                return@withContext
            }

            if (baseViewModel.metalRates.isEmpty()) {
                baseViewModel.refreshMetalRates(context = context)
            }

            dashboardViewModel.getRecentSellItem()
            dashboardViewModel.getSalesSummary()
            dashboardViewModel.getTopSellingItems()
            dashboardViewModel.getTopSellingSubCategories()
            dashboardViewModel.getCustomerSummary()
            //checking if the user setup the store or not

        }
        focusManager.clearFocus(force = true)
        keyboardController?.hide()

    }

    if (!isFirstLogin.value) {
        PermissionRequester(
            permissions = listOf(
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.POST_NOTIFICATIONS
            )
        ) {}
    }


    Box(Modifier.fillMaxSize()) {
        if (isLandscape()) LandscapeDashboardScreen(
            dashboardViewModel, pullRefreshState, showTimeRangeDialog
        )
        else PortraitDashboardScreen(
            dashboardViewModel, pullRefreshState, showTimeRangeDialog
        )


        // Pull to refresh indicator
        PullRefreshIndicator(
            refreshing = isRefreshing.value,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Time Range Selection Dialog
        if (showTimeRangeDialog.value) {
            TimeRangeSelectionDialog(
                currentRange = dashboardViewModel.selectedRange.value,
                onRangeSelected = { newRange ->
                    dashboardViewModel.updateTimeRange(newRange)
                    showTimeRangeDialog.value = false
                },
                onDismiss = {
                    showTimeRangeDialog.value = false
                })
        }

        // Show update dialogs if needed
        if (baseViewModel.showUpdateDialog.value) {
            baseViewModel.updateInfo.value?.let { updateInfo ->
                OptionalUpdateDialog(
                    updateInfo = updateInfo,
                    onUpdateClick = { baseViewModel.onUpdateClick(context) },
                    onDismiss = { baseViewModel.dismissUpdateDialog() },
                    onBackupClick = { baseViewModel.onUpdateClick(context) })
            }
        }

        if (baseViewModel.showForceUpdateDialog.value) {
            baseViewModel.updateInfo.value?.let { updateInfo ->
                com.velox.jewelvault.ui.components.ForceUpdateDialog(
                    updateInfo = updateInfo,
                    onUpdateClick = { baseViewModel.onUpdateClick(context) })
            }
        }

    }


}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun LandscapeDashboardScreen(
    dashboardViewModel: DashboardViewModel,
    pullRefreshState: PullRefreshState,
    showTimeRangeDialog: MutableState<Boolean>,
) {

    val navHost = LocalNavController.current
    val baseViewModel = LocalBaseViewModel.current
    LocalSubNavController.current
    LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    LocalFocusManager.current

    Box(
        Modifier
            .pullRefresh(pullRefreshState)
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(topStart = 18.dp))
            .padding(5.dp)
    ) {
        keyboardController?.hide()
        Column(
            Modifier.fillMaxSize()
        ) {
            //cash flow over view
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                FlowOverView(Modifier
                    .fillMaxHeight()
                    .width(300.dp),dashboardViewModel) { showDialog ->
                    showTimeRangeDialog.value = showDialog
                }
                Spacer(Modifier.width(5.dp))
                TopFiveSales(Modifier.weight(1f), dashboardViewModel) { showDialog ->
                    showTimeRangeDialog.value = showDialog
                }
                Spacer(Modifier.width(5.dp))
                CategorySales( Modifier
                    .fillMaxHeight()
                    .width(200.dp), dashboardViewModel) { showDialog ->
                    showTimeRangeDialog.value = showDialog
                }
                Spacer(Modifier.width(5.dp))
                CustomerOverview(Modifier
                    .fillMaxHeight()
                    .width(200.dp),
                    dashboardViewModel)
                Spacer(Modifier.width(5.dp))

                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(190.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)
                        )
                        .padding(5.dp), verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier

                            .weight(2f)
                            .fillMaxWidth(), contentAlignment = Alignment.Center
                    ) {

                        Box(modifier = Modifier
                            .bounceClick {
                                navHost.navigate(Screens.SellInvoice.route)
                            }
                            .fillMaxSize()
                            .background(
                                MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)
                            ), contentAlignment = Alignment.Center) {
                            Text(
                                "Create Invoice",
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.surface,

                                )
                        }

                        Icon(
                            imageVector = Icons.TwoTone.Pentagon,
                            contentDescription = null,
                            modifier = Modifier
                                .bounceClick {
                                    navHost.navigate(Screens.DraftInvoice.route)
                                }
                                .align(Alignment.TopStart)
                                .padding(2.dp)
                                .size(30.dp)
                                .background(Color.White, RoundedCornerShape(8.dp))
                                .padding(7.dp))


                    }
                    Spacer(Modifier.height(5.dp))

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                    ) {
                        Box(modifier = Modifier
                            .bounceClick {
                                navHost.navigate(Screens.QrScanScreen.route)
                            }
                            .weight(1f)
                            .fillMaxSize()
                            .background(
                                MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp)
                            ), contentAlignment = Alignment.Center) {
                            Text("Cam", textAlign = TextAlign.Center)
                        }
                        Spacer(Modifier.width(5.dp))
                        Box(modifier = Modifier
                            .bounceClick {
                                dashboardViewModel.getSubCategoryCount {
                                    if (it > 2) {
                                        mainScope {
                                            navHost.navigate(Screens.Purchase.route)
                                        }
                                    } else {
                                        baseViewModel.snackBarState =
                                            "Please add more sub categories."
                                    }
                                }

                            }
                            .weight(1f)
                            .fillMaxSize()
                            .background(
                                MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp)
                            ), contentAlignment = Alignment.Center) {
                            Text("P.Bill", textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(5.dp))
            //Recent Item Sold
            RecentItemSold(Modifier, dashboardViewModel.recentSellsItem)
        }


    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun PortraitDashboardScreen(
    dashboardViewModel: DashboardViewModel,
    pullRefreshState: PullRefreshState,
    showTimeRangeDialog: MutableState<Boolean>,
) {

    val navHost = LocalNavController.current
    val baseViewModel = LocalBaseViewModel.current
    LocalSubNavController.current
    LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    LocalFocusManager.current

    Box(
        Modifier
            .pullRefresh(pullRefreshState)
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(topStart = 18.dp))
            .padding(5.dp)
    ) {
        keyboardController?.hide()
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            //cash flow over view\

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)
                    )
                    .padding(5.dp), verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {

                    Box(modifier = Modifier
                        .bounceClick {
                            navHost.navigate(Screens.SellInvoice.route)
                        }
                        .fillMaxSize()
                        .background(
                            MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)
                        ), contentAlignment = Alignment.Center) {
                        Text(
                            "Create Invoice",
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.surface,

                            )
                    }

                    Icon(
                        imageVector = Icons.TwoTone.Pentagon,
                        contentDescription = null,
                        modifier = Modifier
                            .bounceClick {
                                navHost.navigate(Screens.DraftInvoice.route)
                            }
                            .align(Alignment.TopStart)
                            .padding(2.dp)
                            .fillMaxHeight()
                            .size(30.dp)
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .padding(7.dp),
                        )


                }
                Spacer(Modifier.height(5.dp))

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                ) {
                    Box(modifier = Modifier
                        .bounceClick {
                            navHost.navigate(Screens.QrScanScreen.route)
                        }
                        .weight(1f)
                        .fillMaxSize()
                        .background(
                            MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp)
                        ), contentAlignment = Alignment.Center) {
                        Text("Cam", textAlign = TextAlign.Center)
                    }
                    Spacer(Modifier.width(5.dp))
                    Box(modifier = Modifier
                        .bounceClick {
                            dashboardViewModel.getSubCategoryCount {
                                if (it > 3) {
                                    mainScope {
                                        navHost.navigate(Screens.Purchase.route)
                                    }
                                } else {
                                    baseViewModel.snackBarState = "Please add more sub categories."
                                }
                            }

                        }
                        .weight(1f)
                        .fillMaxSize()
                        .background(
                            MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp)
                        ), contentAlignment = Alignment.Center) {
                        Text("P.Bill", textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                    }
                }
            }



            Spacer(Modifier.height(5.dp))
            MetalRateView(Modifier.fillMaxWidth())
            Spacer(Modifier.height(5.dp))
            Row(Modifier.fillMaxWidth().height(150.dp), horizontalArrangement = Arrangement.Center) {
                FlowOverView(Modifier
                    .fillMaxSize().weight(1f),dashboardViewModel) { showDialog ->
                    showTimeRangeDialog.value = showDialog
                }
                Spacer(Modifier.width(5.dp))
                CustomerOverview(Modifier
                    .fillMaxSize().weight(1f), dashboardViewModel)
            }
            Spacer(Modifier.height(5.dp))
            TopFiveSales(Modifier.fillMaxWidth().defaultMinSize(minHeight = 120.dp), dashboardViewModel) { showDialog ->
                showTimeRangeDialog.value = showDialog
            }
            Spacer(Modifier.height(5.dp))
            CategorySales( Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 120.dp),
                dashboardViewModel) { showDialog ->
                showTimeRangeDialog.value = showDialog
            }

            Spacer(Modifier.height(5.dp))
            //Recent Item Sold
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            ) {
                RecentItemSold(Modifier.fillMaxSize(), dashboardViewModel.recentSellsItem)
            }
        }


    }
}


@Composable
fun CategorySales(
    modifier: Modifier = Modifier,
    dashboardViewModel: DashboardViewModel,
    onShowTimeRangeDialog: (Boolean) -> Unit
) {
    Column(
        modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.TwoTone.Category,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(4.dp))
            Text("Category Overview", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text(
                text = getTimeRangeDisplayText(dashboardViewModel.selectedRange.value),
                fontSize = 9.sp,
                color = Color.Gray,
                modifier = Modifier.clickable {
                    onShowTimeRangeDialog(true)
                })
        }

        if (dashboardViewModel.topSubCategories.isNotEmpty()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
            ) {
                dashboardViewModel.topSubCategories.forEach {
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
                        Spacer(Modifier.width(5.dp))
                        Text(
                            "₹${(it.totalPrice).to3FString()}, ",
                            fontSize = 10.sp,
                            modifier = Modifier
                                .weight(3f)
                                .height(20.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            "${it.totalFnWt.to3FString()}g",
                            fontSize = 10.sp,
                            modifier = Modifier
                                .weight(1f)
                                .height(20.dp)
                        )
                    }
                }
            }
        } else {
            // Loading state
            Column(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp)),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "No Bill Found", fontSize = 9.sp, color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun TopFiveSales(
    modifier: Modifier,
    dashboardViewModel: DashboardViewModel,
    onShowTimeRangeDialog: (Boolean) -> Unit
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.AutoMirrored.TwoTone.TrendingUp,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(4.dp))
            Text("Top 5 Selling Items", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text(
                text = getTimeRangeDisplayText(dashboardViewModel.selectedRange.value),
                fontSize = 9.sp,
                color = Color.Gray,
                modifier = Modifier.clickable {
                    onShowTimeRangeDialog(true)
                })
        }

        if (dashboardViewModel.topSellingItemsMap.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxSize()
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
        } else {
            // Loading state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp)),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "No Bill Found", fontSize = 9.sp, color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun MetalRateView(
    modifier: Modifier,
) {

    val baseViewModel = LocalBaseViewModel.current
    val context = LocalContext.current
    val latestTime = baseViewModel.metalRates.firstOrNull()?.updatedDate.orEmpty()
    val hasApiRates = baseViewModel.metalRates.any { rate ->
        !rate.source.trim().equals("cache", ignoreCase = true)
    }
    val rates = baseViewModel.metalRates.filter {
        it.metal.equals("Gold", true) || it.metal.equals("Silver", true)
    }
    val groupedRates = rates.groupBy { it.metal.lowercase() }
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(25.dp    )) {
            Icon(
                imageVector = Icons.TwoTone.Scale,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(4.dp))
            Text("Metal Rate", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))

            if (hasApiRates && latestTime.isNotBlank()) {
                Text(latestTime, fontSize = 10.sp, color = Color.Gray)
                Spacer(Modifier.width(6.dp))
            } else {
                Icon(
                    imageVector = Icons.TwoTone.CloudOff,
                    contentDescription = "Using cached rates",
                    tint = Color.Gray,
                    modifier = Modifier.size(14.dp)
                )
            }

            IconButton(
                onClick = {
                    if (!baseViewModel.metalRatesLoading.value) {
                        ioScope {
                            baseViewModel.refreshOnlineMetalRates(context)
                        }
                    }
                },
                enabled = !baseViewModel.metalRatesLoading.value
            ) {
                Icon(
                    imageVector = Icons.TwoTone.Refresh,
                    contentDescription = "Refresh metal rates",
                    tint = if (baseViewModel.metalRatesLoading.value) Color.Gray else MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(Modifier.height(6.dp))
        when {
            baseViewModel.metalRatesLoading.value -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Fetching latest rates...", fontSize = 10.sp, color = Color.Gray)
                }
            }

            rates.isEmpty() -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("No metal rates available", fontSize = 10.sp, color = Color.Gray)
                }
            }

            else -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                        .padding(8.dp)
                ) {
                    listOf("Gold", "Silver").forEach { metal ->
                        val metalRates = groupedRates[metal.lowercase()].orEmpty()
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                        ) {
                            Text(
                                metal,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(4.dp))
                            if (metalRates.isEmpty()) {
                                Text(
                                    "No $metal rates",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            } else {
                                val orderedRates = if (metal.equals("gold", true)) {
                                    metalRates.sortedWith(
                                        compareBy<MetalRate> {
                                            when (it.caratOrPurity.replace(" ", "").uppercase()) {
                                                "24K" -> 0
                                                "22K" -> 1
                                                "18K" -> 2
                                                else -> 3
                                            }
                                        }.thenBy { it.caratOrPurity }
                                    )
                                } else {
                                    metalRates.sortedBy { it.caratOrPurity }
                                }

                                orderedRates.forEach { rate ->
                                    Row(
                                        modifier = Modifier.padding(vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            rate.caratOrPurity,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(Modifier.weight(1f))
                                        Text(
                                            formatMetalPrice(rate.price),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatMetalPrice(price: String): String {
    val trimmed = price.trim()
    return if (trimmed.startsWith("₹") || trimmed.startsWith("?")) trimmed else "₹$trimmed"
}


@Composable
fun ItemViewItem(
    category: String, items: List<TopItemByCategory>
) {
    Column(modifier = Modifier.width(120.dp)) {
        items.take(5).forEach { item ->
            Row(
                modifier = Modifier.height(20.dp), verticalAlignment = Alignment.CenterVertically
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
                    item.totalFnWt.toString(), fontSize = 10.sp, fontWeight = FontWeight.Medium
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
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
fun FlowOverView(
    modifier: Modifier=Modifier, dashboardViewModel: DashboardViewModel, onShowTimeRangeDialog: (Boolean) -> Unit
) {
    Column(
        modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(5.dp),

        ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.TwoTone.Analytics,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(4.dp))
            Text("Flow Overview", fontSize = 10.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.weight(1f))
            Text(
                text = getTimeRangeDisplayText(dashboardViewModel.selectedRange.value),
                fontSize = 10.sp,
                color = Color.Gray,
                modifier = Modifier.clickable {
                    onShowTimeRangeDialog(true)
                })
        }

        if (dashboardViewModel.salesSummary.value != null) {
            Column(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                    .padding(8.dp)
            ) {
                Spacer(Modifier.weight(1f))
                Column {
                    Text(
                        "₹${(dashboardViewModel.salesSummary.value?.totalAmount ?: 0.0).to3FString()}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text("Total Sales", fontSize = 10.sp, color = Color.Gray)
                }
                Spacer(Modifier.weight(1f))
                Column {
                    Text(
                        "${dashboardViewModel.salesSummary.value?.invoiceCount ?: 0} ",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text("Total Invoice", fontSize = 10.sp, color = Color.Gray)
                }
            }
        } else {
            // Loading state
            Column(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp)),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Loading...", fontSize = 9.sp, color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun CustomerOverview(
    modifier: Modifier = Modifier, dashboardViewModel: DashboardViewModel,
) {
    Column(
        modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.TwoTone.People,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(4.dp))
            Text("Customer Overview", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
        }

        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                .padding(2.dp)
        ) {
            // Customer Statistics
            dashboardViewModel.customerSummary.value?.let { summary ->
                // Grid Layout
                Column {
                    // Row 1: Total Customers (Large Card)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(45.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.TwoTone.People,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                "${summary.totalCustomers}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                        }
                    }
                    Spacer(Modifier.height(5.dp))

                    // Row 2: Khata Books and Outstanding (Two Small Cards)
                    Row {
                        // Khata Book Card
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(
                                    alpha = 0.3f
                                )
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(6.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            "${summary.activeKhataBookCustomers}",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                        Text(
                                            "₹${summary.currentMonthKhataBookPayments.to1FString()}",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )

                                    }
                                }
                                Text(
                                    "Khata Customers",
                                    fontSize = 7.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Spacer(Modifier.width(5.dp))

                        // Outstanding Balance Card
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.Red.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(6.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            "${dashboardViewModel.customersWithOutstandingBalance.size}",
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Red
                                        )
                                        Text(
                                            "₹${summary.totalOutstandingBalance.to1FString()}",
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Red
                                        )

                                    }
                                }
                                Text(
                                    text = "Outstanding",
                                    fontSize = 7.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            } ?: run {
                // Loading state
                Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Loading...", fontSize = 9.sp, color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun TimeRangeSelectionDialog(
    currentRange: TimeRange, onRangeSelected: (TimeRange) -> Unit, onDismiss: () -> Unit
) {
    val timeRangeOptions = listOf(
        TimeRange.WEEKLY to "Weekly",
        TimeRange.MONTHLY to "Monthly",
        TimeRange.THREE_MONTHS to "3 Months",
        TimeRange.SIX_MONTHS to "6 Months",
        TimeRange.YEARLY to "Yearly"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Select Time Range", fontWeight = FontWeight.Bold, fontSize = 16.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                timeRangeOptions.forEach { (range, displayText) ->
                    val isSelected = currentRange == range
                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onRangeSelected(range)
                        }
                        .background(
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else Color.Transparent, shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = displayText,
                            modifier = Modifier.weight(1f),
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurface
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.TwoTone.Pentagon,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss, modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(
                    "Cancel", fontSize = 14.sp, fontWeight = FontWeight.Medium
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.padding(16.dp)
    )
}

fun getTimeRangeDisplayText(timeRange: TimeRange): String {
    return when (timeRange) {
        TimeRange.WEEKLY -> "Weekly"
        TimeRange.MONTHLY -> "Monthly"
        TimeRange.THREE_MONTHS -> "3 Months"
        TimeRange.SIX_MONTHS -> "6 Months"
        TimeRange.YEARLY -> "Yearly"
    }
}


@Composable
fun RecentItemSold(
    modifier: Modifier, recentSellsItem: SnapshotStateList<IndividualSellItem>
) {
    val subNavController = LocalSubNavController.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
    ) {
        // Colored header with icon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary
                        )
                    ), shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                )
                .padding(vertical = 10.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.TwoTone.Scale, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text(
                "Recent Sells", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp
            )
            Spacer(Modifier.weight(1f))
        }

        // Card for table
        Card(
            modifier = Modifier.fillMaxSize()
        ) {
            // Define headers for all essential fields
            val headers = listOf(
                "Date",
                "Order ID",
                "Customer Name",
                "Mobile No",
                "Address",
                "GSTIN/PAN",
                "Name",
                "Quantity",
                "Gross Wt",
                "Net Wt",
                "Fine Wt",
                "Purity",
                "Metal Price",
                "Price",
                "Tax",
                "Tax Amount",
                "Charge",
                "M.Charge",
                "Other Charge",
                "Total Amount",
                "HUID",
                "Add. Description"
            )

            // Convert IndividualSellItem to List<String> for each row
            val tableData = recentSellsItem.map { item ->
                listOf(
                    item.orderDate.toString(),
                    item.orderId,
                    item.name,
                    item.mobileNo,
                    item.address ?: "",
                    item.gstin_pan ?: "",
                    "${item.catName} (${item.catId}) - ${item.subCatName} (${item.subCatId}) - ${item.itemAddName}",
                    item.quantity.toString(),
                    "${item.gsWt.to3FString()}g",
                    "${item.ntWt.to3FString()}g",
                    "${item.fnWt.to3FString()}g",
                    item.purity,
                    "₹${item.fnMetalPrice.to3FString()}",
                    "₹${item.price.to3FString()}",
                    "${item.cgst.to1FString()} + ${item.sgst.to1FString()} + ${item.igst.to1FString()}",
                    "₹${item.tax.to3FString()}",
                    "${item.crg.to3FString()} ${item.crgType}",
                    "₹${item.charge.to3FString()}",
                    "${item.othCrgDes}: ₹${item.othCrg.to3FString()}",
                    "₹${
                        CalculationUtils.totalPrice(
                            item.price, item.charge, item.othCrg, item.tax
                        ).to3FString()
                    }",
                    item.huid,
                    "${item.addDesKey}: ${item.addDesValue}"
                )
            }

            TextListView(
                modifier = Modifier.fillMaxWidth().height(500.dp),
                headerList = headers,
                items = tableData,
                onItemClick = { clickedItem ->
                    val orderId = clickedItem[1]
                    if (orderId.isNotEmpty()) {
                        subNavController.navigate("${SubScreens.OrderItemDetail.route}/$orderId")
                    }
                },
                onItemLongClick = { longClickedItem ->
                    // Handle item long click if needed
                })
        }
    }
}

