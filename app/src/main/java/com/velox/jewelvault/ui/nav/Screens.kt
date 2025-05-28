package com.velox.jewelvault.ui.nav

sealed class Screens(val route:String) {
    data object Splash : Screens("splash")
    data object Login : Screens("login")
    data object Main : Screens("main")
    data object SellInvoice : Screens("sellInvoice")
    data object QrScanScreen : Screens("qrScan")
    data object SellPreview : Screens("sellPreview")
    data object Purchase : Screens("purchase")
}

sealed class SubScreens(val route:String) {
    data object Dashboard : SubScreens("dashboard")
    data object Profile : SubScreens("profile")
    data object Inventory : SubScreens("inventory")
    data object InventoryItem : SubScreens("inventoryItem")
    data object InventoryFilter : SubScreens("inventoryFilter")
    data object OrderItemDetail : SubScreens("report")
    data object Ledger : SubScreens("ledger")
    data object Setting : SubScreens("Setting")
}