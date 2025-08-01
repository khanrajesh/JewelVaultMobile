package com.velox.jewelvault.data.roomdb.entity.order

import androidx.room.Embedded
import androidx.room.Relation


data class OrderWithItems(
    @Embedded val order: OrderEntity,
    @Relation(
        parentColumn = "orderId",
        entityColumn = "orderId"
    )
    val items: List<OrderItemEntity>
)
