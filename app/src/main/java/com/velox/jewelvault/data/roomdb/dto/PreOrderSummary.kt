package com.velox.jewelvault.data.roomdb.dto

import java.sql.Timestamp

data class PreOrderSummary(
    val preOrderId: String,
    val orderDate: Timestamp,
    val deliveryDate: Timestamp,
    val status: String,
    val customerMobile: String,
    val customerName: String? = null,
    val categories: String? = null,
    val totalQuantity: Int = 0,
    val estimatedWeight: Double = 0.0,
    val estimatedPrice: Double = 0.0,
    val advanceAmount: Double = 0.0,
)
