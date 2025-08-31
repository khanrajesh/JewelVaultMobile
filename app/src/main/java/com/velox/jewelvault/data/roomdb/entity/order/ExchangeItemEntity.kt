package com.velox.jewelvault.data.roomdb.entity.order

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.velox.jewelvault.data.roomdb.TableNames
import java.sql.Timestamp

@Entity(
    tableName = TableNames.EXCHANGE_ITEM,
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["orderId"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ExchangeItemEntity(
    @PrimaryKey
    val exchangeItemId: String,
    val orderId: String,
    val orderDate: Timestamp,
    val customerMobile: String,
    val metalType: String, // "Gold", "Silver", etc.
    val purity: String, // Using existing PurityType values
    val grossWeight: Double,
    val fineWeight: Double,
    val price: Double,
    val isExchangedByMetal: Boolean, // true = by metal rate, false = by price
    val exchangeValue: Double, // Calculated value based on exchange method
    val addDate: Timestamp = Timestamp(System.currentTimeMillis())
)


