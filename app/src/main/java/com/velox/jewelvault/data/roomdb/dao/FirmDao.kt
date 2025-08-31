package com.velox.jewelvault.data.roomdb.dao

import androidx.room.*
import com.velox.jewelvault.data.roomdb.entity.purchase.FirmEntity
import com.velox.jewelvault.data.roomdb.TableNames
import kotlinx.coroutines.flow.Flow

@Dao
interface FirmDao {
    
    @Query("SELECT * FROM ${TableNames.FIRM}")
    suspend fun getAllFirms(): List<FirmEntity>
    
    @Query("SELECT * FROM ${TableNames.FIRM} WHERE firmId = :firmId")
    suspend fun getFirmById(firmId: String): FirmEntity?
    
    @Query("SELECT * FROM ${TableNames.FIRM} WHERE firmName = :firmName AND firmMobileNumber = :mobileNumber")
    suspend fun getFirmByNameAndMobile(firmName: String, mobileNumber: String): FirmEntity?
    
    @Query("SELECT * FROM ${TableNames.FIRM} WHERE firmName LIKE '%' || :searchQuery || '%'")
    suspend fun searchFirms(searchQuery: String): List<FirmEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFirm(firm: FirmEntity)
    
    @Update
    suspend fun updateFirm(firm: FirmEntity)
    
    @Delete
    suspend fun deleteFirm(firm: FirmEntity)
    
    @Query("DELETE FROM ${TableNames.FIRM} WHERE firmId = :firmId")
    suspend fun deleteFirmById(firmId: String)
    
    @Query("SELECT COUNT(*) FROM ${TableNames.FIRM}")
    suspend fun getFirmCount(): Int
}
