package com.velox.jewelvault.data.roomdb.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.velox.jewelvault.data.roomdb.TableNames
import com.velox.jewelvault.utils.generateId

@Entity(tableName = TableNames.STORE)
data class StoreEntity (
    @PrimaryKey val storeId: String,  // Auto-increment primary key
    val userId: String,
    val proprietor: String,
    val name: String,
    val email: String, // <-- fixed typo here
    val phone: String,
    val address: String,
    val registrationNo:String,
    val gstinNo:String,
    val panNo:String,
    val image:String,
    val invoiceNo:Int=0,
    val upiId:String="",
)