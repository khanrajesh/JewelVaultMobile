package com.velox.jewelvault.data.roomdb.entity.purchase

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.velox.jewelvault.data.roomdb.TableNames

@Entity(
    tableName = TableNames.PURCHASE_ORDER_ITEM,
    foreignKeys = [ForeignKey(
        entity = PurchaseOrderEntity::class,
        parentColumns = ["purchaseOrderId"],
        childColumns = ["purchaseOrderId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("purchaseOrderId")]
)
data class PurchaseOrderItemEntity(
    @PrimaryKey
    val purchaseItemId: String,
    val purchaseOrderId: String,
    val catId: String,
    val catName: String,
    val subCatId: String,
    val subCatName: String,
    val gsWt: Double,
    val purity: String,
    val ntWt: Double,
    val fnWt: Double,
    val fnRate: Double,
    val wastagePercent: Double,
)
