package com.velox.jewelvault.data.roomdb.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.sql.Timestamp

@Entity
data class SubCategoryEntity(
    @PrimaryKey(autoGenerate = true) val subCatId: Int = 0,  // Auto-increment primary key
    val catId: Int,
    val userId: Int,
    val storeId:Int,
    val catName: String,
    val subCatName: String,
    val quantity: Int,
    val gsWt: Double,
    val fnWt: Double,
)
