package com.velox.jewelvault.data.roomdb.dao

import androidx.room.*
import com.velox.jewelvault.data.roomdb.entity.UsersEntity
import com.velox.jewelvault.data.roomdb.TableNames

@Dao
interface UsersDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UsersEntity): Long
    // Returns new row ID

    @Update
    suspend fun updateUser(user: UsersEntity): Int
    // Returns number of rows updated

    @Delete
    suspend fun deleteUser(user: UsersEntity): Int
    // Returns number of rows deleted

    @Query("SELECT COUNT(*) FROM ${TableNames.USERS}")
    suspend fun getUserCount(): Int

    @Query("SELECT * FROM ${TableNames.USERS} WHERE id = :userId")
    suspend fun getUserById(userId: String): UsersEntity?

    @Query("SELECT * FROM ${TableNames.USERS} WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UsersEntity?

    @Query("SELECT * FROM ${TableNames.USERS} WHERE mobileNo = :mobileNo LIMIT 1")
    suspend fun getUserByMobile(mobileNo: String): UsersEntity?

    @Query("UPDATE ${TableNames.USERS} SET pin = :pin WHERE id = :userId")
    suspend fun updatePin(userId: String, pin: String): Int
    // Returns number of rows updated

    @Query("DELETE FROM ${TableNames.USERS}")
    suspend fun deleteAllUsers(): Int
    
    @Query("SELECT * FROM ${TableNames.USERS}")
    suspend fun getAllUsers(): List<UsersEntity>
}
