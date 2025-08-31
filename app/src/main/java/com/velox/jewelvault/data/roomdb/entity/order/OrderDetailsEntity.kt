package com.velox.jewelvault.data.roomdb.entity.order

import androidx.room.Embedded
import androidx.room.Relation

//OrderWithItemsAndExchanges
data class OrderDetailsEntity(
    @Embedded val order: OrderEntity,
    @Relation(
        parentColumn = "orderId",
        entityColumn = "orderId"
    )
    val items: List<OrderItemEntity>,
    @Relation(
        parentColumn = "orderId",
        entityColumn = "orderId"
    )
    val exchangeItems: List<ExchangeItemEntity>
)