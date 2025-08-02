package com.velox.jewelvault.data.roomdb.dao
import androidx.room.*
import com.velox.jewelvault.data.roomdb.entity.UsersEntity
import com.velox.jewelvault.data.roomdb.entity.StoreEntity
import com.velox.jewelvault.data.roomdb.entity.CategoryEntity
import com.velox.jewelvault.data.roomdb.entity.SubCategoryEntity
import com.velox.jewelvault.data.roomdb.entity.ItemEntity
import com.velox.jewelvault.data.roomdb.TableNames

@Dao
interface MasterDao {

    // Deleting a user should delete all associated stores, categories, subcategories, and items
    @Transaction
    @Query("DELETE FROM ${TableNames.USERS} WHERE userId = :userId")
    suspend fun deleteUserAndRelatedData(userId: String)

    // Deleting all stores associated with a user
    @Transaction
    @Query("DELETE FROM ${TableNames.STORE} WHERE userId = :userId")
    suspend fun deleteStoresByUser(userId: String)

    // Deleting all categories associated with a store
    @Transaction
    @Query("DELETE FROM ${TableNames.CATEGORY} WHERE storeId = :storeId")
    suspend fun deleteCategoriesByStore(storeId: String)

    // Deleting all subcategories associated with a category
    @Transaction
    @Query("DELETE FROM ${TableNames.SUB_CATEGORY} WHERE catId = :catId")
    suspend fun deleteSubCategoriesByCategory(catId: String)

    // Deleting all items associated with a subcategory
    @Transaction
    @Query("DELETE FROM ${TableNames.ITEM} WHERE subCatId = :subCatId")
    suspend fun deleteItemsBySubCategory(subCatId: String)

    // Deleting all items associated with a category
    @Transaction
    @Query("DELETE FROM ${TableNames.ITEM} WHERE catId = :catId")
    suspend fun deleteItemsByCategory(catId: String)

    // Deleting all items associated with a store
    @Transaction
    @Query("DELETE FROM ${TableNames.ITEM} WHERE storeId = :storeId")
    suspend fun deleteItemsByStore(storeId: String)
}
