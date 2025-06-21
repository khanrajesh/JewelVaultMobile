package com.velox.jewelvault.data.roomdb.entity.purchase

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "metal_exchange",
    foreignKeys = [ForeignKey(
        entity = PurchaseOrderEntity::class,
        parentColumns = ["purchaseOrderId"],
        childColumns = ["purchaseOrderId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("purchaseOrderId")]
)
data class MetalExchangeEntity(
    @PrimaryKey(autoGenerate = true) val exchangeId: Int = 0,
    val purchaseOrderId: Int,
    val catId: Int,
    val catName: String,
    val subCatId: Int,
    val subCatName: String,
    val fnWeight: Double
)
