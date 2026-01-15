package com.velox.jewelvault.data.roomdb.entity.preorder

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.velox.jewelvault.data.roomdb.TableNames

@Entity(
    tableName = TableNames.PRE_ORDER_ITEM,
    foreignKeys = [
        ForeignKey(
            entity = PreOrderEntity::class,
            parentColumns = ["preOrderId"],
            childColumns = ["preOrderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("preOrderId")
    ]
)
data class PreOrderItemEntity(
    @PrimaryKey
    val preOrderItemId: String,
    val preOrderId: String,
    val catId: String,
    val catName: String,
    val quantity: Int = 1,
    val estimatedGrossWt: Double = 0.0,
    val estimatedPrice: Double = 0.0,
    val addDesKey: String = "",
    val addDesValue: String = "",
    val note: String? = null,
)

