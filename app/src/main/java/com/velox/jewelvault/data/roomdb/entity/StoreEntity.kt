package com.velox.jewelvault.data.roomdb.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class StoreEntity (
    @PrimaryKey(autoGenerate = true) val storeId: Int = 0,  // Auto-increment primary key
    val userId: Int,
    val proprietor: String,
    val name: String,
    val eamil: String,
    val phone: String,
    val address: String,
    val registrationNo:String,
    val gstinNo:String,
    val panNo:String,
    val image:String,
    val invoiceNo:Int=0,
)