package com.velox.jewelvault.data.roomdb.dto

import androidx.compose.runtime.snapshots.SnapshotStateList
import com.velox.jewelvault.data.roomdb.entity.SubCategoryEntity


data class CatSubCatDto(
    val catId: Int,
    val catName: String,
    val gsWt: Double,
    val fnWt: Double,
    val userId: Int,
    val storeId: Int,
    val subCategoryList: SnapshotStateList<SubCategoryEntity> = SnapshotStateList()
)
