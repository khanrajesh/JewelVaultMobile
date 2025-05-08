package com.velox.jewelvault.data.roomdb.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.velox.jewelvault.data.roomdb.entity.ItemEntity
import kotlinx.coroutines.flow.Flow
import java.sql.Timestamp

@Dao
interface ItemDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(item: ItemEntity):Long

    @Query("SELECT * FROM ItemEntity ORDER BY addDate DESC")
    fun getAll(): Flow<List<ItemEntity>>

    // ✅ Filter by any combination of parameters (nullable allows for optional filters)
    @Query("""
        SELECT * FROM ItemEntity
        WHERE (:catId IS NULL OR catId = :catId)
          AND (:subCatId IS NULL OR subCatId = :subCatId)
          AND (:type IS NULL OR type = :type)
          AND (:purity IS NULL OR purity = :purity)
          AND (:crgType IS NULL OR crgType = :crgType)
          AND (:startDate IS NULL OR addDate >= :startDate)
          AND (:endDate IS NULL OR addDate <= :endDate)
    """)
    fun filterItems(
        catId: Int? = null,
        subCatId: Int? = null,
        type: String? = null,
        purity: String? = null,
        crgType: String? = null,
        startDate: Timestamp? = null,
        endDate: Timestamp? = null
    ): Flow<List<ItemEntity>>

    // ✅ Update quantity (especially when type is "lot")
    @Query("""
        UPDATE ItemEntity
        SET quantity = :newQuantity,
            modifiedDate = :modifiedDate
        WHERE itemId = :itemId AND type = 'lot'
    """)
    suspend fun updateQuantityIfLot(itemId: Int, newQuantity: Int, modifiedDate: Timestamp)

    // ✅ Delete by orderId
    @Query("DELETE FROM ItemEntity WHERE itemId = :itemId")
    suspend fun deleteById(itemId: Int)

    // ✅ Delete all (optional utility)
    @Query("DELETE FROM ItemEntity")
    suspend fun deleteAll()

    // ✅ Get by ID
    @Query("SELECT * FROM ItemEntity WHERE itemId = :itemId")
    suspend fun getItemById(itemId: Int): ItemEntity?

    // ✅ Update item (full object update)
    @Update
    suspend fun updateItem(item: ItemEntity)
}
