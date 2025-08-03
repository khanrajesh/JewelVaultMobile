package com.velox.jewelvault.data.roomdb.entity.category

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.velox.jewelvault.data.roomdb.TableNames

@Entity(tableName = TableNames.SUB_CATEGORY)
data class SubCategoryEntity(
    @PrimaryKey val subCatId: String ,
    val catId: String,
    val userId: String,
    val storeId:String,
    val catName: String,
    val subCatName: String,
    val quantity: Int = 0,
    val gsWt: Double = 0.0,
    val fnWt: Double = 0.0,
)
