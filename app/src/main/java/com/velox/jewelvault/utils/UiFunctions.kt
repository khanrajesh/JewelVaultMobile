package com.velox.jewelvault.utils

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.navigation.NavHostController
import com.velox.jewelvault.BaseViewModel

fun isTablet(context: Context): Boolean {
    return context.resources.configuration.smallestScreenWidthDp >= 600
}

@Composable
fun isLandscape(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
}

val LocalNavController = staticCompositionLocalOf<NavHostController> {
    error("No NavController provided")
}
val LocalSubNavController = staticCompositionLocalOf<NavHostController> {
    error("No NavController provided")
}
val LocalBaseViewModel = staticCompositionLocalOf<BaseViewModel> {
    error("BaseViewModel not provided")
}
