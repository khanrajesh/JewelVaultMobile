package com.velox.jewelvault.data.roomdb.entity.purchase

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.velox.jewelvault.data.roomdb.TableNames

@Entity(
    tableName = TableNames.PURCHASE_ORDER,
    foreignKeys = [ForeignKey(
        entity = SellerEntity::class,
        parentColumns = ["sellerId"],
        childColumns = ["sellerId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("sellerId")]
)
data class PurchaseOrderEntity(
    @PrimaryKey
    val purchaseOrderId: String,
    val sellerId: String,
    val billNo: String,
    val billDate: String,   // Bill issued date
    val entryDate: String,  // Actual entry date
    val extraChargeDescription: String?,
    val extraCharge: Double?,
    val totalFinalWeight: Double?, // Total fnWt after exchange
    val totalFinalAmount: Double?, // Final calculated value
    val notes: String?,
    val cgstPercent: Double,
    val sgstPercent: Double,
    val igstPercent: Double,
    )
