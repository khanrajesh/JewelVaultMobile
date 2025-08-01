package com.velox.jewelvault.data.roomdb.dto

data class PurchaseItemInputDto(
    val billNo: String = "",
    val catId: String = "",
    val catName: String = "",
    val subCatId: String = "",
    val subCatName: String = "",
    val name: String = "",
    val gsWt: Double = 0.0,
    val ntWt: Double = 0.0,
    val purity: String = "",
    val fnWt: Double = 0.0,
    val fnRatePerGm: Double=0.0,
    val wastage: Double = 0.0,
    val extraChargeDes: String = "",
    val extraCharge: Double = 0.0,
    val toAdd:Boolean
)
