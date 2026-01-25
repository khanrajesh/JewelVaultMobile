package com.velox.jewelvault.ui.nav

sealed class Screens(val route:String) {
    data object Splash : Screens("splash")
    data object Login : Screens("login")
    data object StartLoading : Screens("startLoading")
    data object Main : Screens("main")
    data object SellInvoice : Screens("sellInvoice")
    data object QrScanScreen : Screens("qrScan")
    data object SellPreview : Screens("sellPreview")
    data object Purchase : Screens("purchase")

    data object DraftInvoice : Screens("draftInvoice")
}

sealed class SubScreens(val route:String) {
    data object Dashboard : SubScreens("dashboard")
    data object Profile : SubScreens("profile")
    data object Inventory : SubScreens("inventory")
    data object InventoryItem : SubScreens("inventoryItem")
    data object InventoryFilter : SubScreens("inventoryFilter")
    data object ImportItems : SubScreens("importItems")
    data object OrderItemDetail : SubScreens("report")
    data object OrderAndPurchase : SubScreens("ledger")
    data object PurchaseItemDetail : SubScreens("PurchaseItemDetail")
    data object Setting : SubScreens("Setting")
    data object Permissions : SubScreens("permissions")
    data object UserManagement : SubScreens("userManagement")
    data object Customers : SubScreens("customers")
    data object CustomersDetails : SubScreens("customersDetails")
    data object BackUpSetting : SubScreens("backUpSetting")
    data object Audit : SubScreens("audit")
    data object PrivacyPolicy : SubScreens("privacyPolicy")
    data object TermsAndConditions : SubScreens("termsAndConditions")
    data object BluetoothScanConnect : SubScreens("bluetooth_scan_connect")
    data object BluetoothManagePrinters : SubScreens("bluetooth_manage_printers")
    data object Guide : SubScreens("guide")
    
    // Label related screens
    data object LabelTemplateList : SubScreens("label_template_list")
    data object LabelDesigner : SubScreens("label_designer")
    data object ScanAddItem : SubScreens("scanAddItem")

    data object KhataBookPlans : SubScreens("khataBookPlans")
    data object SubscriptionDetails : SubScreens("subscriptionDetails")
}
