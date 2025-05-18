package com.velox.jewelvault.data.roomdb.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.sql.Timestamp

@Entity
data class CustomerEntity(
    @PrimaryKey val mobileNo: String,
    val name: String,
    val address: String? = null,
    val gstin_pan: String? = null,
    val addDate: Timestamp,
)