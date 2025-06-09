package com.velox.jewelvault.data.roomdb.entity.purchase

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "purchase_orders",
    foreignKeys = [ForeignKey(
        entity = SellerEntity::class,
        parentColumns = ["sellerId"],
        childColumns = ["sellerId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("sellerId")]
)
data class PurchaseOrderEntity(
    @PrimaryKey(autoGenerate = true)
    val purchaseOrderId: Int = 0,
    val sellerId: Int,
    val billNo: String,
    val billDate: String,   // Bill issued date
    val entryDate: String,  // Actual entry date
    val extraChargeDescription: String?,
    val extraCharge: Double?,
    val totalFinalWeight: Double?, // Total fnWt after exchange
    val totalFinalAmount: Double?, // Final calculated value
    val notes: String?
)
