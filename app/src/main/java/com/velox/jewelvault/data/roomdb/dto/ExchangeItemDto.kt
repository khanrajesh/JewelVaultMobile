package com.velox.jewelvault.data.roomdb.dto

import com.velox.jewelvault.data.roomdb.entity.order.ExchangeItemEntity
import java.sql.Timestamp

data class ExchangeItemDto(
    val exchangeItemId: String = "",
    val orderId: String = "",
    val orderDate: Timestamp = Timestamp(System.currentTimeMillis()),
    val customerMobile: String = "",
    val metalType: String = "",
    val purity: String = "",
    val grossWeight: Double = 0.0,
    val fineWeight: Double = 0.0,
    val price: Double = 0.0,
    val isExchangedByMetal: Boolean = true,
    val exchangeValue: Double = 0.0,
    val addDate: Timestamp = Timestamp(System.currentTimeMillis())
) {
    fun toEntity(): ExchangeItemEntity {
        return ExchangeItemEntity(
            exchangeItemId = exchangeItemId,
            orderId = orderId,
            orderDate = orderDate,
            customerMobile = customerMobile,
            metalType = metalType,
            purity = purity,
            grossWeight = grossWeight,
            fineWeight = fineWeight,
            price = price,
            isExchangedByMetal = isExchangedByMetal,
            exchangeValue = exchangeValue,
            addDate = addDate
        )
    }
    
    companion object {
        fun fromEntity(entity: ExchangeItemEntity): ExchangeItemDto {
            return ExchangeItemDto(
                exchangeItemId = entity.exchangeItemId,
                orderId = entity.orderId,
                orderDate = entity.orderDate,
                customerMobile = entity.customerMobile,
                metalType = entity.metalType,
                purity = entity.purity,
                grossWeight = entity.grossWeight,
                fineWeight = entity.fineWeight,
                price = entity.price,
                isExchangedByMetal = entity.isExchangedByMetal,
                exchangeValue = entity.exchangeValue,
                addDate = entity.addDate
            )
        }
    }
}
