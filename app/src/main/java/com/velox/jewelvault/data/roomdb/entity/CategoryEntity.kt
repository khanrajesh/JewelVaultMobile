package com.velox.jewelvault.data.roomdb.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.velox.jewelvault.data.roomdb.TableNames
import com.velox.jewelvault.utils.generateId

@Entity(tableName = TableNames.CATEGORY)
data class CategoryEntity(
    @PrimaryKey val catId: String,
    val catName: String,
    val gsWt: Double=0.0,
    val fnWt: Double=0.0,
    val userId:String,
    val storeId:String,
)
