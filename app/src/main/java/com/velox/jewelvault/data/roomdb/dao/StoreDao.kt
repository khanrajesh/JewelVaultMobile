package com.velox.jewelvault.data.roomdb.dao

import androidx.room.*
import com.velox.jewelvault.data.roomdb.entity.StoreEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StoreDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStore(store: StoreEntity): Long
    // Returns new row ID

    @Update
    suspend fun updateStore(store: StoreEntity): Int
    // Returns number of rows updated

    @Delete
    suspend fun deleteStore(store: StoreEntity): Int
    // Returns number of rows deleted

    @Query("SELECT * FROM StoreEntity WHERE storeId = :storeId")
    suspend fun getStoreById(storeId: Int): StoreEntity?

    @Query("SELECT * FROM StoreEntity WHERE userId = :userId")
    fun getStoresByUserId(userId: Int): Flow<List<StoreEntity>>
    // Returns Flow for real-time observation

    @Query("DELETE FROM StoreEntity")
    suspend fun deleteAllStores(): Int
    
    @Query("SELECT * FROM StoreEntity")
    suspend fun getAllStores(): List<StoreEntity>

    @Query("""
        UPDATE StoreEntity SET
            proprietor = :proprietor,
            phone = :phone,
            address = :address,
            registrationNo = :registrationNo,
            gstinNo = :gstinNo,
            panNo = :panNo,
            image = :image
        WHERE storeId = :storeId
    """)
    suspend fun updateStoreDetails(
        storeId: Int,
        proprietor: String,
        phone: String,
        address: String,
        registrationNo: String,
        gstinNo: String,
        panNo: String,
        image: String
    ): Int
    // Returns number of rows updated
}
