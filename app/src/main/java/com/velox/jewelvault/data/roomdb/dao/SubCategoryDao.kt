package com.velox.jewelvault.data.roomdb.dao


import androidx.room.*
import com.velox.jewelvault.data.roomdb.entity.SubCategoryEntity

@Dao
interface SubCategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubCategory(subCategory: SubCategoryEntity): Long
    // Returns new row ID

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSubCategories(subCategories: List<SubCategoryEntity>): List<Long>

    @Update
    suspend fun updateSubCategory(subCategory: SubCategoryEntity): Int
    // Number of rows updated

    @Delete
    suspend fun deleteSubCategory(subCategory: SubCategoryEntity): Int
    // Number of rows deleted

    @Query("SELECT * FROM SubCategoryEntity WHERE userId = :userId")
    suspend fun getSubCategoriesByUserId(userId: Int): List<SubCategoryEntity>

    @Query("SELECT * FROM SubCategoryEntity WHERE catId = :catId")
    suspend fun getSubCategoriesByCatId(catId: Int): List<SubCategoryEntity>

    @Query("SELECT * FROM SubCategoryEntity WHERE subCatId = :subCatId")
    suspend fun getSubCategoryById(subCatId: Int): SubCategoryEntity?

    @Query("UPDATE SubCategoryEntity SET gsWt = :gsWt, fnWt = :fnWt WHERE subCatId = :subCatId")
    suspend fun updateWeights(subCatId: Int, gsWt: Double, fnWt: Double): Int

    @Query("UPDATE SubCategoryEntity SET quantity = :quantity WHERE subCatId = :subCatId")
    suspend fun updateQuantity(subCatId: Int, quantity: Int): Int

    @Query(
        """
    UPDATE SubCategoryEntity 
    SET gsWt = :gsWt, fnWt = :fnWt, quantity = :quantity 
    WHERE subCatId = :subCatId
    """
    )
    suspend fun updateWeightsAndQuantity(
        subCatId: Int,
        gsWt: Double,
        fnWt: Double,
        quantity: Int
    ): Int

    @Query("DELETE FROM SubCategoryEntity")
    suspend fun deleteAllSubCategories(): Int


    @Query("""
    SELECT COUNT(*)
    FROM SubCategoryEntity""")
    suspend fun getSubCategoryCount(): Int
}
