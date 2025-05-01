package com.velox.jewelvault.ui.nav

sealed class Screens(val route:String) {
    data object Splash : Screens("splash")
    data object Login : Screens("login")
    data object Signup : Screens("signup")
    data object Dashboard : Screens("dashboard")
    data object SellInvoice : Screens("sellInvoice")
    data object QrScanScreen : Screens("qrScan")
    data object SellPreview : Screens("sellPreview")
    data object AddItem : Screens("add_item")
    data object AddCategory : Screens("add_category")
    data object SubCategory : Screens("subcategory")
}

sealed class SubScreens(val route:String) {
    data object Main : SubScreens("main")
}