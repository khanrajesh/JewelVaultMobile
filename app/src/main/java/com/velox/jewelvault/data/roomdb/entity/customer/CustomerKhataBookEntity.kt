package com.velox.jewelvault.data.roomdb.entity.customer

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.velox.jewelvault.data.roomdb.TableNames
import java.sql.Timestamp

@Entity(
    tableName = TableNames.CUSTOMER_KHATA_BOOK,
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["mobileNo"],
            childColumns = ["customerMobile"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CustomerKhataBookEntity(
    @PrimaryKey
    val khataBookId:String,
    val customerMobile: String,
    val planName: String, // Name of the khata book plan
    val startDate: Timestamp,
    val endDate: Timestamp,
    val monthlyAmount: Double,
    val totalMonths: Int, // Usually 12 months
    val totalAmount: Double, // monthlyAmount * totalMonths
    val status: String, // "active", "completed", "cancelled"
    val notes: String? = null,
    val userId: String,
    val storeId: String
) {
    // Helper properties
    val isActive: Boolean
        get() = status == "active"
    
    val isCompleted: Boolean
        get() = status == "completed"
    
    val isCancelled: Boolean
        get() = status == "cancelled"
} 