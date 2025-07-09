package com.velox.jewelvault.ui.screen.setting

import androidx.activity.compose.BackHandler
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.velox.jewelvault.ui.nav.SubScreens
import com.velox.jewelvault.ui.screen.dashboard.DashboardViewModel
import com.velox.jewelvault.utils.LocalSubNavController

@Composable
fun SettingScreen(dashboardViewModel: DashboardViewModel) {
    val subNavController = LocalSubNavController.current

    BackHandler {
        subNavController.navigate(SubScreens.Dashboard.route){
            popUpTo(SubScreens.Dashboard.route) {
                inclusive = true
            }
        }
    }
    Text("Setting Screen")
}