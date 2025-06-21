package com.velox.jewelvault.data.roomdb.dto

data class PurchaseItemInputDto(
    val billNo: String = "",
    val catId: Int = -1,
    val catName: String = "",
    val subCatId: Int = -1,
    val subCatName: String = "",
    val name: String = "",
    val gsWt: Double = 0.0,
    val ntWt: Double = 0.0,
    val purity: String = "",
    val fnWt: Double = 0.0,
    val fnRate: Double=0.0,
    val wastage: Double = 0.0,
    val extraChargeDes: String = "",
    val extraCharge: Double = 0.0,
    val toAdd:Boolean
)
data class PurchaseMetalRateDto(
    val catId: Int,
    val catName: String,
    val subCatId: Int,
    val subCatName: String,
    val fnWt: Double
)