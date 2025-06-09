package com.velox.jewelvault.data.roomdb.entity.order

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.sql.Timestamp

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["orderId"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class OrderItemEntity(
    @PrimaryKey(autoGenerate = true)
    val orderItemId: Int = 0,
    val orderId: Int,
    val orderDate: Timestamp,
    val itemId: Int,
    val customerMobile:String,
    val catId: Int,
    val catName: String,
    val itemAddName: String,
    val subCatId: Int,
    val subCatName: String,
    val entryType: String,
    val quantity: Int,
    val gsWt: Double,
    val ntWt: Double,
    val fnWt: Double,
    val fnMetalPrice: Double,
    val purity: String,
    val crgType: String,
    val crg: Double,
    val othCrgDes: String,
    val othCrg: Double,
    val cgst: Double,
    val sgst: Double,
    val igst: Double,
    val huid: String,
    val addDesKey:String,
    val addDesValue:String,
    val price: Double,
    val charge: Double,
    val tax: Double,

    //seller info
    val sellerFirmId: Int = 0,
    val purchaseOrderId: Int = 0,
    val purchaseItemId: Int = 0,
)
