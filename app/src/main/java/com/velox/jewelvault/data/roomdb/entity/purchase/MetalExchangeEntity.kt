package com.velox.jewelvault.data.roomdb.entity.purchase

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.velox.jewelvault.data.roomdb.TableNames

@Entity(
    tableName = TableNames.METAL_EXCHANGE,
    foreignKeys = [ForeignKey(
        entity = PurchaseOrderEntity::class,
        parentColumns = ["purchaseOrderId"],
        childColumns = ["purchaseOrderId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("purchaseOrderId")]
)
data class MetalExchangeEntity(
    @PrimaryKey val exchangeId: String,
    val purchaseOrderId: String,
    val catId: String,
    val catName: String,
    val subCatId: String,
    val subCatName: String,
    val fnWeight: Double
)
