package com.velox.jewelvault.data.roomdb.dto

import androidx.room.Embedded
import androidx.room.Relation
import com.velox.jewelvault.data.roomdb.entity.purchase.MetalExchangeEntity
import com.velox.jewelvault.data.roomdb.entity.purchase.PurchaseOrderEntity
import com.velox.jewelvault.data.roomdb.entity.purchase.PurchaseOrderItemEntity
import com.velox.jewelvault.data.roomdb.entity.purchase.FirmEntity
import com.velox.jewelvault.data.roomdb.entity.purchase.SellerEntity

data class PurchaseOrderWithDetails(
    @Embedded val order: PurchaseOrderEntity,

    @Relation(
        parentColumn = "purchaseOrderId",
        entityColumn = "purchaseOrderId"
    )
    val items: List<PurchaseOrderItemEntity> = emptyList(),

    @Relation(
        parentColumn = "purchaseOrderId",
        entityColumn = "purchaseOrderId"
    )
    val exchanges: List<MetalExchangeEntity> = emptyList(),

    @Relation(
        parentColumn = "sellerId",
        entityColumn = "sellerId"
    )
    val seller: SellerEntity? = null
)
