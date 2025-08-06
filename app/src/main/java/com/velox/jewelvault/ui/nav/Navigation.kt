package com.velox.jewelvault.ui.nav

import androidx.compose.material.icons.filled.History
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.velox.jewelvault.BaseViewModel
import com.velox.jewelvault.ui.screen.SplashScreen
import com.velox.jewelvault.ui.screen.dashboard.DashboardScreen
import com.velox.jewelvault.ui.screen.dashboard.DashboardViewModel
import com.velox.jewelvault.ui.screen.inventory.InventoryCategoryScreen
import com.velox.jewelvault.ui.screen.inventory.InventoryFilterScreen
import com.velox.jewelvault.ui.screen.inventory.InventoryItemScreen
import com.velox.jewelvault.ui.screen.inventory.InventoryViewModel
import com.velox.jewelvault.ui.screen.login.LoginScreen
import com.velox.jewelvault.ui.screen.login.LoginViewModel
import com.velox.jewelvault.ui.screen.main.MainScreen
import com.velox.jewelvault.ui.screen.order_and_purchase.OrderAndPurchaseScreen
import com.velox.jewelvault.ui.screen.order_and_purchase.OrderAndReportViewModel
import com.velox.jewelvault.ui.screen.order_and_purchase.order_item.OrderItemDetailScreen
import com.velox.jewelvault.ui.screen.order_and_purchase.order_item.OrderItemViewModel
import com.velox.jewelvault.ui.screen.order_and_purchase.purchase_item.PurchaseItemDetailScreen
import com.velox.jewelvault.ui.screen.order_and_purchase.purchase_item.PurchaseItemViewModel
import com.velox.jewelvault.ui.screen.profile.ProfileScreen
import com.velox.jewelvault.ui.screen.profile.ProfileViewModel
import com.velox.jewelvault.ui.screen.purchase.PurchaseScreen
import com.velox.jewelvault.ui.screen.purchase.PurchaseViewModel
import com.velox.jewelvault.ui.screen.qr_bar_scanner.QrBarScannerScreen
import com.velox.jewelvault.ui.screen.qr_bar_scanner.QrBarScannerViewModel
import com.velox.jewelvault.ui.screen.sell_invoice.SellInvoiceScreen
import com.velox.jewelvault.ui.screen.sell_invoice.SellInvoiceViewModel
import com.velox.jewelvault.ui.screen.sell_invoice.SellPreviewScreen
import com.velox.jewelvault.ui.screen.setting.SettingScreen
import com.velox.jewelvault.ui.screen.user_management.UserManagementScreen
import com.velox.jewelvault.ui.screen.customers.CustomerScreen
import com.velox.jewelvault.ui.screen.customers.CustomerViewModel
import com.velox.jewelvault.ui.screen.customers.CustomerDetailScreen
import com.velox.jewelvault.ui.screen.customers.KhataBookPlansScreen
import com.velox.jewelvault.ui.screen.draft_invoice.DraftInvoiceScreen
import com.velox.jewelvault.ui.screen.draft_invoice.DraftInvoiceViewModel
import com.velox.jewelvault.ui.screen.draft_invoice.DraftPreviewScreen
import com.velox.jewelvault.ui.screen.setting.BackupSettingsScreen
import com.velox.jewelvault.utils.LocalBaseViewModel
import com.velox.jewelvault.utils.LocalNavController
import com.velox.jewelvault.utils.LocalSubNavController

@Composable
fun AppNavigation(
    navController: NavHostController,
    baseViewModel: BaseViewModel,
    startDestination: String = Screens.Login.route
) {


    CompositionLocalProvider(
        LocalNavController provides navController,
        LocalBaseViewModel provides baseViewModel,
    ) {
        val sellInvoiceViewModel = hiltViewModel<SellInvoiceViewModel>()
        val draftInvoiceViewModel = hiltViewModel<DraftInvoiceViewModel>()

        NavHost(navController = navController, startDestination = startDestination) {
            composable(Screens.Splash.route) {
                SplashScreen(navHost = navController)
            }
            composable(Screens.Login.route) {
                LoginScreen(hiltViewModel<LoginViewModel>())
            }
            composable(Screens.Main.route) {
                MainScreen()
            }
            composable(Screens.SellInvoice.route) {
                SellInvoiceScreen(sellInvoiceViewModel)
            }
            composable(Screens.SellPreview.route) {
                SellPreviewScreen(sellInvoiceViewModel)
            }
            composable(Screens.QrScanScreen.route) {
                QrBarScannerScreen(hiltViewModel<QrBarScannerViewModel>())
            }
            composable(Screens.Purchase.route) {
                PurchaseScreen(hiltViewModel<PurchaseViewModel>())
            }
            composable(Screens.DraftInvoice.route) {
                DraftInvoiceScreen(draftInvoiceViewModel)
            }
            composable(Screens.DraftPreview.route) {
                DraftPreviewScreen(draftInvoiceViewModel)
            }
        }
    }
}

