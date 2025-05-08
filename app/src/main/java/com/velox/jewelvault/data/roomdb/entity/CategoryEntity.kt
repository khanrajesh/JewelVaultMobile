package com.velox.jewelvault.data.roomdb.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val catId: Int = 0,  // Auto-increment primary key
    val catName: String,
    val gsWt: Double,
    val fnWt: Double,
    val userId:Int,
    val storeId:Int,

)
