package com.velox.jewelvault.data.roomdb.entity.preorder

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.velox.jewelvault.data.roomdb.TableNames
import com.velox.jewelvault.data.roomdb.entity.customer.CustomerEntity
import java.sql.Timestamp

@Entity(
    tableName = TableNames.PRE_ORDER,
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["mobileNo"],
            childColumns = ["customerMobile"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("customerMobile")
    ]
)
data class PreOrderEntity(
    @PrimaryKey
    val preOrderId: String,
    val customerMobile: String,
    val storeId: String,
    val userId: String,
    val orderDate: Timestamp,
    val deliveryDate: Timestamp,
    val status: String = "CONFIRMED",
    val note: String? = null,
    val createdAt: Timestamp,
    val updatedAt: Timestamp,
)

