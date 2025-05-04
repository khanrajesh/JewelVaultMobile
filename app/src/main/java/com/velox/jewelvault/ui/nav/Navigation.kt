package com.velox.jewelvault.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.velox.jewelvault.BaseViewModel
import com.velox.jewelvault.ui.screen.MainScreen
import com.velox.jewelvault.ui.screen.LoginScreen
import com.velox.jewelvault.ui.screen.DashboardScreen
import com.velox.jewelvault.ui.screen.InventoryScreen
import com.velox.jewelvault.ui.screen.LedgerScreen
import com.velox.jewelvault.ui.screen.ProfileScreen
import com.velox.jewelvault.ui.screen.QrBarScannerScreen
import com.velox.jewelvault.ui.screen.ReportScreen
import com.velox.jewelvault.ui.screen.SellInvoiceScreen
import com.velox.jewelvault.ui.screen.SellPreviewScreen
import com.velox.jewelvault.ui.screen.SplashScreen
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
        LocalBaseViewModel provides baseViewModel
    ) {
        NavHost(navController = navController, startDestination = startDestination) {
            composable(Screens.Splash.route) {
                SplashScreen(navHost = navController)
            }
            composable(Screens.Login.route) {
                LoginScreen()
            }
            composable(Screens.Main.route) {
                MainScreen()
            }
            composable(Screens.SellInvoice.route) {
                SellInvoiceScreen()
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
                ProfileScreen()
            }
            composable(SubScreens.Inventory.route) {
                InventoryScreen()
            }
            composable(SubScreens.Ledger.route) {
                LedgerScreen()
            }
            composable(SubScreens.Report.route) {
                ReportScreen()
            }
        }
    }
}