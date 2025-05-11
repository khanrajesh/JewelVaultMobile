package com.velox.jewelvault.data.roomdb.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val catId: Int = 0,
    val catName: String,
    val gsWt: Double=0.0,
    val fnWt: Double=0.0,
    val userId:Int,
    val storeId:Int,
)
