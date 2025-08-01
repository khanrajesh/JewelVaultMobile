package com.velox.jewelvault.data.roomdb.dao


import androidx.room.*
import com.velox.jewelvault.data.roomdb.entity.SubCategoryEntity
import com.velox.jewelvault.data.roomdb.TableNames

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



    @Query("SELECT * FROM ${TableNames.SUB_CATEGORY} WHERE catId = :catId AND subCatName = :subCatName LIMIT 1")
    suspend fun getSubCategoryByName(catId: String, subCatName: String): SubCategoryEntity?

    @Query("SELECT * FROM ${TableNames.SUB_CATEGORY} WHERE userId = :userId")
    suspend fun getSubCategoriesByUserId(userId: String): List<SubCategoryEntity>

    @Query("SELECT * FROM ${TableNames.SUB_CATEGORY} WHERE catId = :catId")
    suspend fun getSubCategoriesByCatId(catId: String): List<SubCategoryEntity>

    @Query("SELECT * FROM ${TableNames.SUB_CATEGORY} WHERE subCatId = :subCatId")
    suspend fun getSubCategoryById(subCatId: String): SubCategoryEntity?

    @Query("UPDATE ${TableNames.SUB_CATEGORY} SET gsWt = :gsWt, fnWt = :fnWt WHERE subCatId = :subCatId")
    suspend fun updateWeights(subCatId: String, gsWt: Double, fnWt: Double): Int

    @Query("UPDATE  ${TableNames.SUB_CATEGORY} SET quantity = :quantity WHERE subCatId = :subCatId")
    suspend fun updateQuantity(subCatId: String, quantity: Int): Int

    @Query(
        """
    UPDATE  ${TableNames.SUB_CATEGORY} 
    SET gsWt = :gsWt, fnWt = :fnWt, quantity = :quantity 
    WHERE subCatId = :subCatId
    """
    )
    suspend fun updateWeightsAndQuantity(
        subCatId: String,
        gsWt: Double,
        fnWt: Double,
        quantity: Int
    ): Int

    @Query("DELETE FROM  ${TableNames.SUB_CATEGORY}")
    suspend fun deleteAllSubCategories(): Int
    
    @Query("SELECT * FROM  ${TableNames.SUB_CATEGORY}")
    suspend fun getAllSubCategories(): List<SubCategoryEntity>


    @Query("""
    SELECT COUNT(*)
    FROM  ${TableNames.SUB_CATEGORY}""")
    suspend fun getSubCategoryCount(): Int
}
