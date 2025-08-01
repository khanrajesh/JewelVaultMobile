package com.velox.jewelvault.data.roomdb.entity.order

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.velox.jewelvault.data.roomdb.TableNames
import java.sql.Timestamp

@Entity(
    tableName = TableNames.ORDER_ITEM,
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
    @PrimaryKey
    val orderItemId: String,
    val orderId: String,
    val orderDate: Timestamp,
    val itemId: String,
    val customerMobile:String,
    val catId: String,
    val catName: String,
    val itemAddName: String,
    val subCatId: String,
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
    val sellerFirmId: String,
    val purchaseOrderId: String,
    val purchaseItemId: String,
)
