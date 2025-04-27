package com.velox.jewelvault.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.velox.jewelvault.BaseViewModel
import com.velox.jewelvault.ui.screen.DashboardScreen
import com.velox.jewelvault.ui.screen.LoginScreen
import com.velox.jewelvault.ui.screen.SellInvoiceScreen
import com.velox.jewelvault.ui.screen.SellPreviewScreen
import com.velox.jewelvault.ui.screen.SplashScreen
import com.velox.jewelvault.utils.LocalBaseViewModel
import com.velox.jewelvault.utils.LocalNavController

@Composable
fun AppNavigation(navController: NavHostController, baseViewModel: BaseViewModel, startDestination:String = Screens.Login.route) {

    CompositionLocalProvider(
        LocalNavController provides navController,
        LocalBaseViewModel provides baseViewModel
    ) {
        NavHost(navController = navController, startDestination=startDestination) {
            composable(Screens.Splash.route) {
                SplashScreen(navHost = navController)
            }
            composable(Screens.Login.route) {
                LoginScreen()
            }
            composable(Screens.Dashboard.route) {
                DashboardScreen()
            }
            composable(Screens.SellInvoice.route) {
                SellInvoiceScreen()
            }
            composable(Screens.SellPreview.route) {
                SellPreviewScreen()
            }
        }
    }
}
