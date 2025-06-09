package com.velox.jewelvault.data.roomdb.entity.purchase

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "purchase_order_items",
    foreignKeys = [ForeignKey(
        entity = PurchaseOrderEntity::class,
        parentColumns = ["purchaseOrderId"],
        childColumns = ["purchaseOrderId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("purchaseOrderId")]
)
data class PurchaseOrderItemEntity(
    @PrimaryKey(autoGenerate = true)
    val purchaseItemId: Int = 0,
    val purchaseOrderId: Int,
    val catId: Int,
    val categoryName: String,
    val subCatId: Int,
    val subCatName: String,
    val purchaseFnMetalRate: Double,
    val grossWeight: Double,
    val purity: Double,
    val netWeight: Double,
    val wastagePercent: Double,
    val cgstPercent: Double,
    val sgstPercent: Double,
    val igstPercent: Double
)
