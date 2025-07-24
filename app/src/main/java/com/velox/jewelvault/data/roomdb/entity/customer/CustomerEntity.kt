package com.velox.jewelvault.data.roomdb.entity.customer

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.sql.Timestamp

@Entity(tableName = "customer_entity")
data class CustomerEntity(
    @PrimaryKey val mobileNo: String,
    val name: String,
    val address: String? = null,
    val gstin_pan: String? = null,
    val addDate: Timestamp,
    val lastModifiedDate: Timestamp,
    val totalItemBought: Int = 0,
    val totalAmount: Double = 0.0,
    val notes: String? = null,
    val isActive: Boolean = true,
    val userId: Int = 0,
    val storeId: Int = 0
)