@Composable
fun SubAppNavigation(
    subNavController: NavHostController,
    navController: NavHostController,
    baseViewModel: BaseViewModel,
    startDestination: String = SubScreens.Dashboard.route
) {

    val inventoryViewModel = hiltViewModel<InventoryViewModel>()
    val dashboardViewModel = hiltViewModel<DashboardViewModel>()

    CompositionLocalProvider(
        LocalSubNavController provides subNavController,
        LocalNavController provides navController,
        LocalBaseViewModel provides baseViewModel
    ) {
        NavHost(navController = subNavController, startDestination = startDestination) {
            composable(SubScreens.Setting.route) {
                SettingScreen(dashboardViewModel)
            }
            composable(SubScreens.UserManagement.route) {
                UserManagementScreen()
            }
            composable(SubScreens.Dashboard.route) {
                DashboardScreen(dashboardViewModel)
            }
            composable("${SubScreens.Profile.route}/{firstLaunch}",
                arguments = listOf(
                    navArgument("firstLaunch") { type = NavType.BoolType }
                )
                ) {backStackEntry->
                val firstLaunch = backStackEntry.arguments?.getBoolean("firstLaunch") ?: false
                ProfileScreen(hiltViewModel<ProfileViewModel>(),firstLaunch)
            }
            composable(SubScreens.Inventory.route) {
                InventoryCategoryScreen(inventoryViewModel)
            }
            composable(
                "${SubScreens.InventoryItem.route}/{catId}/{catName}/{subCatId}/{subCatName}",
                arguments = listOf(
                    navArgument("catId") { type = NavType.StringType },
                    navArgument("catName") { type = NavType.StringType },
                    navArgument("subCatId") { type = NavType.StringType },
                    navArgument("subCatName") { type = NavType.StringType },
                )
            ) { backStackEntry ->
                val catId = backStackEntry.arguments?.getString("catId") ?: return@composable
                val catName = backStackEntry.arguments?.getString("catName") ?: return@composable
                val subCatId = backStackEntry.arguments?.getString("subCatId") ?: return@composable
                val subCatName =
                    backStackEntry.arguments?.getString("subCatName") ?: return@composable
                InventoryItemScreen(inventoryViewModel, catId, catName, subCatId, subCatName)
            }


            composable(SubScreens.InventoryFilter.route) {
                InventoryFilterScreen(inventoryViewModel)
            }

            composable(SubScreens.Customers.route) {
                CustomerScreen( hiltViewModel())
            }
            
            composable(
                route = "customer_detail/{customerMobile}",
                arguments = listOf(
                    navArgument("customerMobile") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val customerMobile = backStackEntry.arguments?.getString("customerMobile") ?: ""
                CustomerDetailScreen(customerMobile)
            }
            
            composable("khata_book_plans") {
                KhataBookPlansScreen(subNavController)
            }

            composable(SubScreens.OrderAndPurchase.route) {
                OrderAndPurchaseScreen(hiltViewModel<OrderAndReportViewModel>())
            }
            composable("${SubScreens.OrderItemDetail.route}/{orderId}",
                arguments = listOf(navArgument("orderId") { type = NavType.StringType })
                ) {backStackEntry ->
                val orderId = backStackEntry.arguments?.getString("orderId") ?: return@composable
                OrderItemDetailScreen(hiltViewModel<OrderItemViewModel>(), orderId)
            }

            composable("${SubScreens.PurchaseItemDetail.route}/{purchaseOrderId}",
                arguments = listOf(navArgument("purchaseOrderId") { type = NavType.StringType })) {backStackEntry ->
                val purchaseOrderId = backStackEntry.arguments?.getString("purchaseOrderId") ?: return@composable
                PurchaseItemDetailScreen(hiltViewModel<PurchaseItemViewModel>(),purchaseOrderId)
            }
            composable(
                SubScreens.BackUpSetting.route
            ) {
                BackupSettingsScreen(onBackPressed = { navController.popBackStack() })
            }
        }
    }
}



