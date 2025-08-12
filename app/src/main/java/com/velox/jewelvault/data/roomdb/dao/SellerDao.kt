package com.velox.jewelvault.data.roomdb.dao

import androidx.room.*
import com.velox.jewelvault.data.roomdb.entity.purchase.SellerEntity
import com.velox.jewelvault.data.roomdb.TableNames
import kotlinx.coroutines.flow.Flow

@Dao
interface SellerDao {
    
    @Query("SELECT * FROM ${TableNames.SELLER}")
    suspend fun getAllSellers(): List<SellerEntity>
    
    @Query("SELECT * FROM ${TableNames.SELLER} WHERE sellerId = :sellerId")
    suspend fun getSellerById(sellerId: String): SellerEntity?
    
    @Query("SELECT * FROM ${TableNames.SELLER} WHERE name = :sellerName AND mobileNumber = :mobileNumber")
    suspend fun getSellerByNameAndMobile(sellerName: String, mobileNumber: String): SellerEntity?
    
    @Query("SELECT * FROM ${TableNames.SELLER} WHERE name LIKE '%' || :searchQuery || '%'")
    suspend fun searchSellers(searchQuery: String): List<SellerEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeller(seller: SellerEntity)
    
    @Update
    suspend fun updateSeller(seller: SellerEntity)
    
    @Delete
    suspend fun deleteSeller(seller: SellerEntity)
    
    @Query("DELETE FROM ${TableNames.SELLER} WHERE sellerId = :sellerId")
    suspend fun deleteSellerById(sellerId: String)
    
    @Query("SELECT COUNT(*) FROM ${TableNames.SELLER}")
    suspend fun getSellerCount(): Int
}
