package com.velox.jewelvault.data.roomdb.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.sql.Timestamp

@Entity
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val itemId: Int = 0,  // Auto-increment primary key
    val itemAddName: String,
    val catId: Int,
    val userId: Int,
    val storeId: Int,
    val catName: String,
    val subCatId: Int,
    val subCatName: String,
    val type: String,
    val quantity: Int,
    val gsWt: Double,
    val ntWt: Double,
    val fnWt: Double,
    val purity: String,
    val crgType: String,
    val crg: Double,
    val othCrgDes: String,
    val othCrg: Double,
    val cgst: Double,
    val sgst: Double,
    val igst: Double,
    val huid: String,
    val addDate: Timestamp,
    val modifiedDate: Timestamp
)
