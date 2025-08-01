package com.velox.jewelvault.data.roomdb.dto

import java.sql.Timestamp

data class ItemSelectedModel(
    val itemId: String,
    val itemAddName: String,
    val catId: String,
    val userId: String,
    val storeId: String,
    val catName: String,
    val subCatId: String,
    val subCatName: String,
    val entryType: String,
    val quantity: Int,
    val gsWt: Double,
    val ntWt: Double,
    val fnWt: Double,
    val fnMetalPrice: Double=0.0,
    val purity: String,
    val crgType: String,
    val crg: Double,
    val othCrgDes: String,
    val othCrg: Double,
    val cgst: Double=0.0,
    val sgst: Double=0.0,
    val igst: Double=0.0,
    val addDesKey:String,
    val addDesValue:String,
    val huid: String,
    val price: Double=0.0,
    val chargeAmount: Double=0.0,
    val tax: Double=0.0,
    val addDate:Timestamp?=null,
    //seller info
    val sellerFirmId: String,
    val purchaseOrderId: String,
    val purchaseItemId: String,

)
