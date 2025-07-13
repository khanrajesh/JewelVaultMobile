package com.velox.jewelvault.data.roomdb.dao

import androidx.room.*
import com.velox.jewelvault.data.roomdb.entity.UsersEntity

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

    @Query("SELECT COUNT(*) FROM UsersEntity")
    suspend fun getUserCount(): Int

    @Query("SELECT * FROM UsersEntity WHERE id = :userId")
    suspend fun getUserById(userId: Int): UsersEntity?

    @Query("SELECT * FROM UsersEntity WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UsersEntity?

    @Query("SELECT * FROM UsersEntity WHERE mobileNo = :mobileNo LIMIT 1")
    suspend fun getUserByMobile(mobileNo: String): UsersEntity?

    @Query("UPDATE UsersEntity SET pin = :pin WHERE id = :userId")
    suspend fun updatePin(userId: Int, pin: String): Int
    // Returns number of rows updated

    @Query("DELETE FROM UsersEntity")
    suspend fun deleteAllUsers(): Int
}
