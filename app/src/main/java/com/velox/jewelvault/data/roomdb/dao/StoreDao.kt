package com.velox.jewelvault.data.roomdb.dao

import androidx.room.*
import com.velox.jewelvault.data.roomdb.entity.StoreEntity
import com.velox.jewelvault.data.roomdb.TableNames
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

    @Query("SELECT * FROM ${TableNames.STORE} WHERE storeId = :storeId")
    suspend fun getStoreById(storeId: String): StoreEntity?

    @Query("SELECT * FROM ${TableNames.STORE} WHERE userId = :userId")
    fun getStoresByUserId(userId: String): Flow<List<StoreEntity>>
    // Returns Flow for real-time observation

    @Query("DELETE FROM ${TableNames.STORE}")
    suspend fun deleteAllStores()
    
    @Query("SELECT * FROM ${TableNames.STORE}")
    suspend fun getAllStores(): List<StoreEntity>

    @Query("""
        UPDATE ${TableNames.STORE} SET
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
        storeId: String,
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
