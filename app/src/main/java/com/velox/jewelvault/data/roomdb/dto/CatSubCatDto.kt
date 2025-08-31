package com.velox.jewelvault.data.roomdb.dto

import androidx.compose.runtime.snapshots.SnapshotStateList
import com.velox.jewelvault.data.roomdb.entity.category.SubCategoryEntity


data class CatSubCatDto(
    val catId: String,
    val catName: String,
    val gsWt: Double,
    val fnWt: Double,
    val userId: String,
    val storeId: String,
    val subCategoryList: SnapshotStateList<SubCategoryEntity> = SnapshotStateList()
)
