package com.velox.jewelvault.data.roomdb.entity.order

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.sql.Timestamp


@Entity
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val orderId: Long = 0,
    val customerMobile: String,
    val storeId: Int,
    val userId: Int,
    val orderDate: Timestamp,
    val totalAmount: Double = 0.0,
    val totalTax: Double = 0.0,
    val totalCharge: Double = 0.0,
    val note: String? = null
)

