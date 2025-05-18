package com.velox.jewelvault.data.roomdb.entity.order

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

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
    val id: Long = 0,
    val orderId: Long,
    val itemId: Int,
    val itemAddName: String,
    val catId: Int,
    val catName: String,
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
    val price: Double,
    val charge: Double,
    val tax: Double
)
