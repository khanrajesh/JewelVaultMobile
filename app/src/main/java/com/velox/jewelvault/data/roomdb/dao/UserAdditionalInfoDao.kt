package com.velox.jewelvault.data.roomdb.dao

import androidx.room.*
import com.velox.jewelvault.data.roomdb.entity.users.UserAdditionalInfoEntity
import com.velox.jewelvault.data.roomdb.TableNames

@Dao
interface UserAdditionalInfoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserAdditionalInfo(userInfo: UserAdditionalInfoEntity): Long

    @Update
    suspend fun updateUserAdditionalInfo(userInfo: UserAdditionalInfoEntity): Int

    @Delete
    suspend fun deleteUserAdditionalInfo(userInfo: UserAdditionalInfoEntity): Int

    @Query("SELECT * FROM ${TableNames.USER_ADDITIONAL_INFO} WHERE userId = :userId")
    suspend fun getUserAdditionalInfoById(userId: String): UserAdditionalInfoEntity?

    @Query("SELECT * FROM ${TableNames.USER_ADDITIONAL_INFO} WHERE isActive = 1")
    suspend fun getAllActiveUserAdditionalInfo(): List<UserAdditionalInfoEntity>

    @Query("UPDATE ${TableNames.USER_ADDITIONAL_INFO} SET isActive = :isActive, updatedAt = :updatedAt WHERE userId = :userId")
    suspend fun updateUserStatus(userId: String, isActive: Boolean, updatedAt: Long = System.currentTimeMillis()): Int

    @Query("DELETE FROM ${TableNames.USER_ADDITIONAL_INFO}")
    suspend fun deleteAllUserAdditionalInfo(): Int
} 