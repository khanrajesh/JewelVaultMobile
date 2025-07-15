package com.velox.jewelvault.data.roomdb.dto

import java.sql.Timestamp

data class CustomerSummaryDto(
    val mobileNo: String,
    val name: String,
    val address: String?,
    val gstin_pan: String?,
    val addDate: Timestamp,
    val totalItemBought: Int,
    val totalAmount: Double,
    val lastOrderDate: Timestamp?,
    val totalOrders: Int,
    val isActive: Boolean,
    // Outstanding balance calculated from CustomerTransactionEntity
    val outstandingBalance: Double = 0.0,
    val lastOutstandingPaymentDate: Timestamp? = null,
    // Khata book info - now supports multiple active plans
    val activeKhataBookCount: Int = 0,
    val totalKhataBookAmount: Double = 0.0,
    val totalKhataBookPaidAmount: Double = 0.0,
    val totalKhataBookRemainingAmount: Double = 0.0,
    val khataBookStatus: String = "none" // "none", "active", "completed"
) {
    val hasOutstandingBalance: Boolean
        get() = outstandingBalance > 0
    
    val hasKhataBook: Boolean
        get() = activeKhataBookCount > 0 && khataBookStatus == "active"
    
    val khataBookProgress: Double
        get() = if (totalKhataBookAmount > 0) totalKhataBookPaidAmount / totalKhataBookAmount else 0.0
} 