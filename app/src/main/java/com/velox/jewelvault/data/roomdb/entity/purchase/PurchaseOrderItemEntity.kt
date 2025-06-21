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
    val catName: String,
    val subCatId: Int,
    val subCatName: String,
    val gsWt: Double,
    val purity: String,
    val ntWt: Double,
    val fnWt: Double,
    val fnRate: Double,
    val wastagePercent: Double,
)
