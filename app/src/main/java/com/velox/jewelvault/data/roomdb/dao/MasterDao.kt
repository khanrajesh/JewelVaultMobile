package com.velox.jewelvault.data.roomdb.dao
import androidx.room.*
import com.velox.jewelvault.data.roomdb.entity.UsersEntity
import com.velox.jewelvault.data.roomdb.entity.StoreEntity
import com.velox.jewelvault.data.roomdb.entity.CategoryEntity
import com.velox.jewelvault.data.roomdb.entity.SubCategoryEntity
import com.velox.jewelvault.data.roomdb.entity.ItemEntity

@Dao
interface MasterDao {

    // Deleting a user should delete all associated stores, categories, subcategories, and items
    @Transaction
    @Query("DELETE FROM UsersEntity WHERE id = :userId")
    suspend fun deleteUserAndRelatedData(userId: Int)

    // Deleting all stores associated with a user
    @Transaction
    @Query("DELETE FROM StoreEntity WHERE userId = :userId")
    suspend fun deleteStoresByUser(userId: Int)

    // Deleting all categories associated with a store
    @Transaction
    @Query("DELETE FROM CategoryEntity WHERE storeId = :storeId")
    suspend fun deleteCategoriesByStore(storeId: Int)

    // Deleting all subcategories associated with a category
    @Transaction
    @Query("DELETE FROM SubCategoryEntity WHERE catId = :catId")
    suspend fun deleteSubCategoriesByCategory(catId: Int)

    // Deleting all items associated with a subcategory
    @Transaction
    @Query("DELETE FROM ItemEntity WHERE subCatId = :subCatId")
    suspend fun deleteItemsBySubCategory(subCatId: Int)

    // Deleting all items associated with a category
    @Transaction
    @Query("DELETE FROM ItemEntity WHERE catId = :catId")
    suspend fun deleteItemsByCategory(catId: Int)

    // Deleting all items associated with a store
    @Transaction
    @Query("DELETE FROM ItemEntity WHERE storeId = :storeId")
    suspend fun deleteItemsByStore(storeId: Int)
}
