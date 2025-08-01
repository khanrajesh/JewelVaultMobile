package com.velox.jewelvault.data.roomdb.entity.order

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.velox.jewelvault.data.roomdb.TableNames
import java.sql.Timestamp


@Entity(tableName = TableNames.ORDER)
data class OrderEntity(
    @PrimaryKey
    val orderId: String,
    val customerMobile: String,
    val storeId: String,
    val userId: String,
    val orderDate: Timestamp,
    val totalAmount: Double = 0.0,
    val totalTax: Double = 0.0,
    val totalCharge: Double = 0.0,
    val note: String? = null
)

