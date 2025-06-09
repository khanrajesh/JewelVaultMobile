package com.velox.jewelvault.data.roomdb.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.velox.jewelvault.data.roomdb.entity.CategoryEntity


@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCategory(category: CategoryEntity): Long
    // Returns new row ID if successful, or -1 if failed

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAllCategories(categories: List<CategoryEntity>): List<Long>
    // List of row IDs

    @Update
    suspend fun updateCategory(category: CategoryEntity): Int
    // Returns number of rows updated (0 if failed)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity): Int
    // Returns number of rows deleted

    @Query("SELECT * FROM CategoryEntity WHERE userId = :userId AND storeId = :storeId")
    suspend fun getCategoriesByUserIdAndStoreId(userId: Int, storeId:Int): List<CategoryEntity>

    @Query("SELECT * FROM CategoryEntity WHERE userId = :userId")
    suspend fun getCategoriesByUserId(userId: Int): List<CategoryEntity>
    // Empty list on failure or no data

    @Query("SELECT * FROM CategoryEntity WHERE catId = :catId")
    suspend fun getCategoryById(catId: Int): CategoryEntity?

    @Query("SELECT * FROM CategoryEntity WHERE catName = :catName")
    suspend fun getCategoryByName(catName: String): CategoryEntity?
    // Returns null if not found

    @Query("DELETE FROM CategoryEntity")
    suspend fun deleteAllCategories(): Int
    // Returns number of rows deleted

    @Query("UPDATE CategoryEntity SET gsWt = :gsWt, fnWt = :fnWt WHERE catId = :catId")
    suspend fun updateWeights(catId: Int, gsWt: Double, fnWt: Double): Int
    // Returns number of rows updated
}
