package com.velox.jewelvault.data.roomdb.dto

data class PurchaseItemInputDto(
    val billNo: String = "",
    val catId: String = "",
    val subCatId: String = "",
    val name: String = "",
    val gsWt: String = "",
    val purity: String = "",
    val fnWt: String = "",
    val wastage: String = "",
    val fnMetalRate: String = "",
    val cgst: String = "1.5",
    val sgst: String = "1.5",
    val igst: String = "",
    val extraChargeDes: String = "",
    val extraCharge: String = ""
)
data class PurchaseMetalRateDto(
    val categoryId: String,
    val metalName: String, // e.g., "Gold", "Silver"
    val ratePerGram: String // or Float/Double if you're doing calculations
)