package com.velox.jewelvault.data.roomdb.entity.customer

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.velox.jewelvault.data.roomdb.TableNames

@Entity(
    tableName = TableNames.CUSTOMER_KHATA_BOOK_PLAN,
    indices = [Index(value = ["userId", "storeId"])]
)
data class CustomerKhataBookPlanEntity(
    @PrimaryKey val planId: String,
    val name: String,
    val payMonths: Int,
    val benefitMonths: Int,
    val description: String,
    val benefitPercentage: Double,
    val userId: String,
    val storeId: String,
    val createdAt: Long,
    val updatedAt: Long
)
