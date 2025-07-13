package com.velox.jewelvault.data.roomdb.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery
import com.velox.jewelvault.data.roomdb.entity.ItemEntity
import kotlinx.coroutines.flow.Flow
import java.sql.Timestamp

@Dao
interface ItemDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(item: ItemEntity): Long

    @Query("SELECT * FROM ItemEntity ORDER BY addDate DESC")
    fun getAll(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM ItemEntity WHERE userId = :userId AND storeId = :storeId ORDER BY addDate DESC")
    fun getAllItemsByUserIdAndStoreId(userId: Int, storeId: Int): List<ItemEntity>

    @Query("SELECT * FROM ItemEntity WHERE purchaseOrderId = :purchaseOrderId ORDER BY addDate DESC")
    fun getItemByPurchaseOrderId(purchaseOrderId: Int): Flow<List<ItemEntity>>

    // ✅ Enhanced Filter by any combination of parameters with ranges and search
    @Query(
        """
        SELECT * FROM ItemEntity
        WHERE (:catId IS NULL OR catId = :catId)
          AND (:subCatId IS NULL OR subCatId = :subCatId)
          AND (:type IS NULL OR entryType = :type)
          AND (:purity IS NULL OR purity = :purity)
          AND (:crgType IS NULL OR crgType = :crgType)
          AND (:startDate IS NULL OR addDate >= :startDate)
          AND (:endDate IS NULL OR addDate <= :endDate)
          AND (:minGsWt IS NULL OR gsWt >= :minGsWt)
          AND (:maxGsWt IS NULL OR gsWt <= :maxGsWt)
          AND (:minNtWt IS NULL OR ntWt >= :minNtWt)
          AND (:maxNtWt IS NULL OR ntWt <= :maxNtWt)
          AND (:minFnWt IS NULL OR fnWt >= :minFnWt)
          AND (:maxFnWt IS NULL OR fnWt <= :maxFnWt)
          AND (:minQuantity IS NULL OR quantity >= :minQuantity)
          AND (:maxQuantity IS NULL OR quantity <= :maxQuantity)
          AND (:firmId IS NULL OR sellerFirmId = :firmId)
          AND (:purchaseOrderId IS NULL OR purchaseOrderId = :purchaseOrderId)
        ORDER BY addDate DESC
    """
    )
    fun filterItems(
        catId: Int? = null,
        subCatId: Int? = null,
        type: String? = null,
        purity: String? = null,
        crgType: String? = null,
        startDate: Timestamp? = null,
        endDate: Timestamp? = null,
        minGsWt: Double? = null,
        maxGsWt: Double? = null,
        minNtWt: Double? = null,
        maxNtWt: Double? = null,
        minFnWt: Double? = null,
        maxFnWt: Double? = null,
        minQuantity: Int? = null,
        maxQuantity: Int? = null,
        firmId: Int? = null,
        purchaseOrderId: Int? = null
    ): Flow<List<ItemEntity>>

    // ✅ Get recent items (last 7 days)
    @Query("SELECT * FROM ItemEntity WHERE addDate >= datetime('now', '-7 days') ORDER BY addDate DESC")
    fun getRecentItems(): Flow<List<ItemEntity>>

    // ✅ Get items by date range
    @Query("SELECT * FROM ItemEntity WHERE addDate BETWEEN :startDate AND :endDate ORDER BY addDate DESC")
    fun getItemsByDateRange(startDate: Timestamp, endDate: Timestamp): Flow<List<ItemEntity>>

    // ✅ Search items by HUID
    @Query("SELECT * FROM ItemEntity WHERE huid LIKE '%' || :huid || '%' ORDER BY addDate DESC")
    fun searchItemsByHUID(huid: String): Flow<List<ItemEntity>>

    // ✅ Search items by name
    @Query("SELECT * FROM ItemEntity WHERE itemAddName LIKE '%' || :name || '%' ORDER BY addDate DESC")
    fun searchItemsByName(name: String): Flow<List<ItemEntity>>

    @RawQuery(observedEntities = [ItemEntity::class])
    fun filterItems(query: SupportSQLiteQuery): Flow<List<ItemEntity>>

    // ✅ Update quantity (especially when type is "lot")
    @Query(
        """
        UPDATE ItemEntity
        SET quantity = :newQuantity,
            modifiedDate = :modifiedDate
        WHERE itemId = :itemId AND entryType = 'lot'
    """
    )
    suspend fun updateQuantityIfLot(itemId: Int, newQuantity: Int, modifiedDate: Timestamp)

    @Query(
        """
    SELECT * FROM ItemEntity 
    WHERE  catId = :catId AND subCatName = 'Fine' 
    LIMIT 1
"""
    )
    suspend fun getFineItemByCat(catId: Int): ItemEntity?

    @Query("DELETE FROM ItemEntity WHERE itemId = :itemId")
    suspend fun deleteById(itemId: Int)

    @Query("DELETE FROM ItemEntity WHERE itemId = :itemId AND catId = :catId AND subCatId = :subCatId")
    suspend fun deleteById(itemId: Int, catId: Int, subCatId: Int): Int

    // ✅ Delete all (optional utility)
    @Query("DELETE FROM ItemEntity")
    suspend fun deleteAll()

    // ✅ Get by ID
    @Query("SELECT * FROM ItemEntity WHERE itemId = :itemId")
    suspend fun getItemById(itemId: Int): ItemEntity?

    // ✅ Update item (full object update)
    @Update
    suspend fun updateItem(item: ItemEntity): Int
}
