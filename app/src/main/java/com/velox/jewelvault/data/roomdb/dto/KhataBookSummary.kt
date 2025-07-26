package com.velox.jewelvault.data.roomdb.dto

data class KhataBookSummary(
    val khataBookId: Int,
    val customerMobile: String,
    val planName: String,
    val monthlyAmount: Double,
    val totalMonths: Int,
    val status: String,
    val remainingMonths: Int,
    val remainingAmount: Double
)