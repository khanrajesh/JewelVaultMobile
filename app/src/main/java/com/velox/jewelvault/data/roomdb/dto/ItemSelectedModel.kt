package com.velox.jewelvault.data.roomdb.dto

import java.sql.Timestamp

data class ItemSelectedModel(
    val itemId: Int,
    val itemAddName: String,
    val catId: Int,
    val userId: Int,
    val storeId: Int,
    val catName: String,
    val subCatId: Int,
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
    val huid: String,
    val price: Double=0.0,
    val charge: Double=0.0,
    val tax: Double=0.0,
    val addDate:Timestamp?=null

)
