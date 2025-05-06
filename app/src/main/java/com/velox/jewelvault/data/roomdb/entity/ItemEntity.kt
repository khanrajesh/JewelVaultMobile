package com.velox.jewelvault.data.roomdb.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val orderId: Int = 0,
    val productId: Int,
    val quantity: Int,
    val discount: Double
)
