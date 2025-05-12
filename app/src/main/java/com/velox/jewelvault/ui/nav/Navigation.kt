package com.velox.jewelvault.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.velox.jewelvault.BaseViewModel
import com.velox.jewelvault.ui.screen.main.MainScreen
import com.velox.jewelvault.ui.screen.login.LoginScreen
import com.velox.jewelvault.ui.screen.dashboard.DashboardScreen
import com.velox.jewelvault.ui.screen.inventory.InventoryItemScreen
import com.velox.jewelvault.ui.screen.inventory.InventoryScreen
import com.velox.jewelvault.ui.screen.LedgerScreen
import com.velox.jewelvault.ui.screen.profile.ProfileScreen
import com.velox.jewelvault.ui.screen.QrBarScannerScreen
import com.velox.jewelvault.ui.screen.ReportScreen
import com.velox.jewelvault.ui.screen.sell_invoice.SellInvoiceScreen
import com.velox.jewelvault.ui.screen.SellPreviewScreen
import com.velox.jewelvault.ui.screen.SplashScreen
import com.velox.jewelvault.ui.screen.inventory.InventoryViewModel
import com.velox.jewelvault.ui.screen.login.LoginViewModel
import com.velox.jewelvault.ui.screen.profile.ProfileViewModel
import com.velox.jewelvault.ui.screen.sell_invoice.SellInvoiceViewModel
import com.velox.jewelvault.utils.LocalBaseViewModel
import com.velox.jewelvault.utils.LocalSubNavController
import com.velox.jewelvault.utils.LocalNavController

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
                SellPreviewScreen()
            }
            composable(Screens.QrScanScreen.route) {
                QrBarScannerScreen()
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
    val profileViewModel = hiltViewModel<ProfileViewModel>()

    CompositionLocalProvider(
        LocalSubNavController provides subNavController,
        LocalNavController provides navController,
        LocalBaseViewModel provides baseViewModel
    ) {
        NavHost(navController = subNavController, startDestination = startDestination) {
            composable(SubScreens.Dashboard.route) {
                DashboardScreen()
            }
            composable(SubScreens.Profile.route) {
                ProfileScreen(profileViewModel)
            }
            composable(SubScreens.Inventory.route) {
                InventoryScreen(inventoryViewModel)
            }
            composable(SubScreens.Ledger.route) {
                LedgerScreen()
            }
            composable(SubScreens.Report.route) {
                ReportScreen()
            }
            composable(
                "${SubScreens.InventoryItem.route}/{catId}/{catName}/{subCatId}/{subCatName}",
                arguments = listOf(
                    navArgument("catId") { type = NavType.IntType },
                    navArgument("catName") { type = NavType.StringType },
                    navArgument("subCatId") { type = NavType.IntType },
                    navArgument("subCatName") { type = NavType.StringType },
                )
            ) {backStackEntry->
                val catId = backStackEntry.arguments?.getInt("catId") ?: return@composable
                val catName = backStackEntry.arguments?.getString("catName") ?: return@composable
                val subCatId = backStackEntry.arguments?.getInt("subCatId") ?: return@composable
                val subCatName = backStackEntry.arguments?.getString("subCatName") ?: return@composable
                InventoryItemScreen(inventoryViewModel,catId,catName,subCatId,subCatName)
            }
        }
    }
}