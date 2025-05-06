package com.velox.jewelvault.data.roomdb.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.velox.jewelvault.data.roomdb.entity.ItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(order: ItemEntity)

    @Query("SELECT * FROM `ItemEntity`")
    fun getAll(): Flow<List<ItemEntity>>
}