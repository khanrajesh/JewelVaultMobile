package com.velox.jewelvault.data.roomdb.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.velox.jewelvault.data.roomdb.TableNames
import com.velox.jewelvault.utils.generateId
import java.sql.Timestamp

@Entity(tableName = TableNames.ITEM)
data class ItemEntity(
    @PrimaryKey val itemId: String,  // Auto-increment primary key
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
    val purity: String,
    val crgType: String,
    val crg: Double,
    val othCrgDes: String,
    val othCrg: Double,
    val cgst: Double,
    val sgst: Double,
    val igst: Double,
    val huid: String,
    val unit: String = "gm",
    val addDesKey:String,
    val addDesValue:String,
    val addDate: Timestamp,
    val modifiedDate: Timestamp,
    //seller info
    val sellerFirmId: String ,
    val purchaseOrderId: String ,
    val purchaseItemId: String ,
    )

